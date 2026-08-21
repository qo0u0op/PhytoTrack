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
import java.util.Optional;

import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.UserRepository;

/**
 * JWT 認證過濾器（Authentication Filter）
 *
 * 每個請求進來時：
 *   1. 從 Authorization header 取出 "Bearer <token>"
 *   2. 交由 JwtTokenProvider 驗證並取出使用者資訊
 *   3. 將認證結果寫入 SecurityContext，後續 Controller 即可用 @AuthenticationPrincipal 取得
 *
 * 設計說明：舊版直接以 Token 內的角色建構主體（無狀態），
 * 現為支援「停用後既有 token 立即失效」與「角色變更即時生效」（見 user-admin spec），
 *改為每請求以 userId 查 DB 驗證 active 並以 DB 的 role/active 建構主體。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
    this.jwtTokenProvider = jwtTokenProvider;
    this.userRepository = userRepository;
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
        Long userId = claims.get("userId", Long.class);
        Optional<User> userOpt = Optional.empty();
        if (userId != null) {
          userOpt = userRepository.findById(userId);
        } else {
          // 相容舊 token（無 userId）：以 subject 回落查詢
          String subject = claims.getSubject();
          if (subject != null) {
            userOpt = userRepository.findByUsername(subject);
          }
        }

        // 停用或不存在的帳號不寫入 SecurityContext，後續由 SecurityFilterChain 回 401
        if (userOpt.isEmpty() || !userOpt.get().isActive()) {
          filterChain.doFilter(request, response);
          return;
        }

        // 以 DB 的最新 role/active 建構主體（角色變更即時生效）
        UserPrincipal principal = UserPrincipal.from(userOpt.get());

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
