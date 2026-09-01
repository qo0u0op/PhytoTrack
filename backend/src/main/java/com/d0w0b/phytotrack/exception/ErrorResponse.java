package com.d0w0b.phytotrack.exception;

import java.util.Map;

/**
 * 統一的錯誤回應格式 (Error Response)
 *
 * 所有錯誤都以相同形狀回傳，前端 axios 攔截器只需解析一種格式 (見 ADR-010)。
 *
 * 範例：
 * {
 *   "error": {
 *     "code": "USERNAME_TAKEN",
 *     "message": "帳號已存在",
 *     "details": { "username": "admin" }
 *   },
 *   "requestId": "req-abc-123"
 * }
 *
 * 設計重點：
 *   - requestId 由 RequestIdFilter 產生並寫入 MDC，此處讀取回傳，與伺服器 log 對照
 *   - details 於無補充資訊時為空物件
 */
public record ErrorResponse (ErrorBody error,
    String requestId) {

  public record ErrorBody (String code, String message, Map<String, Object> details) {
  }

  public static ErrorResponse of (String code, String message, String requestId) {
    return new ErrorResponse (new ErrorBody (code, message, Map.of ()), requestId);
  }

  public static ErrorResponse of (String code, String message, String requestId, Map<String, Object> details) {
    return new ErrorResponse (new ErrorBody (code, message, details == null ? Map.of () : details), requestId);
  }
}
