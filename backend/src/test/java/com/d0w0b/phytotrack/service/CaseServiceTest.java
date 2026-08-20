package com.d0w0b.phytotrack.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
import java.time.YearMonth;
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

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  /** 以指定角色登入 SecurityContext（用於轉移規則的 ADMIN 判斷） */
  private void authenticateAs(String role) {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("test", "pass",
            List.of(new SimpleGrantedAuthority("ROLE_" + role))));
  }

  /** 組更新請求：狀態與更新契約欄位可選 */
  private CaseUpdateRequest updateReq(String status) {
    return new CaseUpdateRequest(
        null, null, null, null, null, status,
        null, null, null, null,
        null, null, null, null, null,
        null, null, null, null);
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
  void list_withEmptyFilter_shouldMapToSummary() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findAll(PageRequest.of(0, 10)))
        .thenReturn(new PageImpl<>(List.of(c)));

    Page<CaseSummaryResponse> page = caseService.list(CaseFilter.empty(), PageRequest.of(0, 10));

    assertThat(page.getContent()).hasSize(1);
    assertThat(page.getContent().get(0).cropName()).isEqualTo("柑橘");
    assertThat(page.getContent().get(0).senderName()).isEqualTo("王小明");
  }

  @Test
  void list_withFilter_shouldQueryWithSpecification() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    PageRequest pageable = PageRequest.of(0, 10);
    when(caseRepository.findAll(any(Specification.class), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(c)));

    CaseFilter filter = new CaseFilter(36L, 1L, "王", null, null, "RESOLVED");
    Page<CaseSummaryResponse> page = caseService.list(filter, pageable);

    // 非空條件應走 Specification 查詢
    verify(caseRepository).findAll(any(Specification.class), eq(pageable));
    assertThat(page.getContent()).hasSize(1);
  }

  @Test
  void list_withInvalidStatus_shouldThrowBadRequest() {
    CaseFilter filter = new CaseFilter(null, null, null, null, null, "DRAFT");

    assertThatThrownBy(() -> caseService.list(filter, PageRequest.of(0, 10)))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> {
          ApiException ex = (ApiException) e;
          assertThat(ex.getCode()).isEqualTo("INVALID_STATUS");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        });
    // 非法狀態不應觸發查詢
    verify(caseRepository, never()).findAll(any(Specification.class), any(Pageable.class));
  }

  @Test
  void update_shouldChangeOnlyProvidedFields() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setCropScale("舊值");
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    CaseResponse response = caseService.update(1L,
        new CaseUpdateRequest(LocalDate.of(2026, 8, 20), "新面積", null, null, null, "RESOLVED",
            null, null, null, null, null, null, null, null, null,
            null, null, null, null));

    assertThat(response.cropScale()).isEqualTo("新面積");
    // 未提供的欄位應保持原值
    assertThat(response.damageScale()).isEqualTo("約3成");
  }

  @Test
  void update_shouldTransitionPendingToResolved() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    CaseResponse response = caseService.update(1L, updateReq("RESOLVED"));

    assertThat(response.status()).isEqualTo("RESOLVED");
  }

  @Test
  void update_shouldRejectSkippingStatus() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    assertThatThrownBy(() -> caseService.update(1L, updateReq("CLOSED")))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> {
          ApiException ex = (ApiException) e;
          assertThat(ex.getCode()).isEqualTo("INVALID_STATUS_TRANSITION");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        });
    // 非法轉移狀態不變
    assertThat(c.getStatus()).isEqualTo(CaseStatus.PENDING);
  }

  @Test
  void update_shouldRejectStatusRegression() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setStatus(CaseStatus.RESOLVED);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    assertThatThrownBy(() -> caseService.update(1L, updateReq("PENDING")))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> {
          ApiException ex = (ApiException) e;
          assertThat(ex.getCode()).isEqualTo("INVALID_STATUS_TRANSITION");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        });
    assertThat(c.getStatus()).isEqualTo(CaseStatus.RESOLVED);
  }

  @Test
  void update_closingByStaffShouldBeForbidden() {
    authenticateAs("STAFF");
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setStatus(CaseStatus.RESOLVED);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    assertThatThrownBy(() -> caseService.update(1L, updateReq("CLOSED")))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> {
          ApiException ex = (ApiException) e;
          assertThat(ex.getCode()).isEqualTo("STATUS_TRANSITION_FORBIDDEN");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        });
    assertThat(c.getStatus()).isEqualTo(CaseStatus.RESOLVED);
  }

  @Test
  void update_closingByAdminShouldSucceed() {
    authenticateAs("ADMIN");
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setStatus(CaseStatus.RESOLVED);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    CaseResponse response = caseService.update(1L, updateReq("CLOSED"));

    assertThat(response.status()).isEqualTo("CLOSED");
  }

  @Test
  void update_contentOnClosedByStaffShouldBeForbidden() {
    authenticateAs("STAFF");
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setStatus(CaseStatus.CLOSED);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    CaseUpdateRequest request = new CaseUpdateRequest(
        LocalDate.of(2026, 8, 19), null, null, null, null, null,
        null, null, null, null,
        null, null, null, null, null,
        null, null, null, null);

    assertThatThrownBy(() -> caseService.update(1L, request))
        .isInstanceOf(ApiException.class)
        .satisfies(e -> {
          ApiException ex = (ApiException) e;
          assertThat(ex.getCode()).isEqualTo("CLOSED_CASE_READONLY");
          assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        });
    assertThat(c.getReceiveDate()).isEqualTo(LocalDate.of(2026, 8, 18));
  }

  @Test
  void update_contentOnClosedByAdminShouldSucceed() {
    authenticateAs("ADMIN");
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setStatus(CaseStatus.CLOSED);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    CaseUpdateRequest request = new CaseUpdateRequest(
        LocalDate.of(2026, 8, 19), null, null, null, null, null,
        null, null, null, null,
        null, null, null, null, null,
        null, null, null, null);

    caseService.update(1L, request);

    assertThat(c.getReceiveDate()).isEqualTo(LocalDate.of(2026, 8, 19));
  }

  @Test
  void update_statusUnchangedOnClosedByStaffShouldBeAllowed() {
    authenticateAs("STAFF");
    Case c = caseWithRefs();
    c.setCaseId(1L);
    c.setStatus(CaseStatus.CLOSED);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    // 同值 no-op：CLOSED → CLOSED 不屬內容修改，不應被拒
    CaseResponse response = caseService.update(1L, updateReq("CLOSED"));

    assertThat(response.status()).isEqualTo("CLOSED");
  }

  @Test
  void update_withoutStatus_shouldNotChange() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));

    CaseResponse response = caseService.update(1L, updateReq(null));

    assertThat(response.status()).isEqualTo("PENDING");
  }

  @Test
  void update_sender_shouldCreateNewSenderWhenIdentityChangesToUnknown() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
    when(senderRepository.findByNameAndPhone("李四", "0911111111")).thenReturn(Optional.empty());
    when(senderRepository.save(any(Sender.class))).thenAnswer(i -> i.getArgument(0));

    CaseUpdateRequest request = new CaseUpdateRequest(
        null, null, null, null, null, null,
        null, null, null, null,
        "李四", "0911111111", null, null, null,
        null, null, null, null);
    caseService.update(1L, request);

    // 新身分：建立新送件人並關聯，原共享送件人不動
    assertThat(c.getSender().getName()).isEqualTo("李四");
    assertThat(c.getSender().getPhone()).isEqualTo("0911111111");
    verify(senderRepository).save(any(Sender.class));
  }

  @Test
  void update_sender_shouldReuseExistingSenderWhenIdentityAlreadyExists() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
    Sender existing = sender();
    existing.setName("李四");
    existing.setPhone("0911111111");
    when(senderRepository.findByNameAndPhone("李四", "0911111111")).thenReturn(Optional.of(existing));

    CaseUpdateRequest request = new CaseUpdateRequest(
        null, null, null, null, null, null,
        null, null, null, null,
        "李四", "0911111111", null, null, null,
        null, null, null, null);
    caseService.update(1L, request);

    // 目標身分已存在：關聯既有送件人，不建立新列、不改原送件人
    assertThat(c.getSender()).isSameAs(existing);
    verify(senderRepository, never()).save(any(Sender.class));
    Sender original = sender();
    assertThat(original.getName()).isEqualTo("王小明");
  }

  @Test
  void update_sender_shouldUpdateAddressOnCurrentSenderWhenIdentityUnchanged() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
    Sender current = c.getSender();
    when(senderRepository.findByNameAndPhone("王小明", "0912345678")).thenReturn(Optional.of(current));

    CaseUpdateRequest request = new CaseUpdateRequest(
        null, null, null, null, null, null,
        null, null, null, null,
        null, null, "臺中市霧峰區新地址", null, null,
        null, null, null, null);
    caseService.update(1L, request);

    // 身分未變：沿用原送件人，僅更新地址
    assertThat(c.getSender()).isSameAs(current);
    assertThat(c.getSender().getAddress()).isEqualTo("臺中市霧峰區新地址");
    verify(senderRepository, never()).save(any(Sender.class));
  }

  @Test
  void update_shouldReplaceJunctions() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
    when(damageRepository.findById(3L)).thenReturn(Optional.of(damage(3L)));

    CaseUpdateRequest add = new CaseUpdateRequest(
        null, null, null, null, null, null,
        null, null, null, null,
        null, null, null, null, null,
        List.of(3L), null, null, null);
    caseService.update(1L, add);
    assertThat(c.getCaseDamages()).hasSize(1);

    CaseUpdateRequest clear = new CaseUpdateRequest(
        null, null, null, null, null, null,
        null, null, null, null,
        null, null, null, null, null,
        List.of(), null, null, null);
    caseService.update(1L, clear);
    assertThat(c.getCaseDamages()).isEmpty();
  }

  @Test
  void update_junctions_shouldIgnoreDuplicateIds() {
    Case c = caseWithRefs();
    c.setCaseId(1L);
    when(caseRepository.findById(1L)).thenReturn(Optional.of(c));
    when(damageRepository.findById(3L)).thenReturn(Optional.of(damage(3L)));

    // 重複 id：差集法以 Set 去重，不得建立重複 junction 撞 UNIQUE(case_id, damage_id)
    CaseUpdateRequest request = new CaseUpdateRequest(
        null, null, null, null, null, null,
        null, null, null, null,
        null, null, null, null, null,
        List.of(3L, 3L), null, null, null);
    caseService.update(1L, request);

    assertThat(c.getCaseDamages()).hasSize(1);
    assertThat(c.getCaseDamages().get(0).getDamage().getDamageId()).isEqualTo(3L);
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
  // 統計（statistics）
  // ---------------------------------------------------------------

  @Test
  void statistics_shouldAggregateCounts() {
    Crop citrus = crop(36L);
    citrus.setCrop("柑橘");
    Crop rice = crop(37L);
    rice.setCrop("水稻");
    PestCategory fungus = pestCategory(1L);
    fungus.setPestCategory("真菌");
    PestCategory bacterium = pestCategory(2L);
    bacterium.setPestCategory("細菌");

    LocalDate thisMonth = LocalDate.now().withDayOfMonth(10);
    LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(15);

    Case c1 = caseWithRefs();
    c1.setCrop(citrus);
    c1.setStatus(CaseStatus.RESOLVED);
    c1.setReceiveDate(thisMonth);
    c1.setCasePestCategories(new java.util.ArrayList<>(List.of(junction(fungus))));

    Case c2 = caseWithRefs();
    c2.setCrop(citrus);
    c2.setStatus(CaseStatus.RESOLVED);
    c2.setReceiveDate(lastMonth);
    c2.setCasePestCategories(new java.util.ArrayList<>(List.of(junction(fungus), junction(bacterium))));

    Case c3 = caseWithRefs();
    c3.setCrop(rice);
    c3.setStatus(CaseStatus.PENDING);
    c3.setReceiveDate(LocalDate.now().withDayOfMonth(5));
    c3.setCasePestCategories(new java.util.ArrayList<>(List.of(junction(fungus))));

    when(caseRepository.count()).thenReturn(3L);
    when(caseRepository.countByStatus(any())).thenReturn(1L);
    when(caseRepository.countByReceiveDateGreaterThanEqual(any())).thenReturn(2L);
    when(caseRepository.findAll()).thenReturn(List.of(c1, c2, c3));

    CaseStatisticsResponse stats = caseService.statistics();

    assertThat(stats.totalCases()).isEqualTo(3L);
    assertThat(stats.monthNewCases()).isEqualTo(2L);
    assertThat(stats.pendingCases()).isEqualTo(1L);

    assertThat(stats.topCrops()).extracting(CountName::name).containsExactly("柑橘", "水稻");
    assertThat(stats.topCrops()).extracting(CountName::count).containsExactly(2L, 1L);

    assertThat(stats.topPestCategories())
        .extracting(CountName::name).containsExactly("真菌", "細菌");
    assertThat(stats.topPestCategories()).extracting(CountName::count).containsExactly(3L, 1L);

    // 狀態比例依 CaseStatus 順序且缺的補 0
    assertThat(stats.statusRatio()).extracting(StatusCount::status)
        .containsExactly("PENDING", "RESOLVED", "CLOSED");
    assertThat(stats.statusRatio()).extracting(StatusCount::count)
        .containsExactly(1L, 2L, 0L);

    // 近 6 月（含本月），本月 2 案、上個月 1 案
    assertThat(stats.monthlyTrend()).hasSize(6);
    assertThat(stats.monthlyTrend()).extracting(MonthCount::month)
        .contains(YearMonth.now().toString(), YearMonth.now().minusMonths(1).toString());
    assertThat(stats.monthlyTrend().stream()
        .filter(mc -> mc.month().equals(YearMonth.now().toString()))
        .findFirst().orElseThrow().count()).isEqualTo(2L);
    assertThat(stats.monthlyTrend().stream()
        .filter(mc -> mc.month().equals(YearMonth.now().minusMonths(1).toString()))
        .findFirst().orElseThrow().count()).isEqualTo(1L);
  }

  @Test
  void statistics_shouldReturnZerosWhenEmpty() {
    when(caseRepository.count()).thenReturn(0L);
    when(caseRepository.countByStatus(any())).thenReturn(0L);
    when(caseRepository.countByReceiveDateGreaterThanEqual(any())).thenReturn(0L);
    when(caseRepository.findAll()).thenReturn(List.of());

    CaseStatisticsResponse stats = caseService.statistics();

    assertThat(stats.totalCases()).isZero();
    assertThat(stats.monthNewCases()).isZero();
    assertThat(stats.pendingCases()).isZero();
    assertThat(stats.topCrops()).isEmpty();
    assertThat(stats.topPestCategories()).isEmpty();
    assertThat(stats.statusRatio()).hasSize(3);
    assertThat(stats.statusRatio()).allMatch(sc -> sc.count() == 0);
    assertThat(stats.monthlyTrend()).hasSize(6);
    assertThat(stats.monthlyTrend()).allMatch(mc -> mc.count() == 0);
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
    s.setDistrict(district(1L));
    s.setSenderType(senderType(1L, "農民"));
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

  private CasePestCategory junction(PestCategory pc) {
    CasePestCategory j = new CasePestCategory();
    j.setPestCategory(pc);
    return j;
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
    c.setStatus(CaseStatus.PENDING);
    c.setSender(sender());
    c.setMethod(method(1L));
    c.setCrop(crop(36L));
    c.setService(service(1L));
    c.setDelivery(delivery(1L));
    c.setCreatedBy(user());
    return c;
  }
}