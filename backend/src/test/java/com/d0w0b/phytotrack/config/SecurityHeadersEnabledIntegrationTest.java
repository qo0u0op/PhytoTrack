package com.d0w0b.phytotrack.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
@TestPropertySource (properties = {
    "app.security-headers.enabled=true",
    "app.rate-limit.enabled=false",
    "app.cors.allowed-origins="
})
class SecurityHeadersEnabledIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void 啟用時回應含四標頭 () throws Exception {
    mockMvc.perform (get ("/api/auth/login"))
        .andExpect (header ().string ("Content-Security-Policy", org.hamcrest.Matchers.containsString ("default-src 'self'")))
        .andExpect (header ().string ("Strict-Transport-Security", "max-age=31536000; includeSubDomains"))
        .andExpect (header ().string ("X-Content-Type-Options", "nosniff"))
        .andExpect (header ().string ("X-Frame-Options", "DENY"));
  }
}
