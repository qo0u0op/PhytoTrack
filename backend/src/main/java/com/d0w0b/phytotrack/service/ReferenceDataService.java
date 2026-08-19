package com.d0w0b.phytotrack.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.d0w0b.phytotrack.dto.ReferenceDtos.CityResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.CropCategoryResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.IdNameResponse;
import com.d0w0b.phytotrack.dto.ReferenceDtos.PestTypeResponse;
import com.d0w0b.phytotrack.models.City;
import com.d0w0b.phytotrack.models.CropCategory;
import com.d0w0b.phytotrack.models.PestType;
import com.d0w0b.phytotrack.repository.CityRepository;
import com.d0w0b.phytotrack.repository.CropCategoryRepository;
import com.d0w0b.phytotrack.repository.DamageRepository;
import com.d0w0b.phytotrack.repository.DeliveryRepository;
import com.d0w0b.phytotrack.repository.HintRepository;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.repository.MethodRepository;
import com.d0w0b.phytotrack.repository.PestTypeRepository;
import com.d0w0b.phytotrack.repository.SenderTypeRepository;
import com.d0w0b.phytotrack.repository.ServiceRepository;

import java.util.Comparator;
import java.util.List;

/**
 * 參照資料服務（Reference Data Service）
 *
 * 提供診斷表單的下拉選單資料（作物、病蟲害類別、被害部位等）。
 * 全部為唯讀，方法標記 @Transactional(readOnly = true) 以便在交易內
 * 取用 @EntityGraph 或 Lazy 載入的關聯資料。
 */
@Service
public class ReferenceDataService {

  private final CropCategoryRepository cropCategoryRepository;
  private final PestTypeRepository pestTypeRepository;
  private final DamageRepository damageRepository;
  private final HintRepository hintRepository;
  private final MethodRepository methodRepository;
  private final DeliveryRepository deliveryRepository;
  private final ServiceRepository serviceRepository;
  private final CityRepository cityRepository;
  private final SenderTypeRepository senderTypeRepository;
  private final IdentifierRepository identifierRepository;

  public ReferenceDataService(CropCategoryRepository cropCategoryRepository,
                              PestTypeRepository pestTypeRepository,
                              DamageRepository damageRepository,
                              HintRepository hintRepository,
                              MethodRepository methodRepository,
                              DeliveryRepository deliveryRepository,
                              ServiceRepository serviceRepository,
                              CityRepository cityRepository,
                              SenderTypeRepository senderTypeRepository,
                              IdentifierRepository identifierRepository) {
    this.cropCategoryRepository = cropCategoryRepository;
    this.pestTypeRepository = pestTypeRepository;
    this.damageRepository = damageRepository;
    this.hintRepository = hintRepository;
    this.methodRepository = methodRepository;
    this.deliveryRepository = deliveryRepository;
    this.serviceRepository = serviceRepository;
    this.cityRepository = cityRepository;
    this.senderTypeRepository = senderTypeRepository;
    this.identifierRepository = identifierRepository;
  }

  /** 作物分類（含作物清單） */
  @Transactional(readOnly = true)
  public List<CropCategoryResponse> cropCategories() {
    return cropCategoryRepository.findAllByOrderByCropCategoryIdAsc().stream()
        .map(this::toCropCategoryResponse)
        .toList();
  }

  /** 害物類型（含小分類清單，依 sortOrder 排序） */
  @Transactional(readOnly = true)
  public List<PestTypeResponse> pestTypes() {
    return pestTypeRepository.findAllByOrderByPestTypeIdAsc().stream()
        .map(this::toPestTypeResponse)
        .toList();
  }

  /** 被害部位 */
  @Transactional(readOnly = true)
  public List<IdNameResponse> damages() {
    return damageRepository.findAll().stream()
        .map(d -> new IdNameResponse(d.getDamageId(), d.getDamage()))
        .toList();
  }

  /** 防治建議 */
  @Transactional(readOnly = true)
  public List<IdNameResponse> hints() {
    return hintRepository.findAll().stream()
        .map(h -> new IdNameResponse(h.getHintId(), h.getHint()))
        .toList();
  }

  /** 耕種方式 */
  @Transactional(readOnly = true)
  public List<IdNameResponse> methods() {
    return methodRepository.findAll().stream()
        .map(m -> new IdNameResponse(m.getMethodId(), m.getMethod()))
        .toList();
  }

  /** 送件方式 */
  @Transactional(readOnly = true)
  public List<IdNameResponse> deliveries() {
    return deliveryRepository.findAll().stream()
        .map(d -> new IdNameResponse(d.getDeliverId(), d.getDeliver()))
        .toList();
  }

  /** 服務類別 */
  @Transactional(readOnly = true)
  public List<IdNameResponse> services() {
    return serviceRepository.findAll().stream()
        .map(s -> new IdNameResponse(s.getServiceId(), s.getService()))
        .toList();
  }

  /** 縣市（含鄉鎮市區清單） */
  @Transactional(readOnly = true)
  public List<CityResponse> cities() {
    return cityRepository.findAllByOrderBySortOrderAsc().stream()
        .map(this::toCityResponse)
        .toList();
  }

  /** 送件人身分別 */
  @Transactional(readOnly = true)
  public List<IdNameResponse> senderTypes() {
    return senderTypeRepository.findAll().stream()
        .map(s -> new IdNameResponse(s.getSenderTypeId(), s.getSenderType()))
        .toList();
  }

  /** 診斷簽名人 */
  @Transactional(readOnly = true)
  public List<IdNameResponse> identifiers() {
    return identifierRepository.findAll().stream()
        .map(i -> new IdNameResponse(i.getIdentifierId(), i.getIdentifier()))
        .toList();
  }

  private CropCategoryResponse toCropCategoryResponse(CropCategory category) {
    List<CropCategoryResponse.CropItem> crops = category.getCrops().stream()
        .map(c -> new CropCategoryResponse.CropItem(c.getCropId(), c.getCrop()))
        .toList();
    return new CropCategoryResponse(category.getCropCategoryId(), category.getCropCategory(), crops);
  }

  private PestTypeResponse toPestTypeResponse(PestType pestType) {
    List<PestTypeResponse.PestCategoryItem> categories = pestType.getCategories().stream()
        .sorted(Comparator.comparingInt(c -> c.getSortOrder()))
        .map(c -> new PestTypeResponse.PestCategoryItem(
            c.getPestCategoryId(), c.getPestCategoryCode(), c.getPestCategory(), c.getSortOrder()))
        .toList();
    return new PestTypeResponse(pestType.getPestTypeId(), pestType.getPestType(), categories);
  }

  private CityResponse toCityResponse(City city) {
    List<CityResponse.DistrictItem> districts = city.getDistricts().stream()
        .sorted(Comparator.comparingInt(d -> d.getSortOrder()))
        .map(d -> new CityResponse.DistrictItem(d.getDistrictId(), d.getDistrict(), d.getSortOrder()))
        .toList();
    return new CityResponse(city.getCityId(), city.getCity(), districts);
  }
}