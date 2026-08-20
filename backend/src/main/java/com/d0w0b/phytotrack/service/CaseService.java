package com.d0w0b.phytotrack.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.dto.CaseDtos.CaseCreateRequest;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseFilter;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseSummaryResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseUpdateRequest;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Case;
import com.d0w0b.phytotrack.models.CaseDamage;
import com.d0w0b.phytotrack.models.CaseHint;
import com.d0w0b.phytotrack.models.CaseIdentifier;
import com.d0w0b.phytotrack.models.CasePestCategory;
import com.d0w0b.phytotrack.models.CaseStatus;
import com.d0w0b.phytotrack.models.Crop;
import com.d0w0b.phytotrack.models.Damage;
import com.d0w0b.phytotrack.models.Delivery;
import com.d0w0b.phytotrack.models.District;
import com.d0w0b.phytotrack.models.Hint;
import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.Method;
import com.d0w0b.phytotrack.models.PestCategory;
import com.d0w0b.phytotrack.models.Sender;
import com.d0w0b.phytotrack.models.SenderType;
import com.d0w0b.phytotrack.repository.CaseRepository;
import com.d0w0b.phytotrack.repository.CaseSpecifications;
import com.d0w0b.phytotrack.repository.CropRepository;
import com.d0w0b.phytotrack.repository.DamageRepository;
import com.d0w0b.phytotrack.repository.DeliveryRepository;
import com.d0w0b.phytotrack.repository.DistrictRepository;
import com.d0w0b.phytotrack.repository.HintRepository;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.MethodRepository;
import com.d0w0b.phytotrack.repository.PestCategoryRepository;
import com.d0w0b.phytotrack.repository.SenderRepository;
import com.d0w0b.phytotrack.repository.SenderTypeRepository;
import com.d0w0b.phytotrack.repository.ServiceRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 案件服務（Case Service）：診斷案件的商業邏輯
 *
 * 職責：
 *   - 建立案件（含送件人、參照資料關聯、多對多關聯）
 *   - 分頁列表與詳細查詢
 *   - 更新與刪除
 *
 * 設計重點：
 *   - 讀取方法標記 @Transactional(readOnly = true)，讓 Lazy 關聯能在交易內取用
 *   - 寫入方法為單一交易：任一環節失敗即全部回滾（Rollback），確保資料一致
 */
@Service
public class CaseService {

  private final CaseRepository caseRepository;
  private final SenderRepository senderRepository;
  private final SenderTypeRepository senderTypeRepository;
  private final DistrictRepository districtRepository;
  private final MethodRepository methodRepository;
  private final CropRepository cropRepository;
  private final ServiceRepository serviceRepository;
  private final DeliveryRepository deliveryRepository;
  private final DamageRepository damageRepository;
  private final HintRepository hintRepository;
  private final PestCategoryRepository pestCategoryRepository;
  private final IdentifierRepository identifierRepository;

  public CaseService(CaseRepository caseRepository,
                     SenderRepository senderRepository,
                     SenderTypeRepository senderTypeRepository,
                     DistrictRepository districtRepository,
                     MethodRepository methodRepository,
                     CropRepository cropRepository,
                     ServiceRepository serviceRepository,
                     DeliveryRepository deliveryRepository,
                     DamageRepository damageRepository,
                     HintRepository hintRepository,
                     PestCategoryRepository pestCategoryRepository,
                     IdentifierRepository identifierRepository) {
    this.caseRepository = caseRepository;
    this.senderRepository = senderRepository;
    this.senderTypeRepository = senderTypeRepository;
    this.districtRepository = districtRepository;
    this.methodRepository = methodRepository;
    this.cropRepository = cropRepository;
    this.serviceRepository = serviceRepository;
    this.deliveryRepository = deliveryRepository;
    this.damageRepository = damageRepository;
    this.hintRepository = hintRepository;
    this.pestCategoryRepository = pestCategoryRepository;
    this.identifierRepository = identifierRepository;
  }

  /** 分頁查詢案件清單（摘要）；無任何條件時行為與現有列表一致 */
  @Transactional(readOnly = true)
  public Page<CaseSummaryResponse> list(CaseFilter filter, Pageable pageable) {
    // 狀態字串先在此解析（fail-fast）：非法值於 Service 層即拋錯，不會進入查詢
    CaseStatus status = filter.status() != null ? parseStatus(filter.status()) : null;
    if (filter.isEmpty()) {
      return caseRepository.findAll(pageable).map(this::toSummary);
    }
    return caseRepository.findAll(CaseSpecifications.build(filter, status), pageable)
        .map(this::toSummary);
  }

