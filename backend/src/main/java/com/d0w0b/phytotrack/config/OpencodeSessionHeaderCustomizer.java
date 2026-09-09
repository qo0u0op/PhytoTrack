package com.d0w0b.phytotrack.config;

import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * OpenCode Go 要求每個對話帶穩定的 x-opencode-session 以利路由與 prompt 快取
 * 參考 https://opencode.ai/docs/zh-tw/go/#where-can-i-use-it
 */
@Component
public class OpencodeSessionHeaderCustomizer implements org.springframework.boot.restclient.RestClientCustomizer {

  @Override
  public void customize (RestClient.Builder builder) {
    builder.requestInterceptor (wrapWithSessionHeader ());
  }

  private ClientHttpRequestInterceptor wrapWithSessionHeader () {
    return (request, body, execution) -> {
      // 僅對 opencode.ai 的請求加 header，避免污染其他 RestClient 調用
      String host = request.getURI ().getHost ();
      if (host != null && host.contains ("opencode.ai")) {
        // 每個請求以 UUID 作為 session，若需跨多次 analyze 保持同一 session，可改為基於 caseId+userId 的穩定值
        String sessionId = UUID.randomUUID ().toString ();
        request.getHeaders ().add ("x-opencode-session", sessionId);
        // 依文件建議帶自訂 UA，避免被視為通用 SDK
        String ua = request.getHeaders ().getFirst ("User-Agent");
        if (ua == null || ua.contains ("RestClient")) {
          request.getHeaders ().set ("User-Agent", "PhytoTrack/1.0 (opencode-go; mimo-v2.5)");
        }
      }
      return execution.execute (request, body);
    };
  }
}
