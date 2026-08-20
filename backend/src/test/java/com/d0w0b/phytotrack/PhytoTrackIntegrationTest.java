package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 端到端整合測試（Integration Test）
 *
 * 以完整 Spring Context + 真實 SQLite（test profile）走完整流程：
 *   登入（JWT）→ 查詢參照資料 → 建立案件 → 列表查詢 → RBAC 拒絕
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhytoTrackIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  /** 僅用於解析測試回應，獨立於應用程式的 Jackson 設定 */
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void endToEnd_loginCreateListAndRbac() throws Exception {
    // 1. 登入 admin（DataInitializer 於啟動時建立 admin/admin123）
    String adminToken = login("admin", "admin123");

    // 2. 從參照資料端點取得建立案件所需的真實 ID
    long districtId = firstNestedId("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId("/api/ref/sender-types", adminToken);
    long methodId = firstId("/api/ref/methods", adminToken);
    long cropId = firstCropId("/api/ref/crop-categories", adminToken);
    long serviceId = firstId("/api/ref/services", adminToken);
    long deliverId = firstId("/api/ref/deliveries", adminToken);
    long damageId = firstId("/api/ref/damages", adminToken);
    long secondDamageId = nthId("/api/ref/damages", adminToken, 1);
    long hintId = firstId("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId("/api/ref/pest-types", adminToken);

    Map<String, Object> caseBody = new LinkedHashMap<>();
    caseBody.put("receiveDate", "2026-08-18");
    caseBody.put("cropScale", "2 分地");
    caseBody.put("damageScale", "約 3 成");
    caseBody.put("pestDescription", "葉片出現斑點");
    caseBody.put("senderName", "張三");
    caseBody.put("senderPhone", "0912345678");
    caseBody.put("senderAddress", "測試路 1 號");
    caseBody.put("senderDistrictId", districtId);
    caseBody.put("senderTypeId", senderTypeId);
    caseBody.put("methodId", methodId);
    caseBody.put("cropId", cropId);
    caseBody.put("serviceId", serviceId);
    caseBody.put("deliverId", deliverId);
    caseBody.put("damageIds", List.of(damageId));
    caseBody.put("hintIds", List.of(hintId));
    caseBody.put("pestCategoryIds", List.of(pestCategoryId));
    caseBody.put("identifierIds", List.of());
    String caseJson = objectMapper.writeValueAsString(caseBody);

    // 3. 建立案件（admin 具 STAFF+ 權限）
    MvcResult created = mockMvc.perform(post("/api/cases")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(caseJson))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.senderName").value("張三"))
        .andExpect(jsonPath("$.receiveDate").value("2026-08-18"))
        .andReturn();
    long caseId = objectMapper.readTree(
        created.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("caseId").asLong();

    // 4. 更新案件：多對多送「與既有相同」的集合時不得重建同鍵 junction（回歸
    //    case_damages UNIQUE 衝突：差集替換使新增與刪除無交集）
    Map<String, Object> sameJunctionBody = new LinkedHashMap<>();
    sameJunctionBody.put("damageIds", List.of(damageId));
    sameJunctionBody.put("hintIds", List.of(hintId));
    sameJunctionBody.put("pestCategoryIds", List.of(pestCategoryId));
    sameJunctionBody.put("identifierIds", List.of());
    mockMvc.perform(put("/api/cases/{id}", caseId)
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(sameJunctionBody)))
        .andExpect(status().isOk());

    // 5. 更新案件：真正的整組替換（移除 d1、新增 d2，且 status PENDING→RESOLVED）
    Map<String, Object> replaceJunctionBody = new LinkedHashMap<>();
    replaceJunctionBody.put("status", "RESOLVED");
    replaceJunctionBody.put("damageIds", List.of(secondDamageId));
    replaceJunctionBody.put("hintIds", List.of(hintId));
    replaceJunctionBody.put("pestCategoryIds", List.of(pestCategoryId));
    replaceJunctionBody.put("identifierIds", List.of());
    mockMvc.perform(put("/api/cases/{id}", caseId)
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(replaceJunctionBody)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RESOLVED"))
        .andExpect(jsonPath("$.damages[0].id").value(secondDamageId));

    // 7. 列表查詢（登入即可）
    mockMvc.perform(get("/api/cases")
            .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").isNumber());

    // 8. 註冊一般檢視員（VIEWER）並登入
    String viewerUsername = "viewer_it_" + System.nanoTime();
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","displayName":"整合檢視員","password":"password123"}
                """.formatted(viewerUsername)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.role").value("ROLE_VIEWER"));
    String viewerToken = login(viewerUsername, "password123");

    // 9. RBAC：VIEWER 建立案件被拒絕（403，統一錯誤格式）
    mockMvc.perform(post("/api/cases")
            .header(HttpHeaders.AUTHORIZATION, bearer(viewerToken))
            .contentType(MediaType.APPLICATION_JSON)
            .content(caseJson))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());

    // 10. 未登入存取受保護端點被拒絕
    mockMvc.perform(get("/api/cases"))
        .andExpect(status().isForbidden());
  }

  private String login(String username, String password) throws Exception {
    MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"%s","password":"%s"}
                """.formatted(username, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andReturn();
    String token = objectMapper.readTree(
        result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("token").asText();
    assertThat(token).isNotBlank();
    return token;
  }

  private JsonNode getJson(String url, String token) throws Exception {
    MvcResult result = mockMvc.perform(get(url)
            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(
        result.getResponse().getContentAsString(StandardCharsets.UTF_8));
  }

  private long firstId(String url, String token) throws Exception {
    return nthId(url, token, 0);
  }

  private long nthId(String url, String token, int index) throws Exception {
    JsonNode array = getJson(url, token);
    assertThat(array.isArray() && array.size() > index).as(
        "參照資料 %s 應有第 %d 筆種子資料", url, index + 1).isTrue();
    return array.get(index).path("id").asLong();
  }

  private long firstNestedId(String url, String token, String childField) throws Exception {
    JsonNode array = getJson(url, token);
    assertThat(array.isArray() && array.size() > 0).as("參照資料 %s 應有種子資料", url).isTrue();
    return array.get(0).path(childField).get(0).path("id").asLong();
  }

  private long firstCropId(String url, String token) throws Exception {
    JsonNode array = getJson(url, token);
    assertThat(array.isArray() && array.size() > 0).as("參照資料 %s 應有種子資料", url).isTrue();
    return array.get(0).path("crops").get(0).path("id").asLong();
  }

  private long firstPestCategoryId(String url, String token) throws Exception {
    JsonNode array = getJson(url, token);
    assertThat(array.isArray() && array.size() > 0).as("參照資料 %s 應有種子資料", url).isTrue();
    return array.get(0).path("categories").get(0).path("id").asLong();
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
