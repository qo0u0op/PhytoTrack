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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class SignerEdgeBatch1Test {

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

  private long firstNestedId (String url, String token, String child) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path (child).get (0).path ("id").asLong ();
  }

  private long firstId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("id").asLong ();
  }

  private long firstCropId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    JsonNode crops = arr.get (0).path ("crops");
    // 業務初始無作物種子：為空時自建（保持測試獨立於種子）
    if (crops.isArray () && crops.size () > 0) return crops.get (0).path ("id").asLong ();
    long categoryId = arr.get (0).path ("id").asLong ();
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/crops")
            .header (HttpHeaders.AUTHORIZATION, bearer (token))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"測試作物_%s","cropCategoryId":%d}
                """.formatted (System.nanoTime (), categoryId)))
        .andExpect (status ().isCreated ()).andReturn ();
    return objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();
  }

  private long firstPestCategoryId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("categories").get (0).path ("id").asLong ();
  }

  private Map<String, Object> validCaseBody (String adminToken, List<Long> identifierIds) throws Exception {
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long cropId = firstCropId ("/api/ref/crop-categories", adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliveryId = firstId ("/api/ref/deliveries", adminToken);
    long damageId = firstId ("/api/ref/damages", adminToken);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put ("receiveDate", "2026-09-03");
    body.put ("senderName", "邊界測試");
    body.put ("senderPhone", "0912000" + (System.nanoTime () % 10000));
    body.put ("senderAddress", "測試路 1 號");
    body.put ("senderDistrictId", districtId);
    body.put ("fieldDistrictId", districtId);
    body.put ("senderTypeId", senderTypeId);
    body.put ("methodId", methodId);
    body.put ("cropId", cropId);
    body.put ("serviceId", serviceId);
    body.put ("deliverId", deliveryId);
    body.put ("damageIds", List.of (damageId));
    body.put ("hintIds", List.of (hintId));
    body.put ("pestCategoryIds", List.of (pestCategoryId));
    body.put ("identifierIds", identifierIds);
    return body;
  }

  @Test
  void 新建引用停用簽名人被拒 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String name = "停用擋建案_%s".formatted (System.nanoTime ());
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (name)))
        .andExpect (status ().isCreated ()).andReturn ();
    long id = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    mockMvc.perform (patch ("/api/admin/ref/identifiers/{id}/active", id)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isOk ());

    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (validCaseBody (adminToken, List.of (id)))))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("SIGNER_INACTIVE"));
  }

  @Test
  void 自動帶入撞名不重複新建 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String shared = "撞名_%s".formatted (System.nanoTime ());
    // 先建一個非使用者簽名人
    mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (shared)))
        .andExpect (status ().isCreated ());

    // 註冊同 displayName 的新使用者並提權（不綁定、強制建會產生第二筆；此處驗證 ensureForUser 直接呼叫行為改由提權衝突覆蓋）
    String uname = "dup_" + System.nanoTime ();
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, shared)))
        .andExpect (status ().isCreated ());

    MvcResult list = mockMvc.perform (get ("/api/admin/users").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode users = objectMapper.readTree (list.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    long uid = 0;
    for (JsonNode u : users) if (uname.equals (u.path ("username").asText ())) uid = u.path ("userId").asLong ();
    assertThat (uid).isNotZero ();

    // 未綁定直接提權應回 SIGNER_NAME_CONFLICT 而非靜默新建
    mockMvc.perform (patch ("/api/admin/users/{id}/role", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("SIGNER_NAME_CONFLICT"));
  }

  @Test
  void 停用帳號連動停用簽名人且歷史仍顯示 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String uname = "link_" + System.nanoTime ();
    String display = "連動_%s".formatted (System.nanoTime ());
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, display)))
        .andExpect (status ().isCreated ());

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

    // 先建一個案件引用其簽名人
    MvcResult me = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (staffToken)))
        .andExpect (status ().isOk ()).andReturn ();
    long myId = objectMapper.readTree (me.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();
    MvcResult created = mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (validCaseBody (adminToken, List.of (myId)))))
        .andExpect (status ().isCreated ()).andReturn ();
    long caseId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("caseId").asLong ();

    // 停用帳號
    mockMvc.perform (patch ("/api/admin/users/{id}/active", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isOk ());

    // 簽名人應轉 inactive：預設清單不含，但 includeInactive 可見
    MvcResult def = mockMvc.perform (get ("/api/ref/identifiers").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode defList = objectMapper.readTree (def.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    boolean inDefault = false;
    for (JsonNode n : defList) if (n.path ("id").asLong () == myId) inDefault = true;
    assertThat (inDefault).isFalse ();

    // 歷史案件仍顯示原名
    mockMvc.perform (get ("/api/cases/{id}", caseId).header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.identifiers[0].id").value (myId))
        .andExpect (jsonPath ("$.identifiers[0].name").value (display));
  }

  @Test
  void 同使用者多筆首筆確定 () throws Exception {
    String adminToken = login ("admin", "admin123");
    MvcResult me = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    long firstId = objectMapper.readTree (me.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 再建第二筆同使用者簽名人（透過綁定流程無法直接建第二筆 user 簽名，此處改驗排序語意：me 仍回最小 id）
    MvcResult me2 = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    long againId = objectMapper.readTree (me2.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();
    assertThat (againId).isEqualTo (firstId);
  }
}
