package com.d0w0b.phytotrack.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import com.d0w0b.phytotrack.dto.StatisticsDtos.CaseStatisticsResponse;
import com.d0w0b.phytotrack.dto.StatisticsDtos.CountName;
import com.d0w0b.phytotrack.dto.StatisticsDtos.MonthCount;
import com.d0w0b.phytotrack.dto.StatisticsDtos.StatusCount;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Case;
import com.d0w0b.phytotrack.models.CaseDamage;
import com.d0w0b.phytotrack.models.CaseHint;
import com.d0w0b.phytotrack.models.CaseIdentifier;
import com.d0w0b.phytotrack.models.CasePestCategory;
import com.d0w0b.phytotrack.models.CaseStatus;
import com.d0w0b.phytotrack.models.Crop;
import com.d0w0b.phytotrack.models.Damage;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import com.d0w0b.phytotrack.models.Delivery;
import com.d0w0b.phytotrack.models.City;
import com.d0w0b.phytotrack.models.District;
import com.d0w0b.phytotrack.models.Hint;
import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.Method;
import com.d0w0b.phytotrack.models.PestCategory;
import com.d0w0b.phytotrack.models.PestType;
import com.d0w0b.phytotrack.models.Sender;
import com.d0w0b.phytotrack.models.SenderType;
import com.d0w0b.phytotrack.repository.CaseRepository;
import com.d0w0b.phytotrack.repository.CaseSearchViewRepository;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 案件服務 (Case Service)：診斷案件的商業邏輯
 *
 * 職責：
 *   - 建立案件 (含送件人、參照資料關聯、多對多關聯)
 *   - 分頁列表與詳細查詢
 *   - 更新與刪除
 *
 * 設計重點：
 *   - 讀取方法標記 @Transactional (readOnly = true)，讓 Lazy 關聯能在交易內取用
 *   - 寫入方法為單一交易：任一環節失敗即全部回滾 (Rollback)，確保資料一致
 */
@Service
public class CaseService {

  private final CaseRepository caseRepository;
  private final CaseSearchViewRepository caseSearchViewRepository;
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

  public CaseService (CaseRepository caseRepository,
                     CaseSearchViewRepository caseSearchViewRepository,
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
    this.caseSearchViewRepository = caseSearchViewRepository;
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

  /** 分頁查詢案件清單 (摘要)；經視圖 `v_case_search` 篩選後回補實體以保留遮蔽 */
  @Transactional (readOnly = true)
  public Page<CaseSummaryResponse> list (CaseFilter filter, Pageable pageable) {
    CaseStatus status = filter.status () != null ? parseStatus (filter.status ()) : null;
    if (filter.isEmpty ()) {
      return caseRepository.findAll (pageable).map (this::toSummary);
    }
    Page<com.d0w0b.phytotrack.models.CaseSearchView> viewPage =
        caseSearchViewRepository.findAll (CaseSpecifications.buildView (filter, status), pageable);
    List<Long> ids = viewPage.getContent ().stream ().map (com.d0w0b.phytotrack.models.CaseSearchView::getCaseId).toList ();
    if (ids.isEmpty ()) {
      return new org.springframework.data.domain.PageImpl<>(List.of (), pageable, viewPage.getTotalElements ());
    }
    Map<Long, Case> caseMap = caseRepository.findAllById (ids).stream ()
        .collect (Collectors.toMap (Case::getCaseId, Function.identity ()));
    List<CaseSummaryResponse> content = ids.stream ()
        .map (caseMap::get).filter (java.util.Objects::nonNull).map (this::toSummary).toList ();
    return new org.springframework.data.domain.PageImpl<>(content, pageable, viewPage.getTotalElements ());
  }

  /** 列舉字串解析為 CaseStatus (fail-fast)；非法值拋 400 INVALID_STATUS */
  private static CaseStatus parseStatus (String status) {
    try {
      return CaseStatus.valueOf (status);
    } catch (IllegalArgumentException e) {
      throw new ApiException ("INVALID_STATUS", HttpStatus.BAD_REQUEST, "無效的狀態：" + status);
    }
  }

  /** 查詢案件詳細 (含所有多對多關聯) */
  @Transactional (readOnly = true)
  public CaseResponse detail (Long id) {
    return toDetail (findByIdOrThrow (id));
  }

  /** 建立案件 */
  @Transactional
  public CaseResponse create (CaseCreateRequest request) {
    // 先驗證所有參照，避免 sender 已落庫後才因其他參照不存在而回滾前的非原子中間狀態 (ACID)
    if (request.senderId () != null) {
      senderRepository.findById (request.senderId ())
          .orElseThrow (() -> new ApiException ("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
    } else {
      // 新建送件人時預檢 district / senderType
      getRef (districtRepository, request.senderDistrictId (), "鄉鎮市區");
      getRef (senderTypeRepository, request.senderTypeId (), "身分別");
      String phone = request.senderPhone ();
      String displayName = request.senderDisplayName ();
      boolean hasPhone = phone != null && !phone.isBlank ();
      boolean hasDisplay = displayName != null && !displayName.isBlank ();
      if (!hasPhone && !hasDisplay) {
        throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "電話與顯示名稱至少需提供一項");
      }
    }
    getRef (methodRepository, request.methodId (), "耕種方式");
    getRef (cropRepository, request.cropId (), "作物");
    getRef (serviceRepository, request.serviceId (), "服務類別");
    getRef (deliveryRepository, request.deliverId (), "送件方式");
    if (request.fieldDistrictId () == null) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "田區位置不可為空");
    }
    getRef (districtRepository, request.fieldDistrictId (), "田區位置");
    if (request.damageIds () != null) {
      for (Long id : new HashSet<>(request.damageIds ())) {
        getRef (damageRepository, id, "被害部位");
      }
    }
    if (request.hintIds () != null) {
      for (Long id : new HashSet<>(request.hintIds ())) {
        getRef (hintRepository, id, "防治建議");
      }
    }
    if (request.pestCategoryWithNotes () != null) {
      for (var note : request.pestCategoryWithNotes ()) {
        getRef (pestCategoryRepository, note.pestCategoryId (), "病蟲害分類");
      }
    } else if (request.pestCategoryIds () != null) {
      for (Long id : new HashSet<>(request.pestCategoryIds ())) {
        getRef (pestCategoryRepository, id, "病蟲害分類");
      }
    }
    if (request.identifierIds () != null) {
      for (Long id : new HashSet<>(request.identifierIds ())) {
        getRef (identifierRepository, id, "診斷簽名人");
      }
    }

    Case caseEntity = new Case ();
    caseEntity.setReceiveDate (request.receiveDate ());
    caseEntity.setCropScale (request.cropScale ());
    caseEntity.setDamageScale (request.damageScale ());
    caseEntity.setCaseDescription (request.caseDescription ());
    caseEntity.setHintDescription (request.hintDescription ());
    // 新案件一律從待處理 (PENDING) 開始
    caseEntity.setStatus (CaseStatus.PENDING);

    caseEntity.setSender (findOrCreateSender (request));
    caseEntity.setMethod (getRef (methodRepository, request.methodId (), "耕種方式"));
    caseEntity.setCrop (getRef (cropRepository, request.cropId (), "作物"));
    caseEntity.setService (getRef (serviceRepository, request.serviceId (), "服務類別"));
    caseEntity.setDelivery (getRef (deliveryRepository, request.deliverId (), "送件方式"));
    caseEntity.setFieldDistrict (getRef (districtRepository, request.fieldDistrictId (), "田區位置"));

    addDamages (caseEntity, request.damageIds ());
    addHints (caseEntity, request.hintIds ());
    if (request.pestCategoryWithNotes () != null) {
      for (var note : request.pestCategoryWithNotes ()) {
        CasePestCategory j = new CasePestCategory ();
        j.setCaseEntity (caseEntity);
        j.setPestCategory (getRef (pestCategoryRepository, note.pestCategoryId (), "病蟲害分類"));
        j.setPestNote (note.pestNote ());
        caseEntity.getCasePestCategories ().add (j);
      }
    } else {
      addPestCategories (caseEntity, request.pestCategoryIds ());
    }
    addIdentifiers (caseEntity, request.identifierIds ());

    caseRepository.save (caseEntity);
    return toDetail (caseEntity);
  }

