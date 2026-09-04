package com.d0w0b.phytotrack.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.d0w0b.phytotrack.config.RequestIdFilter;

import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 全域例外處理器 (Global Exception Handler)
 *
 * Controller 不需要各自 try-catch，所有例外統一在此轉換為 ErrorResponse (見 ADR-010)。
 * 設計要點：
 *   - 業務例外 (ApiException) 依其 code/status 回應，並記錄 log (含 requestId)
 *   - 參數驗證失敗 (Bean Validation) 收集所有欄位錯誤至 details
 *   - 未預期的例外回應 500，細節只進日誌 (Log)，不洩漏給前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger (GlobalExceptionHandler.class);

  /** 業務例外 */
  @ExceptionHandler (ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException (ApiException ex, HttpServletRequest request) {
    log.warn ("業務錯誤 [{}] {} {}：{}",
        ex.getCode (), request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build (ex.getCode (), ex.getMessage (), ex.getStatus (), request, ex.getDetails ());
  }

  /** Bean Validation 參數驗證失敗 */
  @ExceptionHandler (MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation (MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, Object> details = new HashMap<>();
    for (FieldError error : ex.getBindingResult ().getFieldErrors ()) {
      details.put (error.getField (), error.getDefaultMessage ());
    }
    String message = details.entrySet ().stream ()
        .map (entry -> entry.getKey () + ": " + entry.getValue ())
        .collect (Collectors.joining ("; "));
    log.warn ("參數驗證失敗 {} {}：{}",
        request.getMethod (), request.getRequestURI (), message);
    return build ("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST, request, details);
  }

  /** 請求參數型別轉換失敗 (如日期格式錯誤、數字非數字) */
  @ExceptionHandler (MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch (MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    String message = "參數 " + ex.getName () + " 格式錯誤：" + ex.getValue ();
    log.warn ("參數型別錯誤 {} {}：{}", request.getMethod (), request.getRequestURI (), message);
    return build ("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST, request, null);
  }

  /** 登入失敗 (帳號或密碼錯誤) */
  @ExceptionHandler (BadCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleBadCredentials (BadCredentialsException ex, HttpServletRequest request) {
    log.warn ("登入失敗 {} {}：{}", request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build ("INVALID_CREDENTIALS", "帳號或密碼錯誤", HttpStatus.UNAUTHORIZED, request, null);
  }

  /** 帳號已停用 (CustomUserDetailsService 的 isEnabled==false 觸發) */
  @ExceptionHandler (DisabledException.class)
  public ResponseEntity<ErrorResponse> handleDisabled (DisabledException ex, HttpServletRequest request) {
    log.warn ("帳號已停用 {} {}：{}", request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build ("ACCOUNT_DISABLED", "帳號已停用", HttpStatus.FORBIDDEN, request, null);
  }

  /** 權限不足 */
  @ExceptionHandler (AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied (AccessDeniedException ex, HttpServletRequest request) {
    log.warn ("權限不足 {} {}：{}", request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build ("ACCESS_DENIED", "權限不足", HttpStatus.FORBIDDEN, request, null);
  }

  /** 404：靜態資源或未知路徑（避免落入 500） */
  @ExceptionHandler (NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResource (NoResourceFoundException ex, HttpServletRequest request) {
    log.warn ("資源不存在 {} {}：{}", request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build ("NOT_FOUND", "資源不存在", HttpStatus.NOT_FOUND, request, null);
  }

  /** 405：方法不允許 */
  @ExceptionHandler (HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported (HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    log.warn ("方法不允許 {} {}：{}", request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build ("METHOD_NOT_ALLOWED", "方法不允許", HttpStatus.METHOD_NOT_ALLOWED, request, null);
  }

  /** 415：不支援的媒體類型 */
  @ExceptionHandler (HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported (HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    log.warn ("不支援的媒體類型 {} {}：{}", request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build ("UNSUPPORTED_MEDIA_TYPE", "不支援的媒體類型", HttpStatus.UNSUPPORTED_MEDIA_TYPE, request, null);
  }

  /** 400：JSON 解析失敗 / 請求體不可讀 */
  @ExceptionHandler (HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleNotReadable (HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.warn ("請求體不可讀 {} {}：{}", request.getMethod (), request.getRequestURI (), ex.getMessage ());
    return build ("VALIDATION_ERROR", "請求格式錯誤", HttpStatus.BAD_REQUEST, request, null);
  }

  /** 未預期的系統例外 (500) */
  @ExceptionHandler (Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected (Exception ex, HttpServletRequest request) {
    // 細節只進日誌 (含 requestId)，不回應給前端
    log.error ("未預期的錯誤：{} {}", request.getMethod (), request.getRequestURI (), ex);
    return build ("INTERNAL_ERROR", "系統發生錯誤，請稍後再試",
        HttpStatus.INTERNAL_SERVER_ERROR, request, null);
  }

  private ResponseEntity<ErrorResponse> build (String code, String message, HttpStatus status, HttpServletRequest request,
      Map<String, Object> details) {
    // requestId 由 RequestIdFilter 寫入 MDC，此處讀取以與伺服器 log 對照
    String requestId = MDC.get (RequestIdFilter.MDC_REQUEST_ID);
    if (requestId == null) {
      requestId = UUID.randomUUID ().toString ();
    }
    return ResponseEntity.status (status).body (ErrorResponse.of (code, message, requestId, details));
  }
}
