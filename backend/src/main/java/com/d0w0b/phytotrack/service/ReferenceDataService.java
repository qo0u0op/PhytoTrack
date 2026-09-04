package com.d0w0b.phytotrack.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.dto.ReferenceDtos.CityResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestTypeResponse;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.City;
import com.d0w0b.phytotrack.models.Crop;
import com.d0w0b.phytotrack.models.CropCategory;
import com.d0w0b.phytotrack.models.Damage;
import com.d0w0b.phytotrack.models.Delivery;
import com.d0w0b.phytotrack.models.District;
import com.d0w0b.phytotrack.models.Hint;
import com.d0w0b.phytotrack.models.Identifier;
import com.d0w0b.phytotrack.models.Method;
import com.d0w0b.phytotrack.models.PestCategory;
import com.d0w0b.phytotrack.models.PestType;
import com.d0w0b.phytotrack.models.SenderType;
import com.d0w0b.phytotrack.repository.CaseRepository;
import com.d0w0b.phytotrack.repository.CityRepository;
import com.d0w0b.phytotrack.repository.CropCategoryRepository;
import com.d0w0b.phytotrack.repository.CropRepository;
import com.d0w0b.phytotrack.repository.DamageRepository;
import com.d0w0b.phytotrack.repository.DeliveryRepository;
import com.d0w0b.phytotrack.repository.DistrictRepository;
import com.d0w0b.phytotrack.repository.HintRepository;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.MethodRepository;
import com.d0w0b.phytotrack.repository.PestCategoryRepository;
import com.d0w0b.phytotrack.repository.PestTypeRepository;
import com.d0w0b.phytotrack.repository.SenderRepository;
import com.d0w0b.phytotrack.repository.SenderTypeRepository;
import com.d0w0b.phytotrack.repository.ServiceRepository;

import java.util.Comparator;
import java.util.List;

/**
 * 參照資料服務 (Reference Data Service)
 *
 * 提供診斷表單的下拉選單資料 (作物、病蟲害類別、被害部位等)。
 * 全部為唯讀，方法標記 @Transactional (readOnly = true) 以便在交易內
 * 取用 @EntityGraph 或 Lazy 載入的關聯資料。
 * 寫入方法 (ADMIN 專用) 提供新增/修改/刪除，刪除前檢查是否被案件引用。
 */
@Service
public class ReferenceDataService {

  private final CropCategoryRepository cropCategoryRepository;
  private final CropRepository cropRepository;
  private final PestTypeRepository pestTypeRepository;
  private final PestCategoryRepository pestCategoryRepository;
  private final DamageRepository damageRepository;
  private final HintRepository hintRepository;
  private final MethodRepository methodRepository;
  private final DeliveryRepository deliveryRepository;
  private final ServiceRepository serviceRepository;
  private final CityRepository cityRepository;
  private final DistrictRepository districtRepository;
  private final SenderTypeRepository senderTypeRepository;
  private final IdentifierRepository identifierRepository;
  private final CaseRepository caseRepository;
  private final SenderRepository senderRepository;

  public ReferenceDataService (CropCategoryRepository cropCategoryRepository,
                              CropRepository cropRepository,
                              PestTypeRepository pestTypeRepository,
                              PestCategoryRepository pestCategoryRepository,
                              DamageRepository damageRepository,
                              HintRepository hintRepository,
                              MethodRepository methodRepository,
                              DeliveryRepository deliveryRepository,
                              ServiceRepository serviceRepository,
                              CityRepository cityRepository,
                              DistrictRepository districtRepository,
                              SenderTypeRepository senderTypeRepository,
                              IdentifierRepository identifierRepository,
                              CaseRepository caseRepository,
                              SenderRepository senderRepository) {
    this.cropCategoryRepository = cropCategoryRepository;
    this.cropRepository = cropRepository;
    this.pestTypeRepository = pestTypeRepository;
    this.pestCategoryRepository = pestCategoryRepository;
    this.damageRepository = damageRepository;
    this.hintRepository = hintRepository;
    this.methodRepository = methodRepository;
    this.deliveryRepository = deliveryRepository;
    this.serviceRepository = serviceRepository;
    this.cityRepository = cityRepository;
    this.districtRepository = districtRepository;
    this.senderTypeRepository = senderTypeRepository;
    this.identifierRepository = identifierRepository;
    this.caseRepository = caseRepository;
    this.senderRepository = senderRepository;
  }

