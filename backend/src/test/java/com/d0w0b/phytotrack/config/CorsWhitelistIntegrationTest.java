package com.d0w0b.phytotrack.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CORS 白名單整合測試（security-review Phase2）
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
@TestPropertySource (properties = {
    "app.cors.allowed-origins=https://app.example.com,https://admin.example.com",
    "app.rate-limit.enabled=false",
    "app.security-headers.enabled=false"
})
class CorsWhitelistIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void 白名單內放行 () throws Exception {
    mockMvc.perform (get ("/api/auth/login").header ("Origin", "https://app.example.com"))
        .andExpect (header ().string ("Access-Control-Allow-Origin", "https://app.example.com"));
  }

  @Test
  void 非白名單拒絕 () throws Exception {
    var result = mockMvc.perform (get ("/api/auth/login").header ("Origin", "https://evil.com")).andReturn ();
    assertThat (result.getResponse ().getHeader ("Access-Control-Allow-Origin")).isNullOrEmpty ();
  }

  @Test
  void 預檢請求白名單放行 () throws Exception {
    mockMvc.perform (options ("/api/auth/login").header ("Origin", "https://app.example.com")
            .header ("Access-Control-Request-Method", "POST"))
        .andExpect (status ().isOk ())
        .andExpect (header ().string ("Access-Control-Allow-Origin", "https://app.example.com"));
  }

  @Test
  void 預檢非白名單不回AllowOrigin () throws Exception {
    var result = mockMvc.perform (options ("/api/auth/login").header ("Origin", "https://evil.com")
        .header ("Access-Control-Request-Method", "POST")).andReturn ();
    assertThat (result.getResponse ().getHeader ("Access-Control-Allow-Origin")).isNullOrEmpty ();
  }

  @Test
  void 空Origin不影響無跨源請求 () throws Exception {
    // 無 Origin header 的請求不應含 CORS 標頭
    var result = mockMvc.perform (get ("/api/auth/login")).andReturn ();
    assertThat (result.getResponse ().getHeader ("Access-Control-Allow-Origin")).isNullOrEmpty ();
  }
}
