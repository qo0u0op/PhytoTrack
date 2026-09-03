package com.d0w0b.phytotrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.beans.factory.annotation.Autowired;

import com.d0w0b.phytotrack.config.RateLimitFilter;
import com.d0w0b.phytotrack.security.JwtAuthenticationFilter;
import com.d0w0b.phytotrack.security.RestAuthenticationEntryPoint;

/**
 * Spring Security 設定
 *
 * 設計重點：
 *   - 無狀態 (Stateless) JWT 認證：不使用 session，每個請求由 JwtAuthenticationFilter 驗證
 *   - RBAC (角色權限)：透過 requestMatchers 設定 URL 級規則，方法級用 @PreAuthorize
 *   - BCrypt 密碼編碼器 (Password Encoder)
 *   - 明確列出公開端點 (註冊、登入、Swagger、llama 健康檢查)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RateLimitFilter rateLimitFilter;

  public SecurityConfig (JwtAuthenticationFilter jwtAuthenticationFilter,
      @Autowired (required = false) RateLimitFilter rateLimitFilter) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rateLimitFilter = rateLimitFilter;
  }

  /**
   * 安全過濾器鏈 (Security Filter Chain)
   *
   * 宣告式地定義「哪些 URL 公開、哪些需要認證/角色」。
   */
  @Bean
  public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
    http
        // 停用 CSRF (跨站請求偽造) 防護：JWT 使用 header 而非 Cookie，無 CSRF 風險
        .csrf (AbstractHttpConfigurer::disable)
        .cors (Customizer.withDefaults ())
        // 無狀態：每次請求獨立，不建立 HTTP Session
        .sessionManagement (session -> session.sessionCreationPolicy (SessionCreationPolicy.STATELESS))
        // 未認證 (無 token / 無效 / 過期)→ 401 統一錯誤格式；已認證角色不足 → 403
        .exceptionHandling (e -> e.authenticationEntryPoint (new RestAuthenticationEntryPoint ()))
        .authorizeHttpRequests (auth -> auth
            // 公開：註冊與登入、放棄停用申請
            .requestMatchers ("/api/auth/register", "/api/auth/login", "/api/auth/abandon-deactivate", "/api/auth/check-username", "/api/auth/check-email").permitAll ()
            // 公開：llama-server 健康檢查
            .requestMatchers ("/api/ai/health").permitAll ()
            // 公開：OpenAPI 文件與 Swagger UI
            .requestMatchers ("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll ()
            // Actuator：health/info 公開，metrics 僅 ADMIN（非 dev 限權，見 api-observability Phase2）
            .requestMatchers ("/actuator/health", "/actuator/info").permitAll ()
            .requestMatchers ("/actuator/**").hasRole ("ADMIN")
            // 其餘一律需登入
            .anyRequest ().authenticated ())
        // 在標準使用者名稱密碼驗證過濾器之前掛上 JWT 過濾器
        .addFilterBefore (jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    // 速率限制過濾器置於 JWT 之前（匿名亦限流），若 bean 存在才掛載（WebMvcTest 可 mock）
    if (rateLimitFilter != null) {
      http.addFilterBefore (rateLimitFilter, JwtAuthenticationFilter.class);
    }
    return http.build ();
  }

  /**
   * BCrypt 密碼編碼器 (Password Encoder)
   *
   * 單向雜湊 + 自動加鹽 (Salt)+ 可調成本 (Cost)，抵抗彩虹表與暴力破解。
   * 密碼一律以 encode () 儲存、matches () 驗證，絕不存明文。
   */
  @Bean
  public PasswordEncoder passwordEncoder () {
    return new BCryptPasswordEncoder (12);
  }

  /**
   * 認證管理器 (Authentication Manager)
   *
   * 供登入流程執行「使用者名稱 + 密碼」認證，內部會串接
   * CustomUserDetailsService 與 PasswordEncoder。
   */
  @Bean
  public AuthenticationManager authenticationManager (AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager ();
  }
}
