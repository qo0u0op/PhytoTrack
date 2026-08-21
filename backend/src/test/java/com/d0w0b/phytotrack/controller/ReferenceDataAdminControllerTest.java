package com.d0w0b.phytotrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.security.JwtAuthenticationFilter;
import com.d0w0b.phytotrack.service.ReferenceDataService;

@WebMvcTest(ReferenceDataAdminController.class)
@Import({SecurityConfig.class, ReferenceDataAdminControllerTest.TestSecurityStrategy.class})
class ReferenceDataAdminControllerTest {

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
  private ReferenceDataService referenceDataService;

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

  // 401 未登入
  @Test
  void createDamage_shouldBeUnauthorizedWhenUnauthenticated() throws Exception {
    mockMvc.perform(post("/api/admin/ref/damages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"新被害部位"}
                """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
  }

  // 403 非 ADMIN
  @Test
  @WithMockUser(roles = "STAFF")
  void createDamage_shouldBeForbiddenForNonAdmin() throws Exception {
    mockMvc.perform(post("/api/admin/ref/damages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"新被害部位"}
                """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
  }

  // 400 名稱空白
  @Test
  @WithMockUser(roles = "ADMIN")
  void createDamage_shouldRejectBlankName() throws Exception {
    mockMvc.perform(post("/api/admin/ref/damages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":""}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  // 201 成功
  @Test
  @WithMockUser(roles = "ADMIN")
  void createDamage_shouldSucceedForAdmin() throws Exception {
    when(referenceDataService.createDamage(eq("新被害部位")))
        .thenReturn(new IdNameResponse(99L, "新被害部位"));

    mockMvc.perform(post("/api/admin/ref/damages")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"新被害部位"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(99))
        .andExpect(jsonPath("$.name").value("新被害部位"));
  }

  // 200 更新成功
  @Test
  @WithMockUser(roles = "ADMIN")
  void updateDamage_shouldSucceed() throws Exception {
    when(referenceDataService.updateDamage(eq(1L), eq("更新後")))
        .thenReturn(new IdNameResponse(1L, "更新後"));

    mockMvc.perform(put("/api/admin/ref/damages/{id}", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"更新後"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("更新後"));
  }

  // 404 不存在
  @Test
  @WithMockUser(roles = "ADMIN")
  void updateDamage_shouldReturn404WhenNotFound() throws Exception {
    when(referenceDataService.updateDamage(eq(999L), any()))
        .thenThrow(new ApiException("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "被害部位不存在"));

    mockMvc.perform(put("/api/admin/ref/damages/{id}", 999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"更新後"}
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("REFERENCE_NOT_FOUND"));
  }

  // 409 被引用
  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDamage_shouldReturn409WhenInUse() throws Exception {
    org.mockito.Mockito.doThrow(new ApiException("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除"))
        .when(referenceDataService).deleteDamage(eq(1L));

    mockMvc.perform(delete("/api/admin/ref/damages/{id}", 1L))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("REFERENCE_IN_USE"));
  }

  // 404 刪除不存在
  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDamage_shouldReturn404WhenNotFound() throws Exception {
    org.mockito.Mockito.doThrow(new ApiException("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "被害部位不存在"))
        .when(referenceDataService).deleteDamage(eq(999L));

    mockMvc.perform(delete("/api/admin/ref/damages/{id}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("REFERENCE_NOT_FOUND"));
  }

  // 204 刪除成功
  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDamage_shouldSucceed() throws Exception {
    mockMvc.perform(delete("/api/admin/ref/damages/{id}", 1L))
        .andExpect(status().isNoContent());
  }

  // 非法 ID 格式 400
  @Test
  @WithMockUser(roles = "ADMIN")
  void deleteDamage_shouldReturn400ForInvalidId() throws Exception {
    mockMvc.perform(delete("/api/admin/ref/damages/{id}", "abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  // crops 400 category not found -> 404 via service
  @Test
  @WithMockUser(roles = "ADMIN")
  void createCrop_shouldRejectBlankName() throws Exception {
    mockMvc.perform(post("/api/admin/ref/crops")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"","cropCategoryId":1}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void createCrop_shouldSucceed() throws Exception {
    when(referenceDataService.createCrop(eq("新作物"), eq(1L)))
        .thenReturn(new IdNameResponse(100L, "新作物"));

    mockMvc.perform(post("/api/admin/ref/crops")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name":"新作物","cropCategoryId":1}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(100));
  }
}
