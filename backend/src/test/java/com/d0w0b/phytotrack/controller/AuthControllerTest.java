package com.d0w0b.phytotrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.d0w0b.phytotrack.config.SecurityConfig;
import com.d0w0b.phytotrack.dto.AuthDtos.AuthResponse;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.security.JwtAuthenticationFilter;
import com.d0w0b.phytotrack.service.AuthService;

/**
 * 認證控制器（AuthController）Web 層測試
 *
 * @WebMvcTest 只載入 Controller、全域例外處理與 Security 設定，不需啟動資料庫
 * （兌現 ADR-003「業務邏輯可獨立於 HTTP 層測試」）。以 @WithMockUser / MockMvc
 * 驗證公開端點、Bean Validation 與錯誤回應契約（ADR-010）。
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUpFilterToPassThrough() throws Exception {
    // JWT 解析屬無狀態細節，此處模擬其「直接放行」；授權規則由 @WithMockUser + @PreAuthorize 驗證
    doAnswer(invocation -> {
      FilterChain chain = invocation.getArgument(2, FilterChain.class);
      chain.doFilter(invocation.getArgument(0, HttpServletRequest.class),
          invocation.getArgument(1, HttpServletResponse.class));
      return null;
    }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
  }

  @Test
  void register_shouldReturnCreated() throws Exception {
    when(authService.register(any())).thenReturn(
        new UserResponse(1L, "junit-user", "測試使用者", null, "ROLE_VIEWER", true));

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"junit-user","displayName":"測試使用者","password":"secret123"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("junit-user"))
        .andExpect(jsonPath("$.role").value("ROLE_VIEWER"));
  }

  @Test
  void register_shouldRejectInvalidPayloadWithDetails() throws Exception {
    // 帳號過短、顯示名稱空白、密碼過短：全部觸發 Bean Validation
    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"a","displayName":"","password":"1"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        // ADR-010：details 收集各欄位錯誤
        .andExpect(jsonPath("$.error.details.username").exists())
        .andExpect(jsonPath("$.error.details.displayName").exists())
        .andExpect(jsonPath("$.error.details.password").exists())
        // 每個錯誤回應都帶 requestId（可觀測性底線）
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }

  @Test
  void login_shouldReturnToken() throws Exception {
    when(authService.login(any())).thenReturn(
        new AuthResponse("jwt-token",
            new UserResponse(1L, "admin", "管理員", null, "ROLE_ADMIN", true)));

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"username":"admin","password":"admin123"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").value("jwt-token"))
        .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"));
  }

  @Test
  void me_shouldBeProtectedBySecurityChain() throws Exception {
    // 未登入存取受保護端點：由 SecurityFilterChain 拒絕（401，統一錯誤格式）
    mockMvc.perform(post("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.requestId").isNotEmpty());
  }
}