  /** 更新案件 (僅更新有提供的欄位)；狀態變更需符合轉移規則 (見 CaseStatus) */
  @Transactional
  public CaseResponse update (Long id, CaseUpdateRequest request) {
    Case caseEntity = findByIdOrThrow (id);

    // 已結案案件：僅管理者可修改內容欄位 (狀態同值為合法 no-op，狀態轉移另由規則把關)
    if (caseEntity.getStatus () == CaseStatus.CLOSED && !isAdmin () && hasContentUpdate (request)) {
      throw new ApiException ("CLOSED_CASE_READONLY", HttpStatus.FORBIDDEN,
          "案件已結案，僅管理者可修改內容");
    }

    // 先驗證所有參照 ID (fail-fast)，避免部分欄位已寫入後才因參照不存在而回滾，確保原子性語意清晰
    if (request.methodId () != null) {
      getRef (methodRepository, request.methodId (), "耕種方式");
    }
    if (request.cropId () != null) {
      getRef (cropRepository, request.cropId (), "作物");
    }
    if (request.serviceId () != null) {
      getRef (serviceRepository, request.serviceId (), "服務類別");
    }
    if (request.deliverId () != null) {
      getRef (deliveryRepository, request.deliverId (), "送件方式");
    }
    if (request.senderId () != null) {
      senderRepository.findById (request.senderId ())
          .orElseThrow (() -> new ApiException ("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
    }
    validateJunctionRefs (request);

    if (request.receiveDate () != null) {
      caseEntity.setReceiveDate (request.receiveDate ());
    }
    if (request.cropScale () != null) {
      caseEntity.setCropScale (request.cropScale ());
    }
    if (request.damageScale () != null) {
      caseEntity.setDamageScale (request.damageScale ());
    }
    if (request.caseDescription () != null) {
      caseEntity.setCaseDescription (request.caseDescription ());
    }
    if (request.hintDescription () != null) {
      caseEntity.setHintDescription (request.hintDescription ());
    }
    if (request.status () != null) {
      applyStatusTransition (caseEntity, parseStatus (request.status ()));
    }
    if (request.methodId () != null) {
      caseEntity.setMethod (getRef (methodRepository, request.methodId (), "耕種方式"));
    }
    if (request.cropId () != null) {
      caseEntity.setCrop (getRef (cropRepository, request.cropId (), "作物"));
    }
    if (request.serviceId () != null) {
      caseEntity.setService (getRef (serviceRepository, request.serviceId (), "服務類別"));
    }
    if (request.deliverId () != null) {
      caseEntity.setDelivery (getRef (deliveryRepository, request.deliverId (), "送件方式"));
    }
    if (request.fieldDistrictId () != null) {
      caseEntity.setFieldDistrict (getRef (districtRepository, request.fieldDistrictId (), "田區位置"));
    }
    applySenderUpdate (caseEntity, request);
    replaceJunctions (caseEntity, request);

    return toDetail (caseEntity);
  }

  private void validateJunctionRefs (CaseUpdateRequest request) {
    if (request.damageIds () != null) {
      for (Long id : new HashSet<>(request.damageIds ())) {
        getRef (damageRepository, id, "被害部位");
      }
    }
    if (request.hintIds () != null) {
      for (Long id : new HashSet<>(request.hintIds ())) {
        getRef (hintRepository, id, "防治建議");
      }
    }
    if (request.pestCategoryWithNotes () != null) {
      for (var note : request.pestCategoryWithNotes ()) {
        getRef (pestCategoryRepository, note.pestCategoryId (), "病蟲害分類");
      }
    } else if (request.pestCategoryIds () != null) {
      for (Long id : new HashSet<>(request.pestCategoryIds ())) {
        getRef (pestCategoryRepository, id, "病蟲害分類");
      }
    }
    if (request.identifierIds () != null) {
      for (Long id : new HashSet<>(request.identifierIds ())) {
        getRef (identifierRepository, id, "診斷簽名人");
      }
    }
    if (request.senderDistrictId () != null) {
      getRef (districtRepository, request.senderDistrictId (), "鄉鎮市區");
    }
    if (request.senderTypeId () != null) {
      getRef (senderTypeRepository, request.senderTypeId (), "身分別");
    }
    if (request.fieldDistrictId () != null) {
      getRef (districtRepository, request.fieldDistrictId (), "田區位置");
    }
  }

  /** 依轉移規則更新狀態；非法轉移拋 400 且狀態不變 */
  private void applyStatusTransition (Case caseEntity, CaseStatus target) {
    CaseStatus current = caseEntity.getStatus ();
    if (target == current) {
      return;
    }
    if (current == CaseStatus.PENDING && target == CaseStatus.RESOLVED) {
      // STAFF/ADMIN：update 端點已限制角色
      caseEntity.setStatus (target);
      return;
    }
    if (current == CaseStatus.RESOLVED && target == CaseStatus.CLOSED) {
      if (!isAdmin ()) {
        throw new ApiException ("STATUS_TRANSITION_FORBIDDEN", HttpStatus.FORBIDDEN,
            "僅管理者可將案件標記為已結案");
      }
      caseEntity.setStatus (target);
      return;
    }
    throw new ApiException ("INVALID_STATUS_TRANSITION", HttpStatus.BAD_REQUEST,
        "非法的狀態轉移：" + current + " → " + target);
  }

  /** 目前登入者是否為 ADMIN (用於 RESOLVED → CLOSED 的轉移授權) */
  private boolean isAdmin () {
    Authentication auth = SecurityContextHolder.getContext ().getAuthentication ();
    return auth != null && auth.getAuthorities ().stream ()
        .anyMatch (a -> "ROLE_ADMIN".equals (a.getAuthority ()));
  }

  private boolean isViewer () {
    Authentication auth = SecurityContextHolder.getContext ().getAuthentication ();
    return auth != null && auth.getAuthorities ().stream ()
        .anyMatch (a -> "ROLE_VIEWER".equals (a.getAuthority ()));
  }

  /** 請求是否帶有任何「非狀態」欄位 (用於 CLOSED 案件的管理者限改判斷) */
  private boolean hasContentUpdate (CaseUpdateRequest request) {
    return request.receiveDate () != null
        || request.cropScale () != null
        || request.damageScale () != null
        || request.caseDescription () != null
        || request.hintDescription () != null
        || request.methodId () != null
        || request.cropId () != null
        || request.serviceId () != null
        || request.deliverId () != null
        || request.fieldDistrictId () != null
        || request.senderId () != null
        || request.senderName () != null
        || request.senderDisplayName () != null
        || request.senderPhone () != null
        || request.senderAddress () != null
        || request.senderDistrictId () != null
        || request.senderTypeId () != null
        || request.damageIds () != null
        || request.hintIds () != null
        || request.pestCategoryIds () != null
        || request.pestCategoryWithNotes () != null
        || request.identifierIds () != null;
  }

  /**
   * 更新送件人：若提供 senderId 則沿用，否則依有提供的欄位建立新送件人。
   * 不再以 name+phone 強制去重，符合弱識別人工確認語意。
   */
  private void applySenderUpdate (Case caseEntity, CaseUpdateRequest request) {
    if (request.senderId () != null) {
      Sender sender = senderRepository.findById (request.senderId ())
          .orElseThrow (() -> new ApiException ("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
      caseEntity.setSender (sender);
      return;
    }
    boolean anyProvided = request.senderName () != null || request.senderDisplayName () != null
        || request.senderPhone () != null || request.senderAddress () != null
        || request.senderDistrictId () != null || request.senderTypeId () != null;
    if (!anyProvided) {
      return;
    }
    // 若僅提供部分欄位，建立新 Sender 以避免改動被多案件共享的既有 row
    String name = request.senderName () != null ? request.senderName () : caseEntity.getSender ().getName ();
    String displayName = request.senderDisplayName () != null ? request.senderDisplayName () : caseEntity.getSender ().getDisplayName ();
    String phone = request.senderPhone () != null ? request.senderPhone () : caseEntity.getSender ().getPhone ();
    String address = request.senderAddress () != null ? request.senderAddress () : caseEntity.getSender ().getAddress ();
    Long districtId = request.senderDistrictId () != null ? request.senderDistrictId () : Optional.ofNullable (caseEntity.getSender ().getDistrict ()).map (District::getDistrictId).orElse (null);
    Long senderTypeId = request.senderTypeId () != null ? request.senderTypeId () : Optional.ofNullable (caseEntity.getSender ().getSenderType ()).map (SenderType::getSenderTypeId).orElse (null);
    boolean hasPhone = phone != null && !phone.isBlank ();
    boolean hasDisplay = displayName != null && !displayName.isBlank ();
    if (!hasPhone && !hasDisplay) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "電話與顯示名稱至少需提供一項");
    }
    Sender sender = new Sender ();
    sender.setName (name);
    sender.setDisplayName (displayName);
    sender.setPhone (phone);
    sender.setAddress (address);
    if (districtId != null) {
      sender.setDistrict (getRef (districtRepository, districtId, "鄉鎮市區"));
    } else {
      sender.setDistrict (caseEntity.getSender ().getDistrict ());
    }
    if (senderTypeId != null) {
      sender.setSenderType (getRef (senderTypeRepository, senderTypeId, "身分別"));
    } else {
      sender.setSenderType (caseEntity.getSender ().getSenderType ());
    }
    senderRepository.save (sender);
    caseEntity.setSender (sender);
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
  private void replaceJunctions (Case caseEntity, CaseUpdateRequest request) {
    if (request.damageIds () != null) {
      replaceJunctionGroup (caseEntity, caseEntity.getCaseDamages (), request.damageIds (),
          j -> j.getDamage ().getDamageId (),
          (c, id) -> {
            CaseDamage junction = new CaseDamage ();
            junction.setCaseEntity (c);
            junction.setDamage (getRef (damageRepository, id, "被害部位"));
            return junction;
          });
    }
    // 害物明細：優先使用 pestCategoryWithNotes (含 note)，否則回退至舊的 pestCategoryIds
    if (request.pestCategoryWithNotes () != null) {
      // 直接 clear+add，因為同分類多筆且含 note 需保留重複 categoryId 的不同 note
      caseEntity.getCasePestCategories ().clear ();
      for (var note : request.pestCategoryWithNotes ()) {
        CasePestCategory j = new CasePestCategory ();
        j.setCaseEntity (caseEntity);
        j.setPestCategory (getRef (pestCategoryRepository, note.pestCategoryId (), "病蟲害分類"));
        j.setPestNote (note.pestNote ());
        caseEntity.getCasePestCategories ().add (j);
      }
    } else if (request.pestCategoryIds () != null) {
      replaceJunctionGroup (caseEntity, caseEntity.getCasePestCategories (),
          request.pestCategoryIds (), j -> j.getPestCategory ().getPestCategoryId (),
          (c, id) -> {
            CasePestCategory junction = new CasePestCategory ();
            junction.setCaseEntity (c);
            junction.setPestCategory (getRef (pestCategoryRepository, id, "病蟲害分類"));
            return junction;
          });
    }
    if (request.hintIds () != null) {
      replaceJunctionGroup (caseEntity, caseEntity.getCaseHints (), request.hintIds (),
          j -> j.getHint ().getHintId (),
          (c, id) -> {
            CaseHint junction = new CaseHint ();
            junction.setCaseEntity (c);
            junction.setHint (getRef (hintRepository, id, "防治建議"));
            return junction;
          });
    }
    if (request.identifierIds () != null) {
      replaceJunctionGroup (caseEntity, caseEntity.getCaseIdentifiers (),
          request.identifierIds (), j -> j.getIdentifier ().getIdentifierId (),
          (c, id) -> {
            CaseIdentifier junction = new CaseIdentifier ();
            junction.setCaseEntity (c);
            junction.setIdentifier (getRef (identifierRepository, id, "診斷簽名人"));
            return junction;
          });
    }
  }

  /** 建立 Junction 的工廠 (泛型化四組多對多) */
  @FunctionalInterface
  private interface JunctionFactory<J> {
    J create (Case caseEntity, Long refId);
  }

  /** 差集式整組替換：刪目標外的既有 junction、補目標缺少的 junction (ids 以 Set 去重，避免重複 id 建立重複 junction) */
  private <J> void replaceJunctionGroup (Case caseEntity, List<J> junctions, List<Long> ids,
      Function<J, Long> idGetter, JunctionFactory<J> factory) {
    Set<Long> target = new HashSet<>(ids);
    List<J> toRemove = junctions.stream ()
        .filter (j -> !target.contains (idGetter.apply (j)))
        .toList ();
    toRemove.forEach (junctions::remove);
    Set<Long> have = junctions.stream ().map (idGetter).collect (Collectors.toSet ());
    for (Long refId : target) {
      if (!have.contains (refId)) {
        junctions.add (factory.create (caseEntity, refId));
      }
    }
  }

  /** 刪除案件 (多對多關聯以 Cascade 一併刪除) */
  @Transactional
  public void delete (Long id) {
    caseRepository.delete (findByIdOrThrow (id));
  }

  /**
   * 案件統計總覽 (見 spec case-statistics)。
   *
   * 以「收件日期 (receiveDate)」為月份基礎 (與 case-search 篩選一致)：
   * 本月新增＝收件日 ≥ 本月初；趨勢近 6 月逐月計數。top 作物／因素與
   * 趨勢以 findAll () (EntityGraph 預抓關聯) Java 聚合，本機資料量小故採
   * 單一查詢；空資料庫時各項為 0 或空清單。
   * 期別過濾：HISTORICAL 全量、ANNUAL 依年、MONTHLY 依年月；breakdown 以期別內案件為分母，
   * 害物/防治建議多對多 >1 標為複合因素/複合建議 (實例數計數，不去重)。
   */
  @Transactional (readOnly = true)
  public CaseStatisticsResponse statistics () {
    return statistics ("HISTORICAL", null, null, null);
  }

  @Transactional (readOnly = true)
  public CaseStatisticsResponse statistics (String period, Integer year, Integer month) {
    return statistics (period, year, month, null);
  }

  @Transactional (readOnly = true)
  public CaseStatisticsResponse statistics (String period, Integer year, Integer month, Integer half) {
    String normalizedPeriod = period == null || period.isBlank () ? "HISTORICAL" : period.toUpperCase ();
    if (!List.of ("HISTORICAL", "ANNUAL", "MONTHLY", "HALF_YEAR").contains (normalizedPeriod)) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "無效的 period：" + period);
    }
    if ("ANNUAL".equals (normalizedPeriod) && year == null) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "年度統計需提供 year");
    }
    if ("MONTHLY".equals (normalizedPeriod) && (year == null || month == null)) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "月度統計需提供 year 與 month");
    }
    if ("HALF_YEAR".equals (normalizedPeriod) && (year == null || half == null)) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "半年度統計需提供 year 與 half");
    }
    if (half != null && (half < 1 || half > 2)) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "無效的 half：" + half);
    }
    if (month != null && (month < 1 || month > 12)) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "無效的 month：" + month);
    }

    long total = caseRepository.count ();
    long monthNew = caseRepository.countByReceiveDateGreaterThanEqual (LocalDate.now ().withDayOfMonth (1));
    long pending = caseRepository.countByStatus (CaseStatus.PENDING);

    List<Case> all = caseRepository.findAll ();
    List<Integer> availableYears = all.stream ()
        .map (c -> c.getReceiveDate ().getYear ())
        .distinct ().sorted (java.util.Comparator.reverseOrder ()).toList ();

    List<Case> filtered = filterByPeriod (all, normalizedPeriod, year, month, half);
    long periodTotal = filtered.size ();

    long distinctSenders = filtered.stream ()
        .map (c -> {
          String phone = c.getSender ().getPhone ();
          String displayName = c.getSender ().getDisplayName ();
          boolean hasPhone = phone != null && !phone.isBlank ();
          return hasPhone ? phone.trim () : (displayName != null ? displayName.trim () : null);
        })
        .filter (s -> s != null && !s.isBlank ())
        .collect (Collectors.toSet ()).size ();
    // topN 仍基於期別內
    List<CountName> topCrops = topN (filtered.stream ()
        .collect (Collectors.groupingBy (c -> c.getCrop ().getCrop (), Collectors.counting ())));
    List<CountName> topPestCategories = topN (filtered.stream ()
        .flatMap (c -> c.getCasePestCategories ().stream ())
        .collect (Collectors.groupingBy (j -> j.getPestCategory ().getPestCategory (), Collectors.counting ())));
    List<StatusCount> statusRatio = Arrays.stream (CaseStatus.values ())
        .map (status -> new StatusCount (status.name (),
            filtered.stream ().filter (c -> c.getStatus () == status).count ()))
        .toList ();
    // 近6月趨勢维持歷史期別 (不受期別篩選影響)
    List<MonthCount> monthlyTrend = monthlyTrend (all);
    long compositeCases = filtered.stream ()
        .filter (c -> c.getCasePestCategories ().size () > 1)
        .count ();
    // 期別 breakdown (實例數，不去重)
    List<CountName> cropCategoryBreakdown = breakdown (filtered.stream ()
        .collect (Collectors.groupingBy (c -> {
          if (c.getCrop () == null || c.getCrop ().getCropCategory () == null) return "未分類";
          return c.getCrop ().getCropCategory ().getCropCategory ();
        }, Collectors.counting ())));
    List<CountName> pestTypeBreakdown = pestTypeBreakdown (filtered);
    List<CountName> deliveryBreakdown = breakdown (filtered.stream ()
        .collect (Collectors.groupingBy (c -> c.getDelivery () != null ? c.getDelivery ().getDeliver () : "未知", Collectors.counting ())));
    List<CountName> methodBreakdown = breakdown (filtered.stream ()
        .collect (Collectors.groupingBy (c -> c.getMethod () != null ? c.getMethod ().getMethod () : "未知", Collectors.counting ())));
    List<CountName> hintBreakdown = hintBreakdown (filtered);
    long compositeFactorCases = compositeCases;
    long compositeHintCases = filtered.stream ().filter (c -> c.getCaseHints ().size () > 1).count ();
    List<CountName> fieldCityBreakdown = topN (filtered.stream ()
        .collect (Collectors.groupingBy (c -> Optional.ofNullable (c.getFieldDistrict ()).map (District::getCity).map (City::getCity).orElse ("未知"),
            Collectors.counting ())));

    return new CaseStatisticsResponse (total, monthNew, pending, distinctSenders,
        topCrops, topPestCategories, statusRatio, monthlyTrend, compositeCases,
        cropCategoryBreakdown, pestTypeBreakdown, deliveryBreakdown, methodBreakdown, hintBreakdown,
        compositeFactorCases, compositeHintCases, availableYears, normalizedPeriod, year, month, periodTotal,
        fieldCityBreakdown);
  }

  private List<Case> filterByPeriod (List<Case> all, String period, Integer year, Integer month, Integer half) {
    if ("HISTORICAL".equals (period)) return all;
    if ("ANNUAL".equals (period)) {
      return all.stream ().filter (c -> c.getReceiveDate ().getYear () == year).toList ();
    }
    if ("HALF_YEAR".equals (period)) {
      LocalDate start = half == 1 ? LocalDate.of (year, 1, 1) : LocalDate.of (year, 7, 1);
      LocalDate end = half == 1 ? LocalDate.of (year, 6, 30) : LocalDate.of (year, 12, 31);
      return all.stream ().filter (c -> !c.getReceiveDate ().isBefore (start) && !c.getReceiveDate ().isAfter (end)).toList ();
    }
    // MONTHLY
    YearMonth ym = YearMonth.of (year, month);
    LocalDate start = ym.atDay (1);
    LocalDate end = ym.atEndOfMonth ();
    return all.stream ().filter (c -> !c.getReceiveDate ().isBefore (start) && !c.getReceiveDate ().isAfter (end)).toList ();
  }

  private List<CountName> breakdown (Map<String, Long> counts) {
    return counts.entrySet ().stream ()
        .sorted (Map.Entry.<String, Long>comparingByValue ().reversed ().thenComparing (Map.Entry.comparingByKey ()))
        .map (e -> new CountName (e.getKey (), e.getValue ()))
        .toList ();
  }

  private List<CountName> pestTypeBreakdown (List<Case> filtered) {
    Map<String, Long> counts = new java.util.LinkedHashMap<>();
    for (Case c : filtered) {
      if (c.getCasePestCategories () == null || c.getCasePestCategories ().isEmpty ()) {
        counts.merge ("無", 1L, Long::sum);
      } else if (c.getCasePestCategories ().size () > 1) {
        counts.merge ("複合因素", 1L, Long::sum);
      } else {
        var pc = c.getCasePestCategories ().get (0).getPestCategory ();
        String pt = (pc != null && pc.getPestType () != null) ? pc.getPestType ().getPestType () : "未知";
        counts.merge (pt, 1L, Long::sum);
      }
    }
    return breakdown (counts);
  }

  private List<CountName> hintBreakdown (List<Case> filtered) {
    Map<String, Long> counts = new java.util.LinkedHashMap<>();
    for (Case c : filtered) {
      if (c.getCaseHints () == null || c.getCaseHints ().isEmpty ()) {
        counts.merge ("無", 1L, Long::sum);
      } else if (c.getCaseHints ().size () > 1) {
        counts.merge ("複合建議", 1L, Long::sum);
      } else {
        String h = c.getCaseHints ().get (0).getHint () != null ? c.getCaseHints ().get (0).getHint ().getHint () : "未知";
        counts.merge (h, 1L, Long::sum);
      }
    }
    return breakdown (counts);
  }

  /** topN：依計數遞減排序 (同數值再依名稱穩定排序) 取前 10 */
  private List<CountName> topN (Map<String, Long> counts) {
    return counts.entrySet ().stream ()
        .sorted (Map.Entry.<String, Long>comparingByValue ().reversed ()
            .thenComparing (Map.Entry.comparingByKey ()))
        .limit (10)
        .map (e -> new CountName (e.getKey (), e.getValue ()))
        .toList ();
  }

  /** 近 6 月 (含本月) 逐月案件數趨勢，依收件日期分組 */
  private List<MonthCount> monthlyTrend (List<Case> all) {
    YearMonth current = YearMonth.now ();
    List<MonthCount> result = new ArrayList<>();
    for (int i = 5; i >= 0; i--) {
      YearMonth month = current.minusMonths (i);
      long count = all.stream ()
          .filter (c -> YearMonth.from (c.getReceiveDate ()).equals (month))
          .count ();
      result.add (new MonthCount (month.toString (), count));
    }
    return result;
  }

  /**
   * CSV 匯出 (見 spec case-report)：依篩選查詢全部案件 (不分頁，收件編號升序)
   * 組 CSV，輸出含 UTF-8 BOM 供 Excel 開啟中文。篩選語意與列表 (case-search) 一致。
   */
  @Transactional (readOnly = true)
  public String exportCsv (CaseFilter filter) {
    CaseStatus status = filter.status () != null ? parseStatus (filter.status ()) : null;
    if (filter.isEmpty ()) {
      List<Case> cases = caseRepository.findAll (Sort.by ("caseId"));
      return toCsv (cases);
    }
    List<com.d0w0b.phytotrack.models.CaseSearchView> viewList =
        caseSearchViewRepository.findAll (CaseSpecifications.buildView (filter, status), Sort.by ("caseId"));
    List<Long> ids = viewList.stream ().map (com.d0w0b.phytotrack.models.CaseSearchView::getCaseId).toList ();
    if (ids.isEmpty ()) {
      return toCsv (List.of ());
    }
    List<Case> cases = caseRepository.findAllById (ids);
    // 保持收件編號升序
    cases.sort (java.util.Comparator.comparing (Case::getCaseId));
    return toCsv (cases);
  }

  /** 組 CSV 內容：首列為欄位名，含 UTF-8 BOM，對齊 diagnoses.typ (Q1-Q5 定版，含 7 項調整) */
  private String toCsv (List<Case> cases) {
    StringBuilder sb = new StringBuilder ("\uFEFF");
    sb.append (join ("收件編號", "收件日期", "狀態",
        "田區位置", "身分別", "姓名", "顯示名稱", "電話", "住址",
        "服務類別", "送件方式", "耕作方式", "作物種類", "作物名稱",
        "被害部位", "栽培面積", "被害面積", "土壤栽培用藥紀錄",
        "病害", "蟲害", "有害動物", "生理因子", "其他", "診斷結果",
        "建議事項", "防治描述", "鑑定者", "建立者", "建立時間", "更新時間"));
    for (Case c : cases) {
      String fieldCity = Optional.ofNullable (c.getFieldDistrict ()).map (District::getCity).map (City::getCity).orElse ("");
      String fieldDistrict = Optional.ofNullable (c.getFieldDistrict ()).map (District::getDistrict).orElse ("");
      String fieldLocation = fieldCity + fieldDistrict;
      String senderAddressFull = (Optional.ofNullable (c.getSender ().getDistrict ()).map (District::getCity).map (City::getCity).orElse ("") + Optional.ofNullable (c.getSender ().getDistrict ()).map (District::getDistrict).orElse ("") + Optional.ofNullable (c.getSender ().getAddress ()).orElse (""));
      // 診斷結果：pest_note 串接 (caseDescription 已為土壤紀錄)，此處以 pest_notes 呈現
      String pestNotes = c.getCasePestCategories ().stream ()
          .map (j -> {
            String note = j.getPestNote ();
            return note != null && !note.isBlank () ? j.getPestCategory ().getPestCategory () + "(" + note + ")" : null;
          }).filter (java.util.Objects::nonNull).collect (Collectors.joining ("、"));
      if (pestNotes.isBlank ()) pestNotes = "";
      // 五類分組 (依 pestTypeId)
      String[] pestByType = pestByType (c);
      // 建議事項更名：其他 → 其他回覆 (Q4，顯示層)
      String hints = c.getCaseHints ().stream ().map (j -> {
        String n = j.getHint ().getHint ();
        return "其他".equals (n) ? "其他回覆" : n;
      }).collect (Collectors.joining ("、"));
      sb.append ('\n').append (join (String.valueOf (c.getCaseId ()),
          String.valueOf (c.getReceiveDate ()),
          statusDisplay (c.getStatus ()),
          fieldLocation,
          senderTypeNameOf (c),
          c.getSender ().getName (),
          c.getSender ().getDisplayName (),
          c.getSender ().getPhone (),
          senderAddressFull,
          c.getService () != null ? c.getService ().getService () : null,
          c.getDelivery () != null ? c.getDelivery ().getDeliver () : null,
          c.getMethod () != null ? c.getMethod ().getMethod () : null,
          Optional.ofNullable (c.getCrop ()).map (Crop::getCropCategory).map (cat -> cat.getCropCategory ()).orElse (null),
          c.getCrop ().getCrop (),
          names (c.getCaseDamages (), d -> d.getDamage ().getDamage ()),
          c.getCropScale (),
          c.getDamageScale (),
          c.getCaseDescription (),
          pestByType[0], pestByType[1], pestByType[2], pestByType[3], pestByType[4],
          pestNotes,
          hints,
          c.getHintDescription (),
          names (c.getCaseIdentifiers (), j -> j.getIdentifier ().getIdentifier ()),
          c.getCreatedBy () != null ? c.getCreatedBy ().getDisplayName () : null,
          fmtTs (c.getCreatedAt ()),
          fmtTs (c.getUpdatedAt ())));
    }
    return sb.toString ();
  }

  /** 五類分組：病害 (1)/蟲害 (2)/有害動物 (3)/生理因子 (4)/其他 (5)，無資料顯示「無」 */
  private String[] pestByType (Case c) {
    String[] result = new String[]{"無", "無", "無", "無", "無"};
    if (c.getCasePestCategories () == null || c.getCasePestCategories ().isEmpty ()) return result;
    java.util.Map<Long, List<String>> grouped = c.getCasePestCategories ().stream ()
        .collect (Collectors.groupingBy (j -> Optional.ofNullable (j.getPestCategory ().getPestType ()).map (PestType::getPestTypeId).orElse (5L),
            Collectors.mapping (j -> {
              String base = j.getPestCategory ().getPestCategory ();
              String note = j.getPestNote ();
              return note != null && !note.isBlank () ? base + "(" + note + ")" : base;
            }, Collectors.toList ())));
    for (int i = 0; i < 5; i++) {
      Long typeId = (long) (i + 1);
      List<String> names = grouped.get (typeId);
      if (names != null && !names.isEmpty ()) {
        result[i] = String.join ("、", names);
      }
    }
    return result;
  }

  /** 多對多關聯名稱組串 (以「、」連結) */
  private static <T> String names (List<T> items, Function<T, String> nameOf) {
    return items.stream ().map (nameOf).collect (Collectors.joining ("、"));
  }

  /** 單列：各欄位轉義後以逗號連結 */
  private static String join (String... fields) {
    return Arrays.stream (fields).map (CaseService::csvEscape).collect (Collectors.joining (","));
  }

  /** 狀態中文顯示 */
  private static String statusDisplay (CaseStatus status) {
    if (status == null) return "";
    return switch (status) {
      case PENDING -> "待處理";
      case RESOLVED -> "已處理";
      case CLOSED -> "已結案";
    };
  }

  /** CSV 欄位轉義：全欄位以引號包覆，內部引號以 "" 轉義（電話與中文皆為字串） */
  private static String csvEscape (String value) {
    String v = value == null ? "" : value;
    return "\"" + v.replace ("\"", "\"\"") + "\"";
  }

  private static final DateTimeFormatter CSV_DT_FMT = DateTimeFormatter.ofPattern ("yyyy-MM-dd'T'HH:mm:ss");

  private static String fmtTs (java.time.LocalDateTime dt) {
    if (dt == null) return "";
    return dt.truncatedTo (ChronoUnit.SECONDS).format (CSV_DT_FMT);
  }

  // ------------------------------------------------------------------
  // 私有輔助方法
  // ------------------------------------------------------------------

  /** 依 senderId 沿用或依欄位建立新送件人 (phone 與 displayName 至少一有值) */
  private Sender findOrCreateSender (CaseCreateRequest request) {
    if (request.senderId () != null) {
      return senderRepository.findById (request.senderId ())
          .orElseThrow (() -> new ApiException ("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
    }
    String phone = request.senderPhone ();
    String displayName = request.senderDisplayName ();
    boolean hasPhone = phone != null && !phone.isBlank ();
    boolean hasDisplay = displayName != null && !displayName.isBlank ();
    if (!hasPhone && !hasDisplay) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "電話與顯示名稱至少需提供一項");
    }
    Sender sender = new Sender ();
    sender.setName (request.senderName ());
    sender.setDisplayName (displayName);
    sender.setPhone (phone);
    sender.setAddress (request.senderAddress ());
    sender.setDistrict (getRef (districtRepository, request.senderDistrictId (), "鄉鎮市區"));
    sender.setSenderType (getRef (senderTypeRepository, request.senderTypeId (), "身分別"));
    return senderRepository.save (sender);
  }

  /** 建立案件的多對多關聯 (Junction Record)：被害部位 */
  private void addDamages (Case caseEntity, List<Long> damageIds) {
    if (damageIds == null) {
      return;
    }
    for (Long damageId : damageIds) {
      Damage damage = getRef (damageRepository, damageId, "被害部位");
      CaseDamage junction = new CaseDamage ();
      junction.setCaseEntity (caseEntity);
      junction.setDamage (damage);
      caseEntity.getCaseDamages ().add (junction);
    }
  }

  /** 建立案件的多對多關聯 (Junction Record)：防治建議 */
  private void addHints (Case caseEntity, List<Long> hintIds) {
    if (hintIds == null) {
      return;
    }
    for (Long hintId : hintIds) {
      Hint hint = getRef (hintRepository, hintId, "防治建議");
      CaseHint junction = new CaseHint ();
      junction.setCaseEntity (caseEntity);
      junction.setHint (hint);
      caseEntity.getCaseHints ().add (junction);
    }
  }

  /** 建立案件的多對多關聯 (Junction Record)：病蟲害分類 */
  private void addPestCategories (Case caseEntity, List<Long> pestCategoryIds) {
    if (pestCategoryIds == null) {
      return;
    }
    for (Long pestCategoryId : pestCategoryIds) {
      PestCategory category = getRef (pestCategoryRepository, pestCategoryId, "病蟲害分類");
      CasePestCategory junction = new CasePestCategory ();
      junction.setCaseEntity (caseEntity);
      junction.setPestCategory (category);
      caseEntity.getCasePestCategories ().add (junction);
    }
  }

  /** 建立案件的多對多關聯 (Junction Record)：診斷簽名人 */
  private void addIdentifiers (Case caseEntity, List<Long> identifierIds) {
    if (identifierIds == null) {
      return;
    }
    for (Long identifierId : identifierIds) {
      Identifier identifier = getRef (identifierRepository, identifierId, "診斷簽名人");
      CaseIdentifier junction = new CaseIdentifier ();
      junction.setCaseEntity (caseEntity);
      junction.setIdentifier (identifier);
      caseEntity.getCaseIdentifiers ().add (junction);
    }
  }

  /** 依 ID 查詢參照資料，不存在則拋出業務例外 */
  private <T> T getRef (JpaRepository<T, Long> repository, Long id, String label) {
    return repository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, label + "不存在 (ID：" + id + ")"));
  }

  private Case findByIdOrThrow (Long id) {
    return caseRepository.findById (id)
        .orElseThrow (() -> new ApiException ("CASE_NOT_FOUND", HttpStatus.NOT_FOUND, "案件不存在"));
  }

  /** 轉換為摘要回應；pestCategoryCount>1 標註複合案件 */
  private CaseSummaryResponse toSummary (Case caseEntity) {
    boolean viewer = isViewer ();
    String senderName = viewer ? null : caseEntity.getSender ().getName ();
    String senderDisplayName = viewer ? null : caseEntity.getSender ().getDisplayName ();
    String senderPhone = viewer ? null : caseEntity.getSender ().getPhone ();
    String senderAddress = viewer ? null : caseEntity.getSender ().getAddress ();
    Long senderDistrictId = Optional.ofNullable (caseEntity.getSender ().getDistrict ())
        .map (District::getDistrictId).orElse (null);
    String senderDistrictName = districtNameOf (caseEntity);
    String senderCityName = cityNameOf (caseEntity);
    int pestCategoryCount = caseEntity.getCasePestCategories () != null ? caseEntity.getCasePestCategories ().size () : 0;
    String deliveryName = Optional.ofNullable (caseEntity.getDelivery ()).map (Delivery::getDeliver).orElse (null);
    String pestCategoryNames = caseEntity.getCasePestCategories () != null ? caseEntity.getCasePestCategories ().stream ()
        .map (j -> {
          String base = j.getPestCategory ().getPestCategory ();
          String note = j.getPestNote ();
          return note != null && !note.isBlank () ? base + "(" + note + ")" : base;
        }).collect (Collectors.joining ("、")) : null;
    if (pestCategoryNames != null && pestCategoryNames.isBlank ()) pestCategoryNames = null;
    String pestTypeNames = caseEntity.getCasePestCategories () != null ? caseEntity.getCasePestCategories ().stream ()
        .map (j -> Optional.ofNullable (j.getPestCategory ().getPestType ()).map (PestType::getPestType).orElse (null))
        .filter (java.util.Objects::nonNull).distinct ().collect (Collectors.joining ("、")) : null;
    if (pestTypeNames != null && pestTypeNames.isBlank ()) pestTypeNames = null;
    return new CaseSummaryResponse (caseEntity.getCaseId (),
        caseEntity.getReceiveDate (),
        caseEntity.getCrop ().getCrop (),
        senderName,
        senderDisplayName,
        senderPhone,
        senderAddress,
        caseEntity.getSender ().getSenderId (),
        senderDistrictId,
        senderDistrictName,
        senderCityName,
        caseEntity.getService ().getService (),
        deliveryName,
        caseEntity.getStatus ().name (),
        caseEntity.getCreatedAt (),
        pestCategoryCount,
        pestCategoryNames,
        pestTypeNames);
  }

  /** 轉換為詳細回應 (於交易內取用 Lazy 關聯) */
  private CaseResponse toDetail (Case caseEntity) {
    List<CaseResponse.IdName> damages = caseEntity.getCaseDamages ().stream ()
        .map (j -> new CaseResponse.IdName (j.getDamage ().getDamageId (), j.getDamage ().getDamage ()))
        .collect (Collectors.toList ());
    List<CaseResponse.IdName> hints = caseEntity.getCaseHints ().stream ()
        .map (j -> new CaseResponse.IdName (j.getHint ().getHintId (), j.getHint ().getHint ()))
        .collect (Collectors.toList ());
    List<CaseResponse.IdNameWithNote> pestCategories = caseEntity.getCasePestCategories ().stream ()
        .map (j -> {
          Long ptId = Optional.ofNullable (j.getPestCategory ().getPestType ()).map (t -> t.getPestTypeId ()).orElse (null);
          String ptName = Optional.ofNullable (j.getPestCategory ().getPestType ()).map (t -> t.getPestType ()).orElse (null);
          return new CaseResponse.IdNameWithNote (j.getPestCategory ().getPestCategoryId (),
              j.getPestCategory ().getPestCategory (),
              j.getPestNote (),
              ptId,
              ptName);
        })
        .collect (Collectors.toList ());
    List<CaseResponse.IdName> identifiers = caseEntity.getCaseIdentifiers ().stream ()
        .map (j -> new CaseResponse.IdName (j.getIdentifier ().getIdentifierId (), j.getIdentifier ().getIdentifier ()))
        .collect (Collectors.toList ());

    // 送件人鄉鎮/身分別可能未設定 (如更新時僅換新身分未帶 district/type)
    Long senderDistrictId = Optional.ofNullable (caseEntity.getSender ().getDistrict ())
        .map (District::getDistrictId).orElse (null);
    String senderDistrictName = districtNameOf (caseEntity);
    String senderCityName = cityNameOf (caseEntity);
    Long senderTypeId = Optional.ofNullable (caseEntity.getSender ().getSenderType ())
        .map (SenderType::getSenderTypeId).orElse (null);
    String senderTypeName = Optional.ofNullable (caseEntity.getSender ().getSenderType ()).map (SenderType::getSenderType).orElse (null);
    Long fieldDistrictId = Optional.ofNullable (caseEntity.getFieldDistrict ())
        .map (District::getDistrictId).orElse (null);
    String fieldDistrictName = Optional.ofNullable (caseEntity.getFieldDistrict ()).map (District::getDistrict).orElse (null);
    String fieldCityName = Optional.ofNullable (caseEntity.getFieldDistrict ()).map (District::getCity).map (City::getCity).orElse (null);
    String cropCategoryName = Optional.ofNullable (caseEntity.getCrop ())
        .map (Crop::getCropCategory).map (c -> c.getCropCategory ()).orElse (null);
    boolean viewer = isViewer ();
    String senderName = viewer ? null : caseEntity.getSender ().getName ();
    String senderDisplayName = viewer ? null : caseEntity.getSender ().getDisplayName ();
    String senderPhone = viewer ? null : caseEntity.getSender ().getPhone ();
    String senderAddress = viewer ? null : caseEntity.getSender ().getAddress ();

    return new CaseResponse (caseEntity.getCaseId (),
        caseEntity.getReceiveDate (),
        caseEntity.getCropScale (),
        caseEntity.getDamageScale (),
        caseEntity.getCaseDescription (),
        caseEntity.getHintDescription (),
        caseEntity.getStatus ().name (),
        caseEntity.getCreatedAt (),
        caseEntity.getUpdatedAt (),
        caseEntity.getSender ().getSenderId (),
        senderName,
        senderDisplayName,
        senderPhone,
        senderAddress,
        senderDistrictId,
        senderDistrictName,
        senderCityName,
        senderTypeId,
        senderTypeName,
        fieldDistrictId,
        fieldDistrictName,
        fieldCityName,
        cropCategoryName,
        caseEntity.getCrop ().getCrop (),
        caseEntity.getMethod ().getMethod (),
        caseEntity.getService ().getService (),
        caseEntity.getDelivery ().getDeliver (),
        caseEntity.getCreatedBy ().getDisplayName (),
        damages,
        hints,
        pestCategories,
        identifiers);
  }

  /** 送件人鄉鎮市區名稱 (null-safe：歷史資料或更新流程可能未帶 district) */
  private static String districtNameOf (Case c) {
    return Optional.ofNullable (c.getSender ().getDistrict ())
        .map (District::getDistrict).orElse (null);
  }

  /** 送件人縣市名稱 (null-safe) */
  private static String cityNameOf (Case c) {
    return Optional.ofNullable (c.getSender ().getDistrict ())
        .map (District::getCity).map (City::getCity).orElse (null);
  }

  /** 送件人身分別名稱 (null-safe：同上) */
  private static String senderTypeNameOf (Case c) {
    return Optional.ofNullable (c.getSender ().getSenderType ())
        .map (SenderType::getSenderType).orElse (null);
  }
}