package com.d0w0b.phytotrack.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.d0w0b.phytotrack.config.RequestIdFilter;
import com.d0w0b.phytotrack.exception.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 未認證的入口 (Authentication Entry Point)
 *
 * 未登入或攜帶無效／過期 token 存取受保護資源時，回 401 與統一錯誤格式
 * (見 ADR-010)，前端攔截器據此清除本機登入狀態並導向登入頁。
 * 權限不足 (已登入但角色不符) 仍由 GlobalExceptionHandler 回 403。
 *
 * 由 SecurityConfig 以 Bean 方式建立 (@WebMvcTest slice 不掃描 @Component)。
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper = new ObjectMapper ();

  @Override
  public void commence (HttpServletRequest request, HttpServletResponse response,
      AuthenticationException authException) throws IOException {
    String requestId = MDC.get (RequestIdFilter.MDC_REQUEST_ID);
    if (requestId == null) {
      requestId = UUID.randomUUID ().toString ();
    }
    response.setStatus (HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType (MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding (StandardCharsets.UTF_8.name ());
    response.getWriter ().write (objectMapper.writeValueAsString (ErrorResponse.of ("UNAUTHORIZED", "請先登入", requestId)));
  }
}