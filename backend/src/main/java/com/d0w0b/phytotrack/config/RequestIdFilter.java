package com.d0w0b.phytotrack.config;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * 請求 ID 過濾器 (Request ID Filter)
 *
 * 為每個請求產生唯一的 requestId，寫入 MDC (Mapped Diagnostic Context)
 * 並回傳於回應標頭，讓前端 log 與伺服器 log 得以對照 (見 ADR-010 的可觀測性底線)。
 * 若請求已攜帶 X-Request-Id (例如上層閘道轉發)，則沿用之。
 */
@Component
@Order (Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String MDC_REQUEST_ID = "requestId";

  @Override
  protected void doFilterInternal (HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String requestId = request.getHeader (REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank ()) {
      requestId = UUID.randomUUID ().toString ();
    } else {
      // 限長度 ≤64 並過濾不可列印字元，避免日誌注入
      requestId = requestId.replaceAll ("[^\\x20-\\x7E]", "");
      if (requestId.length () > 64) {
        requestId = requestId.substring (0, 64);
      }
      if (requestId.isBlank ()) {
        requestId = UUID.randomUUID ().toString ();
      }
    }

    MDC.put (MDC_REQUEST_ID, requestId);
    response.setHeader (REQUEST_ID_HEADER, requestId);
    try {
      filterChain.doFilter (request, response);
    } finally {
      // 清除 MDC，避免 Thread 重複使用時殘留
      MDC.remove (MDC_REQUEST_ID);
    }
  }
}
