package com.d0w0b.phytotrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

/**
 * 安全回應標頭過濾器（SecurityHeadersFilter）
 *
 * 非 dev 或 app.security-headers.enabled=true 時，為所有回應注入
 * CSP / HSTS / nosniff / DENY，已存在標頭不覆蓋。
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

  private static final String CSP_VALUE =
      "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; font-src 'self' data:";
  private static final String HSTS_VALUE = "max-age=31536000; includeSubDomains";
  private static final String NO_SNIFF = "nosniff";
  private static final String DENY = "DENY";

  private final Environment env;
  private final boolean enabled;

  public SecurityHeadersFilter (Environment env,
      @Value ("${app.security-headers.enabled:false}") boolean enabled) {
    this.env = env;
    this.enabled = enabled;
  }

  @Override
  protected void doFilterInternal (HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    boolean isDev = Arrays.asList (env.getActiveProfiles ()).contains ("dev")
        || env.getActiveProfiles ().length == 0;
    // dev 且未顯式啟用時，不注入 HSTS/CSP（避免本地 http 誤導）
    boolean shouldInject = enabled || !isDev;

    if (shouldInject) {
      // CSP 兼顧 Swagger inline style
      setIfAbsent (response, "Content-Security-Policy", CSP_VALUE);
      setIfAbsent (response, "Strict-Transport-Security", HSTS_VALUE);
      setIfAbsent (response, "X-Content-Type-Options", NO_SNIFF);
      setIfAbsent (response, "X-Frame-Options", DENY);
    }

    filterChain.doFilter (request, response);
  }

  private void setIfAbsent (HttpServletResponse response, String name, String value) {
    if (!response.containsHeader (name)) {
      response.setHeader (name, value);
    }
  }
}
