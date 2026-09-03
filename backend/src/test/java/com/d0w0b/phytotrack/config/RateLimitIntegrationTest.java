package com.d0w0b.phytotrack.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 速率限制整合測試（security-review Phase2）
 *
 * 驗證：同一 IP 10 次內放行、第 11 次 429、混合計數、不同 IP 獨立、test 預設關閉。
 * 以 MockMvc + RemoteAddr 模擬 IP，enabled 透過 TestPropertySource 覆蓋為 true。
 */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles ("test")
@TestPropertySource (properties = {
    "app.rate-limit.enabled=true",
    "app.rate-limit.requests-per-minute=10",
    "app.rate-limit.window-seconds=60"
})
class RateLimitIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private RateLimitService rateLimitService;

  @BeforeEach
  void clearBuckets () {
    rateLimitService.clearAll ();
  }

  @Test
  void 暴力嘗試受限_第11次429 () throws Exception {
    String ip = "10.0.0.1_" + System.nanoTime ();
    for (int i = 0; i < 10; i++) {
      mockMvc.perform (post ("/api/auth/login")
              .with (req -> { req.setRemoteAddr (ip); return req; })
              .contentType (MediaType.APPLICATION_JSON)
              .content ("""
                  {"username":"nonexist","password":"wrong"}
                  """))
          .andExpect (status ().isUnauthorized ());
    }
    // 第 11 次應 429
    mockMvc.perform (post ("/api/auth/login")
            .with (req -> { req.setRemoteAddr (ip); return req; })
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"nonexist","password":"wrong"}
                """))
        .andExpect (status ().isTooManyRequests ())
        .andExpect (jsonPath ("$.error.code").value ("RATE_LIMITED"))
        .andExpect (jsonPath ("$.requestId").isNotEmpty ())
        .andExpect (header ().string ("Retry-After", "60"));
  }

  @Test
  void 混合計數_login與register共享桶 () throws Exception {
    String ip = "10.0.0.2_" + System.nanoTime ();
    for (int i = 0; i < 5; i++) {
      mockMvc.perform (post ("/api/auth/login")
              .with (req -> { req.setRemoteAddr (ip); return req; })
              .contentType (MediaType.APPLICATION_JSON)
              .content ("""
                  {"username":"nonexist","password":"wrong"}
                  """))
          .andExpect (status ().isUnauthorized ());
    }
    for (int i = 0; i < 5; i++) {
      mockMvc.perform (post ("/api/auth/register")
              .with (req -> { req.setRemoteAddr (ip); return req; })
              .contentType (MediaType.APPLICATION_JSON)
              .content ("""
                  {"username":"rate_mix_%s_%d","displayName":"mix","password":"password123"}
                  """.formatted (System.nanoTime (), i)))
          .andExpect (status ().isCreated ());
    }
    // 第 11 次（register）應 429
    mockMvc.perform (post ("/api/auth/register")
            .with (req -> { req.setRemoteAddr (ip); return req; })
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"rate_mix_final_%s","displayName":"mix","password":"password123"}
                """.formatted (System.nanoTime ())))
        .andExpect (status ().isTooManyRequests ())
        .andExpect (jsonPath ("$.error.code").value ("RATE_LIMITED"));
  }

  @Test
  void 不同IP獨立計數 () throws Exception {
    String ipA = "10.0.0.10_" + System.nanoTime ();
    String ipB = "10.0.0.11_" + System.nanoTime ();
    for (int i = 0; i < 10; i++) {
      mockMvc.perform (post ("/api/auth/login")
              .with (req -> { req.setRemoteAddr (ipA); return req; })
              .contentType (MediaType.APPLICATION_JSON)
              .content ("""
                  {"username":"nonexist","password":"wrong"}
                  """))
          .andExpect (status ().isUnauthorized ());
    }
    // ipA 第 11 次 429
    mockMvc.perform (post ("/api/auth/login")
            .with (req -> { req.setRemoteAddr (ipA); return req; })
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"nonexist","password":"wrong"}
                """))
        .andExpect (status ().isTooManyRequests ());

    // ipB 仍放行
    mockMvc.perform (post ("/api/auth/login")
            .with (req -> { req.setRemoteAddr (ipB); return req; })
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"nonexist","password":"wrong"}
                """))
        .andExpect (status ().isUnauthorized ());
  }

  @Test
  void 非受限路徑不受限 () throws Exception {
    String ip = "10.0.0.99_" + System.nanoTime ();
    // 同一 IP 先打 10 次 login（達上限）
    for (int i = 0; i < 10; i++) {
      mockMvc.perform (post ("/api/auth/login")
              .with (req -> { req.setRemoteAddr (ip); return req; })
              .contentType (MediaType.APPLICATION_JSON)
              .content ("""
                  {"username":"nonexist","password":"wrong"}
                  """))
          .andExpect (status ().isUnauthorized ());
    }
    // GET /api/cases 需認證，未帶 token 應 401 而非 429（證明限流未攔截 GET）
    mockMvc.perform (org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get ("/api/cases")
            .with (req -> { req.setRemoteAddr (ip); return req; }))
        .andExpect (status ().isUnauthorized ())
        .andExpect (jsonPath ("$.error.code").value ("UNAUTHORIZED"));
    // 登入第 11 次仍 429
    mockMvc.perform (post ("/api/auth/login")
            .with (req -> { req.setRemoteAddr (ip); return req; })
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"nonexist","password":"wrong"}
                """))
        .andExpect (status ().isTooManyRequests ());
  }
}
