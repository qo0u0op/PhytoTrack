package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 簽名人解綁與恢復測試 (signer-unlink-relink)
 *
 * 降權／停用解綁留歷史（former_user_id），升權／啟用憑歷史恢復原筆；
 * 他人同名仍走撞名流程（既有 SIGNER_NAME_CONFLICT 不變）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class SignerUnlinkRelinkTest {

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

  private long userIdOf (String adminToken, String username) throws Exception {
    MvcResult list = mockMvc.perform (get ("/api/admin/users").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    for (JsonNode u : objectMapper.readTree (list.getResponse ().getContentAsString (StandardCharsets.UTF_8))) {
      if (username.equals (u.path ("username").asText ())) return u.path ("userId").asLong ();
    }
    throw new IllegalStateException ("找不到使用者：" + username);
  }

  private List<JsonNode> identifiers (String adminToken, boolean includeInactive) throws Exception {
    MvcResult r = mockMvc.perform (get ("/api/ref/identifiers")
            .param ("includeInactive", String.valueOf (includeInactive))
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    List<JsonNode> out = new ArrayList<>();
    objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8)).forEach (out::add);
    return out;
  }

  private List<JsonNode> named (List<JsonNode> all, String name) {
    return all.stream ().filter (n -> name.equals (n.path ("name").asText ())).toList ();
  }

  /** 註冊唯一使用者並提權為 STAFF（自動建簽名人），回傳 userId */
  private long registerAndPromote (String adminToken, String tag, String display) throws Exception {
    String uname = "relink_%s_%s".formatted (tag, System.nanoTime ());
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, display)))
        .andExpect (status ().isCreated ());
    long uid = userIdOf (adminToken, uname);
    mockMvc.perform (patch ("/api/admin/users/{id}/role", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isOk ());
    return uid;
  }

  @Test
  void 降權解綁保留可見且升權恢復原筆 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String display = "解綁_%s".formatted (System.nanoTime ());
    long uid = registerAndPromote (adminToken, "demote", display);

    long signerId = named (identifiers (adminToken, true), display).get (0).path ("id").asLong ();

    // 降權至 VIEWER：解綁但維持 active，id 不變，候選仍可見
    mockMvc.perform (patch ("/api/admin/users/{id}/role", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_VIEWER"}
                """))
        .andExpect (status ().isOk ());
    JsonNode unlinked = named (identifiers (adminToken, true), display).get (0);
    assertThat (unlinked.path ("id").asLong ()).isEqualTo (signerId);
    assertThat (unlinked.path ("userId").isNull ()).isTrue ();
    assertThat (unlinked.path ("active").asBoolean ()).isTrue ();
    assertThat (named (identifiers (adminToken, false), display)).hasSize (1);

    // 重新升權：原筆恢復，不新增第二筆
    mockMvc.perform (patch ("/api/admin/users/{id}/role", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isOk ());
    List<JsonNode> after = named (identifiers (adminToken, true), display);
    assertThat (after).hasSize (1);
    assertThat (after.get (0).path ("id").asLong ()).isEqualTo (signerId);
    assertThat (after.get (0).path ("userId").asLong ()).isEqualTo (uid);
    assertThat (after.get (0).path ("active").asBoolean ()).isTrue ();
  }

  @Test
  void 停用解綁隱藏且啟用恢復 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String display = "停用_%s".formatted (System.nanoTime ());
    long uid = registerAndPromote (adminToken, "deact", display);

    long signerId = named (identifiers (adminToken, true), display).get (0).path ("id").asLong ();

    // 停用：解綁＋停用，候選隱藏、管理頁可見
    mockMvc.perform (patch ("/api/admin/users/{id}/active", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isOk ());
    JsonNode off = named (identifiers (adminToken, true), display).get (0);
    assertThat (off.path ("id").asLong ()).isEqualTo (signerId);
    assertThat (off.path ("userId").isNull ()).isTrue ();
    assertThat (off.path ("active").asBoolean ()).isFalse ();
    assertThat (named (identifiers (adminToken, false), display)).isEmpty ();

    // 重新啟用：原筆恢復可見
    mockMvc.perform (patch ("/api/admin/users/{id}/active", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":true}
                """))
        .andExpect (status ().isOk ());
    List<JsonNode> after = named (identifiers (adminToken, true), display);
    assertThat (after).hasSize (1);
    assertThat (after.get (0).path ("id").asLong ()).isEqualTo (signerId);
    assertThat (after.get (0).path ("userId").asLong ()).isEqualTo (uid);
    assertThat (after.get (0).path ("active").asBoolean ()).isTrue ();
    assertThat (named (identifiers (adminToken, false), display)).hasSize (1);
  }

  @Test
  void 他人同名仍撞名不誤恢復 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String shared = "他名_%s".formatted (System.nanoTime ());
    // 先建他人的非使用者簽名人
    mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (shared)))
        .andExpect (status ().isCreated ());

    // 從未擁有簽名人的新使用者提權：仍回 SIGNER_NAME_CONFLICT（不誤恢復他人名下）
    String uname = "stranger_%s".formatted (System.nanoTime ());
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, shared)))
        .andExpect (status ().isCreated ());
    long uid = userIdOf (adminToken, uname);
    mockMvc.perform (patch ("/api/admin/users/{id}/role", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("SIGNER_NAME_CONFLICT"));
  }
}
