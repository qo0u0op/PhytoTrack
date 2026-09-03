package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/**
 * 案件內聯參照原子建立測試 (case-atomic-ref-creation)
 *
 * 以完整 Spring Context + 真實 SQLite (test profile) 驗證：
 * 提交才可見、內聯與顯式併用、交易失敗全回滾、編輯內聯原子。
 * 名稱皆以 nanoTime 保證唯一，避免測試共用資料庫互相干擾。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class CaseAtomicRefTest {

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
    return arr.get (0).path ("crops").get (0).path ("id").asLong ();
  }

  private long firstPestCategoryId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("categories").get (0).path ("id").asLong ();
  }

  private Map<String, Object> validCaseBody (String adminToken) throws Exception {
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
    body.put ("senderName", "原子測試");
    body.put ("senderPhone", "0912000" + (System.nanoTime () % 100000));
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
    body.put ("identifierIds", List.of ());
    return body;
  }

  private boolean cropVisible (String token, String name) throws Exception {
    MvcResult r = mockMvc.perform (get ("/api/ref/crop-categories").header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    for (JsonNode cat : arr) {
      for (JsonNode crop : cat.path ("crops")) {
        if (name.equals (crop.path ("name").asText ())) return true;
      }
    }
    return false;
  }

  private boolean identifierVisible (String token, String name) throws Exception {
    MvcResult r = mockMvc.perform (get ("/api/ref/identifiers").header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    for (JsonNode idf : arr) {
      if (name.equals (idf.path ("name").asText ())) return true;
    }
    return false;
  }

  @Test
  void 內聯作物隨案件一併提交才可見 () throws Exception {
    String adminToken = login ("admin", "admin123");
    long categoryId = firstId ("/api/ref/crop-categories", adminToken);
    String name = "內聯作物_%s".formatted (System.nanoTime ());
    assertThat (cropVisible (adminToken, name)).isFalse ();

    Map<String, Object> body = validCaseBody (adminToken);
    body.remove ("cropId");
    body.put ("inlineCrop", Map.of ("name", name, "cropCategoryId", categoryId));
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (body)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.cropName").value (name));

    assertThat (cropVisible (adminToken, name)).isTrue ();
  }

  @Test
  void 內聯簽名人原子建立且同名復用 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String name = "內聯簽名_%s".formatted (System.nanoTime ());

    // 先經管理頁建立同名簽名人，記下 id
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (name)))
        .andExpect (status ().isCreated ()).andReturn ();
    long existingId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 案件內聯同名：應復用既有 id，不新增重複
    Map<String, Object> body = validCaseBody (adminToken);
    body.put ("inlineIdentifiers", List.of (Map.of ("name", name)));
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (body)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.identifiers[?(@.id == %d)]".formatted (existingId)).isArray ());
  }

  @Test
  void 內聯與顯式併用 () throws Exception {
    String adminToken = login ("admin", "admin123");
    long categoryId = firstId ("/api/ref/crop-categories", adminToken);
    String cropName = "覆蓋作物_%s".formatted (System.nanoTime ());
    String signerName = "併用簽名_%s".formatted (System.nanoTime ());

    Map<String, Object> body = validCaseBody (adminToken);
    // 顯式 cropId 與 inlineCrop 並存：inline 覆蓋
    body.put ("inlineCrop", Map.of ("name", cropName, "cropCategoryId", categoryId));
    body.put ("inlineIdentifiers", List.of (Map.of ("name", signerName)));
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (body)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.cropName").value (cropName));

    assertThat (identifierVisible (adminToken, signerName)).isTrue ();
  }

  @Test
  void 交易失敗全回滾 () throws Exception {
    String adminToken = login ("admin", "admin123");
    long categoryId = firstId ("/api/ref/crop-categories", adminToken);
    String name = "回滾作物_%s".formatted (System.nanoTime ());

    // 缺必填 receiveDate → 400，內聯作物亦不得落庫
    Map<String, Object> body = validCaseBody (adminToken);
    body.remove ("receiveDate");
    body.remove ("cropId");
    body.put ("inlineCrop", Map.of ("name", name, "cropCategoryId", categoryId));
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (body)))
        .andExpect (status ().isBadRequest ());

    assertThat (cropVisible (adminToken, name)).isFalse ();
  }

  @Test
  void 編輯時內聯新增亦原子 () throws Exception {
    String adminToken = login ("admin", "admin123");
    Map<String, Object> body = validCaseBody (adminToken);
    MvcResult created = mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (body)))
        .andExpect (status ().isCreated ()).andReturn ();
    long caseId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("caseId").asLong ();

    String signerName = "編輯簽名_%s".formatted (System.nanoTime ());
    Map<String, Object> update = new LinkedHashMap<>();
    update.put ("inlineIdentifiers", List.of (Map.of ("name", signerName)));
    mockMvc.perform (put ("/api/cases/{id}", caseId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (update)))
        .andExpect (status ().isOk ());

    assertThat (identifierVisible (adminToken, signerName)).isTrue ();
  }
}
