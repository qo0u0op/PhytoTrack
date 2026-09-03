package com.d0w0b.phytotrack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 跨來源資源共用 (CORS，Cross-Origin Resource Sharing) 設定
 *
 * 前後分離後，Vue 開發伺服器 (http://localhost:5173) 與後端 (:8080) 不同源，
 * 瀏覽器會擋下跨來源請求，因此需要允許前端來源。
 *
 * Phase2（security-review）：由 env CORS_ALLOWED_ORIGINS 白名單驅動，
 * dev 為空沿用 *，prod 為空預設拒絕跨源。
 */
@Configuration
public class CorsConfig {

  private static final Logger log = LoggerFactory.getLogger (CorsConfig.class);

  private final Environment env;
  private final String allowedOriginsRaw;

  public CorsConfig (Environment env,
      @Value ("${app.cors.allowed-origins:}") String allowedOriginsRaw) {
    this.env = env;
    this.allowedOriginsRaw = allowedOriginsRaw;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource () {
    CorsConfiguration config = new CorsConfiguration ();

    List<String> allowedOrigins = parseAllowedOrigins (allowedOriginsRaw);

    if (allowedOrigins.isEmpty ()) {
      boolean isDev = Arrays.asList (env.getActiveProfiles ()).contains ("dev")
          || env.getActiveProfiles ().length == 0;
      // dev 或無 profile（測試未指定 profile）時沿用 * 相容本地開發
      if (isDev) {
        config.setAllowedOriginPatterns (List.of ("*"));
        log.info ("CORS allowed-origins=* (dev profile, raw='{}')", allowedOriginsRaw);
      } else {
        // prod 且未配置：空列表，預設拒絕跨源（不回 Allow-Origin）
        config.setAllowedOrigins (Collections.emptyList ());
        log.info ("CORS allowed-origins=[] (prod, no whitelist, raw='{}')", allowedOriginsRaw);
      }
    } else {
      config.setAllowedOrigins (allowedOrigins);
      log.info ("CORS allowed-origins={} (profile={}, raw='{}')",
          allowedOrigins, Arrays.toString (env.getActiveProfiles ()), allowedOriginsRaw);
    }

    config.setAllowedMethods (List.of ("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders (List.of ("*"));
    config.setExposedHeaders (List.of ("Authorization", "Content-Disposition", "X-Request-Id"));
    config.setAllowCredentials (false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource ();
    // 套用至所有 /api/** 路徑
    source.registerCorsConfiguration ("/api/**", config);
    return source;
  }

  private List<String> parseAllowedOrigins (String raw) {
    if (raw == null || raw.isBlank ()) {
      return List.of ();
    }
    return Arrays.stream (raw.split (","))
        .map (String::trim)
        .filter (s -> !s.isEmpty ())
        .distinct ()
        .collect (Collectors.toList ());
  }
}