  /** 作物分類 (含作物清單) */
  @Transactional (readOnly = true)
  public List<CropCategoryResponse> cropCategories () {
    return cropCategoryRepository.findAllByOrderByCropCategoryIdAsc ().stream ()
        .map (this::toCropCategoryResponse)
        .toList ();
  }

  /** 害物類型 (含小分類清單，依代碼升冪排序) */
  @Transactional (readOnly = true)
  public List<PestTypeResponse> pestTypes () {
    return pestTypeRepository.findAllByOrderByPestTypeIdAsc ().stream ()
        .map (this::toPestTypeResponse)
        .toList ();
  }

  /** 被害部位 */
  @Transactional (readOnly = true)
  public List<IdNameResponse> damages () {
    return damageRepository.findAll ().stream ()
        .map (d -> new IdNameResponse (d.getDamageId (), d.getDamage ()))
        .toList ();
  }

  /** 防治建議 */
  @Transactional (readOnly = true)
  public List<IdNameResponse> hints () {
    return hintRepository.findAll ().stream ()
        .map (h -> new IdNameResponse (h.getHintId (), h.getHint ()))
        .toList ();
  }

  /** 耕種方式 */
  @Transactional (readOnly = true)
  public List<IdNameResponse> methods () {
    return methodRepository.findAll ().stream ()
        .map (m -> new IdNameResponse (m.getMethodId (), m.getMethod ()))
        .toList ();
  }

  /** 送件方式 */
  @Transactional (readOnly = true)
  public List<IdNameResponse> deliveries () {
    return deliveryRepository.findAll ().stream ()
        .map (d -> new IdNameResponse (d.getDeliverId (), d.getDeliver ()))
        .toList ();
  }

  /** 服務類別 */
  @Transactional (readOnly = true)
  public List<IdNameResponse> services () {
    return serviceRepository.findAll ().stream ()
        .map (s -> new IdNameResponse (s.getServiceId (), s.getService ()))
        .toList ();
  }

  /** 縣市 (含鄉鎮市區清單) */
  @Transactional (readOnly = true)
  public List<CityResponse> cities () {
    return cityRepository.findAllByOrderByCityIdAsc ().stream ()
        .map (this::toCityResponse)
        .toList ();
  }

  /** 送件人身分別 */
  @Transactional (readOnly = true)
  public List<IdNameResponse> senderTypes () {
    return senderTypeRepository.findAll ().stream ()
        .map (s -> new IdNameResponse (s.getSenderTypeId (), s.getSenderType ()))
        .toList ();
  }

  /** 診斷簽名人 */
  @Transactional (readOnly = true)
  public List<IdNameResponse> identifiers () {
    return identifiers (false).stream ().map (r -> new IdNameResponse (r.id (), r.name ())).toList ();
  }

  @Transactional (readOnly = true)
  public List<com.d0w0b.phytotrack.dto.ReferenceDtos.IdentifierResponse> identifiers (boolean includeInactive) {
    List<Identifier> list = includeInactive ? identifierRepository.findAll ()
        : identifierRepository.findByActiveTrue ();
    return list.stream ()
        .map (i -> new com.d0w0b.phytotrack.dto.ReferenceDtos.IdentifierResponse (i.getIdentifierId (), i.getIdentifier (), i.isActive (),
            i.getUser () != null ? i.getUser ().getUserId () : null,
            i.getUser () != null ? i.getUser ().getUsername () : null))
        .toList ();
  }

