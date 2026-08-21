package com.d0w0b.phytotrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 認證相關的資料傳輸物件（DTO，Data Transfer Object）
 *
 * record 為不可變（Immutable）物件，天然適合當作 API 邊界的請求/回應載體。
 * 使用 DTO 而非直接綁定 Entity，可防止 Mass Assignment 提權（見 ADR-005）。
 */
public final class AuthDtos {

  private AuthDtos() {
  }

  /** 註冊請求：只允許這幾個欄位進入系統 */
  public record RegisterRequest(
      @NotBlank(message = "帳號不可為空白")
      @Size(min = 3, max = 30, message = "帳號長度需介於 3 到 30 之間")
      String username,

      @NotBlank(message = "顯示名稱不可為空白")
      @Size(max = 50, message = "顯示名稱不可超過 50 字元")
      String displayName,

      @NotBlank(message = "密碼不可為空白")
      @Size(min = 6, max = 72, message = "密碼長度需介於 6 到 72 之間")
      String password,

      @Email(message = "電子信箱格式不正確")
      String email) {
  }

  /** 登入請求 */
  public record LoginRequest(
      @NotBlank(message = "帳號不可為空白") String username,
      @NotBlank(message = "密碼不可為空白") String password) {
  }

  /** 登入成功回應：攜帶 JWT 與使用者資訊 */
  public record AuthResponse(String token, UserResponse user) {
  }

  /** 使用者資訊回應：回傳安全欄位，不包含密碼 */
  public record UserResponse(
      Long userId,
      String username,
      String displayName,
      String email,
      String role,
      boolean active) {
  }

  /** 管理者調整角色請求 */
  public record RoleUpdateRequest(
      @NotBlank(message = "角色不可為空白")
      String role) {
  }

  /** 管理者啟停用帳號請求 */
  public record ActiveUpdateRequest(
      @NotNull(message = "啟用狀態不可為空")
      Boolean active) {
  }

  /** 管理者重設密碼請求 */
  public record ResetPasswordRequest(
      @NotBlank(message = "新密碼不可為空白")
      @Size(min = 6, max = 72, message = "密碼長度需介於 6 到 72 之間")
      String newPassword) {
  }
}