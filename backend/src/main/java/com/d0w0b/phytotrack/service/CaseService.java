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
import com.d0w0b.phytotrack.models.Delivery;
import com.d0w0b.phytotrack.models.City;
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
    caseEntity.setCaseDescription(request.caseDescription());
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
    if (request.pestCategoryWithNotes() != null) {
      for (var note : request.pestCategoryWithNotes()) {
        CasePestCategory j = new CasePestCategory();
        j.setCaseEntity(caseEntity);
        j.setPestCategory(getRef(pestCategoryRepository, note.pestCategoryId(), "病蟲害分類"));
        j.setPestNote(note.pestNote());
        caseEntity.getCasePestCategories().add(j);
      }
    } else {
      addPestCategories(caseEntity, request.pestCategoryIds());
    }
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
    if (request.caseDescription() != null) {
      caseEntity.setCaseDescription(request.caseDescription());
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

  private boolean isViewer() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> "ROLE_VIEWER".equals(a.getAuthority()));
  }

  /** 請求是否帶有任何「非狀態」欄位（用於 CLOSED 案件的管理者限改判斷） */
  private boolean hasContentUpdate(CaseUpdateRequest request) {
    return request.receiveDate() != null
        || request.cropScale() != null
        || request.damageScale() != null
        || request.caseDescription() != null
        || request.hintDescription() != null
        || request.methodId() != null
        || request.cropId() != null
        || request.serviceId() != null
        || request.deliverId() != null
        || request.senderId() != null
        || request.senderName() != null
        || request.senderDisplayName() != null
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
   * 更新送件人：若提供 senderId 則沿用，否則依有提供的欄位建立新送件人。
   * 不再以 name+phone 強制去重，符合弱識別人工確認語意。
   */
  private void applySenderUpdate(Case caseEntity, CaseUpdateRequest request) {
    if (request.senderId() != null) {
      Sender sender = senderRepository.findById(request.senderId())
          .orElseThrow(() -> new ApiException("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
      caseEntity.setSender(sender);
      return;
    }
    boolean anyProvided = request.senderName() != null || request.senderDisplayName() != null
        || request.senderPhone() != null || request.senderAddress() != null
        || request.senderDistrictId() != null || request.senderTypeId() != null;
    if (!anyProvided) {
      return;
    }
    // 若僅提供部分欄位，建立新 Sender 以避免改動被多案件共享的既有 row
    String name = request.senderName() != null ? request.senderName() : caseEntity.getSender().getName();
    String displayName = request.senderDisplayName() != null ? request.senderDisplayName() : caseEntity.getSender().getDisplayName();
    String phone = request.senderPhone() != null ? request.senderPhone() : caseEntity.getSender().getPhone();
    String address = request.senderAddress() != null ? request.senderAddress() : caseEntity.getSender().getAddress();
    Long districtId = request.senderDistrictId() != null ? request.senderDistrictId() : Optional.ofNullable(caseEntity.getSender().getDistrict()).map(District::getDistrictId).orElse(null);
    Long senderTypeId = request.senderTypeId() != null ? request.senderTypeId() : Optional.ofNullable(caseEntity.getSender().getSenderType()).map(SenderType::getSenderTypeId).orElse(null);
    boolean hasPhone = phone != null && !phone.isBlank();
    boolean hasDisplay = displayName != null && !displayName.isBlank();
    if (!hasPhone && !hasDisplay) {
      throw new ApiException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "電話與顯示名稱至少需提供一項");
    }
    Sender sender = new Sender();
    sender.setName(name);
    sender.setDisplayName(displayName);
    sender.setPhone(phone);
    sender.setAddress(address);
    if (districtId != null) {
      sender.setDistrict(getRef(districtRepository, districtId, "鄉鎮市區"));
    } else {
      sender.setDistrict(caseEntity.getSender().getDistrict());
    }
    if (senderTypeId != null) {
      sender.setSenderType(getRef(senderTypeRepository, senderTypeId, "身分別"));
    } else {
      sender.setSenderType(caseEntity.getSender().getSenderType());
    }
    senderRepository.save(sender);
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
    // 害物明細：優先使用 pestCategoryWithNotes（含 note），否則回退至舊的 pestCategoryIds
    if (request.pestCategoryWithNotes() != null) {
      // 直接 clear+add，因為同分類多筆且含 note 需保留重複 categoryId 的不同 note
      caseEntity.getCasePestCategories().clear();
      for (var note : request.pestCategoryWithNotes()) {
        CasePestCategory j = new CasePestCategory();
        j.setCaseEntity(caseEntity);
        j.setPestCategory(getRef(pestCategoryRepository, note.pestCategoryId(), "病蟲害分類"));
        j.setPestNote(note.pestNote());
        caseEntity.getCasePestCategories().add(j);
      }
    } else if (request.pestCategoryIds() != null) {
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

  /**
   * 案件統計總覽（見 spec case-statistics）。
   *
   * 以「收件日期（receiveDate）」為月份基礎（與 case-search 篩選一致）：
   * 本月新增＝收件日 ≥ 本月初；趨勢近 6 月逐月計數。top 作物／病蟲害與
   * 趨勢以 findAll()（EntityGraph 預抓關聯）Java 聚合，本機資料量小故採
   * 單一查詢；空資料庫時各項為 0 或空清單。
   */
  @Transactional(readOnly = true)
  public CaseStatisticsResponse statistics() {
    long total = caseRepository.count();
    long monthNew = caseRepository.countByReceiveDateGreaterThanEqual(
        LocalDate.now().withDayOfMonth(1));
    long pending = caseRepository.countByStatus(CaseStatus.PENDING);

    List<Case> all = caseRepository.findAll();
    long distinctSenders = all.stream()
        .map(c -> {
          String phone = c.getSender().getPhone();
          String displayName = c.getSender().getDisplayName();
          boolean hasPhone = phone != null && !phone.isBlank();
          return hasPhone ? phone.trim() : (displayName != null ? displayName.trim() : null);
        })
        .filter(s -> s != null && !s.isBlank())
        .collect(Collectors.toSet()).size();
    List<CountName> topCrops = topN(all.stream()
        .collect(Collectors.groupingBy(c -> c.getCrop().getCrop(), Collectors.counting())));
    List<CountName> topPestCategories = topN(all.stream()
        .flatMap(c -> c.getCasePestCategories().stream())
        .collect(Collectors.groupingBy(
            j -> j.getPestCategory().getPestCategory(), Collectors.counting())));
    List<StatusCount> statusRatio = Arrays.stream(CaseStatus.values())
        .map(status -> new StatusCount(status.name(),
            all.stream().filter(c -> c.getStatus() == status).count()))
        .toList();
    List<MonthCount> monthlyTrend = monthlyTrend(all);

    return new CaseStatisticsResponse(total, monthNew, pending, distinctSenders,
        topCrops, topPestCategories, statusRatio, monthlyTrend);
  }

  /** topN：依計數遞減排序（同數值再依名稱穩定排序）取前 5 */
  private List<CountName> topN(Map<String, Long> counts) {
    return counts.entrySet().stream()
        .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
            .thenComparing(Map.Entry.comparingByKey()))
        .limit(5)
        .map(e -> new CountName(e.getKey(), e.getValue()))
        .toList();
  }

  /** 近 6 月（含本月）逐月案件數趨勢，依收件日期分組 */
  private List<MonthCount> monthlyTrend(List<Case> all) {
    YearMonth current = YearMonth.now();
    List<MonthCount> result = new ArrayList<>();
    for (int i = 5; i >= 0; i--) {
      YearMonth month = current.minusMonths(i);
      long count = all.stream()
          .filter(c -> YearMonth.from(c.getReceiveDate()).equals(month))
          .count();
      result.add(new MonthCount(month.toString(), count));
    }
    return result;
  }

  /**
   * CSV 匯出（見 spec case-report）：依篩選查詢全部案件（不分頁，收件日期升序）
   * 組 CSV，輸出含 UTF-8 BOM 供 Excel 開啟中文。篩選語意與列表（case-search）一致。
   */
  @Transactional(readOnly = true)
  public String exportCsv(CaseFilter filter) {
    CaseStatus status = filter.status() != null ? parseStatus(filter.status()) : null;
    List<Case> cases = caseRepository.findAll(
        CaseSpecifications.build(filter, status), Sort.by("receiveDate"));
    return toCsv(cases);
  }

  /** 組 CSV 內容：首列為欄位名，含 UTF-8 BOM */
  private String toCsv(List<Case> cases) {
    StringBuilder sb = new StringBuilder("\uFEFF");
    sb.append(join(
        "案件編號", "收件日期", "狀態", "送件人", "電話", "縣市鄉鎮", "地址", "身分別",
        "作物", "種植面積", "被害面積", "被害部位", "病蟲害", "病害描述", "防治建議",
        "簽名人", "耕種方式", "服務", "交付", "建立時間", "更新時間"));
    for (Case c : cases) {
      sb.append('\n').append(join(
          String.valueOf(c.getCaseId()),
          String.valueOf(c.getReceiveDate()),
          c.getStatus().name(),
          c.getSender().getName(),
          c.getSender().getPhone(),
          districtNameOf(c),
          c.getSender().getAddress(),
          senderTypeNameOf(c),
          c.getCrop().getCrop(),
          c.getCropScale(),
          c.getDamageScale(),
          names(c.getCaseDamages(), d -> d.getDamage().getDamage()),
          names(c.getCasePestCategories(), j -> j.getPestCategory().getPestCategory()),
          c.getCaseDescription(),
          c.getHintDescription(),
          names(c.getCaseIdentifiers(), j -> j.getIdentifier().getIdentifier()),
          c.getMethod() != null ? c.getMethod().getMethod() : null,
          c.getService() != null ? c.getService().getService() : null,
          c.getDelivery() != null ? c.getDelivery().getDeliver() : null,
          String.valueOf(c.getCreatedAt()),
          String.valueOf(c.getUpdatedAt())));
    }
    return sb.toString();
  }

  /** 多對多關聯名稱組串（以「、」連結） */
  private static <T> String names(List<T> items, Function<T, String> nameOf) {
    return items.stream().map(nameOf).collect(Collectors.joining("、"));
  }

  /** 單列：各欄位轉義後以逗號連結 */
  private static String join(String... fields) {
    return Arrays.stream(fields).map(CaseService::csvEscape).collect(Collectors.joining(","));
  }

  /** CSV 欄位轉義：含逗號／引號／換行時以引號包覆，內部引號重複 */
  private static String csvEscape(String value) {
    String v = value == null ? "" : value;
    if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
      return "\"" + v.replace("\"", "\"\"") + "\"";
    }
    return v;
  }

  // ------------------------------------------------------------------
  // 私有輔助方法
  // ------------------------------------------------------------------

  /** 依 senderId 沿用或依欄位建立新送件人（phone 與 displayName 至少一有值） */
  private Sender findOrCreateSender(CaseCreateRequest request) {
    if (request.senderId() != null) {
      return senderRepository.findById(request.senderId())
          .orElseThrow(() -> new ApiException("SENDER_NOT_FOUND", HttpStatus.NOT_FOUND, "送件人不存在"));
    }
    String phone = request.senderPhone();
    String displayName = request.senderDisplayName();
    boolean hasPhone = phone != null && !phone.isBlank();
    boolean hasDisplay = displayName != null && !displayName.isBlank();
    if (!hasPhone && !hasDisplay) {
      throw new ApiException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "電話與顯示名稱至少需提供一項");
    }
    Sender sender = new Sender();
    sender.setName(request.senderName());
    sender.setDisplayName(displayName);
    sender.setPhone(phone);
    sender.setAddress(request.senderAddress());
    sender.setDistrict(getRef(districtRepository, request.senderDistrictId(), "鄉鎮市區"));
    sender.setSenderType(getRef(senderTypeRepository, request.senderTypeId(), "身分別"));
    return senderRepository.save(sender);
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
    boolean viewer = isViewer();
    String senderName = viewer ? null : caseEntity.getSender().getName();
    String senderDisplayName = viewer ? null : caseEntity.getSender().getDisplayName();
    String senderPhone = viewer ? null : caseEntity.getSender().getPhone();
    String senderAddress = viewer ? null : caseEntity.getSender().getAddress();
    Long senderDistrictId = Optional.ofNullable(caseEntity.getSender().getDistrict())
        .map(District::getDistrictId).orElse(null);
    String senderDistrictName = districtNameOf(caseEntity);
    String senderCityName = cityNameOf(caseEntity);
    return new CaseSummaryResponse(
        caseEntity.getCaseId(),
        caseEntity.getReceiveDate(),
        caseEntity.getCrop().getCrop(),
        senderName,
        senderDisplayName,
        senderPhone,
        senderAddress,
        caseEntity.getSender().getSenderId(),
        senderDistrictId,
        senderDistrictName,
        senderCityName,
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
    List<CaseResponse.IdNameWithNote> pestCategories = caseEntity.getCasePestCategories().stream()
        .map(j -> new CaseResponse.IdNameWithNote(
            j.getPestCategory().getPestCategoryId(), j.getPestCategory().getPestCategory(), j.getPestNote()))
        .collect(Collectors.toList());
    List<CaseResponse.IdName> identifiers = caseEntity.getCaseIdentifiers().stream()
        .map(j -> new CaseResponse.IdName(
            j.getIdentifier().getIdentifierId(), j.getIdentifier().getIdentifier()))
        .collect(Collectors.toList());

    // 送件人鄉鎮/身分別可能未設定（如更新時僅換新身分未帶 district/type）
    Long senderDistrictId = Optional.ofNullable(caseEntity.getSender().getDistrict())
        .map(District::getDistrictId).orElse(null);
    String senderDistrictName = districtNameOf(caseEntity);
    String senderCityName = cityNameOf(caseEntity);
    Long senderTypeId = Optional.ofNullable(caseEntity.getSender().getSenderType())
        .map(SenderType::getSenderTypeId).orElse(null);
    boolean viewer = isViewer();
    String senderName = viewer ? null : caseEntity.getSender().getName();
    String senderDisplayName = viewer ? null : caseEntity.getSender().getDisplayName();
    String senderPhone = viewer ? null : caseEntity.getSender().getPhone();
    String senderAddress = viewer ? null : caseEntity.getSender().getAddress();

    return new CaseResponse(
        caseEntity.getCaseId(),
        caseEntity.getReceiveDate(),
        caseEntity.getCropScale(),
        caseEntity.getDamageScale(),
        caseEntity.getCaseDescription(),
        caseEntity.getHintDescription(),
        caseEntity.getStatus().name(),
        caseEntity.getCreatedAt(),
        caseEntity.getUpdatedAt(),
        caseEntity.getSender().getSenderId(),
        senderName,
        senderDisplayName,
        senderPhone,
        senderAddress,
        senderDistrictId,
        senderDistrictName,
        senderCityName,
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

  /** 送件人鄉鎮市區名稱（null-safe：歷史資料或更新流程可能未帶 district） */
  private static String districtNameOf(Case c) {
    return Optional.ofNullable(c.getSender().getDistrict())
        .map(District::getDistrict).orElse(null);
  }

  /** 送件人縣市名稱（null-safe） */
  private static String cityNameOf(Case c) {
    return Optional.ofNullable(c.getSender().getDistrict())
        .map(District::getCity).map(City::getCity).orElse(null);
  }

  /** 送件人身分別名稱（null-safe：同上） */
  private static String senderTypeNameOf(Case c) {
    return Optional.ofNullable(c.getSender().getSenderType())
        .map(SenderType::getSenderType).orElse(null);
  }
}