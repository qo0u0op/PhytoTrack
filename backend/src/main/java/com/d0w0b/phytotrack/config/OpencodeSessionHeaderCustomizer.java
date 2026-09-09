package com.d0w0b.phytotrack.config;

import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * OpenCode Go 要求每個對話帶穩定的 x-opencode-session 以利路由與 prompt 快取
 * 參考 https://opencode.ai/docs/zh-tw/go/#where-can-i-use-it
 * 僅對 opencode.ai 的請求注入，local 的 llama-server 不受影響
 */
@Component
public class OpencodeSessionHeaderCustomizer implements OpenAiHttpClientBuilderCustomizer {

  @Override
  public void customize (SpringAiOpenAiHttpClient.Builder builder) {
    builder.interceptor (chain -> {
      var request = chain.request ();
      String host = request.url ().host ();
      if (host != null && host.contains ("opencode.ai")) {
        var newRequest = request.newBuilder ()
            .header ("x-opencode-session", UUID.randomUUID ().toString ())
            .header ("User-Agent", "PhytoTrack/1.0 (opencode-go; mimo-v2.5)")
            .build ();
        return chain.proceed (newRequest);
      }
      return chain.proceed (request);
    });
  }
}