  @Transactional
  public IdNameResponse updateIdentifierActive (Long id, boolean active) {
    Identifier e = identifierRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "簽名人不存在"));
    if (!active && e.isActive () && identifierRepository.findByActiveTrue ().size () <= 1) {
      throw new ApiException ("LAST_ACTIVE_SIGNER", HttpStatus.CONFLICT, "不可停用最後一個啟用中簽名人");
    }
    e.setActive (active);
    return new IdNameResponse (e.getIdentifierId (), e.getIdentifier ());
  }

  // ===== 寫入：IdName 通用類型 =====

  @Transactional
  public IdNameResponse createDamage (String name) {
    Damage e = new Damage ();
    e.setDamage (name.trim ());
    damageRepository.save (e);
    return new IdNameResponse (e.getDamageId (), e.getDamage ());
  }

  @Transactional
  public IdNameResponse updateDamage (Long id, String name) {
    Damage e = damageRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "被害部位不存在"));
    e.setDamage (name.trim ());
    return new IdNameResponse (e.getDamageId (), e.getDamage ());
  }

  @Transactional
  public void deleteDamage (Long id) {
    Damage e = damageRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "被害部位不存在"));
    if (caseRepository.existsByCaseDamagesDamageDamageId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    damageRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createHint (String name) {
    Hint e = new Hint ();
    e.setHint (name.trim ());
    hintRepository.save (e);
    return new IdNameResponse (e.getHintId (), e.getHint ());
  }

  @Transactional
  public IdNameResponse updateHint (Long id, String name) {
    Hint e = hintRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "防治建議不存在"));
    e.setHint (name.trim ());
    return new IdNameResponse (e.getHintId (), e.getHint ());
  }

  @Transactional
  public void deleteHint (Long id) {
    Hint e = hintRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "防治建議不存在"));
    if (caseRepository.existsByCaseHintsHintHintId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    hintRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createMethod (String name) {
    Method e = new Method ();
    e.setMethod (name.trim ());
    methodRepository.save (e);
    return new IdNameResponse (e.getMethodId (), e.getMethod ());
  }

  @Transactional
  public IdNameResponse updateMethod (Long id, String name) {
    Method e = methodRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "耕種方式不存在"));
    e.setMethod (name.trim ());
    return new IdNameResponse (e.getMethodId (), e.getMethod ());
  }

  @Transactional
  public void deleteMethod (Long id) {
    Method e = methodRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "耕種方式不存在"));
    if (caseRepository.existsByMethodMethodId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    methodRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createDelivery (String name) {
    Delivery e = new Delivery ();
    e.setDeliver (name.trim ());
    deliveryRepository.save (e);
    return new IdNameResponse (e.getDeliverId (), e.getDeliver ());
  }

  @Transactional
  public IdNameResponse updateDelivery (Long id, String name) {
    Delivery e = deliveryRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "送件方式不存在"));
    e.setDeliver (name.trim ());
    return new IdNameResponse (e.getDeliverId (), e.getDeliver ());
  }

  @Transactional
  public void deleteDelivery (Long id) {
    Delivery e = deliveryRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "送件方式不存在"));
    if (caseRepository.existsByDeliveryDeliverId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    deliveryRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createService (String name) {
    com.d0w0b.phytotrack.models.Service e = new com.d0w0b.phytotrack.models.Service ();
    e.setService (name.trim ());
    serviceRepository.save (e);
    return new IdNameResponse (e.getServiceId (), e.getService ());
  }

  @Transactional
  public IdNameResponse updateService (Long id, String name) {
    com.d0w0b.phytotrack.models.Service e = serviceRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "服務類別不存在"));
    e.setService (name.trim ());
    return new IdNameResponse (e.getServiceId (), e.getService ());
  }

  @Transactional
  public void deleteService (Long id) {
    com.d0w0b.phytotrack.models.Service e = serviceRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "服務類別不存在"));
    if (caseRepository.existsByServiceServiceId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    serviceRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createIdentifier (String name) {
    String trimmed = IdentifierNames.display (name);
    synchronized (("signer:" + IdentifierNames.normalize (trimmed)).intern ()) {
      boolean exists = identifierRepository.findByActiveTrue ().stream ()
          .anyMatch (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), trimmed));
      if (exists) {
        throw new ApiException ("DISPLAY_NAME_EXISTS", HttpStatus.CONFLICT, "顯示名稱已存在");
      }
      Identifier e = new Identifier ();
      e.setIdentifier (trimmed);
      e.setActive (true);
      try {
        identifierRepository.save (e);
      } catch (org.springframework.dao.DataIntegrityViolationException ex) {
        throw new ApiException ("DISPLAY_NAME_EXISTS", HttpStatus.CONFLICT, "顯示名稱已存在");
      }
      return new IdNameResponse (e.getIdentifierId (), e.getIdentifier ());
    }
  }

  /**
   * 案件內聯共用：同分類同名作物存在即復用，否則新建。
   * 與管理頁 `createCrop` 同一去重語意，差別在內聯不拋 409 而是回既有實體。
   */
  @Transactional
  public Crop findOrCreateCrop (String name, Long cropCategoryId) {
    if (name == null || name.trim ().isEmpty ()) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "作物名稱不可為空白");
    }
    if (cropCategoryId == null) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "作物分類不可為空");
    }
    String trimmed = name.trim ();
    var existing = cropRepository.findByCropIgnoreCaseAndCropCategoryCropCategoryId (trimmed, cropCategoryId);
    if (existing.isPresent ()) return existing.get ();
    CropCategory category = cropCategoryRepository.findById (cropCategoryId)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "作物分類不存在"));
    Crop e = new Crop ();
    e.setCrop (trimmed);
    e.setCropCategory (category);
    try {
      return cropRepository.save (e);
    } catch (org.springframework.dao.DataIntegrityViolationException ex) {
      // 併發同時新建：查回既有
      return cropRepository.findByCropIgnoreCaseAndCropCategoryCropCategoryId (trimmed, cropCategoryId)
          .orElseThrow (() -> ex);
    }
  }

  /**
   * 案件內聯共用：同名 `active` 非使用者簽名人存在即復用（正規化比對，取 id 最小者），否則新建
   * `user IS NULL, active=true`。與管理頁同名檢查同一比對語意，內聯不拋 409。
   */
  @Transactional
  public Identifier findOrCreateIdentifier (String name) {
    String trimmed = IdentifierNames.display (name);
    if (trimmed.isEmpty ()) {
      throw new ApiException ("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "簽名人名稱不可為空白");
    }
    synchronized (("signer:" + IdentifierNames.normalize (trimmed)).intern ()) {
      var existingOpt = identifierRepository.findByUserIsNullAndActiveTrue ().stream ()
          .filter (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), trimmed))
          .sorted (java.util.Comparator.comparing (Identifier::getIdentifierId))
          .findFirst ();
      if (existingOpt.isPresent ()) return existingOpt.get ();
      Identifier e = new Identifier ();
      e.setIdentifier (trimmed);
      e.setUser (null);
      e.setActive (true);
      try {
        return identifierRepository.save (e);
      } catch (org.springframework.dao.DataIntegrityViolationException ex) {
        // 併發同時新建：查回既有
        return identifierRepository.findByUserIsNullAndActiveTrue ().stream ()
            .filter (i -> IdentifierNames.equalsNormalized (i.getIdentifier (), trimmed))
            .sorted (java.util.Comparator.comparing (Identifier::getIdentifierId))
            .findFirst ().orElseThrow (() -> ex);
      }
    }
  }

  @Transactional
  public IdNameResponse updateIdentifier (Long id, String name) {    Identifier e = identifierRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "簽名人不存在"));
    if (e.getUser () != null) {
      throw new ApiException ("USER_LINKED_SIGNER_IMMUTABLE", HttpStatus.CONFLICT, "此簽名人關聯系統使用者，請改個人檔案顯示名稱");
    }
    String trimmed = IdentifierNames.display (name);
    boolean duplicate = identifierRepository.findByActiveTrue ().stream ()
        .anyMatch (other -> !other.getIdentifierId ().equals (id)
            && IdentifierNames.equalsNormalized (other.getIdentifier (), trimmed));
    if (duplicate) {
      throw new ApiException ("DISPLAY_NAME_EXISTS", HttpStatus.CONFLICT, "顯示名稱已存在");
    }
    e.setIdentifier (trimmed);
    return new IdNameResponse (e.getIdentifierId (), e.getIdentifier ());
  }

  @Transactional
  public void deleteIdentifier (Long id) {
    throw new ApiException ("METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED, "簽名人僅可停用，請使用 PATCH .../active");
  }

  @Transactional
  public IdNameResponse createSenderType (String name) {
    SenderType e = new SenderType ();
    e.setSenderType (name.trim ());
    senderTypeRepository.save (e);
    return new IdNameResponse (e.getSenderTypeId (), e.getSenderType ());
  }

  @Transactional
  public IdNameResponse updateSenderType (Long id, String name) {
    SenderType e = senderTypeRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "身分別不存在"));
    e.setSenderType (name.trim ());
    return new IdNameResponse (e.getSenderTypeId (), e.getSenderType ());
  }

  @Transactional
  public void deleteSenderType (Long id) {
    SenderType e = senderTypeRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "身分別不存在"));
    if (senderRepository.existsBySenderTypeSenderTypeId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被送件人引用，無法刪除");
    }
    senderTypeRepository.delete (e);
  }

  // ===== 階層式：Crop / CropCategory / PestCategory =====

  @Transactional
  public IdNameResponse createCrop (String name, Long cropCategoryId) {
    CropCategory category = cropCategoryRepository.findById (cropCategoryId)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "作物分類不存在"));
    if (cropRepository.existsByCropIgnoreCaseAndCropCategoryCropCategoryId (name.trim (), cropCategoryId)) {
      throw new ApiException ("REFERENCE_DUPLICATE", HttpStatus.CONFLICT, "同分類下已有同名作物");
    }
    Crop e = new Crop ();
    e.setCrop (name.trim ());
    e.setCropCategory (category);
    cropRepository.save (e);
    return new IdNameResponse (e.getCropId (), e.getCrop ());
  }

  @Transactional
  public IdNameResponse updateCrop (Long id, String name, Long cropCategoryId) {
    Crop e = cropRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "作物不存在"));
    CropCategory category = cropCategoryRepository.findById (cropCategoryId)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "作物分類不存在"));
    cropRepository.findByCropIgnoreCaseAndCropCategoryCropCategoryId (name.trim (), cropCategoryId)
        .filter (other -> !other.getCropId ().equals (id))
        .ifPresent (other -> {
          throw new ApiException ("REFERENCE_DUPLICATE", HttpStatus.CONFLICT, "同分類下已有同名作物");
        });
    e.setCrop (name.trim ());
    e.setCropCategory (category);
    return new IdNameResponse (e.getCropId (), e.getCrop ());
  }

  @Transactional
  public void deleteCrop (Long id) {
    Crop e = cropRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "作物不存在"));
    if (caseRepository.existsByCropCropId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    cropRepository.delete (e);
  }

  // ===== 縣市 / 鄉鎮市區 =====

  @Transactional
  public IdNameResponse createCity (String name) {
    City e = new City ();
    e.setCity (name.trim ());
    cityRepository.save (e);
    return new IdNameResponse (e.getCityId (), e.getCity ());
  }

  @Transactional
  public IdNameResponse updateCity (Long id, String name) {
    City e = cityRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "縣市不存在"));
    e.setCity (name.trim ());
    return new IdNameResponse (e.getCityId (), e.getCity ());
  }

  @Transactional
  public void deleteCity (Long id) {
    City e = cityRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "縣市不存在"));
    if (districtRepository.existsByCityCityId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "縣市下仍有鄉鎮市區，無法刪除");
    }
    cityRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createDistrict (String name, Long cityId) {
    City city = cityRepository.findById (cityId)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "縣市不存在"));
    District e = new District ();
    e.setDistrict (name.trim ());
    e.setCity (city);
    districtRepository.save (e);
    return new IdNameResponse (e.getDistrictId (), e.getDistrict ());
  }

  @Transactional
  public IdNameResponse updateDistrict (Long id, String name, Long cityId) {
    District e = districtRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "鄉鎮市區不存在"));
    City city = cityRepository.findById (cityId)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "縣市不存在"));
    e.setDistrict (name.trim ());
    e.setCity (city);
    return new IdNameResponse (e.getDistrictId (), e.getDistrict ());
  }

  @Transactional
  public void deleteDistrict (Long id) {
    District e = districtRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "鄉鎮市區不存在"));
    if (senderRepository.existsByDistrictDistrictId (id)
        || caseRepository.existsByFieldDistrictDistrictId (id)
        || caseRepository.existsBySenderDistrictDistrictId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被送件人或案件引用，無法刪除");
    }
    districtRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createCropCategory (String name) {
    CropCategory e = new CropCategory ();
    e.setCropCategory (name.trim ());
    cropCategoryRepository.save (e);
    return new IdNameResponse (e.getCropCategoryId (), e.getCropCategory ());
  }

  @Transactional
  public IdNameResponse updateCropCategory (Long id, String name) {
    CropCategory e = cropCategoryRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "作物分類不存在"));
    e.setCropCategory (name.trim ());
    return new IdNameResponse (e.getCropCategoryId (), e.getCropCategory ());
  }

  @Transactional
  public void deleteCropCategory (Long id) {
    CropCategory e = cropCategoryRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "作物分類不存在"));
    if (cropRepository.existsByCropCategoryCropCategoryId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "分類下仍有作物，無法刪除");
    }
    // 亦檢查是否被案件透過作物間接引用
    if (caseRepository.existsByCropCropCategoryCropCategoryId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    cropCategoryRepository.delete (e);
  }

  @Transactional
  public IdNameResponse createPestCategory (String code, String name, Long pestTypeId) {
    PestType pestType = pestTypeRepository.findById (pestTypeId)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "害物類型不存在"));
    if (pestCategoryRepository.existsByPestTypePestTypeIdAndPestCategoryCodeIgnoreCase (pestTypeId, code.trim ())) {
      throw new ApiException ("REFERENCE_DUPLICATE", HttpStatus.CONFLICT, "同類型下已有相同代碼");
    }
    if (pestCategoryRepository.existsByPestTypePestTypeIdAndPestCategoryIgnoreCase (pestTypeId, name.trim ())) {
      throw new ApiException ("REFERENCE_DUPLICATE", HttpStatus.CONFLICT, "同類型下已有相同名稱");
    }
    PestCategory e = new PestCategory ();
    e.setPestCategoryCode (code.trim ());
    e.setPestCategory (name.trim ());
    e.setPestType (pestType);
    pestCategoryRepository.save (e);
    return new IdNameResponse (e.getPestCategoryId (), e.getPestCategory ());
  }

  @Transactional
  public IdNameResponse updatePestCategory (Long id, String code, String name, Long pestTypeId) {
    PestCategory e = pestCategoryRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "病蟲害分類不存在"));
    PestType pestType = pestTypeRepository.findById (pestTypeId)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "害物類型不存在"));
    pestCategoryRepository.findByPestTypePestTypeIdAndPestCategoryCodeIgnoreCase (pestTypeId, code.trim ())
        .filter (other -> !other.getPestCategoryId ().equals (id))
        .ifPresent (other -> {
          throw new ApiException ("REFERENCE_DUPLICATE", HttpStatus.CONFLICT, "同類型下已有相同代碼");
        });
    pestCategoryRepository.findByPestTypePestTypeIdAndPestCategoryIgnoreCase (pestTypeId, name.trim ())
        .filter (other -> !other.getPestCategoryId ().equals (id))
        .ifPresent (other -> {
          throw new ApiException ("REFERENCE_DUPLICATE", HttpStatus.CONFLICT, "同類型下已有相同名稱");
        });
    e.setPestCategoryCode (code.trim ());
    e.setPestCategory (name.trim ());
    e.setPestType (pestType);
    return new IdNameResponse (e.getPestCategoryId (), e.getPestCategory ());
  }

  @Transactional
  public void deletePestCategory (Long id) {
    PestCategory e = pestCategoryRepository.findById (id)
        .orElseThrow (() -> new ApiException ("REFERENCE_NOT_FOUND", HttpStatus.NOT_FOUND, "病蟲害分類不存在"));
    if (caseRepository.existsByCasePestCategoriesPestCategoryPestCategoryId (id)) {
      throw new ApiException ("REFERENCE_IN_USE", HttpStatus.CONFLICT, "已被案件引用，無法刪除");
    }
    pestCategoryRepository.delete (e);
  }

  private CropCategoryResponse toCropCategoryResponse (CropCategory category) {
    List<CropCategoryResponse.CropItem> crops = category.getCrops ().stream ()
        .map (c -> new CropCategoryResponse.CropItem (c.getCropId (), c.getCrop ()))
        .toList ();
    return new CropCategoryResponse (category.getCropCategoryId (), category.getCropCategory (), crops);
  }

  private PestTypeResponse toPestTypeResponse (PestType pestType) {
    List<PestTypeResponse.PestCategoryItem> categories = pestType.getCategories ().stream ()
        .sorted (Comparator.comparing (PestCategory::getPestCategoryCode))
        .map (c -> new PestTypeResponse.PestCategoryItem (c.getPestCategoryId (), c.getPestCategoryCode (), c.getPestCategory ()))
        .toList ();
    return new PestTypeResponse (pestType.getPestTypeId (), pestType.getPestType (), categories);
  }

  private CityResponse toCityResponse (City city) {
    List<CityResponse.DistrictItem> districts = city.getDistricts ().stream ()
        .sorted (Comparator.comparing (District::getDistrictId))
        .map (d -> new CityResponse.DistrictItem (d.getDistrictId (), d.getDistrict ()))
        .toList ();
    return new CityResponse (city.getCityId (), city.getCity (), districts);
  }
}
