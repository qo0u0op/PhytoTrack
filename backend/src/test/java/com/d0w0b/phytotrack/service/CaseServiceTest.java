package com.d0w0b.phytotrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import com.d0w0b.phytotrack.dto.CaseDtos.CaseCreateRequest;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseSummaryResponse;
import com.d0w0b.phytotrack.dto.CaseDtos.CaseUpdateRequest;
import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.models.Case;
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
import com.d0w0b.phytotrack.models.Service;
import com.d0w0b.phytotrack.models.User;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 案件服務（CaseService）單元測試
 */
@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

  @Mock private CaseRepository caseRepository;
  @Mock private SenderRepository senderRepository;
  @Mock private SenderTypeRepository senderTypeRepository;
  @Mock private DistrictRepository districtRepository;
  @Mock private MethodRepository methodRepository;
  @Mock private CropRepository cropRepository;
  @Mock private ServiceRepository serviceRepository;
  @Mock private DeliveryRepository deliveryRepository;
  @Mock private DamageRepository damageRepository;
  @Mock private HintRepository hintRepository;
  @Mock private PestCategoryRepository pestCategoryRepository;
  @Mock private IdentifierRepository identifierRepository;

  private CaseService caseService;

  @BeforeEach
  void setUp() {
    caseService = new CaseService(caseRepository, senderRepository, senderTypeRepository,
        districtRepository, methodRepository, cropRepository, serviceRepository,
        deliveryRepository, damageRepository, hintRepository, pestCategoryRepository,
        identifierRepository);
  }

  private CaseCreateRequest validRequest() {
    return new CaseCreateRequest(
        LocalDate.of(2026, 8, 18), "2分地", "約3成", "葉片褐斑", "未用藥",
        "王小明", "0912345678", "臺中市霧峰區中正路1號", 1L, 1L,
        1L, 36L, 1L, 1L,
        List.of(3L), List.of(1L), List.of(1L), List.of(1L));
  }

  @Test
  void create_shouldCreateSenderWhenNotExists() {
    when(senderRepository.findByNameAndPhone("王小明", "0912345678")).thenReturn(Optional.empty());
    when(senderTypeRepository.findById(1L))
        .thenReturn(Optional.of(senderType(1L, "農民")));
    when(districtRepository.findById(1L)).thenReturn(Optional.of(district(1L)));
    when(senderRepository.save(any(Sender.class))).thenAnswer(i -> i.getArgument(0));
    when(methodRepository.findById(1L)).thenReturn(Optional.of(method(1L)));
    when(cropRepository.findById(36L)).thenReturn(Optional.of(crop(36L)));
    when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L)));
    when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery(1L)));
    when(damageRepository.findById(3L)).thenReturn(Optional.of(damage(3L)));
    when(hintRepository.findById(1L)).thenReturn(Optional.of(hint(1L)));
    when(pestCategoryRepository.findById(1L)).thenReturn(Optional.of(pestCategory(1L)));
    when(identifierRepository.findById(1L)).thenReturn(Optional.of(identifier(1L)));
    when(caseRepository.save(any(Case.class))).thenAnswer(i -> {
      Case saved = i.getArgument(0);
      saved.setCaseId(7L);
      saved.setCreatedBy(user());
      return saved;
    });

    CaseResponse response = caseService.create(validRequest());

    assertThat(response.caseId()).isEqualTo(7L);
    assertThat(response.senderName()).isEqualTo("王小明");
    assertThat(response.cropName()).isEqualTo("柑橘");
    // 送件人不存在的話應建立新送件人
    verify(senderRepository).save(any(Sender.class));
    // 多對多關聯都應寫入
    assertThat(response.damages()).hasSize(1);
    assertThat(response.hints()).hasSize(1);
    assertThat(response.pestCategories()).hasSize(1);
    assertThat(response.identifiers()).hasSize(1);
  }

  @Test
  void create_shouldReuseExistingSender() {
    when(senderRepository.findByNameAndPhone("王小明", "0912345678"))
        .thenReturn(Optional.of(sender()));
    when(methodRepository.findById(1L)).thenReturn(Optional.of(method(1L)));
    when(cropRepository.findById(36L)).thenReturn(Optional.of(crop(36L)));
    when(serviceRepository.findById(1L)).thenReturn(Optional.of(service(1L)));
    when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery(1L)));
    when(damageRepository.findById(3L)).thenReturn(Optional.of(damage(3L)));
    when(hintRepository.findById(1L)).thenReturn(Optional.of(hint(1L)));
    when(pestCategoryRepository.findById(1L)).thenReturn(Optional.of(pestCategory(1L)));
    when(identifierRepository.findById(1L)).thenReturn(Optional.of(identifier(1L)));
    when(caseRepository.save(any(Case.class))).thenAnswer(i -> {
      Case saved = i.getArgument(0);
      saved.setCreatedBy(user());
      return saved;
    });

    caseService.create(validRequest());

    // 已存在的送件人不應重複建立
    verify(senderRepository, never()).save(any(Sender.class));
  }

  @Test
  void create_shouldThrowWhenCropMissing() {
    when(senderRepository.findByNameAndPhone("王小明", "0912345678"))
        .thenReturn(Optional.of(sender()));
    when(methodRepository.findById(1L)).thenReturn(Optional.of(method(1L)));
    when(cropRepository.findById(36L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> caseService.create(validRequest()))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> {
          ApiException ex = (ApiException) e;
          assertThat(ex.getCode()).isEqualTo("REFERENCE_NOT_FOUND");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        });
  }

  @Test
  void list_shouldMapToSummary() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findAll(PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(c)));

    Page<CaseSummaryResponse> page = caseService.list(PageRequest.of(0, 10));

    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getContent().get(0).cropName()).isEqualTo("柑橘");
    assertThat(page.getContent().get(0).senderName()).isEqualTo("王小明");
  }

  @Test
  void update_shouldChangeOnlyProvidedFields() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setCropScale("舊值");
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    CaseResponse response = caseService.update(1L,
        new CaseUpdateRequest(LocalDate.of(2026, 8, 20), "新面積", null, null, null, 1,
            null, null, null, null));

    assertThat(response.cropScale()).isEqualTo("新面積");
    // 未提供的欄位應保持原值
    assertThat(response.damageScale()).isEqualTo("約3成");
  }

  @Test
  void delete_shouldThrowWhenCaseNotFound() {
    when(caseRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> caseService.delete(99L))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> assertThat(((ApiException) e).getCode()).isEqualTo("CASE_NOT_FOUND"));
    verify(caseRepository, never()).delete(any(Case.class));
  }

  // ---------------------------------------------------------------
  // 測試資料建構輔助
  // ---------------------------------------------------------------

  private User user() {
    User u = new User();
    u.setUserId(1L);
    u.setDisplayName("管理員");
    return u;
  }

  private Sender sender() {
    Sender s = new Sender();
    s.setName("王小明");
    s.setPhone("0912345678");
    return s;
  }

  private SenderType senderType(Long id, String name) {
    SenderType st = new SenderType();
    st.setSenderTypeId(id);
    st.setSenderType(name);
    return st;
  }

  private District district(Long id) {
    District d = new District();
    d.setDistrictId(id);
    d.setDistrict("霧峰區");
    return d;
  }

  private Method method(Long id) {
    Method m = new Method();
    m.setMethodId(id);
    m.setMethod("有機");
    return m;
  }

  private Crop crop(Long id) {
    Crop c = new Crop();
    c.setCropId(id);
    c.setCrop("柑橘");
    return c;
  }

  private Service service(Long id) {
    Service s = new Service();
    s.setServiceId(id);
    s.setService("診斷");
    return s;
  }

  private Delivery delivery(Long id) {
    Delivery d = new Delivery();
    d.setDeliverId(id);
    d.setDeliver("郵寄");
    return d;
  }

  private Damage damage(Long id) {
    Damage d = new Damage();
    d.setDamageId(id);
    d.setDamage("葉");
    return d;
  }

  private Hint hint(Long id) {
    Hint h = new Hint();
    h.setHintId(id);
    h.setHint("耕作防治");
    return h;
  }

  private PestCategory pestCategory(Long id) {
    PestCategory p = new PestCategory();
    p.setPestCategoryId(id);
    p.setPestCategory("真菌");
    return p;
  }

  private Identifier identifier(Long id) {
    Identifier i = new Identifier();
    i.setIdentifierId(id);
    i.setIdentifier("張志明");
    return i;
  }

  private Case caseWithRefs() {
    Case c = new Case();
    c.setReceiveDate(LocalDate.of(2026, 8, 18));
    c.setCropScale("2分地");
    c.setDamageScale("約3成");
    c.setStatus(0);
    c.setSender(sender());
    c.setMethod(method(1L));
    c.setCrop(crop(36L));
    c.setService(service(1L));
    c.setDelivery(delivery(1L));
    c.setCreatedBy(user());
    return c;
  }
}