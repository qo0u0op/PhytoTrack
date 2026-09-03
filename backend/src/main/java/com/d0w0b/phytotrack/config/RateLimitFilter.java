package com.d0w0b.phytotrack.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.d0w0b.phytotrack.exception.ErrorResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

/**
 * 速率限制過濾器（RateLimitFilter）
 *
 * 僅攔截公開認證端點 POST /api/auth/login|register|abandon-deactivate，
 * 每 IP 固定視窗 10/min，超限回 429 + Retry-After + 統一錯誤形狀。
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger (RateLimitFilter.class);
  private static final Set<String> LIMITED_PATHS = Set.of (
      "/api/auth/login",
      "/api/auth/register",
      "/api/auth/abandon-deactivate");

  private final RateLimitService rateLimitService;
  private final ObjectMapper objectMapper;

  public RateLimitFilter (@org.springframework.beans.factory.annotation.Autowired (required = false) RateLimitService rateLimitService) {
    this.rateLimitService = rateLimitService;
    this.objectMapper = new ObjectMapper ();
  }

  @Override
  protected void doFilterInternal (HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String method = request.getMethod ();
    String uri = request.getRequestURI ();

    // 僅限 POST + 目標路徑才限流
    if (!"POST".equalsIgnoreCase (method) || !LIMITED_PATHS.contains (uri)) {
      filterChain.doFilter (request, response);
      return;
    }

    if (rateLimitService == null || !rateLimitService.isEnabled ()) {
      filterChain.doFilter (request, response);
      return;
    }

    String ip = request.getRemoteAddr ();
    boolean allowed = rateLimitService.tryConsume (ip);
    if (allowed) {
      filterChain.doFilter (request, response);
      return;
    }

    // 超限：回 429
    String requestId = MDC.get (RequestIdFilter.MDC_REQUEST_ID);
    if (requestId == null) {
      requestId = response.getHeader (RequestIdFilter.REQUEST_ID_HEADER);
    }
    log.warn ("[{}] rate limited ip={} path={} method={}", requestId, ip, uri, method);

    response.setStatus (429);
    response.setContentType ("application/json;charset=UTF-8");
    // 固定視窗簡化：Retry-After 60 秒
    response.setHeader ("Retry-After", "60");
    // 若尚未有 requestId header，補上
    if (requestId != null && response.getHeader (RequestIdFilter.REQUEST_ID_HEADER) == null) {
      response.setHeader (RequestIdFilter.REQUEST_ID_HEADER, requestId);
    }

    ErrorResponse body = ErrorResponse.of (
        "RATE_LIMITED", "請求過於頻繁，請稍後再試", requestId == null ? "" : requestId);
    String json = objectMapper.writeValueAsString (body);
    response.getWriter ().write (json);
  }
}
