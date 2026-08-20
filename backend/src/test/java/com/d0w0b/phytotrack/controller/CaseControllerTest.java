package com.d0w0b.phytotrack.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.test.context.TestSecurityContextHolderStrategyAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.d0w0b.phytotrack.config.SecurityConfig;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseFilter;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseSummaryResponse;
import com.d0w0b.phytotrack.dto.StatisticsDtos.CaseStatisticsResponse;
import com.d0w0b.phytotrack.dto.StatisticsDtos.CountName;
import com.d0w0b.phytotrack.dto.StatisticsDtos.MonthCount;
import com.d0w0b.phytotrack.dto.StatisticsDtos.StatusCount;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.security.JwtAuthenticationFilter;
import com.d0w0b.phytotrack.service.CaseService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 案件控制器（CaseController）Web 層測試
 *
 * 驗證 RBAC（RBAC）授權規則與 Bean Validation：
 *   - 列表 / 詳細：登入即可（VIEWER / STAFF / ADMIN）
 *   - 建立 / 更新：僅 STAFF / ADMIN
 *   - 刪除：僅 ADMIN
 */
@WebMvcTest(CaseController.class)
@Import({SecurityConfig.class, CaseControllerTest.TestSecurityStrategy.class})
class CaseControllerTest {

  /** Boot 4 web slice 不會自動註冊 @WithMockUser 所需的策略 bean，此處補上 */
  @TestConfiguration(proxyBeanMethods = false)
  static class TestSecurityStrategy {

