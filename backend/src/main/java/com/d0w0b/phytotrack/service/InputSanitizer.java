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
      String msg = field + "不可包含 < 或 >";
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, msg,
          java.util.Map.of(field, msg));
    }
  }

  public static void assertNoHtml (String value, String fieldKey, String fieldLabel) {
    if (value != null && (value.contains ("<") || value.contains (">"))) {
      String msg = fieldLabel + "不可包含 < 或 >";
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, msg,
          java.util.Map.of(fieldKey, msg));
    }
  }

  public static void assertDisplayName (String displayName) {
    assertNoHtml (displayName, "displayName", "顯示名稱");
  }
}
