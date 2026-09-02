package com.d0w0b.phytotrack.controller;

import com.d0w0b.phytotrack.dto.AccountDtos.ChangePasswordRequest;
import com.d0w0b.phytotrack.dto.AccountDtos.DeactivateRequestResponse;
import com.d0w0b.phytotrack.dto.AccountDtos.UpdateProfileRequest;
import com.d0w0b.phytotrack.dto.AuthDtos.UserResponse;
import com.d0w0b.phytotrack.security.UserPrincipal;
import com.d0w0b.phytotrack.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final AccountService accountService;

  public AccountController (AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public ResponseEntity<UserResponse> getProfile (@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok (accountService.getProfile (principal));
  }

  @PutMapping("/profile")
  public ResponseEntity<UserResponse> updateProfile (@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody UpdateProfileRequest request) {
    return ResponseEntity.ok (accountService.updateProfile (principal, request.displayName (), request.email ()));
  }

  @PutMapping("/password")
  public ResponseEntity<Void> changePassword (@AuthenticationPrincipal UserPrincipal principal,
                                              @Valid @RequestBody ChangePasswordRequest request) {
    accountService.changePassword (principal, request.currentPassword (), request.newPassword ());
    return ResponseEntity.noContent ().build ();
  }

  @GetMapping("/check-email")
  public ResponseEntity<java.util.Map<String, Object>> checkEmail (@AuthenticationPrincipal UserPrincipal principal,
                                                                   @RequestParam String email) {
    boolean available = accountService.isEmailAvailable (email, principal.getUserId ());
    return ResponseEntity.ok (java.util.Map.of ("available", available, "email", email));
  }

  @PostMapping("/deactivate-request")
  public ResponseEntity<DeactivateRequestResponse> requestDeactivate (@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.status (201).body (accountService.requestDeactivate (principal));
  }

  @GetMapping("/deactivate-request")
  public ResponseEntity<DeactivateRequestResponse> getMyPending (@AuthenticationPrincipal UserPrincipal principal) {
    var pending = accountService.getMyPending (principal);
    if (pending == null) return ResponseEntity.noContent ().build ();
    return ResponseEntity.ok (pending);
  }

  @DeleteMapping("/deactivate-request")
  public ResponseEntity<Void> cancelDeactivate (@AuthenticationPrincipal UserPrincipal principal) {
    accountService.cancelDeactivate (principal);
    return ResponseEntity.noContent ().build ();
  }
}
