package com.d0w0b.phytotrack.service;

import org.springframework.http.HttpStatus;
import com.d0w0b.phytotrack.exception.ApiException;

/**
 * 輸入消毒工具：防止 Stored XSS
 * 阻擋 < > 字元，避免 <script> 等標籤被持久化
 */
public final class InputSanitizer {
  private InputSanitizer () {}

  public static void assertNoHtml (String value, String field) {
    if (value != null && (value.contains ("<") || value.contains (">"))) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, field + "不可包含 < 或 >");
    }
  }

  public static void assertDisplayName (String displayName) {
    assertNoHtml (displayName, "顯示名稱");
  }
}
