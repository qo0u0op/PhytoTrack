package com.d0w0b.phytotrack.config;

import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI 相容 Header 注入（通用）
 * 讀取 ai.headers.*（phytotrack.toml 的 [ai.headers]），"auto" 時每次產生 UUID
 * 任意鍵皆透傳，適用於 OpenCode Go 的 x-opencode-session 等 provider 專屬標頭
 */
@Component
public class OpenAiHeaderCustomizer implements OpenAiHttpClientBuilderCustomizer {

  private final AiHeadersProperties headersProperties;

  public OpenAiHeaderCustomizer (AiHeadersProperties headersProperties) {
    this.headersProperties = headersProperties;
  }

  @Override
  public void customize (SpringAiOpenAiHttpClient.Builder builder) {
    builder.interceptor (chain -> {
      var request = chain.request ();
      String host = request.url ().host ();
      boolean isOpencode = host != null && host.contains ("opencode.ai");
      Map<String, String> headers = new HashMap<> (headersProperties.getHeaders ());

      // opencode.ai 必須帶 x-opencode-session，未配置時自動補 auto
      if (isOpencode && !headers.containsKey ("x-opencode-session")) {
        headers.put ("x-opencode-session", "auto");
      }
      if (headers.isEmpty ()) return chain.proceed (request);

      var builderReq = request.newBuilder ();
      for (Map.Entry<String, String> e : headers.entrySet ()) {
        String v = e.getValue ();
        if ("auto".equalsIgnoreCase (v)) v = UUID.randomUUID ().toString ();
        builderReq.header (e.getKey (), v);
      }
      if (!headers.containsKey ("User-Agent")) {
        builderReq.header ("User-Agent", "PhytoTrack/1.0 (openai-compatible)");
      }
      return chain.proceed (builderReq.build ());
    });
  }
}
