package com.d0w0b.phytotrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.AuthDtos.ActiveUpdateRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.ResetPasswordRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.RoleUpdateRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.service.AuthService;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 使用者管理控制器 (User Admin Controller)
 *
 * 僅管理者 (ADMIN) 可使用。目前提供使用者清單查詢；
 * 未來可延伸帳號啟停用、角色調整等功能。
 */
@RestController
@RequestMapping ("/api/admin/users")
public class UserAdminController {

  private final AuthService authService;

  public UserAdminController (AuthService authService) {
    this.authService = authService;
  }

  /** 列出所有使用者 */
  @GetMapping
  @PreAuthorize ("hasRole ('ADMIN')")
  public ResponseEntity<List<UserResponse>> listUsers () {
    return ResponseEntity.ok (authService.listUsers ());
  }

  /** 調整使用者角色 */
  @PatchMapping ("/{id}/role")
  @PreAuthorize ("hasRole ('ADMIN')")
  public ResponseEntity<UserResponse> updateRole (@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
    if (request.bindIdentifierId () != null || Boolean.TRUE.equals (request.force ())) {
      return ResponseEntity.ok (authService.updateRole (id, request));
    }
    return ResponseEntity.ok (authService.updateRole (id, request.role ()));
  }

  /** 啟停用帳號 */
  @PatchMapping ("/{id}/active")
  @PreAuthorize ("hasRole ('ADMIN')")
  public ResponseEntity<UserResponse> updateActive (@PathVariable Long id, @Valid @RequestBody ActiveUpdateRequest request) {
    return ResponseEntity.ok (authService.updateActive (id, request.active ()));
  }

  /** 重設使用者密碼 */
  @PostMapping ("/{id}/reset-password")
  @PreAuthorize ("hasRole ('ADMIN')")
  public ResponseEntity<Void> resetPassword (@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword (id, request.newPassword ());
    return ResponseEntity.noContent ().build ();
  }
}