package com.d0w0b.phytotrack.controller;

import com.d0w0b.phytotrack.dto.AccountDtos.DeactivateRequestResponse;
import com.d0w0b.phytotrack.security.UserPrincipal;
import com.d0w0b.phytotrack.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/deactivate-requests")
@PreAuthorize("hasRole('ADMIN')")
public class DeactivateRequestAdminController {

  private final AccountService accountService;

  public DeactivateRequestAdminController (AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public ResponseEntity<List<DeactivateRequestResponse>> listRequests () {
    return ResponseEntity.ok (accountService.listRequests ());
  }

  @PutMapping("/{id}")
  public ResponseEntity<DeactivateRequestResponse> reviewRequest (@PathVariable Long id,
                                                                  @RequestBody Map<String, String> body,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
    String status = body.getOrDefault ("status", body.get ("action"));
    return ResponseEntity.ok (accountService.reviewRequest (id, status, principal));
  }
}
