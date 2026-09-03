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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class CaseSignerAutoFillTest {

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

  private long firstId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    assertThat (arr.isArray () && arr.size () > 0).isTrue ();
    return arr.get (0).path ("id").asLong ();
  }

  private long firstNestedId (String url, String token, String childField) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path (childField).get (0).path ("id").asLong ();
  }

  private long firstCropId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("crops").get (0).path ("id").asLong ();
  }

  private long firstPestCategoryId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("categories").get (0).path ("id").asLong ();
  }

  private static String bearer (String token) {
    return "Bearer " + token;
  }

  @Test
  void 建案空清單自動帶入且有值不覆蓋 () throws Exception {
    String adminToken = login ("admin", "admin123");

    // 建立新 STAFF 以測試「無簽名人即時建立」
    String uname = "signer_" + System.nanoTime ();
    String display = "簽名人A" + System.nanoTime ();
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, display)))
        .andExpect (status ().isCreated ());

    MvcResult reg = mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname + "_dup", display)))
        .andExpect (status ().isCreated ()).andReturn ();
    // 實際使用第一個使用者進行測試
    String targetUname = uname;
    // 取得使用者 id 需透過 list
    MvcResult list = mockMvc.perform (get ("/api/admin/users").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode users = objectMapper.readTree (list.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    long targetUserId = 0;
    for (JsonNode u : users) {
      if (targetUname.equals (u.path ("username").asText ())) {
        targetUserId = u.path ("userId").asLong ();
        break;
      }
    }
    assertThat (targetUserId).isNotZero ();

    // 升為 STAFF → 應自動建立簽名人
    mockMvc.perform (patch ("/api/admin/users/{id}/role", targetUserId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isOk ());

    String staffToken = login (targetUname, "password123");

    // GET /ref/identifiers/me 應回簽名人且名稱等於 displayName
    MvcResult me = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (staffToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.name").value (display))
        .andReturn ();
    long myIdentifierId = objectMapper.readTree (me.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();
    assertThat (myIdentifierId).isNotZero ();

    // 準備建立案件所需參照
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long cropId = firstCropId ("/api/ref/crop-categories", adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliveryId = firstId ("/api/ref/deliveries", adminToken);
    long damageId = firstId ("/api/ref/damages", adminToken);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);

    Map<String, Object> caseBodyEmpty = new LinkedHashMap<>();
    caseBodyEmpty.put ("receiveDate", "2026-09-03");
    caseBodyEmpty.put ("senderName", "測自動帶入");
    caseBodyEmpty.put ("senderPhone", "0912000" + (System.nanoTime () % 10000));
    caseBodyEmpty.put ("senderAddress", "測試路 1 號");
    caseBodyEmpty.put ("senderDistrictId", districtId);
    caseBodyEmpty.put ("fieldDistrictId", districtId);
    caseBodyEmpty.put ("senderTypeId", senderTypeId);
    caseBodyEmpty.put ("methodId", methodId);
    caseBodyEmpty.put ("cropId", cropId);
    caseBodyEmpty.put ("serviceId", serviceId);
    caseBodyEmpty.put ("deliverId", deliveryId);
    caseBodyEmpty.put ("damageIds", List.of (damageId));
    caseBodyEmpty.put ("hintIds", List.of (hintId));
    caseBodyEmpty.put ("pestCategoryIds", List.of (pestCategoryId));
    caseBodyEmpty.put ("identifierIds", List.of ());

    MvcResult created = mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (caseBodyEmpty)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.identifiers[0].id").value (myIdentifierId))
        .andExpect (jsonPath ("$.identifiers[0].name").value (display))
        .andReturn ();
    long caseId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("caseId").asLong ();

    // 已選清單不覆蓋：用 admin 的簽名人之一
    MvcResult idList = mockMvc.perform (get ("/api/ref/identifiers").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode ids = objectMapper.readTree (idList.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    long otherId = ids.get (0).path ("id").asLong ();
    if (otherId == myIdentifierId && ids.size () > 1) otherId = ids.get (1).path ("id").asLong ();
    String otherName = null;
    for (JsonNode n : ids) if (n.path ("id").asLong () == otherId) otherName = n.path ("name").asText ();

    Map<String, Object> caseBodyWith = new LinkedHashMap<>(caseBodyEmpty);
    caseBodyWith.put ("identifierIds", List.of (otherId));
    caseBodyWith.put ("senderPhone", "0912001" + (System.nanoTime () % 10000));
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (caseBodyWith)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.identifiers[0].id").value (otherId))
        .andExpect (jsonPath ("$.identifiers.length()").value (1));

    // 更新空清單自動帶入：將第一個案件簽名人置空，更新為空應帶回 myIdentifier
    mockMvc.perform (put ("/api/cases/{id}", caseId)
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"identifierIds":[]}
                """))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.identifiers[0].id").value (myIdentifierId));

    // 更新 null 保留原值：不傳 identifierIds 應保留
    mockMvc.perform (put ("/api/cases/{id}", caseId)
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"cropScale":"2 分地"}
                """))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.identifiers[0].id").value (myIdentifierId));
  }

  @Test
  void 修改顯示名稱同步更名簽名人 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String uname = "signer2_" + System.nanoTime ();
    String display = "簽名人B" + System.nanoTime ();
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
    String token = login (uname, "password123");
    String newDisplay = display + "_新";
    mockMvc.perform (post ("/api/auth/login").contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","password":"password123"}
                """.formatted (uname)))
        .andExpect (status ().isOk ());
    // 透過 account/profile 改名
    mockMvc.perform (org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put ("/api/account/profile")
            .header (HttpHeaders.AUTHORIZATION, bearer (token))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"displayName":"%s","email":null}
                """.formatted (newDisplay)))
        .andExpect (status ().isOk ());
    // 驗證簽名人已更名
    mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.name").value (newDisplay));
  }
}
