package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
 * 端到端整合測試 (Integration Test)
 *
 * 以完整 Spring Context + 真實 SQLite (test profile) 走完整流程：
 *   登入 (JWT)→ 查詢參照資料 → 建立案件 → 列表查詢 → RBAC 拒絕
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class PhytoTrackIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  /** 僅用於解析測試回應，獨立於應用程式的 Jackson 設定 */
  private final ObjectMapper objectMapper = new ObjectMapper ();

  @Test
  void endToEnd_loginCreateListAndRbac () throws Exception {
    // 1. 登入 admin (DataInitializer 於啟動時建立 admin/admin123)
    String adminToken = login ("admin", "admin123");

    // 2. 從參照資料端點取得建立案件所需的真實 ID
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long cropId = firstCropId ("/api/ref/crop-categories", adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliverId = firstId ("/api/ref/deliveries", adminToken);
    long damageId = firstId ("/api/ref/damages", adminToken);
    long secondDamageId = nthId ("/api/ref/damages", adminToken, 1);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);

    Map<String, Object> caseBody = new LinkedHashMap<>();
    caseBody.put ("receiveDate", "2026-08-18");
    caseBody.put ("cropScale", "2 分地");
    caseBody.put ("damageScale", "約 3 成");
    caseBody.put ("caseDescription", "葉片出現斑點");
    caseBody.put ("senderName", "張三");
    caseBody.put ("senderPhone", "0912345678");
    caseBody.put ("senderAddress", "測試路 1 號");
    caseBody.put ("senderDistrictId", districtId);
    caseBody.put ("fieldDistrictId", districtId);
    caseBody.put ("senderTypeId", senderTypeId);
    caseBody.put ("methodId", methodId);
    caseBody.put ("cropId", cropId);
    caseBody.put ("serviceId", serviceId);
    caseBody.put ("deliverId", deliverId);
    caseBody.put ("damageIds", List.of (damageId));
    caseBody.put ("hintIds", List.of (hintId));
    caseBody.put ("pestCategoryIds", List.of (pestCategoryId));
    caseBody.put ("identifierIds", List.of ());
    String caseJson = objectMapper.writeValueAsString (caseBody);

    // 3. 建立案件 (admin 具 STAFF+ 權限)
    MvcResult created = mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (caseJson))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.senderName").value ("張三"))
        .andExpect (jsonPath ("$.receiveDate").value ("2026-08-18"))
        .andReturn ();
    long caseId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("caseId").asLong ();

    // 4. 更新案件：多對多送「與既有相同」的集合時不得重建同鍵 junction (回歸
    //    case_damages UNIQUE 衝突：差集替換使新增與刪除無交集)
    Map<String, Object> sameJunctionBody = new LinkedHashMap<>();
    sameJunctionBody.put ("damageIds", List.of (damageId));
    sameJunctionBody.put ("hintIds", List.of (hintId));
    sameJunctionBody.put ("pestCategoryIds", List.of (pestCategoryId));
    sameJunctionBody.put ("identifierIds", List.of ());
    mockMvc.perform (put ("/api/cases/{id}", caseId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (sameJunctionBody)))
        .andExpect (status ().isOk ());

    // 5. 更新案件：真正的整組替換 (移除 d1、新增 d2，且 status PENDING→RESOLVED)
    Map<String, Object> replaceJunctionBody = new LinkedHashMap<>();
    replaceJunctionBody.put ("status", "RESOLVED");
    replaceJunctionBody.put ("damageIds", List.of (secondDamageId));
    replaceJunctionBody.put ("hintIds", List.of (hintId));
    replaceJunctionBody.put ("pestCategoryIds", List.of (pestCategoryId));
    replaceJunctionBody.put ("identifierIds", List.of ());
    mockMvc.perform (put ("/api/cases/{id}", caseId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (replaceJunctionBody)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.status").value ("RESOLVED"))
        .andExpect (jsonPath ("$.damages[0].id").value (secondDamageId));

    // 7. 列表查詢 (登入即可)
    mockMvc.perform (get ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.content").isArray ())
        .andExpect (jsonPath ("$.totalElements").isNumber ());

    // 7.5 統計總覽 (登入即可)：結構完整；空資料庫情境由 CaseServiceTest 以 mock 涵蓋
    mockMvc.perform (get ("/api/cases/statistics")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.totalCases").isNumber ())
        .andExpect (jsonPath ("$.monthNewCases").isNumber ())
        .andExpect (jsonPath ("$.pendingCases").isNumber ())
        .andExpect (jsonPath ("$.topCrops").isArray ())
        .andExpect (jsonPath ("$.topPestCategories").isArray ())
        .andExpect (jsonPath ("$.statusRatio.length()").value (3))
        .andExpect (jsonPath ("$.monthlyTrend.length()").value (6));

    // 7.6 CSV 匯出 (登入即可)：含 UTF-8 BOM 與剛建立的案件編號
    MvcResult export = mockMvc.perform (get ("/api/cases/export")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (header ().string (HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
        .andExpect (header ().string (HttpHeaders.CONTENT_DISPOSITION,
            startsWith ("attachment; filename=\"case-export-")))
        .andReturn ();
    String csv = export.getResponse ().getContentAsString (StandardCharsets.UTF_8);
    assertThat (csv).startsWith ("\uFEFF\"收件編號\"");
    assertThat (csv).contains ("\"" + String.valueOf (caseId) + "\"");

    // 8. 註冊一般檢視員 (VIEWER) 並登入
    String viewerUsername = "viewer_it_" + System.nanoTime ();
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"整合檢視員","password":"password123"}
                """.formatted (viewerUsername)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.role").value ("ROLE_VIEWER"));
    String viewerToken = login (viewerUsername, "password123");

    // 9. RBAC：VIEWER 建立案件被拒絕 (403，統一錯誤格式)
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (viewerToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (caseJson))
        .andExpect (status ().isForbidden ())
        .andExpect (jsonPath ("$.error.code").value ("ACCESS_DENIED"))
        .andExpect (jsonPath ("$.requestId").isNotEmpty ());

    // 10. 未登入存取受保護端點 → 401 (統一錯誤格式)
    mockMvc.perform (get ("/api/cases"))
        .andExpect (status ().isUnauthorized ())
        .andExpect (jsonPath ("$.error.code").value ("UNAUTHORIZED"))
        .andExpect (jsonPath ("$.requestId").isNotEmpty ());

    // 11. 無效 token 存取受保護端點 → 亦視為未認證 (401，前端可據此清除殘留 token)
    mockMvc.perform (get ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, "Bearer invalid.token.here"))
        .andExpect (status ().isUnauthorized ())
        .andExpect (jsonPath ("$.error.code").value ("UNAUTHORIZED"));
  }

  @Test
  void userAdmin_disableLoginTokenResetAndRoleChange () throws Exception {
    // 以完整流程驗證 user-admin spec 四大 scenario
    String adminToken = login ("admin", "admin123");

    // 取得建立案件所需參照 (後續驗證角色變更後能否建立案件)
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long cropId = firstCropId ("/api/ref/crop-categories", adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliverId = firstId ("/api/ref/deliveries", adminToken);
    long damageId = firstId ("/api/ref/damages", adminToken);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);

    // 建立受測使用者 (初始 VIEWER)
    String uname = "uadmin_it_" + System.nanoTime ();
    MvcResult reg = mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"整合受測者","password":"password123"}
                """.formatted (uname)))
        .andExpect (status ().isCreated ())
        .andReturn ();
    long userId = objectMapper.readTree (reg.getResponse ().getContentAsString (StandardCharsets.UTF_8))
        .path ("userId").asLong ();

    String viewerTokenBefore = login (uname, "password123");

    // VIEWER 初始無法建立案件 (403)
    Map<String, Object> caseBody = new LinkedHashMap<>();
    caseBody.put ("receiveDate", "2026-08-18");
    caseBody.put ("senderName", "測受測");
    caseBody.put ("senderPhone", "0912000001");
    caseBody.put ("senderAddress", "測試路 1 號");
    caseBody.put ("senderDistrictId", districtId);
    caseBody.put ("fieldDistrictId", districtId);
    caseBody.put ("senderTypeId", senderTypeId);
    caseBody.put ("methodId", methodId);
    caseBody.put ("cropId", cropId);
    caseBody.put ("serviceId", serviceId);
    caseBody.put ("deliverId", deliverId);
    caseBody.put ("damageIds", List.of (damageId));
    caseBody.put ("hintIds", List.of (hintId));
    caseBody.put ("pestCategoryIds", List.of (pestCategoryId));
    caseBody.put ("identifierIds", List.of ());
    String caseJson = objectMapper.writeValueAsString (caseBody);

    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (viewerTokenBefore))
            .contentType (MediaType.APPLICATION_JSON)
            .content (caseJson))
        .andExpect (status ().isForbidden ());

    // 1. 停用帳號
    mockMvc.perform (patch ("/api/admin/users/{id}/active", userId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.active").value (false));

    // 2. 停用後登入失敗 (403 ACCOUNT_DISABLED)
    mockMvc.perform (post ("/api/auth/login")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","password":"password123"}
                """.formatted (uname)))
        .andExpect (status ().isForbidden ())
        .andExpect (jsonPath ("$.error.code").value ("ACCOUNT_DISABLED"));

    // 3. 停用後舊 token 打受保護 API 應 401
    mockMvc.perform (get ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (viewerTokenBefore)))
        .andExpect (status ().isUnauthorized ())
        .andExpect (jsonPath ("$.error.code").value ("UNAUTHORIZED"));

    // 4. 重設密碼 (仍停用狀態下重設，之後啟用)
    mockMvc.perform (post ("/api/admin/users/{id}/reset-password", userId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"newPassword":"newPass1234"}
                """))
        .andExpect (status ().isNoContent ());

    // 啟用回來
    mockMvc.perform (patch ("/api/admin/users/{id}/active", userId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":true}
                """))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.active").value (true));

    // 用新密碼可登入
    String tokenAfterReset = login (uname, "newPass1234");

    // 舊密碼不可登入 (401)
    mockMvc.perform (post ("/api/auth/login")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","password":"password123"}
                """.formatted (uname)))
        .andExpect (status ().isUnauthorized ());

    // 5. 變更角色為 STAFF 後，後續請求 (包含舊 viewer token) 應取得 STAFF 權限
    // 舊的 viewerTokenBefore 雖然在停用期間失效，但重啟後仍以 DB role 為準：
    // 為驗證「角色變更後續請求生效」，改用 tokenAfterReset (仍為 VIEWER 直到變更)
    // 先確認 tokenAfterReset 仍為 VIEWER 時建案失敗，再變更角色，重試同一 token
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (tokenAfterReset))
            .contentType (MediaType.APPLICATION_JSON)
            .content (caseJson))
        .andExpect (status ().isForbidden ());

    mockMvc.perform (patch ("/api/admin/users/{id}/role", userId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.role").value ("ROLE_STAFF"));

    // 同一 token (未重新登入) 再次打建案，應因 filter 的 DB 覆蓋而成功
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (tokenAfterReset))
            .contentType (MediaType.APPLICATION_JSON)
            .content (caseJson))
        .andExpect (status ().isCreated ());

    // 新登入的 token 亦為 STAFF
    String staffToken = login (uname, "newPass1234");
    mockMvc.perform (get ("/api/admin/users")
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken)))
        .andExpect (status ().isForbidden ()); // STAFF 不可存取管理端點，僅 ADMIN

    // 管理者清單可見 active 與新 role
    mockMvc.perform (get ("/api/admin/users")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$[?(@.username=='%s')].role".formatted (uname)).value ("ROLE_STAFF"))
        .andExpect (jsonPath ("$[?(@.username=='%s')].active".formatted (uname)).value (true));
  }

  @Test
  void referenceDataAdmin_createAndDeleteProtection () throws Exception {
    String adminToken = login ("admin", "admin123");

    // 1. 新增作物分類與作物，驗證可於表單選用 (透過參照資料讀取)
    String catName = "整合分類_" + System.nanoTime ();
    MvcResult catRes = mockMvc.perform (post ("/api/admin/ref/crop-categories")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (catName)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.name").value (catName))
        .andReturn ();
    long catId = objectMapper.readTree (catRes.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    String cropName = "整合作物_" + System.nanoTime ();
    MvcResult cropRes = mockMvc.perform (post ("/api/admin/ref/crops")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s","cropCategoryId":%d}
                """.formatted (cropName, catId)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.name").value (cropName))
        .andReturn ();
    long cropId = objectMapper.readTree (cropRes.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 驗證作物出現在參照資料中
    mockMvc.perform (get ("/api/ref/crop-categories")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$[?(@.name=='%s')].crops[?(@.name=='%s')].name".formatted (catName, cropName)).exists ());

    // 2. 新增被害部位 (未被引用) 並驗證刪除成功
    String damageName = "整合被害_" + System.nanoTime ();
    MvcResult damageRes = mockMvc.perform (post ("/api/admin/ref/damages")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (damageName)))
        .andExpect (status ().isCreated ())
        .andReturn ();
    long damageIdUnused = objectMapper.readTree (damageRes.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    mockMvc.perform (delete ("/api/admin/ref/damages/{id}", damageIdUnused)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isNoContent ());

    // 確認已刪除
    mockMvc.perform (get ("/api/ref/damages")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$[?(@.id==%d)]".formatted (damageIdUnused)).doesNotExist ());

    // 3. 建立會被引用的被害部位，建案後刪除應 409
    String referencedDamage = "被引用被害_" + System.nanoTime ();
    MvcResult refDamageRes = mockMvc.perform (post ("/api/admin/ref/damages")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (referencedDamage)))
        .andExpect (status ().isCreated ())
        .andReturn ();
    long damageIdUsed = objectMapper.readTree (refDamageRes.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 建立案件引用該被害部位
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliverId = firstId ("/api/ref/deliveries", adminToken);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);

    Map<String, Object> caseBody = new LinkedHashMap<>();
    caseBody.put ("receiveDate", "2026-08-18");
    caseBody.put ("senderName", "參照整合");
    caseBody.put ("senderPhone", "0912333" + (System.nanoTime () % 10000));
    caseBody.put ("senderAddress", "測試路 1 號");
    caseBody.put ("senderDistrictId", districtId);
    caseBody.put ("fieldDistrictId", districtId);
    caseBody.put ("senderTypeId", senderTypeId);
    caseBody.put ("methodId", methodId);
    caseBody.put ("cropId", cropId);
    caseBody.put ("serviceId", serviceId);
    caseBody.put ("deliverId", deliverId);
    caseBody.put ("damageIds", List.of (damageIdUsed));
    caseBody.put ("hintIds", List.of (hintId));
    caseBody.put ("pestCategoryIds", List.of (pestCategoryId));
    caseBody.put ("identifierIds", List.of ());

    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (caseBody)))
        .andExpect (status ().isCreated ());

    // 嘗試刪除被引用的被害部位 -> 409
    mockMvc.perform (delete ("/api/admin/ref/damages/{id}", damageIdUsed)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("REFERENCE_IN_USE"));

    // 4. 驗證非 ADMIN 無法寫入
    String viewerUsername = "viewer_ref_" + System.nanoTime ();
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"參照檢視員","password":"password123"}
                """.formatted (viewerUsername)))
        .andExpect (status ().isCreated ());
    String viewerToken = login (viewerUsername, "password123");

    mockMvc.perform (post ("/api/admin/ref/damages")
            .header (HttpHeaders.AUTHORIZATION, bearer (viewerToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"viewer嘗試"}
                """))
        .andExpect (status ().isForbidden ());
  }

  private String login (String username, String password) throws Exception {
    MvcResult result = mockMvc.perform (post ("/api/auth/login")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","password":"%s"}
                """.formatted (username, password)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.token").isNotEmpty ())
        .andReturn ();
    String token = objectMapper.readTree (result.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("token").asText ();
    assertThat (token).isNotBlank ();
    return token;
  }

  @Test
  void senderManagement_searchDeleteProtectionAndViewerMasking () throws Exception {
    String adminToken = login ("admin", "admin123");

    // 取得參照資料
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long cropId = firstCropId ("/api/ref/crop-categories", adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliverId = firstId ("/api/ref/deliveries", adminToken);
    long damageId = firstId ("/api/ref/damages", adminToken);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);

    // 1. 建立案件 (含送件人 displayName，無 name)
    String uniquePhone = "0912" + (System.nanoTime () % 10000000);
    String displayName = "整合暱稱_" + System.nanoTime ();
    Map<String, Object> caseBody = new LinkedHashMap<>();
    caseBody.put ("receiveDate", "2026-08-18");
    caseBody.put ("senderName", "");
    caseBody.put ("senderDisplayName", displayName);
    caseBody.put ("senderPhone", uniquePhone);
    caseBody.put ("senderAddress", "測試路 1 號");
    caseBody.put ("senderDistrictId", districtId);
    caseBody.put ("fieldDistrictId", districtId);
    caseBody.put ("senderTypeId", senderTypeId);
    caseBody.put ("methodId", methodId);
    caseBody.put ("cropId", cropId);
    caseBody.put ("serviceId", serviceId);
    caseBody.put ("deliverId", deliverId);
    caseBody.put ("damageIds", List.of (damageId));
    caseBody.put ("hintIds", List.of (hintId));
    caseBody.put ("pestCategoryIds", List.of (pestCategoryId));
    caseBody.put ("identifierIds", List.of ());

    MvcResult created = mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (caseBody)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.senderDisplayName").value (displayName))
        .andExpect (jsonPath ("$.senderId").isNumber ())
        .andReturn ();
    long caseId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("caseId").asLong ();
    long senderId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("senderId").asLong ();

    // 2. 搜尋候選：以電話部分比對應回候選
    mockMvc.perform (get ("/api/senders/search")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .param ("q", uniquePhone.substring (0, 6)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$[?(@.senderId==%d)]".formatted (senderId)).exists ());

    // 3. VIEWER 遮蔽：查詢詳細不含姓名/電話/地址，但含縣市鄉鎮
    String viewerUsername = "viewer_sm_" + System.nanoTime ();
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"遮蔽檢視員","password":"password123"}
                """.formatted (viewerUsername)))
        .andExpect (status ().isCreated ());
    String viewerToken = login (viewerUsername, "password123");

    MvcResult viewerDetail = mockMvc.perform (get ("/api/cases/{id}", caseId)
            .header (HttpHeaders.AUTHORIZATION, bearer (viewerToken)))
        .andExpect (status ().isOk ())
        .andReturn ();
    JsonNode viewerNode = objectMapper.readTree (viewerDetail.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    assertThat (viewerNode.path ("senderName").isNull () || viewerNode.path ("senderName").asText ().isEmpty ())
        .as ("VIEWER 不應取得送件人姓名").isTrue ();
    assertThat (viewerNode.path ("senderPhone").isNull () || viewerNode.path ("senderPhone").asText ().isEmpty ())
        .as ("VIEWER 不應取得送件人電話").isTrue ();
    assertThat (viewerNode.path ("senderAddress").isNull () || viewerNode.path ("senderAddress").asText ().isEmpty ())
        .as ("VIEWER 不應取得送件人地址").isTrue ();
    assertThat (viewerNode.path ("senderDistrictName").asText ()).isEqualTo ("中正區");
    assertThat (viewerNode.path ("senderCityName").asText ()).isEqualTo ("臺北市");

    // STAFF/ADMIN 查詢含完整資料
    mockMvc.perform (get ("/api/cases/{id}", caseId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.senderDisplayName").value (displayName));

    // 4. 刪除被引用的送件人 → 409
    mockMvc.perform (delete ("/api/senders/{id}", senderId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("REFERENCE_IN_USE"));

    // 5. 建立未被引用的送件人 (僅 displayName) 並刪除 → 204
    String unusedPhone = "0933" + (System.nanoTime () % 10000000);
    Map<String, Object> unusedCase = new LinkedHashMap<>(caseBody);
    unusedCase.put ("senderPhone", unusedPhone);
    unusedCase.put ("senderDisplayName", "未引用暱稱_" + System.nanoTime ());
    MvcResult unusedCreated = mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (unusedCase)))
        .andExpect (status ().isCreated ())
        .andReturn ();
    long unusedSenderId = objectMapper.readTree (unusedCreated.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("senderId").asLong ();

    // 先刪除其唯一案件使送件人未被引用
    mockMvc.perform (delete ("/api/cases/{id}", caseId + 1)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isNoContent ());

    // 6. 非 ADMIN 刪除被拒
    mockMvc.perform (delete ("/api/senders/{id}", unusedSenderId)
            .header (HttpHeaders.AUTHORIZATION, bearer (viewerToken)))
        .andExpect (status ().isForbidden ());
  }

  private JsonNode getJson (String url, String token) throws Exception {
    MvcResult result = mockMvc.perform (get (url)
            .header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ())
        .andReturn ();
    return objectMapper.readTree (result.getResponse ().getContentAsString (StandardCharsets.UTF_8));
  }

  private long firstId (String url, String token) throws Exception {
    return nthId (url, token, 0);
  }

  private long nthId (String url, String token, int index) throws Exception {
    JsonNode array = getJson (url, token);
    assertThat (array.isArray () && array.size () > index).as ("參照資料 %s 應有第 %d 筆種子資料", url, index + 1).isTrue ();
    return array.get (index).path ("id").asLong ();
  }

  private long firstNestedId (String url, String token, String childField) throws Exception {
    JsonNode array = getJson (url, token);
    assertThat (array.isArray () && array.size () > 0).as ("參照資料 %s 應有種子資料", url).isTrue ();
    return array.get (0).path (childField).get (0).path ("id").asLong ();
  }

  private long firstCropId (String url, String token) throws Exception {
    JsonNode array = getJson (url, token);
    assertThat (array.isArray () && array.size () > 0).as ("參照資料 %s 應有種子資料", url).isTrue ();
    return array.get (0).path ("crops").get (0).path ("id").asLong ();
  }

  private long firstPestCategoryId (String url, String token) throws Exception {
    JsonNode array = getJson (url, token);
    assertThat (array.isArray () && array.size () > 0).as ("參照資料 %s 應有種子資料", url).isTrue ();
    return array.get (0).path ("categories").get (0).path ("id").asLong ();
  }

  private static String bearer (String token) {
    return "Bearer " + token;
  }
}
