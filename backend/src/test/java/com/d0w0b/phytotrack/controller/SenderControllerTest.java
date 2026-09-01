package com.d0w0b.phytotrack.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.d0w0b.phytotrack.controller.SenderController;
import com.d0w0b.phytotrack.dto.SenderDtos.SenderResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.security.JwtAuthenticationFilter;
import com.d0w0b.phytotrack.service.SenderService;

import java.util.List;

@WebMvcTest (SenderController.class)
@Import ({SecurityConfig.class, SenderControllerTest.TestSecurityStrategy.class})
class SenderControllerTest {

  @TestConfiguration (proxyBeanMethods = false)
  static class TestSecurityStrategy {
    @Bean
    SecurityContextHolderStrategy securityContextHolderStrategy () {
      return new TestSecurityContextHolderStrategyAdapter ();
    }
  }

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @MockitoBean
  private SenderService senderService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @BeforeEach
  void setUp () throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup (context).apply (springSecurity ()).build ();
    doAnswer (invocation -> {
      FilterChain chain = invocation.getArgument (2, FilterChain.class);
      chain.doFilter (invocation.getArgument (0, HttpServletRequest.class),
          invocation.getArgument (1, HttpServletResponse.class));
      return null;
    }).when (jwtAuthenticationFilter).doFilter (any (), any (), any ());
  }

  private SenderResponse sample (long id) {
    return new SenderResponse (id, "王小明", "阿明", "0912345678", "地址", 1L, "霧峰區", "臺中市", 1L, "農民");
  }

  // 401 未登入
  @Test
  void search_shouldBeUnauthorizedWhenUnauthenticated () throws Exception {
    mockMvc.perform (get ("/api/senders/search").param ("q", "王"))
        .andExpect (status ().isUnauthorized ())
        .andExpect (jsonPath ("$.error.code").value ("UNAUTHORIZED"));
  }

  @Test
  void delete_shouldBeUnauthorizedWhenUnauthenticated () throws Exception {
    mockMvc.perform (delete ("/api/senders/{id}", 1L))
        .andExpect (status ().isUnauthorized ());
  }

  // 403 非 ADMIN (刪除)
  @Test
  @WithMockUser (roles = "STAFF")
  void delete_shouldBeForbiddenForNonAdmin () throws Exception {
    mockMvc.perform (delete ("/api/senders/{id}", 1L))
        .andExpect (status ().isForbidden ())
        .andExpect (jsonPath ("$.error.code").value ("ACCESS_DENIED"));
  }

  // 200 搜尋成功 (登入即可)
  @Test
  @WithMockUser (roles = "VIEWER")
  void search_shouldReturnCandidatesForAnyAuthenticatedUser () throws Exception {
    when (senderService.search (eq ("王"))).thenReturn (List.of (sample (1L)));

    mockMvc.perform (get ("/api/senders/search").param ("q", "王"))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$[0].senderId").value (1))
        .andExpect (jsonPath ("$[0].name").value ("王小明"));
  }

  // 400 q 空白
  @Test
  @WithMockUser (roles = "ADMIN")
  void search_shouldRejectBlankQuery () throws Exception {
    when (senderService.search (eq (" ")))
        .thenThrow (new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "搜尋關鍵字不可為空白"));

    mockMvc.perform (get ("/api/senders/search").param ("q", " "))
        .andExpect (status ().isBadRequest ())
        .andExpect (jsonPath ("$.error.code").value ("VALIDATION_ERROR"));
  }

  // 200 列表
  @Test
  @WithMockUser (roles = "ADMIN")
  void list_shouldReturnSenders () throws Exception {
    when (senderService.list ()).thenReturn (List.of (sample (1L), sample (2L)));

    mockMvc.perform (get ("/api/senders"))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.length ()").value (2));
  }

  // 200 詳細
  @Test
  @WithMockUser (roles = "ADMIN")
  void detail_shouldReturnSender () throws Exception {
    when (senderService.detail (eq (1L))).thenReturn (sample (1L));

    mockMvc.perform (get ("/api/senders/{id}", 1L))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.senderId").value (1));
  }

  // 404 詳細不存在
  @Test
  @WithMockUser (roles = "ADMIN")
  void detail_shouldReturn404WhenNotFound () throws Exception {
    when (senderService.detail (eq (999L)))
        .thenThrow (new ApiException ("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));

    mockMvc.perform (get ("/api/senders/{id}", 999L))
        .andExpect (status ().isNotFound ())
        .andExpect (jsonPath ("$.error.code").value ("SENDER_NOT_FOUND"));
  }

  // 204 刪除成功
  @Test
  @WithMockUser (roles = "ADMIN")
  void delete_shouldSucceedForAdmin () throws Exception {
    mockMvc.perform (delete ("/api/senders/{id}", 1L))
        .andExpect (status ().isNoContent ());
  }

  // 409 被引用
  @Test
  @WithMockUser (roles = "ADMIN")
  void delete_shouldReturn409WhenInUse () throws Exception {
    doThrow (new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除"))
        .when (senderService).delete (eq (1L));

    mockMvc.perform (delete ("/api/senders/{id}", 1L))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("REFERENCE_IN_USE"));
  }

  // ===== create / update (STAFF+)=====

  @Test
  @WithMockUser (roles = "STAFF")
  void create_shouldSucceedForStaff () throws Exception {
    when (senderService.create (any ()))
        .thenReturn (sample (10L));

    mockMvc.perform (post ("/api/senders")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"新送件人","displayName":"暱稱","phone":"0912000000","address":"地址","districtId":1,"senderTypeId":1}
                """))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.senderId").value (10));
  }

  @Test
  void create_shouldBeUnauthorizedWhenUnauthenticated () throws Exception {
    mockMvc.perform (post ("/api/senders")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"phone":"0912000000","address":"地址","districtId":1,"senderTypeId":1}
                """))
        .andExpect (status ().isUnauthorized ());
  }

  @Test
  @WithMockUser (roles = "VIEWER")
  void create_shouldBeForbiddenForViewer () throws Exception {
    mockMvc.perform (post ("/api/senders")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"phone":"0912000000","address":"地址","districtId":1,"senderTypeId":1}
                """))
        .andExpect (status ().isForbidden ());
  }

  @Test
  @WithMockUser (roles = "STAFF")
  void update_shouldSucceedForStaff () throws Exception {
    when (senderService.update (eq (1L), any ())).thenReturn (sample (1L));

    mockMvc.perform (put ("/api/senders/{id}", 1L)
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"王小明","displayName":"阿明","phone":"0912345678","address":"新地址","districtId":1,"senderTypeId":1}
                """))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.senderId").value (1));
  }

  @Test
  @WithMockUser (roles = "VIEWER")
  void update_shouldBeForbiddenForViewer () throws Exception {
    mockMvc.perform (put ("/api/senders/{id}", 1L)
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"address":"新地址","districtId":1,"senderTypeId":1}
                """))
        .andExpect (status ().isForbidden ());
  }
}
