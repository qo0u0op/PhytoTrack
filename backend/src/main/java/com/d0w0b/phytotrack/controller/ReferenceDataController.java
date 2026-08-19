package com.d0w0b.phytotrack.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.ReferenceDtos.CityResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestTypeResponse;
import com.d0w0b.phytotrack.service.ReferenceDataService;

import java.util.List;

/**
 * 參照資料控制器（Reference Data Controller）
 *
 * 提供診斷表單所需的各類下拉選單資料，全部為唯讀。
 */
@RestController
@RequestMapping("/api/ref")
public class ReferenceDataController {

  private final ReferenceDataService referenceDataService;

  public ReferenceDataController(ReferenceDataService referenceDataService) {
    this.referenceDataService = referenceDataService;
  }

  /** 作物分類（含作物清單） */
  @GetMapping("/crop-categories")
  public ResponseEntity<List<CropCategoryResponse>> cropCategories() {
    return ResponseEntity.ok(referenceDataService.cropCategories());
  }

  /** 害物類型（含小分類清單） */
  @GetMapping("/pest-types")
  public ResponseEntity<List<PestTypeResponse>> pestTypes() {
    return ResponseEntity.ok(referenceDataService.pestTypes());
  }

  /** 被害部位 */
  @GetMapping("/damages")
  public ResponseEntity<List<IdNameResponse>> damages() {
    return ResponseEntity.ok(referenceDataService.damages());
  }

  /** 防治建議 */
  @GetMapping("/hints")
  public ResponseEntity<List<IdNameResponse>> hints() {
    return ResponseEntity.ok(referenceDataService.hints());
  }

  /** 耕種方式 */
  @GetMapping("/methods")
  public ResponseEntity<List<IdNameResponse>> methods() {
    return ResponseEntity.ok(referenceDataService.methods());
  }

  /** 送件方式 */
  @GetMapping("/deliveries")
  public ResponseEntity<List<IdNameResponse>> deliveries() {
    return ResponseEntity.ok(referenceDataService.deliveries());
  }

  /** 服務類別 */
  @GetMapping("/services")
  public ResponseEntity<List<IdNameResponse>> services() {
    return ResponseEntity.ok(referenceDataService.services());
  }

  /** 縣市（含鄉鎮市區清單） */
  @GetMapping("/cities")
  public ResponseEntity<List<CityResponse>> cities() {
    return ResponseEntity.ok(referenceDataService.cities());
  }

  /** 送件人身分別 */
  @GetMapping("/sender-types")
  public ResponseEntity<List<IdNameResponse>> senderTypes() {
    return ResponseEntity.ok(referenceDataService.senderTypes());
  }

  /** 診斷簽名人 */
  @GetMapping("/identifiers")
  public ResponseEntity<List<IdNameResponse>> identifiers() {
    return ResponseEntity.ok(referenceDataService.identifiers());
  }
}