    @Bean
    SecurityContextHolderStrategy securityContextHolderStrategy() {
      return new TestSecurityContextHolderStrategyAdapter();
    }
  }

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @MockitoBean
  private CaseService caseService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() throws Exception {
    // springSecurity()：套用測試版 SecurityContextRepository，讓 @WithMockUser 生效
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    // JWT 解析屬無狀態細節，此處模擬其「直接放行」；授權規則由 @WithMockUser + @PreAuthorize 驗證
    doAnswer(invocation -> {
      FilterChain chain = invocation.getArgument(2, FilterChain.class);
      chain.doFilter(invocation.getArgument(0, HttpServletRequest.class),
          invocation.getArgument(1, HttpServletResponse.class));
      return null;
    }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
  }

  private static final String VALID_CASE_JSON = """
      {
        "receiveDate": "2026-08-18",
        "senderName": "張三",
        "senderPhone": "0912345678",
        "senderAddress": "測試路 1 號",
        "senderDistrictId": 1,
        "senderTypeId": 1,
        "methodId": 1,
        "cropId": 1,
        "serviceId": 1,
        "deliverId": 1,
        "damageIds": [],
        "hintIds": [],
        "pestCategoryIds": [],
        "identifierIds": []
      }
      """;

  private static CaseResponse sampleResponse() {
    return new CaseResponse(
        1L, LocalDate.of(2026, 8, 18), "2 分地", "約 3 成",
        "葉片出現斑點", null, "PENDING",
        LocalDateTime.now(), LocalDateTime.now(),
        "張三", "0912345678", "測試路 1 號", 1L, "霧峰區", 1L,
        "水稻", "露天", "診斷", "送件",
        "管理員", List.of(), List.of(), List.of(), List.of());
  }

  @Test
  void list_shouldBeProtected() throws Exception {
    // 未登入：由 SecurityFilterChain 拒絕（401，統一錯誤格式）
    mockMvc.perform(get("/api/cases"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "VIEWER")
  void list_shouldReturnPageForAnyAuthenticatedUser() throws Exception {
    when(caseService.list(any(), any())).thenReturn(new PageImpl<>(
        List.of(new CaseSummaryResponse(1L, LocalDate.of(2026, 8, 18), "水稻", "張三", "診斷",
            "PENDING", LocalDateTime.now()))));

    mockMvc.perform(get("/api/cases"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].senderName").value("張三"));
  }

  @Test
  void statistics_shouldBeProtected() throws Exception {
    // 未登入：由 SecurityFilterChain 拒絕（401，統一錯誤格式）
    mockMvc.perform(get("/api/cases/statistics"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  void export_shouldBeProtected() throws Exception {
    // 未登入：由 SecurityFilterChain 拒絕（401，統一錯誤格式）
    mockMvc.perform(get("/api/cases/export"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "VIEWER")
  void export_shouldReturnCsvWithDownloadHeaders() throws Exception {
    when(caseService.exportCsv(any())).thenReturn("\uFEFF案件編號\n1,2026-08-18");

    mockMvc.perform(get("/api/cases/export"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            startsWith("attachment; filename=\"case-export-")))
        .andExpect(content().string(startsWith("\uFEFF案件編號")));
  }

  @Test
  @WithMockUser(roles = "VIEWER")
  void statistics_shouldReturnAggregates() throws Exception {
    when(caseService.statistics()).thenReturn(new CaseStatisticsResponse(
        3L, 2L, 1L,
        List.of(new CountName("柑橘", 2L), new CountName("水稻", 1L)),
        List.of(new CountName("真菌", 3L)),
        List.of(new StatusCount("PENDING", 1L), new StatusCount("RESOLVED", 2L),
            new StatusCount("CLOSED", 0L)),
        List.of(new MonthCount("2026-03", 0L), new MonthCount("2026-08", 3L))));

    mockMvc.perform(get("/api/cases/statistics"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalCases").value(3))
        .andExpect(jsonPath("$.monthNewCases").value(2))
        .andExpect(jsonPath("$.pendingCases").value(1))
        .andExpect(jsonPath("$.topCrops[0].name").value("柑橘"))
        .andExpect(jsonPath("$.topPestCategories[0].count").value(3))
        .andExpect(jsonPath("$.statusRatio[2].count").value(0))
        .andExpect(jsonPath("$.monthlyTrend[1].month").value("2026-08"));
  }

  @Test
  @WithMockUser(roles = "VIEWER")
  void list_shouldPassFilterParameters() throws Exception {
    when(caseService.list(any(), any())).thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/api/cases")
            .param("cropId", "3")
            .param("serviceId", "2")
            .param("senderName", "張")
            .param("receiveDateFrom", "2026-08-01")
            .param("receiveDateTo", "2026-08-31")
            .param("status", "RESOLVED"))
        .andExpect(status().isOk());

    ArgumentCaptor<CaseFilter> captor = ArgumentCaptor.forClass(CaseFilter.class);
    verify(caseService).list(captor.capture(), any());
    CaseFilter filter = captor.getValue();
    assertThat(filter.cropId()).isEqualTo(3L);
    assertThat(filter.serviceId()).isEqualTo(2L);
    assertThat(filter.senderName()).isEqualTo("張");
    assertThat(filter.receiveDateFrom()).isEqualTo(LocalDate.of(2026, 8, 1));
    assertThat(filter.receiveDateTo()).isEqualTo(LocalDate.of(2026, 8, 31));
    assertThat(filter.status()).isEqualTo("RESOLVED");
  }

  @Test
  @WithMockUser(roles = "VIEWER")
  void list_withInvalidStatus_shouldReturnBadRequest() throws Exception {
    when(caseService.list(any(), any())).thenThrow(
        new ApiException("INVALID_STATUS", HttpStatus.BAD_REQUEST, "無效的狀態：DRAFT"));

    mockMvc.perform(get("/api/cases").param("status", "DRAFT"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_STATUS"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "VIEWER")
  void list_withInvalidDate_shouldReturnBadRequest() throws Exception {
    // 日期參數格式錯誤：型別轉換失敗 → 400 VALIDATION_ERROR（非 500）
    mockMvc.perform(get("/api/cases").param("receiveDateFrom", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.message").value("參數 receiveDateFrom 格式錯誤：abc"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  @WithMockUser(roles = "VIEWER")
  void create_shouldForbidViewer() throws Exception {
    // RBAC：VIEWER 建立案件應被 @PreAuthorize 拒絕（403，統一錯誤格式）
    mockMvc.perform(post("/api/cases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_CASE_JSON))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void create_shouldAllowStaff() throws Exception {
    when(caseService.create(any())).thenReturn(sampleResponse());

    mockMvc.perform(post("/api/cases")
            .contentType(MediaType.APPLICATION_JSON)
            .content(VALID_CASE_JSON))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.senderName").value("張三"));
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void create_shouldRejectMissingRequiredFields() throws Exception {
    // 缺少 receiveDate 與送件人欄位：Bean Validation 回 400 並帶 details
    mockMvc.perform(post("/api/cases")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"cropId":1}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.error.details.receiveDate").exists())
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }
}
