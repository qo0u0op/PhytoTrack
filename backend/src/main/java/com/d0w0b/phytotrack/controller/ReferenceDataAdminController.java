package com.d0w0b.phytotrack.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestCategoryCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestCategoryUpdateRequest;
import com.d0w0b.phytotrack.service.ReferenceDataService;

import jakarta.validation.Valid;

/**
 * 參照資料寫入控制器（ADMIN 專用）
 *
 * 與唯讀的 ReferenceDataController 分離，寫入端點統一以 ADMIN 權限保護。
 */
@RestController
@RequestMapping("/api/admin/ref")
@PreAuthorize("hasRole('ADMIN')")
public class ReferenceDataAdminController {

  private final ReferenceDataService service;

  public ReferenceDataAdminController(ReferenceDataService service) {
    this.service = service;
  }

  // ===== damages =====
  @PostMapping("/damages")
  public ResponseEntity<IdNameResponse> createDamage(@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createDamage(req.name()));
  }

  @PutMapping("/damages/{id}")
  public ResponseEntity<IdNameResponse> updateDamage(@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok(service.updateDamage(id, req.name()));
  }

  @DeleteMapping("/damages/{id}")
  public ResponseEntity<Void> deleteDamage(@PathVariable Long id) {
    service.deleteDamage(id);
    return ResponseEntity.noContent().build();
  }

  // ===== hints =====
  @PostMapping("/hints")
  public ResponseEntity<IdNameResponse> createHint(@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createHint(req.name()));
  }

  @PutMapping("/hints/{id}")
  public ResponseEntity<IdNameResponse> updateHint(@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok(service.updateHint(id, req.name()));
  }

  @DeleteMapping("/hints/{id}")
  public ResponseEntity<Void> deleteHint(@PathVariable Long id) {
    service.deleteHint(id);
    return ResponseEntity.noContent().build();
  }

  // ===== methods =====
  @PostMapping("/methods")
  public ResponseEntity<IdNameResponse> createMethod(@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createMethod(req.name()));
  }

  @PutMapping("/methods/{id}")
  public ResponseEntity<IdNameResponse> updateMethod(@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok(service.updateMethod(id, req.name()));
  }

  @DeleteMapping("/methods/{id}")
  public ResponseEntity<Void> deleteMethod(@PathVariable Long id) {
    service.deleteMethod(id);
    return ResponseEntity.noContent().build();
  }

  // ===== deliveries =====
  @PostMapping("/deliveries")
  public ResponseEntity<IdNameResponse> createDelivery(@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createDelivery(req.name()));
  }

  @PutMapping("/deliveries/{id}")
  public ResponseEntity<IdNameResponse> updateDelivery(@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok(service.updateDelivery(id, req.name()));
  }

  @DeleteMapping("/deliveries/{id}")
  public ResponseEntity<Void> deleteDelivery(@PathVariable Long id) {
    service.deleteDelivery(id);
    return ResponseEntity.noContent().build();
  }

  // ===== services =====
  @PostMapping("/services")
  public ResponseEntity<IdNameResponse> createService(@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createService(req.name()));
  }

  @PutMapping("/services/{id}")
  public ResponseEntity<IdNameResponse> updateService(@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok(service.updateService(id, req.name()));
  }

  @DeleteMapping("/services/{id}")
  public ResponseEntity<Void> deleteService(@PathVariable Long id) {
    service.deleteService(id);
    return ResponseEntity.noContent().build();
  }

  // ===== identifiers =====
  @PostMapping("/identifiers")
  public ResponseEntity<IdNameResponse> createIdentifier(@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createIdentifier(req.name()));
  }

  @PutMapping("/identifiers/{id}")
  public ResponseEntity<IdNameResponse> updateIdentifier(@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok(service.updateIdentifier(id, req.name()));
  }

  @DeleteMapping("/identifiers/{id}")
  public ResponseEntity<Void> deleteIdentifier(@PathVariable Long id) {
    service.deleteIdentifier(id);
    return ResponseEntity.noContent().build();
  }

  // ===== sender-types =====
  @PostMapping("/sender-types")
  public ResponseEntity<IdNameResponse> createSenderType(@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createSenderType(req.name()));
  }

  @PutMapping("/sender-types/{id}")
  public ResponseEntity<IdNameResponse> updateSenderType(@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok(service.updateSenderType(id, req.name()));
  }

  @DeleteMapping("/sender-types/{id}")
  public ResponseEntity<Void> deleteSenderType(@PathVariable Long id) {
    service.deleteSenderType(id);
    return ResponseEntity.noContent().build();
  }

  // ===== crops =====
  @PostMapping("/crops")
  public ResponseEntity<IdNameResponse> createCrop(@Valid @RequestBody CropCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createCrop(req.name(), req.cropCategoryId()));
  }

  @PutMapping("/crops/{id}")
  public ResponseEntity<IdNameResponse> updateCrop(@PathVariable Long id, @Valid @RequestBody CropUpdateRequest req) {
    return ResponseEntity.ok(service.updateCrop(id, req.name(), req.cropCategoryId()));
  }

  @DeleteMapping("/crops/{id}")
  public ResponseEntity<Void> deleteCrop(@PathVariable Long id) {
    service.deleteCrop(id);
    return ResponseEntity.noContent().build();
  }

  // ===== crop-categories =====
  @PostMapping("/crop-categories")
  public ResponseEntity<IdNameResponse> createCropCategory(@Valid @RequestBody CropCategoryCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createCropCategory(req.name()));
  }

  @PutMapping("/crop-categories/{id}")
  public ResponseEntity<IdNameResponse> updateCropCategory(@PathVariable Long id, @Valid @RequestBody CropCategoryUpdateRequest req) {
    return ResponseEntity.ok(service.updateCropCategory(id, req.name()));
  }

  @DeleteMapping("/crop-categories/{id}")
  public ResponseEntity<Void> deleteCropCategory(@PathVariable Long id) {
    service.deleteCropCategory(id);
    return ResponseEntity.noContent().build();
  }

  // ===== pest-categories =====
  @PostMapping("/pest-categories")
  public ResponseEntity<IdNameResponse> createPestCategory(@Valid @RequestBody PestCategoryCreateRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createPestCategory(req.code(), req.name(), req.pestTypeId(), req.sortOrder()));
  }

  @PutMapping("/pest-categories/{id}")
  public ResponseEntity<IdNameResponse> updatePestCategory(@PathVariable Long id, @Valid @RequestBody PestCategoryUpdateRequest req) {
    return ResponseEntity.ok(service.updatePestCategory(id, req.code(), req.name(), req.pestTypeId(), req.sortOrder()));
  }

  @DeleteMapping("/pest-categories/{id}")
  public ResponseEntity<Void> deletePestCategory(@PathVariable Long id) {
    service.deletePestCategory(id);
    return ResponseEntity.noContent().build();
  }
}
