package com.d0w0b.phytotrack.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * 安全標頭測試（security-review Phase2）- 單元
 */
class SecurityHeadersFilterTest {

  @Test
  void 非dev且未啟用仍注入_prod語意 () throws Exception {
    MockEnvironment env = new MockEnvironment ();
    env.setActiveProfiles ("prod");
    SecurityHeadersFilter filter = new SecurityHeadersFilter (env, false);
    MockHttpServletRequest req = new MockHttpServletRequest ("GET", "/api/cases");
    MockHttpServletResponse res = new MockHttpServletResponse ();
    filter.doFilterInternal (req, res, new MockFilterChain ());
    assertThat (res.getHeader ("Content-Security-Policy")).contains ("default-src 'self'");
    assertThat (res.getHeader ("Strict-Transport-Security")).isEqualTo ("max-age=31536000; includeSubDomains");
    assertThat (res.getHeader ("X-Content-Type-Options")).isEqualTo ("nosniff");
    assertThat (res.getHeader ("X-Frame-Options")).isEqualTo ("DENY");
  }

  @Test
  void dev未啟用不注入 () throws Exception {
    MockEnvironment env = new MockEnvironment ();
    env.setActiveProfiles ("dev");
    SecurityHeadersFilter filter = new SecurityHeadersFilter (env, false);
    MockHttpServletRequest req = new MockHttpServletRequest ("GET", "/api/cases");
    MockHttpServletResponse res = new MockHttpServletResponse ();
    filter.doFilterInternal (req, res, new MockFilterChain ());
    assertThat (res.getHeader ("Strict-Transport-Security")).isNull ();
    assertThat (res.getHeader ("Content-Security-Policy")).isNull ();
  }

  @Test
  void dev顯式啟用仍注入 () throws Exception {
    MockEnvironment env = new MockEnvironment ();
    env.setActiveProfiles ("dev");
    SecurityHeadersFilter filter = new SecurityHeadersFilter (env, true);
    MockHttpServletRequest req = new MockHttpServletRequest ("GET", "/api/cases");
    MockHttpServletResponse res = new MockHttpServletResponse ();
    filter.doFilterInternal (req, res, new MockFilterChain ());
    assertThat (res.getHeader ("Content-Security-Policy")).isNotNull ();
  }

  @Test
  void 已存在標頭不覆蓋 () throws Exception {
    MockEnvironment env = new MockEnvironment ();
    env.setActiveProfiles ("prod");
    SecurityHeadersFilter filter = new SecurityHeadersFilter (env, true);
    MockHttpServletRequest req = new MockHttpServletRequest ("GET", "/api/cases");
    MockHttpServletResponse res = new MockHttpServletResponse ();
    res.setHeader ("X-Content-Type-Options", "custom");
    filter.doFilterInternal (req, res, new MockFilterChain ());
    assertThat (res.getHeader ("X-Content-Type-Options")).isEqualTo ("custom");
  }
}
