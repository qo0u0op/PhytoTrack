package com.d0w0b.phytotrack.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.d0w0b.phytotrack.dto.ReferenceDtos.ActiveUpdateRequest;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CityResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestTypeResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.UserRepository;
import com.d0w0b.phytotrack.security.UserPrincipal;
import com.d0w0b.phytotrack.service.IdentifierService;
import com.d0w0b.phytotrack.service.ReferenceDataService;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 參照資料控制器 (Reference Data Controller)
 *
 * 提供診斷表單所需的各類下拉選單資料，全部為唯讀。
 */
@RestController
@RequestMapping ("/api/ref")
public class ReferenceDataController {

  private final ReferenceDataService referenceDataService;
  private final IdentifierService identifierService;
  private final UserRepository userRepository;
  private final IdentifierRepository identifierRepository;

  public ReferenceDataController (ReferenceDataService referenceDataService,
      IdentifierService identifierService,
      UserRepository userRepository,
      @org.springframework.beans.factory.annotation.Autowired (required = false) IdentifierRepository identifierRepository) {
    this.referenceDataService = referenceDataService;
    this.identifierService = identifierService;
    this.userRepository = userRepository;
    this.identifierRepository = identifierRepository;
  }

  /** 作物分類 (含作物清單) */
  @GetMapping ("/crop-categories")
  public ResponseEntity<List<CropCategoryResponse>> cropCategories () {
    return ResponseEntity.ok (referenceDataService.cropCategories ());
  }

  /** 害物類型 (含小分類清單) */
  @GetMapping ("/pest-types")
  public ResponseEntity<List<PestTypeResponse>> pestTypes () {
    return ResponseEntity.ok (referenceDataService.pestTypes ());
  }

  /** 被害部位 */
  @GetMapping ("/damages")
  public ResponseEntity<List<IdNameResponse>> damages () {
    return ResponseEntity.ok (referenceDataService.damages ());
  }

  /** 防治建議 */
  @GetMapping ("/hints")
  public ResponseEntity<List<IdNameResponse>> hints () {
    return ResponseEntity.ok (referenceDataService.hints ());
  }

  /** 耕種方式 */
  @GetMapping ("/methods")
  public ResponseEntity<List<IdNameResponse>> methods () {
    return ResponseEntity.ok (referenceDataService.methods ());
  }

  /** 送件方式 */
  @GetMapping ("/deliveries")
  public ResponseEntity<List<IdNameResponse>> deliveries () {
    return ResponseEntity.ok (referenceDataService.deliveries ());
  }

  /** 服務類別 */
  @GetMapping ("/services")
  public ResponseEntity<List<IdNameResponse>> services () {
    return ResponseEntity.ok (referenceDataService.services ());
  }

  /** 縣市 (含鄉鎮市區清單) */
  @GetMapping ("/cities")
  public ResponseEntity<List<CityResponse>> cities () {
    return ResponseEntity.ok (referenceDataService.cities ());
  }

  /** 送件人身分別 */
  @GetMapping ("/sender-types")
  public ResponseEntity<List<IdNameResponse>> senderTypes () {
    return ResponseEntity.ok (referenceDataService.senderTypes ());
  }

  /** 診斷簽名人 */
  @GetMapping ("/identifiers")
  public ResponseEntity<List<com.d0w0b.phytotrack.dto.ReferenceDtos.IdentifierResponse>> identifiers (@RequestParam (defaultValue = "false") boolean includeInactive) {
    return ResponseEntity.ok (referenceDataService.identifiers (includeInactive));
  }

  /** 當前使用者簽名人（STAFF/ADMIN 自動確保） */
  @GetMapping ("/identifiers/me")
  @PreAuthorize ("hasAnyRole ('STAFF','ADMIN')")
  public ResponseEntity<IdNameResponse> myIdentifier (@AuthenticationPrincipal UserPrincipal principal) {
    User user = userRepository.findById (principal.getUserId ())
        .orElseThrow (() -> new ApiException ("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "使用者不存在"));
    List<Identifier> actives = identifierRepository.findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (user.getUserId ());
    if (!actives.isEmpty ()) {
      Identifier first = actives.get (0);
      return ResponseEntity.ok (new IdNameResponse (first.getIdentifierId (), first.getIdentifier ()));
    }
    List<Identifier> any = identifierRepository.findByUserUserId (user.getUserId ());
    if (!any.isEmpty ()) {
      throw new ApiException ("SIGNER_INACTIVE", HttpStatus.NOT_FOUND, "簽名人已停用，請聯繫管理員啟用或改個人檔案顯示名稱重建");
    }
    Identifier identifier = identifierService.ensureForUser (user);
    return ResponseEntity.ok (new IdNameResponse (identifier.getIdentifierId (), identifier.getIdentifier ()));
  }

  /** 停用/啟用簽名人（僅 ADMIN；STAFF 已禁用） */
  @PatchMapping ("/identifiers/{id}/active")
  @PreAuthorize ("hasRole ('ADMIN')")
  public ResponseEntity<IdNameResponse> updateMyIdentifierActive (@PathVariable Long id,
      @Valid @RequestBody ActiveUpdateRequest req,
      @AuthenticationPrincipal UserPrincipal principal) {
    Identifier e = identifierRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "簽名人不存在"));
    boolean isAdmin = principal.getAuthorities ().stream ().anyMatch (a -> a.getAuthority ().equals ("ROLE_ADMIN"));
    if (!isAdmin) {
      if (e.getUser () == null || !e.getUser ().getUserId ().equals (principal.getUserId ())) {
        throw new ApiException ("ACCESS_DENIED", HttpStatus.FORBIDDEN, "僅可停用自身簽名人");
      }
    } else {
      // ADMIN 不可停用自身簽名人（避免建案時 404）
      if (Boolean.FALSE.equals (req.active ()) && e.getUser () != null
          && e.getUser ().getUserId ().equals (principal.getUserId ())) {
        throw new ApiException ("SELF_DEACTIVATE_FORBIDDEN", HttpStatus.FORBIDDEN, "管理員不可停用自身簽名人");
      }
    }
    return ResponseEntity.ok (referenceDataService.updateIdentifierActive (id, req.active ()));
  }
}