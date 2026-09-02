package com.d0w0b.phytotrack.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.AuthDtos.AuthResponse;
import com.d0w0b.phytotrack.dto.AuthDtos.LoginRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.RegisterRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.security.UserPrincipal;
import com.d0w0b.phytotrack.service.AccountService;
import com.d0w0b.phytotrack.service.AuthService;

/**
 * 認證控制器 (Authentication Controller)：註冊、登入、目前使用者
 *
 * 注意：JWT 為無狀態 (Stateless) 驗證，登出只需前端移除 token，
 * 因此 /logout 僅回傳成功訊息，不需伺服器狀態。
 */
@RestController
@RequestMapping ("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final AccountService accountService;

  public AuthController (AuthService authService, AccountService accountService) {
    this.authService = authService;
    this.accountService = accountService;
  }

  /** 註冊新使用者 (公開端點) */
  @PostMapping ("/register")
  public ResponseEntity<UserResponse> register (@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status (HttpStatus.CREATED).body (authService.register (request));
  }

  /** 登入並取得 JWT (公開端點) */
  @PostMapping ("/login")
  public ResponseEntity<AuthResponse> login (@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok (authService.login (request));
  }

  /** 查詢目前登入者資訊 */
  @PostMapping ("/me")
  public ResponseEntity<UserResponse> me (@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok (authService.me (principal));
  }

  /** 登出 (無狀態 JWT：前端丟棄 token 即完成) */
  @PostMapping ("/logout")
  public ResponseEntity<Void> logout () {
    return ResponseEntity.noContent ().build ();
  }

  /** 放棄停用申請（未登入時，憑帳密取消待審核請求） */
  @PostMapping ("/abandon-deactivate")
  public ResponseEntity<Void> abandonDeactivate (@RequestBody LoginRequest request) {
    accountService.abandonDeactivateByUsername (request.username (), request.password ());
    return ResponseEntity.noContent ().build ();
  }
}