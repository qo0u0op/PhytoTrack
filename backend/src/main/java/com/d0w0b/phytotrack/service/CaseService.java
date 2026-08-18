package com.d0w0b.phytotrack.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.dto.CaseDtos.CaseCreateRequest;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseSummaryResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseUpdateRequest;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Case;
import com.d0w0b.phytotrack.models.CaseDamage;
import com.d0w0b.phytotrack.models.CaseHint;
import com.d0w0b.phytotrack.models.CaseIdentifier;
import com.d0w0b.phytotrack.models.CasePestCategory;
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

import java.util.List;
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

  /** 分頁查詢案件清單（摘要） */
  @Transactional(readOnly = true)
  public Page<CaseSummaryResponse> list(Pageable pageable) {
    return caseRepository.findAll(pageable).map(this::toSummary);
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

    caseEntity.setSender(findOrCreateSender(request));
    caseEntity.setMethod(getRef(methodRepository, request.methodId(), "耕種方式"));
    caseEntity.setCrop(getRef(cropRepository, request.cropId(), "作物"));
    caseEntity.setService(getRef(serviceRepository, request.serviceId(), "服務類別"));
    caseEntity.setDelivery(getRef(deliveryRepository, request.deliverId(), "送件方式"));

    addJunctions(caseEntity, request.damageIds(), request.hintIds(),
        request.pestCategoryIds(), request.identifierIds());

    caseRepository.save(caseEntity);
    return toDetail(caseEntity);
  }

  /** 更新案件（僅更新有提供的欄位） */
  @Transactional
  public CaseResponse update(Long id, CaseUpdateRequest request) {
    Case caseEntity = findByIdOrThrow(id);

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
      caseEntity.setStatus(request.status());
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

    return toDetail(caseEntity);
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

  /** 建立案件的多對多關聯（Junction Record） */
  private void addJunctions(Case caseEntity,
                            List<Long> damageIds,
                            List<Long> hintIds,
                            List<Long> pestCategoryIds,
                            List<Long> identifierIds) {
    if (damageIds != null) {
      for (Long damageId : damageIds) {
        Damage damage = getRef(damageRepository, damageId, "被害部位");
        CaseDamage junction = new CaseDamage();
        junction.setCaseEntity(caseEntity);
        junction.setDamage(damage);
        caseEntity.getCaseDamages().add(junction);
      }
    }
    if (hintIds != null) {
      for (Long hintId : hintIds) {
        Hint hint = getRef(hintRepository, hintId, "防治建議");
        CaseHint junction = new CaseHint();
        junction.setCaseEntity(caseEntity);
        junction.setHint(hint);
        caseEntity.getCaseHints().add(junction);
      }
    }
    if (pestCategoryIds != null) {
      for (Long pestCategoryId : pestCategoryIds) {
        PestCategory category = getRef(pestCategoryRepository, pestCategoryId, "病蟲害分類");
        CasePestCategory junction = new CasePestCategory();
        junction.setCaseEntity(caseEntity);
        junction.setPestCategory(category);
        caseEntity.getCasePestCategories().add(junction);
      }
    }
    if (identifierIds != null) {
      for (Long identifierId : identifierIds) {
        Identifier identifier = getRef(identifierRepository, identifierId, "診斷簽名人");
        CaseIdentifier junction = new CaseIdentifier();
        junction.setCaseEntity(caseEntity);
        junction.setIdentifier(identifier);
        caseEntity.getCaseIdentifiers().add(junction);
      }
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
        caseEntity.getStatus(),
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

    return new CaseResponse(
        caseEntity.getCaseId(),
        caseEntity.getReceiveDate(),
        caseEntity.getCropScale(),
        caseEntity.getDamageScale(),
        caseEntity.getPestDescription(),
        caseEntity.getHintDescription(),
        caseEntity.getStatus(),
        caseEntity.getCreatedAt(),
        caseEntity.getUpdatedAt(),
        caseEntity.getSender().getName(),
        caseEntity.getSender().getPhone(),
        caseEntity.getSender().getAddress(),
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