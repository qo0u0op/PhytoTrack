package com.d0w0b.phytotrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 跨來源資源共用 (CORS，Cross-Origin Resource Sharing) 設定
 *
 * 前後分離後，Vue 開發伺服器 (http://localhost:5173) 與後端 (:8080) 不同源，
 * 瀏覽器會擋下跨來源請求，因此需要允許前端來源。
 *
 * 注意：本專案使用 JWT (Authorization header) 而非 Cookie 傳遞認證，
 * 不依賴 Cookie，故 allowCredentials 可為 false，安全性較高。
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource () {
    CorsConfiguration config = new CorsConfiguration ();
    // 允許所有來源 (區域網路內部工具可接受；正式部署應縮小範圍)
    config.setAllowedOriginPatterns (List.of ("*"));
    config.setAllowedMethods (List.of ("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders (List.of ("*"));
    config.setExposedHeaders (List.of ("Authorization", "Content-Disposition", "X-Request-Id"));
    config.setAllowCredentials (false);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource ();
    // 套用至所有 /api/** 路徑
    source.registerCorsConfiguration ("/api/**", config);
    return source;
  }
}
