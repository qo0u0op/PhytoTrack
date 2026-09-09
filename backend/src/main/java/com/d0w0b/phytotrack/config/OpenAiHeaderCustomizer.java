package com.d0w0b.phytotrack.config;

import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI 相容 Header 注入（通用）
 * 讀取 ai.headers.*（phytotrack.toml 的 [ai.headers]），"auto" 時每次產生 UUID
 * 適用於 OpenCode Go 的 x-opencode-session 等 provider 專屬標頭，Anthropic 另起獨立 Customizer
 */
@Component
public class OpenAiHeaderCustomizer implements OpenAiHttpClientBuilderCustomizer {

  private final Environment env;

  public OpenAiHeaderCustomizer (Environment env) {
    this.env = env;
  }

  @Override
  public void customize (SpringAiOpenAiHttpClient.Builder builder) {
    builder.interceptor (chain -> {
      var request = chain.request ();
      Map<String, String> headers = new HashMap<> ();
      try {
        Map<String, String> configured = Binder.get (env).bind ("ai.headers", Map.class).orElse (Map.of ());
        // Binder with raw Map needs cast, fallback to manual read if empty
        if (configured.isEmpty ()) {
          configured = new HashMap<> ();
          for (String key : new String[]{"x-opencode-session", "User-Agent"}) {
            String v = env.getProperty ("ai.headers." + key);
            if (v != null) configured.put (key, v);
          }
        }
        headers.putAll (configured);
      } catch (Exception ignored) {}

      if (headers.isEmpty ()) return chain.proceed (request);

      var builderReq = request.newBuilder ();
      for (Map.Entry<String, String> e : headers.entrySet ()) {
        String v = e.getValue ();
        if ("auto".equalsIgnoreCase (v)) v = UUID.randomUUID ().toString ();
        builderReq.header (e.getKey (), v);
      }
      // 若未配置 UA，補預設
      if (!headers.containsKey ("User-Agent")) {
        builderReq.header ("User-Agent", "PhytoTrack/1.0 (openai-compatible)");
      }
      return chain.proceed (builderReq.build ());
    });
  }
}
