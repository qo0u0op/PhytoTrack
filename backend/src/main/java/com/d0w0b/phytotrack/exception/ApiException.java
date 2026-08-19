package com.d0w0b.phytotrack.exception;

import org.springframework.http.HttpStatus;

/**
 * 業務例外（Business Exception）基底類別
 *
 * Service 層在遇到「有業務語意的錯誤」時拋出此類例外，
 * 由 GlobalExceptionHandler 統一回應給前端。
 */
public class ApiException extends RuntimeException {

  /** 對外的錯誤代碼（如 USERNAME_TAKEN） */
  private final String code;

  /** 對應的 HTTP 狀態碼（HTTP Status） */
  private final HttpStatus status;

  public ApiException(String code, HttpStatus status, String message) {
    super(message);
    this.code = code;
    this.status = status;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
