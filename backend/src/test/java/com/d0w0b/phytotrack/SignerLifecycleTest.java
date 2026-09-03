package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class SignerLifecycleTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper ();

  private String login (String username, String password) throws Exception {
    MvcResult result = mockMvc.perform (post ("/api/auth/login")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","password":"%s"}
                """.formatted (username, password)))
        .andExpect (status ().isOk ())
        .andReturn ();
    return objectMapper.readTree (result.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("token").asText ();
  }

  private static String bearer (String token) {
    return "Bearer " + token;
  }

  @Test
  void userAsSigner禁直改與獨立簽名人可直改 () throws Exception {
    String adminToken = login ("admin", "admin123");

    // 取得一個 user as signer（admin 本身的簽名人，有 user_id）
    MvcResult all = mockMvc.perform (get ("/api/ref/identifiers").param ("includeInactive", "true")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode list = objectMapper.readTree (all.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    // 找一個有 userId 的（透過 /me 可確保），先取 admin 的 me
    MvcResult me = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    long linkedId = objectMapper.readTree (me.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 對 user as signer 直改應 409
    mockMvc.perform (put ("/api/admin/ref/identifiers/{id}", linkedId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"新名稱"}
                """))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("USER_LINKED_SIGNER_IMMUTABLE"));

    // 建立一個獨立簽名人（user_id == null）
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"獨立簽名人_%s"}
                """.formatted (System.nanoTime ())))
        .andExpect (status ().isCreated ()).andReturn ();
    long unlinkedId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 對獨立簽名人直改應 200
    mockMvc.perform (put ("/api/admin/ref/identifiers/{id}", unlinkedId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"獨立新名_%s"}
                """.formatted (System.nanoTime ())))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.name").value (org.hamcrest.Matchers.containsString ("獨立新名")));
  }

  @Test
  void 停用後預設過濾與includeInactive可見 () throws Exception {
    String adminToken = login ("admin", "admin123");
    // 建立獨立簽名人
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"待停用_%s"}
                """.formatted (System.nanoTime ())))
        .andExpect (status ().isCreated ()).andReturn ();
    long id = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 停用
    mockMvc.perform (patch ("/api/admin/ref/identifiers/{id}/active", id)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isOk ());

    // 預設 GET 不含
    MvcResult def = mockMvc.perform (get ("/api/ref/identifiers").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode defList = objectMapper.readTree (def.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    boolean contains = false;
    for (JsonNode n : defList) if (n.path ("id").asLong () == id) contains = true;
    assertThat (contains).isFalse ();

    // includeInactive=true 含
    MvcResult all = mockMvc.perform (get ("/api/ref/identifiers").param ("includeInactive", "true")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode allList = objectMapper.readTree (all.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    boolean containsAll = false;
    for (JsonNode n : allList) if (n.path ("id").asLong () == id) containsAll = true;
    assertThat (containsAll).isTrue ();

    // 啟用回來
    mockMvc.perform (patch ("/api/admin/ref/identifiers/{id}/active", id)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":true}
                """))
        .andExpect (status ().isOk ());
  }

  @Test
  void staff自助停用與不可停他人 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String uname = "staff_lc_" + System.nanoTime ();
    String display = "LC_" + System.nanoTime ();
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, display)))
        .andExpect (status ().isCreated ());

    // 升 STAFF
    MvcResult list = mockMvc.perform (get ("/api/admin/users").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode users = objectMapper.readTree (list.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    long uid = 0;
    for (JsonNode u : users) if (uname.equals (u.path ("username").asText ())) uid = u.path ("userId").asLong ();
    mockMvc.perform (patch ("/api/admin/users/{id}/role", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isOk ());

    String staffToken = login (uname, "password123");
    // 取自身簽名人
    MvcResult me = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (staffToken)))
        .andExpect (status ().isOk ()).andReturn ();
    long myId = objectMapper.readTree (me.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // STAFF 已禁用停用：自助停用應 403
    mockMvc.perform (patch ("/api/ref/identifiers/{id}/active", myId)
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isForbidden ());

    // 取得他人簽名人（admin 的）
    MvcResult adminMe = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    long adminId = objectMapper.readTree (adminMe.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // staff 試圖停用他人應 403
    mockMvc.perform (patch ("/api/ref/identifiers/{id}/active", adminId)
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isForbidden ());

    // admin 可停用任意（再啟用 staff 的以便清理）
    mockMvc.perform (patch ("/api/admin/ref/identifiers/{id}/active", myId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":true}
                """))
        .andExpect (status ().isOk ());
  }
}
