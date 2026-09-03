package com.d0w0b.phytotrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.test.context.TestSecurityContextHolderStrategyAdapter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.d0w0b.phytotrack.config.SecurityConfig;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.security.JwtAuthenticationFilter;
import com.d0w0b.phytotrack.service.AuthService;

import org.springframework.http.HttpStatus;

/**
 * 使用者管理控制器切片測試：驗證 ADMIN 專用端點的 401/403/400/200 契約
 */
@WebMvcTest(UserAdminController.class)
@Import({SecurityConfig.class, UserAdminControllerTest.TestSecurityStrategy.class})
class UserAdminControllerTest {

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
  private AuthService authService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp() throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    doAnswer(invocation -> {
      FilterChain chain = invocation.getArgument(2, FilterChain.class);
      chain.doFilter(invocation.getArgument(0, HttpServletRequest.class),
          invocation.getArgument(1, HttpServletResponse.class));
      return null;
    }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
  }

  private UserResponse sampleUser(long id, String role, boolean active) {
    return new UserResponse(id, "user" + id, "使用者" + id, null, role, active);
  }

  // --- role ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateRole_shouldSucceedForAdmin() throws Exception {
    when(authService.updateRole(eq(2L), eq("ROLE_STAFF")))
        .thenReturn(sampleUser(2L, "ROLE_STAFF", true));

    mockMvc.perform(patch("/api/admin/users/{id}/role", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ROLE_STAFF"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void updateRole_shouldBeForbiddenForNonAdmin() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/role", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  @Test
  void updateRole_shouldBeUnauthorizedWhenUnauthenticated() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/role", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateRole_shouldRejectBlankRole() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/role", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role":""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateRole_shouldRejectInvalidRole() throws Exception {
    when(authService.updateRole(eq(2L), eq("ROLE_UNKNOWN")))
        .thenThrow(new ApiException("INVALID_ROLE", HttpStatus.BAD_REQUEST, "角色不正確"));

    mockMvc.perform(patch("/api/admin/users/{id}/role", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role":"ROLE_UNKNOWN"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_ROLE"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateRole_shouldReturn404WhenUserNotFound() throws Exception {
    when(authService.updateRole(eq(999L), org.mockito.ArgumentMatchers.anyString()))
        .thenThrow(new ApiException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));

    mockMvc.perform(patch("/api/admin/users/{id}/role", 999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
  }

  // --- active ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateActive_shouldSucceed() throws Exception {
    when(authService.updateActive(eq(2L), eq(false)))
        .thenReturn(sampleUser(2L, "ROLE_VIEWER", false));

    mockMvc.perform(patch("/api/admin/users/{id}/active", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"active":false}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void updateActive_shouldRejectNull() throws Exception {
    mockMvc.perform(patch("/api/admin/users/{id}/active", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  // --- reset-password ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void resetPassword_shouldSucceed() throws Exception {
    mockMvc.perform(post("/api/admin/users/{id}/reset-password", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newPassword":"newPass123"}
                """))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void resetPassword_shouldRejectShortPassword() throws Exception {
    mockMvc.perform(post("/api/admin/users/{id}/reset-password", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newPassword":"123"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(roles = "STAFF")
  void resetPassword_shouldBeForbiddenForNonAdmin() throws Exception {
    mockMvc.perform(post("/api/admin/users/{id}/reset-password", 2L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"newPassword":"newPass123"}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }
}
