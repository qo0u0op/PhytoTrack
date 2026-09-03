package com.d0w0b.phytotrack.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("dev")
@TestPropertySource (properties = {
    "app.security-headers.enabled=false",
    "app.rate-limit.enabled=false",
    "app.cors.allowed-origins="
})
class SecurityHeadersDevDisabledIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void dev未啟用時不含HSTS () throws Exception {
    var result = mockMvc.perform (MockMvcRequestBuilders.get ("/api/auth/login")).andReturn ();
    assertThat (result.getResponse ().getHeader ("Strict-Transport-Security")).isNull ();
  }
}
