package com.d0w0b.phytotrack.security;

import io.jsonwebtoken.Claims;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * JWT 認證過濾器（Authentication Filter）
 *
 * 每個請求進來時：
 *   1. 從 Authorization header 取出 "Bearer <token>"
 *   2. 交由 JwtTokenProvider 驗證並取出使用者資訊
 *   3. 將認證結果寫入 SecurityContext，後續 Controller 即可用 @AuthenticationPrincipal 取得
 *
 * 設計說明：直接以 Token 內的角色建構主體，不查詢資料庫，
 * 符合無狀態（Stateless）設計；角色變更在 Token 過期後才生效。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  /** Bearer 前綴（含空格） */
  private static final String BEARER_PREFIX = "Bearer ";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String header = request.getHeader("Authorization");

    // 尚未認證，且請求帶有 Bearer Token 才嘗試解析
    if (header != null && header.startsWith(BEARER_PREFIX)
        && SecurityContextHolder.getContext().getAuthentication() == null) {

      String token = header.substring(BEARER_PREFIX.length());
      Claims claims = jwtTokenProvider.parseToken(token);

      if (claims != null) {
        // 從 Token 內容重建安全主體（使用者 ID、使用者名稱、角色）
        UserPrincipal principal = new UserPrincipal(
            claims.get("userId", Long.class),
            claims.getSubject(),
            "",
            claims.get("role", String.class),
            true);

        // 建立認證物件並帶上請求細節
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal, null, List.of(principal.getAuthorities().iterator().next()));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }
}
