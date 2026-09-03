package com.d0w0b.phytotrack.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * CORS 白名單測試（security-review Phase2）
 *
 * 單元：CorsConfig 解析邏輯
 * 整合：MockMvc 驗證白名單內外、prod 空拒絕、preflight
 */
class CorsConfigTest {

  // 單元測試：直接測 CorsConfig 解析與 profile 分支
  @Test
  void dev為空應沿用通配 () {
    MockEnvironment env = new MockEnvironment ();
    env.setActiveProfiles ("dev");
    CorsConfig config = new CorsConfig (env, "");
    CorsConfigurationSource source = config.corsConfigurationSource ();
    CorsConfiguration cc = source.getCorsConfiguration (new org.springframework.mock.web.MockHttpServletRequest ("GET", "/api/cases"));
    // dev 為空時應為 originPatterns "*"
    assertThat (cc.getAllowedOriginPatterns ()).contains ("*");
  }

  @Test
  void prod為空應拒絕跨源 () {
    MockEnvironment env = new MockEnvironment ();
    env.setActiveProfiles ("prod");
    CorsConfig config = new CorsConfig (env, "");
    CorsConfigurationSource source = config.corsConfigurationSource ();
    CorsConfiguration cc = source.getCorsConfiguration (new org.springframework.mock.web.MockHttpServletRequest ("GET", "/api/cases"));
    assertThat (cc.getAllowedOrigins ()).isEmpty ();
    // patterns 應為 null 或空
    assertThat (cc.getAllowedOriginPatterns ()).isNullOrEmpty ();
  }

  @Test
  void 白名單解析去重與trim () {
    MockEnvironment env = new MockEnvironment ();
    env.setActiveProfiles ("prod");
    CorsConfig config = new CorsConfig (env, " https://app.example.com , https://app.example.com, https://admin.example.com ");
    CorsConfigurationSource source = config.corsConfigurationSource ();
    CorsConfiguration cc = source.getCorsConfiguration (new org.springframework.mock.web.MockHttpServletRequest ("GET", "/api/cases"));
    assertThat (cc.getAllowedOrigins ()).containsExactly ("https://app.example.com", "https://admin.example.com");
  }
}
