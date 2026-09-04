package com.d0w0b.phytotrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccountDtos {
  private AccountDtos () {}

  public record UpdateProfileRequest (
      @NotBlank(message = "顯示名稱不可為空白")
      @Size(max = 50, message = "顯示名稱不可超過 50 字元")
      @Pattern(regexp = "^[^<>]*$", message = "顯示名稱不可包含 < 或 >")
      String displayName,

      @Email(message = "電子信箱格式不正確")
      String email) {}

  public record ChangePasswordRequest (
      String currentPassword,

      @NotBlank(message = "新密碼不可為空白")
      @Size(min = 6, max = 72, message = "密碼長度需介於 6 到 72 之間")
      String newPassword) {}

  public record DeactivateRequestResponse (Long requestId, Long userId, String username, String status, String createdAt, String reviewedBy) {}
}
