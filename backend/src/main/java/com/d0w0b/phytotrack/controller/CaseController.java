package com.d0w0b.phytotrack.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import com.d0w0b.phytotrack.dto.CaseDtos.CaseCreateRequest;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseFilter;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseSummaryResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseUpdateRequest;
import com.d0w0b.phytotrack.dto.StatisticsDtos.CaseStatisticsResponse;
import com.d0w0b.phytotrack.service.CaseService;

import java.time.LocalDate;

/**
 * 案件控制器（Case Controller）
 *
 * 權限設計（RBAC，見 ADR-004）：
 *   - 列表 / 詳細：登入即可（VIEWER / STAFF / ADMIN）
 *   - 建立 / 更新：僅診斷員與管理者（STAFF / ADMIN）
 *   - 刪除：僅管理者（ADMIN）
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

  private final CaseService caseService;

  public CaseController(CaseService caseService) {
    this.caseService = caseService;
  }

  /**
   * 分頁查詢案件列表，預設依收件日期遞減。
   *
   * 篩選參數皆可選：cropId、serviceId、senderName（部分比對）、
   * receiveDateFrom、receiveDateTo、status（PENDING/RESOLVED/CLOSED）；
   * 多個參數同時存在時以 AND 組合。
   */
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Page<CaseSummaryResponse>> list(
      @RequestParam(required = false) Long cropId,
      @RequestParam(required = false) Long serviceId,
      @RequestParam(required = false) String senderName,
      @RequestParam(required = false) LocalDate receiveDateFrom,
      @RequestParam(required = false) LocalDate receiveDateTo,
      @RequestParam(required = false) String status,
      @PageableDefault(size = 20, sort = "receiveDate", direction = Sort.Direction.DESC) Pageable pageable) {
    CaseFilter filter = new CaseFilter(cropId, serviceId, senderName,
        receiveDateFrom, receiveDateTo, status);
    return ResponseEntity.ok(caseService.list(filter, pageable));
  }

  /** 案件統計總覽（登入即可，見 spec case-statistics） */
  @GetMapping("/statistics")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<CaseStatisticsResponse> statistics() {
    return ResponseEntity.ok(caseService.statistics());
  }

  /** 查詢案件詳細 */
  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<CaseResponse> detail(@PathVariable Long id) {
    return ResponseEntity.ok(caseService.detail(id));
  }

  /** 建立案件 */
  @PostMapping
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<CaseResponse> create(@Valid @RequestBody CaseCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(caseService.create(request));
  }

  /** 更新案件 */
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
  public ResponseEntity<CaseResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody CaseUpdateRequest request) {
    return ResponseEntity.ok(caseService.update(id, request));
  }

  /** 刪除案件（限管理者） */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    caseService.delete(id);
    return ResponseEntity.noContent().build();
  }
}