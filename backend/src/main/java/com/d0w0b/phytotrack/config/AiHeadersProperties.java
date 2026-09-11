package com.d0w0b.phytotrack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 透傳標頭配置：綁定 ai.headers.*（phytotrack.toml 的 [ai.headers]）
 * 任意鍵皆透傳，值為 "auto" 時每次請求產生 UUID
 */
@Component
@ConfigurationProperties (prefix = "ai")
public class AiHeadersProperties {

  private Map<String, String> headers = new HashMap<> ();

  public Map<String, String> getHeaders () {
    return headers;
  }

  public void setHeaders (Map<String, String> headers) {
    this.headers = headers != null ? headers : new HashMap<> ();
  }
}