  /** 列舉字串解析為 CaseStatus（fail-fast）；非法值拋 400 INVALID_STATUS */
  private static CaseStatus parseStatus(String status) {
    try {
      return CaseStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
      throw new ApiException(
          "INVALID_STATUS", HttpStatus.BAD_REQUEST, "無效的狀態：" + status);
    }
  }

  /** 查詢案件詳細（含所有多對多關聯） */
  @Transactional(readOnly = true)
  public CaseResponse detail(Long id) {
    return toDetail(findByIdOrThrow(id));
  }

  /** 建立案件 */
  @Transactional
  public CaseResponse create(CaseCreateRequest request) {
    Case caseEntity = new Case();
    caseEntity.setReceiveDate(request.receiveDate());
    caseEntity.setCropScale(request.cropScale());
    caseEntity.setDamageScale(request.damageScale());
    caseEntity.setPestDescription(request.pestDescription());
    caseEntity.setHintDescription(request.hintDescription());
    // 新案件一律從待處理（PENDING）開始
    caseEntity.setStatus(CaseStatus.PENDING);

    caseEntity.setSender(findOrCreateSender(request));
    caseEntity.setMethod(getRef(methodRepository, request.methodId(), "耕種方式"));
    caseEntity.setCrop(getRef(cropRepository, request.cropId(), "作物"));
    caseEntity.setService(getRef(serviceRepository, request.serviceId(), "服務類別"));
    caseEntity.setDelivery(getRef(deliveryRepository, request.deliverId(), "送件方式"));

    addDamages(caseEntity, request.damageIds());
    addHints(caseEntity, request.hintIds());
    addPestCategories(caseEntity, request.pestCategoryIds());
    addIdentifiers(caseEntity, request.identifierIds());

    caseRepository.save(caseEntity);
    return toDetail(caseEntity);
  }

  /** 更新案件（僅更新有提供的欄位）；狀態變更需符合轉移規則（見 CaseStatus） */
  @Transactional
  public CaseResponse update(Long id, CaseUpdateRequest request) {
    Case caseEntity = findByIdOrThrow(id);

    // 已結案案件：僅管理者可修改內容欄位（狀態同值為合法 no-op，狀態轉移另由規則把關）
    if (caseEntity.getStatus() == CaseStatus.CLOSED && !isAdmin() && hasContentUpdate(request)) {
      throw new ApiException("CLOSED_CASE_READONLY", HttpStatus.FORBIDDEN,
          "案件已結案，僅管理者可修改內容");
    }

    if (request.receiveDate() != null) {
      caseEntity.setReceiveDate(request.receiveDate());
    }
    if (request.cropScale() != null) {
      caseEntity.setCropScale(request.cropScale());
    }
    if (request.damageScale() != null) {
      caseEntity.setDamageScale(request.damageScale());
    }
    if (request.pestDescription() != null) {
      caseEntity.setPestDescription(request.pestDescription());
    }
    if (request.hintDescription() != null) {
      caseEntity.setHintDescription(request.hintDescription());
    }
    if (request.status() != null) {
      applyStatusTransition(caseEntity, parseStatus(request.status()));
    }
    if (request.methodId() != null) {
      caseEntity.setMethod(getRef(methodRepository, request.methodId(), "耕種方式"));
    }
    if (request.cropId() != null) {
      caseEntity.setCrop(getRef(cropRepository, request.cropId(), "作物"));
    }
    if (request.serviceId() != null) {
      caseEntity.setService(getRef(serviceRepository, request.serviceId(), "服務類別"));
    }
    if (request.deliverId() != null) {
      caseEntity.setDelivery(getRef(deliveryRepository, request.deliverId(), "送件方式"));
    }
    applySenderUpdate(caseEntity, request);
    replaceJunctions(caseEntity, request);

    return toDetail(caseEntity);
  }

