package com.d0w0b.phytotrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.service.AuthService;

import java.util.List;

/**
 * 使用者管理控制器（User Admin Controller）
 *
 * 僅管理者（ADMIN）可使用。目前提供使用者清單查詢；
 * 未來可延伸帳號啟停用、角色調整等功能。
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

  private final AuthService authService;

  public UserAdminController(AuthService authService) {
    this.authService = authService;
  }

  /** 列出所有使用者 */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<UserResponse>> listUsers() {
    return ResponseEntity.ok(authService.listUsers());
  }
}