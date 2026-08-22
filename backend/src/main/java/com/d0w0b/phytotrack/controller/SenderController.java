package com.d0w0b.phytotrack.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.SenderDtos.SenderResponse;
import com.d0w0b.phytotrack.dto.SenderDtos.SenderUpsertRequest;
import com.d0w0b.phytotrack.service.SenderService;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 送件人控制器（Sender Controller）
 */
@RestController
@RequestMapping("/api/senders")
public class SenderController {

  private final SenderService senderService;

  public SenderController(SenderService senderService) {
    this.senderService = senderService;
  }

  /** 搜尋送件人候選（登入即可，供建案去重） */
  @GetMapping("/search")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<SenderResponse>> search(@RequestParam String q) {
    return ResponseEntity.ok(senderService.search(q));
  }

  /** 列表（登入即可，ADMIN 管理頁亦用） */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<SenderResponse>> list() {
    return ResponseEntity.ok(senderService.list());
  }

  /** 詳細 */
  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<SenderResponse> detail(@PathVariable Long id) {
    return ResponseEntity.ok(senderService.detail(id));
  }

  /** 建立（STAFF+） */
  @PostMapping
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  public ResponseEntity<SenderResponse> create(@Valid @RequestBody SenderUpsertRequest request) {
    SenderResponse response = senderService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** 編輯（STAFF+） */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  public ResponseEntity<SenderResponse> update(
      @PathVariable Long id, @Valid @RequestBody SenderUpsertRequest request) {
    return ResponseEntity.ok(senderService.update(id, request));
  }

  /** 硬刪除（僅 ADMIN，被引用時 409） */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    senderService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