  /** 依轉移規則更新狀態；非法轉移拋 400 且狀態不變 */
  private void applyStatusTransition(Case caseEntity, CaseStatus target) {
    CaseStatus current = caseEntity.getStatus();
    if (target == current) {
      return;
    }
    if (current == CaseStatus.PENDING && target == CaseStatus.RESOLVED) {
      // STAFF/ADMIN：update 端點已限制角色
      caseEntity.setStatus(target);
      return;
    }
    if (current == CaseStatus.RESOLVED && target == CaseStatus.CLOSED) {
      if (!isAdmin()) {
        throw new ApiException("STATUS_TRANSITION_FORBIDDEN", HttpStatus.FORBIDDEN,
            "僅管理者可將案件標記為已結案");
      }
      caseEntity.setStatus(target);
      return;
    }
    throw new ApiException("INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST,
        "非法的狀態轉移：" + current + " → " + target);
  }

  /** 目前登入者是否為 ADMIN（用於 RESOLVED → CLOSED 的轉移授權） */
  private boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
  }

  /** 請求是否帶有任何「非狀態」欄位（用於 CLOSED 案件的管理者限改判斷） */
  private boolean hasContentUpdate(CaseUpdateRequest request) {
    return request.receiveDate() != null
        || request.cropScale() != null
        || request.damageScale() != null
        || request.pestDescription() != null
        || request.hintDescription() != null
        || request.methodId() != null
        || request.cropId() != null
        || request.serviceId() != null
        || request.deliverId() != null
        || request.senderName() != null
        || request.senderPhone() != null
        || request.senderAddress() != null
        || request.senderDistrictId() != null
        || request.senderTypeId() != null
        || request.damageIds() != null
        || request.hintIds() != null
        || request.pestCategoryIds() != null
        || request.identifierIds() != null;
  }

  /**
   * 更新送件人：任一送件人欄位提供時生效。
   *
   * 目標身分以「有提供的 name/phone」為準，未提供則沿用現送件人身分。
   * 依目標身分尋找既有送件人並關聯之（與 create 的 findOrCreateSender
   * 相同去重語意）；找不到才建立新送件人。如此不會修改可能被其他案件
   * 共享的既有 Sender row，也避免將 name/phone 改成與其他送件人重複而
   * 撞 UNIQUE(name, phone) 回 500。
   */
  private void applySenderUpdate(Case caseEntity, CaseUpdateRequest request) {
    boolean anyProvided = request.senderName() != null || request.senderPhone() != null
        || request.senderAddress() != null || request.senderDistrictId() != null
        || request.senderTypeId() != null;
    if (!anyProvided) {
      return;
    }
    Sender current = caseEntity.getSender();
    String name = request.senderName() != null ? request.senderName() : current.getName();
    String phone = request.senderPhone() != null ? request.senderPhone() : current.getPhone();
    Sender sender = senderRepository.findByNameAndPhone(name, phone)
        .orElseGet(() -> {
          Sender created = new Sender();
          created.setName(name);
          created.setPhone(phone);
          return senderRepository.save(created);
        });
    if (request.senderAddress() != null) {
      sender.setAddress(request.senderAddress());
    }
    if (request.senderDistrictId() != null) {
      sender.setDistrict(getRef(districtRepository, request.senderDistrictId(), "鄉鎮市區"));
    }
    if (request.senderTypeId() != null) {
      sender.setSenderType(getRef(senderTypeRepository, request.senderTypeId(), "身分別"));
    }
    caseEntity.setSender(sender);
  }

  /**
   * 整組替換多對多關聯。
   *
   * 採「差集」語意而非 clear + 重建：只刪除不在目標集合的既有 junction、
   * 只新增目標集合缺少的 junction。原因是 Hibernate flush 時 INSERT 會先於
   * DELETE 執行，若 clear 後對相同 (case_id, ref_id) 重新 insert 會撞
   * SQLite UNIQUE 約束。差集法使「刪除」與「新增」沒有交集，最終結果仍等於
   * 以 ids 整組替換。
   */
  private void replaceJunctions(Case caseEntity, CaseUpdateRequest request) {
    if (request.damageIds() != null) {
      replaceJunctionGroup(caseEntity, caseEntity.getCaseDamages(), request.damageIds(),
          j -> j.getDamage().getDamageId(),
          (c, id) -> {
            CaseDamage junction = new CaseDamage();
            junction.setCaseEntity(c);
            junction.setDamage(getRef(damageRepository, id, "被害部位"));
            return junction;
          });
    }
    if (request.hintIds() != null) {
      replaceJunctionGroup(caseEntity, caseEntity.getCaseHints(), request.hintIds(),
          j -> j.getHint().getHintId(),
          (c, id) -> {
            CaseHint junction = new CaseHint();
            junction.setCaseEntity(c);
            junction.setHint(getRef(hintRepository, id, "防治建議"));
            return junction;
          });
    }
    if (request.pestCategoryIds() != null) {
      replaceJunctionGroup(caseEntity, caseEntity.getCasePestCategories(),
          request.pestCategoryIds(), j -> j.getPestCategory().getPestCategoryId(),
          (c, id) -> {
            CasePestCategory junction = new CasePestCategory();
            junction.setCaseEntity(c);
            junction.setPestCategory(getRef(pestCategoryRepository, id, "病蟲害分類"));
            return junction;
          });
    }
    if (request.identifierIds() != null) {
      replaceJunctionGroup(caseEntity, caseEntity.getCaseIdentifiers(),
          request.identifierIds(), j -> j.getIdentifier().getIdentifierId(),
          (c, id) -> {
            CaseIdentifier junction = new CaseIdentifier();
            junction.setCaseEntity(c);
            junction.setIdentifier(getRef(identifierRepository, id, "診斷簽名人"));
            return junction;
          });
    }
  }

  /** 建立 Junction 的工廠（泛型化四組多對多） */
  @FunctionalInterface
  private interface JunctionFactory<J> {
    J create(Case caseEntity, Long refId);
  }

  /** 差集式整組替換：刪目標外的既有 junction、補目標缺少的 junction（ids 以 Set 去重，避免重複 id 建立重複 junction） */
  private <J> void replaceJunctionGroup(Case caseEntity, List<J> junctions, List<Long> ids,
      Function<J, Long> idGetter, JunctionFactory<J> factory) {
    Set<Long> target = new HashSet<>(ids);
    List<J> toRemove = junctions.stream()
        .filter(j -> !target.contains(idGetter.apply(j)))
        .toList();
    toRemove.forEach(junctions::remove);
    Set<Long> have = junctions.stream().map(idGetter).collect(Collectors.toSet());
    for (Long refId : target) {
      if (!have.contains(refId)) {
        junctions.add(factory.create(caseEntity, refId));
      }
    }
  }

  /** 刪除案件（多對多關聯以 Cascade 一併刪除） */
  @Transactional
  public void delete(Long id) {
    caseRepository.delete(findByIdOrThrow(id));
  }

  // ------------------------------------------------------------------
  // 私有輔助方法
  // ------------------------------------------------------------------

  /** 依姓名 + 電話尋找既有送件人，否則建立新送件人 */
  private Sender findOrCreateSender(CaseCreateRequest request) {
    return senderRepository.findByNameAndPhone(request.senderName(), request.senderPhone())
        .orElseGet(() -> {
          Sender sender = new Sender();
          sender.setName(request.senderName());
          sender.setPhone(request.senderPhone());
          sender.setAddress(request.senderAddress());
          sender.setDistrict(getRef(districtRepository, request.senderDistrictId(), "鄉鎮市區"));
          sender.setSenderType(getRef(senderTypeRepository, request.senderTypeId(), "身分別"));
          return senderRepository.save(sender);
        });
  }

  /** 建立案件的多對多關聯（Junction Record）：被害部位 */
  private void addDamages(Case caseEntity, List<Long> damageIds) {
    if (damageIds == null) {
      return;
    }
    for (Long damageId : damageIds) {
      Damage damage = getRef(damageRepository, damageId, "被害部位");
      CaseDamage junction = new CaseDamage();
      junction.setCaseEntity(caseEntity);
      junction.setDamage(damage);
      caseEntity.getCaseDamages().add(junction);
    }
  }

  /** 建立案件的多對多關聯（Junction Record）：防治建議 */
  private void addHints(Case caseEntity, List<Long> hintIds) {
    if (hintIds == null) {
      return;
    }
    for (Long hintId : hintIds) {
      Hint hint = getRef(hintRepository, hintId, "防治建議");
      CaseHint junction = new CaseHint();
      junction.setCaseEntity(caseEntity);
      junction.setHint(hint);
      caseEntity.getCaseHints().add(junction);
    }
  }

  /** 建立案件的多對多關聯（Junction Record）：病蟲害分類 */
  private void addPestCategories(Case caseEntity, List<Long> pestCategoryIds) {
    if (pestCategoryIds == null) {
      return;
    }
    for (Long pestCategoryId : pestCategoryIds) {
      PestCategory category = getRef(pestCategoryRepository, pestCategoryId, "病蟲害分類");
      CasePestCategory junction = new CasePestCategory();
      junction.setCaseEntity(caseEntity);
      junction.setPestCategory(category);
      caseEntity.getCasePestCategories().add(junction);
    }
  }

  /** 建立案件的多對多關聯（Junction Record）：診斷簽名人 */
  private void addIdentifiers(Case caseEntity, List<Long> identifierIds) {
    if (identifierIds == null) {
      return;
    }
    for (Long identifierId : identifierIds) {
      Identifier identifier = getRef(identifierRepository, identifierId, "診斷簽名人");
      CaseIdentifier junction = new CaseIdentifier();
      junction.setCaseEntity(caseEntity);
      junction.setIdentifier(identifier);
      caseEntity.getCaseIdentifiers().add(junction);
    }
  }

  /** 依 ID 查詢參照資料，不存在則拋出業務例外 */
  private <T> T getRef(JpaRepository<T, Long> repository, Long id, String label) {
    return repository.findById(id)
        .orElseThrow(() -> new ApiException(
            "REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, label + "不存在（ID：" + id + "）"));
  }

  private Case findByIdOrThrow(Long id) {
    return caseRepository.findById(id)
        .orElseThrow(() -> new ApiException("CASE_NOT_FOUND", HttpStatus.NOT_FOUND, "案件不存在"));
  }

  /** 轉換為摘要回應 */
  private CaseSummaryResponse toSummary(Case caseEntity) {
    return new CaseSummaryResponse(
        caseEntity.getCaseId(),
        caseEntity.getReceiveDate(),
        caseEntity.getCrop().getCrop(),
        caseEntity.getSender().getName(),
        caseEntity.getService().getService(),
        caseEntity.getStatus().name(),
        caseEntity.getCreatedAt());
  }

  /** 轉換為詳細回應（於交易內取用 Lazy 關聯） */
  private CaseResponse toDetail(Case caseEntity) {
    List<CaseResponse.IdName> damages = caseEntity.getCaseDamages().stream()
        .map(j -> new CaseResponse.IdName(j.getDamage().getDamageId(), j.getDamage().getDamage()))
        .collect(Collectors.toList());
    List<CaseResponse.IdName> hints = caseEntity.getCaseHints().stream()
        .map(j -> new CaseResponse.IdName(j.getHint().getHintId(), j.getHint().getHint()))
        .collect(Collectors.toList());
    List<CaseResponse.IdName> pestCategories = caseEntity.getCasePestCategories().stream()
        .map(j -> new CaseResponse.IdName(
            j.getPestCategory().getPestCategoryId(), j.getPestCategory().getPestCategory()))
        .collect(Collectors.toList());
    List<CaseResponse.IdName> identifiers = caseEntity.getCaseIdentifiers().stream()
        .map(j -> new CaseResponse.IdName(
            j.getIdentifier().getIdentifierId(), j.getIdentifier().getIdentifier()))
        .collect(Collectors.toList());

    // 送件人鄉鎮/身分別可能未設定（如更新時僅換新身分未帶 district/type）
    Long senderDistrictId = Optional.ofNullable(caseEntity.getSender().getDistrict())
        .map(District::getDistrictId).orElse(null);
    Long senderTypeId = Optional.ofNullable(caseEntity.getSender().getSenderType())
        .map(SenderType::getSenderTypeId).orElse(null);

    return new CaseResponse(
        caseEntity.getCaseId(),
        caseEntity.getReceiveDate(),
        caseEntity.getCropScale(),
        caseEntity.getDamageScale(),
        caseEntity.getPestDescription(),
        caseEntity.getHintDescription(),
        caseEntity.getStatus().name(),
        caseEntity.getCreatedAt(),
        caseEntity.getUpdatedAt(),
        caseEntity.getSender().getName(),
        caseEntity.getSender().getPhone(),
        caseEntity.getSender().getAddress(),
        senderDistrictId,
        senderTypeId,
        caseEntity.getCrop().getCrop(),
        caseEntity.getMethod().getMethod(),
        caseEntity.getService().getService(),
        caseEntity.getDelivery().getDeliver(),
        caseEntity.getCreatedBy().getDisplayName(),
        damages,
        hints,
        pestCategories,
        identifiers);
  }
}