package com.d0w0b.phytotrack.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.ReferenceDtos.ActiveUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.BindSignerRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CityCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CityUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.DistrictCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.DistrictUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestCategoryCreateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestCategoryUpdateRequest;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.security.UserPrincipal;
import com.d0w0b.phytotrack.service.IdentifierService;
import com.d0w0b.phytotrack.service.ReferenceDataService;

import jakarta.validation.Valid;

/**
 * 參照資料寫入控制器 (ADMIN 專用)
 *
 * 與唯讀的 ReferenceDataController 分離，寫入端點統一以 ADMIN 權限保護。
 */
@RestController
@RequestMapping ("/api/admin/ref")
@PreAuthorize ("hasRole ('ADMIN')")
public class ReferenceDataAdminController {

  private final ReferenceDataService service;
  private final IdentifierRepository identifierRepository;
  private final IdentifierService identifierService;

  public ReferenceDataAdminController (ReferenceDataService service,
      @org.springframework.beans.factory.annotation.Autowired (required = false) IdentifierRepository identifierRepository,
      @org.springframework.beans.factory.annotation.Autowired (required = false) IdentifierService identifierService) {
    this.service = service;
    this.identifierRepository = identifierRepository;
    this.identifierService = identifierService;
  }

  // ===== damages =====
  @PostMapping ("/damages")
  public ResponseEntity<IdNameResponse> createDamage (@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createDamage (req.name ()));
  }

  @PutMapping ("/damages/{id}")
  public ResponseEntity<IdNameResponse> updateDamage (@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok (service.updateDamage (id, req.name ()));
  }

  @DeleteMapping ("/damages/{id}")
  public ResponseEntity<Void> deleteDamage (@PathVariable Long id) {
    service.deleteDamage (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== hints =====
  @PostMapping ("/hints")
  public ResponseEntity<IdNameResponse> createHint (@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createHint (req.name ()));
  }

  @PutMapping ("/hints/{id}")
  public ResponseEntity<IdNameResponse> updateHint (@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok (service.updateHint (id, req.name ()));
  }

  @DeleteMapping ("/hints/{id}")
  public ResponseEntity<Void> deleteHint (@PathVariable Long id) {
    service.deleteHint (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== methods =====
  @PostMapping ("/methods")
  public ResponseEntity<IdNameResponse> createMethod (@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createMethod (req.name ()));
  }

  @PutMapping ("/methods/{id}")
  public ResponseEntity<IdNameResponse> updateMethod (@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok (service.updateMethod (id, req.name ()));
  }

  @DeleteMapping ("/methods/{id}")
  public ResponseEntity<Void> deleteMethod (@PathVariable Long id) {
    service.deleteMethod (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== deliveries =====
  @PostMapping ("/deliveries")
  public ResponseEntity<IdNameResponse> createDelivery (@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createDelivery (req.name ()));
  }

  @PutMapping ("/deliveries/{id}")
  public ResponseEntity<IdNameResponse> updateDelivery (@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok (service.updateDelivery (id, req.name ()));
  }

  @DeleteMapping ("/deliveries/{id}")
  public ResponseEntity<Void> deleteDelivery (@PathVariable Long id) {
    service.deleteDelivery (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== services =====
  @PostMapping ("/services")
  public ResponseEntity<IdNameResponse> createService (@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createService (req.name ()));
  }

  @PutMapping ("/services/{id}")
  public ResponseEntity<IdNameResponse> updateService (@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok (service.updateService (id, req.name ()));
  }

  @DeleteMapping ("/services/{id}")
  public ResponseEntity<Void> deleteService (@PathVariable Long id) {
    service.deleteService (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== identifiers =====
  @PostMapping ("/identifiers")
  @PreAuthorize ("hasAnyRole ('STAFF','ADMIN')")
  public ResponseEntity<IdNameResponse> createIdentifier (@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createIdentifier (req.name ()));
  }

  @PutMapping ("/identifiers/{id}")
  public ResponseEntity<IdNameResponse> updateIdentifier (@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok (service.updateIdentifier (id, req.name ()));
  }

  @DeleteMapping ("/identifiers/{id}")
  public ResponseEntity<Void> deleteIdentifier (@PathVariable Long id) {
    service.deleteIdentifier (id);
    return ResponseEntity.noContent ().build ();
  }

  @PatchMapping ("/identifiers/{id}/active")
  public ResponseEntity<IdNameResponse> updateIdentifierActive (@PathVariable Long id, @Valid @RequestBody ActiveUpdateRequest req,
      @org.springframework.security.core.annotation.AuthenticationPrincipal UserPrincipal principal) {
    if (Boolean.FALSE.equals (req.active ()) && principal != null && identifierRepository != null) {
      boolean isAdmin = principal.getAuthorities ().stream ().anyMatch (a -> a.getAuthority ().equals ("ROLE_ADMIN"));
      if (isAdmin) {
        var opt = identifierRepository.findById (id);
        if (opt.isPresent () && opt.get ().getUser () != null
            && opt.get ().getUser ().getUserId ().equals (principal.getUserId ())) {
          throw new ApiException ("SELF_DEACTIVATE_FORBIDDEN", HttpStatus.FORBIDDEN, "管理員不可停用自身簽名人");
        }
      }
    }
    return ResponseEntity.ok (service.updateIdentifierActive (id, req.active ()));
  }

  @PostMapping ("/identifiers/{id}/bind")
  public ResponseEntity<IdNameResponse> bindIdentifier (@PathVariable Long id, @Valid @RequestBody BindSignerRequest req) {
    Identifier bound = identifierService.bindToUser (id, req.userId ());
    return ResponseEntity.ok (new IdNameResponse (bound.getIdentifierId (), bound.getIdentifier ()));
  }

  // ===== sender-types =====
  @PostMapping ("/sender-types")
  public ResponseEntity<IdNameResponse> createSenderType (@Valid @RequestBody IdNameCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createSenderType (req.name ()));
  }

  @PutMapping ("/sender-types/{id}")
  public ResponseEntity<IdNameResponse> updateSenderType (@PathVariable Long id, @Valid @RequestBody IdNameUpdateRequest req) {
    return ResponseEntity.ok (service.updateSenderType (id, req.name ()));
  }

  @DeleteMapping ("/sender-types/{id}")
  public ResponseEntity<Void> deleteSenderType (@PathVariable Long id) {
    service.deleteSenderType (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== cities / districts =====
  @PostMapping ("/cities")
  public ResponseEntity<IdNameResponse> createCity (@Valid @RequestBody CityCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createCity (req.name ()));
  }

  @PutMapping ("/cities/{id}")
  public ResponseEntity<IdNameResponse> updateCity (@PathVariable Long id, @Valid @RequestBody CityUpdateRequest req) {
    return ResponseEntity.ok (service.updateCity (id, req.name ()));
  }

  @DeleteMapping ("/cities/{id}")
  public ResponseEntity<Void> deleteCity (@PathVariable Long id) {
    service.deleteCity (id);
    return ResponseEntity.noContent ().build ();
  }

  @PostMapping ("/districts")
  public ResponseEntity<IdNameResponse> createDistrict (@Valid @RequestBody DistrictCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createDistrict (req.name (), req.cityId ()));
  }

  @PutMapping ("/districts/{id}")
  public ResponseEntity<IdNameResponse> updateDistrict (@PathVariable Long id, @Valid @RequestBody DistrictUpdateRequest req) {
    return ResponseEntity.ok (service.updateDistrict (id, req.name (), req.cityId ()));
  }

  @DeleteMapping ("/districts/{id}")
  public ResponseEntity<Void> deleteDistrict (@PathVariable Long id) {
    service.deleteDistrict (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== crops =====
  @PostMapping ("/crops")
  @PreAuthorize ("hasAnyRole ('STAFF','ADMIN')")
  public ResponseEntity<IdNameResponse> createCrop (@Valid @RequestBody CropCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createCrop (req.name (), req.cropCategoryId ()));
  }

  @PutMapping ("/crops/{id}")
  @PreAuthorize ("hasAnyRole ('STAFF','ADMIN')")
  public ResponseEntity<IdNameResponse> updateCrop (@PathVariable Long id, @Valid @RequestBody CropUpdateRequest req) {
    return ResponseEntity.ok (service.updateCrop (id, req.name (), req.cropCategoryId ()));
  }

  @DeleteMapping ("/crops/{id}")
  public ResponseEntity<Void> deleteCrop (@PathVariable Long id) {
    service.deleteCrop (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== crop-categories =====
  @PostMapping ("/crop-categories")
  public ResponseEntity<IdNameResponse> createCropCategory (@Valid @RequestBody CropCategoryCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED).body (service.createCropCategory (req.name ()));
  }

  @PutMapping ("/crop-categories/{id}")
  public ResponseEntity<IdNameResponse> updateCropCategory (@PathVariable Long id, @Valid @RequestBody CropCategoryUpdateRequest req) {
    return ResponseEntity.ok (service.updateCropCategory (id, req.name ()));
  }

  @DeleteMapping ("/crop-categories/{id}")
  public ResponseEntity<Void> deleteCropCategory (@PathVariable Long id) {
    service.deleteCropCategory (id);
    return ResponseEntity.noContent ().build ();
  }

  // ===== pest-categories =====
  @PostMapping ("/pest-categories")
  public ResponseEntity<IdNameResponse> createPestCategory (@Valid @RequestBody PestCategoryCreateRequest req) {
    return ResponseEntity.status (HttpStatus.CREATED)
        .body (service.createPestCategory (req.code (), req.name (), req.pestTypeId ()));
  }

  @PutMapping ("/pest-categories/{id}")
  public ResponseEntity<IdNameResponse> updatePestCategory (@PathVariable Long id, @Valid @RequestBody PestCategoryUpdateRequest req) {
    return ResponseEntity.ok (service.updatePestCategory (id, req.code (), req.name (), req.pestTypeId ()));
  }

  @DeleteMapping ("/pest-categories/{id}")
  public ResponseEntity<Void> deletePestCategory (@PathVariable Long id) {
    service.deletePestCategory (id);
    return ResponseEntity.noContent ().build ();
  }
}
