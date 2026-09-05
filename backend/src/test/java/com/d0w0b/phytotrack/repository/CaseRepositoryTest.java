package com.d0w0b.phytotrack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import com.d0w0b.phytotrack.dto.CaseDtos.CaseFilter;
import com.d0w0b.phytotrack.models.Case;
import com.d0w0b.phytotrack.models.CaseDamage;
import com.d0w0b.phytotrack.models.CaseHint;
import com.d0w0b.phytotrack.models.CaseIdentifier;
import com.d0w0b.phytotrack.models.CasePestCategory;
import com.d0w0b.phytotrack.models.CaseStatus;
import com.d0w0b.phytotrack.models.Crop;
import com.d0w0b.phytotrack.models.CropCategory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案件資料存取層 (CaseRepository) 資料庫切片測試
 *
 * @DataJpaTest 使用真實 SQLite (test profile 的獨立檔案)，驗證：
 *   - SQLite 方言下的儲存與讀取
 *   - LocalDate / LocalDateTime 日期轉換器 (converter) 回讀一致
 *   - 多對多關聯 (Junction) 隨案件儲存並可讀回
 */
@DataJpaTest
@AutoConfigureTestDatabase (replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles ("test")
class CaseRepositoryTest {

  @Autowired
  private CaseRepository caseRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SenderRepository senderRepository;

  // 業務初始無作物種子：測試自建（保持測試獨立於種子）
  private Crop ensureTestCrop () {
    return cropRepository.findAll ().stream ().findFirst ().orElseGet (() -> {
      CropCategory category = cropCategoryRepository.findAll ().stream ().findFirst ().orElseThrow ();
      Crop c = new Crop ();
      c.setCrop ("測試稻作");
      c.setCropCategory (category);
      return cropRepository.save (c);
    });
  }

  @Autowired
  private DistrictRepository districtRepository;

  @Autowired
  private SenderTypeRepository senderTypeRepository;

  @Autowired
  private CropRepository cropRepository;

  @Autowired
  private CropCategoryRepository cropCategoryRepository;

  @Autowired
  private MethodRepository methodRepository;

  @Autowired
  private ServiceRepository serviceRepository;

  @Autowired
  private DeliveryRepository deliveryRepository;

  @Autowired
  private DamageRepository damageRepository;

  @Autowired
  private HintRepository hintRepository;

  @Autowired
  private PestCategoryRepository pestCategoryRepository;

  @Autowired
  private IdentifierRepository identifierRepository;

  @PersistenceContext
  private EntityManager entityManager;

  @Test
  void saveAndRead_caseWithJunctions_shouldPersistDatesAndAssociations () {
    // 建立者 (createdBy 於測試切片中無 SecurityContext，手動填寫)
    User user = new User ();
    user.setUsername ("case-repo-test-user");
    user.setDisplayName ("資料庫測試員");
    user.setPassword ("encoded");
    user.setRole (User.Role.ROLE_STAFF);
    user.setActive (true);
    user = userRepository.save (user);

    // 送件人 (District / SenderType 取自種子資料)
    District district = districtRepository.findAll ().stream ().findFirst ().orElseThrow ();
    SenderType senderType = senderTypeRepository.findAll ().stream ().findFirst ().orElseThrow ();
    Sender sender = new Sender ();
    sender.setName ("測試送件人-20260818");
    sender.setPhone ("0999-123-456");
    sender.setAddress ("測試路 1 號");
    sender.setDistrict (district);
    sender.setSenderType (senderType);
    sender = senderRepository.save (sender);

    // 參照資料 (Method / Service / Delivery) 取自種子資料；Crop 業務初始無種子，自建
    Crop crop = cropRepository.findAll ().stream ().findFirst ().orElseGet (() -> {
      CropCategory category = cropCategoryRepository.findAll ().stream ().findFirst ().orElseThrow ();
      Crop c = new Crop ();
      c.setCrop ("測試稻作");
      c.setCropCategory (category);
      return cropRepository.save (c);
    });
    Method method = methodRepository.findAll ().stream ().findFirst ().orElseThrow ();
    Service service = serviceRepository.findAll ().stream ().findFirst ().orElseThrow ();
    Delivery delivery = deliveryRepository.findAll ().stream ().findFirst ().orElseThrow ();

    Case caseEntity = new Case ();
    caseEntity.setReceiveDate (LocalDate.of (2026, 8, 18));
    caseEntity.setCropScale ("2 分地");
    caseEntity.setDamageScale ("約 3 成");
    caseEntity.setCaseDescription ("葉片出現斑點");
    caseEntity.setStatus (CaseStatus.PENDING);
    caseEntity.setSender (sender);
    caseEntity.setMethod (method);
    caseEntity.setCrop (crop);
    caseEntity.setService (service);
    caseEntity.setDelivery (delivery);
    caseEntity.setFieldDistrict (district);
    caseEntity.setCreatedBy (user);
    caseEntity.setCreatedAt (LocalDateTime.of (2026, 8, 18, 10, 30));
    caseEntity.setUpdatedAt (LocalDateTime.of (2026, 8, 18, 10, 30));

    // 四組多對多關聯各加一筆
    Damage damage = damageRepository.findAll ().stream ().findFirst ().orElseThrow ();
    CaseDamage caseDamage = new CaseDamage ();
    caseDamage.setCaseEntity (caseEntity);
    caseDamage.setDamage (damage);
    caseEntity.getCaseDamages ().add (caseDamage);

    Hint hint = hintRepository.findAll ().stream ().findFirst ().orElseThrow ();
    CaseHint caseHint = new CaseHint ();
    caseHint.setCaseEntity (caseEntity);
    caseHint.setHint (hint);
    caseEntity.getCaseHints ().add (caseHint);

    PestCategory pestCategory = pestCategoryRepository.findAll ().stream ().findFirst ().orElseThrow ();
    CasePestCategory casePestCategory = new CasePestCategory ();
    casePestCategory.setCaseEntity (caseEntity);
    casePestCategory.setPestCategory (pestCategory);
    caseEntity.getCasePestCategories ().add (casePestCategory);

    Identifier identifier = identifierRepository.findAll ().stream ().findFirst ()
        .orElseGet (() -> {
          Identifier i = new Identifier ();
          i.setIdentifier ("測試簽名人");
          return identifierRepository.save (i);
        });
    CaseIdentifier caseIdentifier = new CaseIdentifier ();
    caseIdentifier.setCaseEntity (caseEntity);
    caseIdentifier.setIdentifier (identifier);
    caseEntity.getCaseIdentifiers ().add (caseIdentifier);

    // 儲存並清除 Persistence Context，強制從資料庫重新載入
    Case saved = caseRepository.saveAndFlush (caseEntity);
    entityManager.clear ();

    Case loaded = caseRepository.findById (saved.getCaseId ()).orElseThrow ();

    // 日期轉換器回讀一致 (LocalDate)
    assertThat (loaded.getReceiveDate ()).isEqualTo (LocalDate.of (2026, 8, 18));
    // 稽核時間由 Auditing 自動填寫 (LocalDateTime 經轉換器回讀不為空即代表轉換正常)
    assertThat (loaded.getCreatedAt ()).isNotNull ();
    assertThat (loaded.getUpdatedAt ()).isNotNull ();
    assertThat (loaded.getStatus ()).isEqualTo (CaseStatus.PENDING);
    assertThat (loaded.getSender ().getName ()).isEqualTo ("測試送件人-20260818");
    assertThat (loaded.getCrop ().getCrop ()).isEqualTo (crop.getCrop ());
    assertThat (loaded.getCreatedBy ().getUsername ()).isEqualTo ("case-repo-test-user");
    assertThat (loaded.getCaseDamages ()).hasSize (1);
    assertThat (loaded.getCaseHints ()).hasSize (1);
    assertThat (loaded.getCasePestCategories ()).hasSize (1);
    assertThat (loaded.getCaseIdentifiers ()).hasSize (1);
  }

  @Test
  void findAll_withFilter_shouldCombineConditionsWithAnd () {
    User user = saveUser ("filter-and-user");
    // 業務初始無作物種子：自建兩筆（取代舊 findById(1L/36L)）
    CropCategory grainCategory = cropCategoryRepository.findAll ().stream ().findFirst ().orElseThrow ();
    Crop rice = new Crop ();
    rice.setCrop ("測試稻作");
    rice.setCropCategory (grainCategory);
    rice = cropRepository.save (rice);
    Crop citrus = new Crop ();
    citrus.setCrop ("測試柑橘");
    citrus.setCropCategory (grainCategory);
    citrus = cropRepository.save (citrus);
    Service diagnosis = serviceRepository.findById (1L).orElseThrow ();
    Service consultation = serviceRepository.findById (3L).orElseThrow ();

    Case ricePending = saveCase (user, rice, diagnosis, "和甲", LocalDate.of (2026, 8, 1), CaseStatus.PENDING);
    Case riceResolved = saveCase (user, rice, diagnosis, "和乙", LocalDate.of (2026, 8, 15), CaseStatus.RESOLVED);
    Case citrusPending = saveCase (user, citrus, consultation, "和丙", LocalDate.of (2026, 8, 20), CaseStatus.PENDING);

    // cropId=rice AND status=PENDING → 僅稻作且待處理
    // 頁面尺寸取大 (共享 test DB 可能有整合測試殘留案件)，確保斷言與殘留量無關
    Specification<Case> spec = CaseSpecifications.build (new CaseFilter (rice.getCropId (), null, null, null, null, "PENDING"), CaseStatus.PENDING);
    Page<Case> page = caseRepository.findAll (spec, PageRequest.of (0, 100));

    assertThat (page.getContent ())
        .extracting (Case::getCaseId)
        .contains (ricePending.getCaseId ())
        .doesNotContain (riceResolved.getCaseId (), citrusPending.getCaseId ());
    assertThat (page.getContent ())
        .allSatisfy (c -> assertThat (c.getStatus ()).isEqualTo (CaseStatus.PENDING));
    final Long riceId = rice.getCropId ();
    assertThat (page.getContent ())
        .allSatisfy (c -> assertThat (c.getCrop ().getCropId ()).isEqualTo (riceId));
  }

  @Test
  void findAll_withSenderNamePartialMatch_shouldReturnMatchingCases () {
    User user = saveUser ("filter-name-user");
    Crop rice = ensureTestCrop ();
    Service diagnosis = serviceRepository.findById (1L).orElseThrow ();

    Case zhangsan = saveCase (user, rice, diagnosis, "比對-張小明", LocalDate.of (2026, 8, 1), CaseStatus.PENDING);
    Case wangxiaohua = saveCase (user, rice, diagnosis, "比對-王小華", LocalDate.of (2026, 8, 15), CaseStatus.PENDING);

    // senderName=張 → 僅送件人姓名含「張」者
    Specification<Case> spec = CaseSpecifications.build (new CaseFilter (null, null, "張", null, null, null), null);
    Page<Case> page = caseRepository.findAll (spec, PageRequest.of (0, 100));

    assertThat (page.getContent ())
        .extracting (Case::getCaseId)
        .contains (zhangsan.getCaseId ())
        .doesNotContain (wangxiaohua.getCaseId ());
    assertThat (page.getContent ())
        .allSatisfy (c -> assertThat (c.getSender ().getName ()).contains ("張"));
  }

  @Test
  void findAll_withDateRange_shouldReturnCasesInRange () {
    User user = saveUser ("filter-date-user");
    Crop rice = ensureTestCrop ();
    Service diagnosis = serviceRepository.findById (1L).orElseThrow ();

    Case inRange = saveCase (user, rice, diagnosis, "期-張小明", LocalDate.of (2026, 8, 15), CaseStatus.PENDING);
    Case before = saveCase (user, rice, diagnosis, "期-李小華", LocalDate.of (2026, 7, 31), CaseStatus.PENDING);
    Case after = saveCase (user, rice, diagnosis, "期-王小華", LocalDate.of (2026, 9, 1), CaseStatus.PENDING);

    Specification<Case> spec = CaseSpecifications.build (new CaseFilter (null, null, null, LocalDate.of (2026, 8, 1), LocalDate.of (2026, 8, 31), null),
        null);
    Page<Case> page = caseRepository.findAll (spec, PageRequest.of (0, 100));

    assertThat (page.getContent ())
        .extracting (Case::getCaseId)
        .contains (inRange.getCaseId ())
        .doesNotContain (before.getCaseId (), after.getCaseId ());
    assertThat (page.getContent ())
        .allSatisfy (c -> {
          assertThat (c.getReceiveDate ()).isAfterOrEqualTo (LocalDate.of (2026, 8, 1));
          assertThat (c.getReceiveDate ()).isBeforeOrEqualTo (LocalDate.of (2026, 8, 31));
        });
  }

  @Test
  void findAll_withoutFilter_shouldReturnAll () {
    User user = saveUser ("filter-all-user");
    Crop rice = ensureTestCrop ();
    Service diagnosis = serviceRepository.findById (1L).orElseThrow ();

    Case first = saveCase (user, rice, diagnosis, "全-張小明", LocalDate.of (2026, 8, 1), CaseStatus.PENDING);
    Case second = saveCase (user, rice, diagnosis, "全-李小華", LocalDate.of (2026, 8, 15), CaseStatus.RESOLVED);

    // 空 filter 走 findAll (Pageable)，回傳全部 (含本次新增)；用大頁面避免 file DB 自增 ID 超過 100 時分頁截斷
    Page<Case> page = caseRepository.findAll (PageRequest.of (0, 1000));

    assertThat (page.getContent ())
        .extracting (Case::getCaseId)
        .contains (first.getCaseId (), second.getCaseId ());
  }

  // ---------------------------------------------------------------
  // 測試資料建構輔助
  // ---------------------------------------------------------------

  private User saveUser (String username) {
    User user = new User ();
    user.setUsername (username);
    user.setDisplayName ("資料庫測試員");
    user.setPassword ("encoded");
    user.setRole (User.Role.ROLE_STAFF);
    user.setActive (true);
    return userRepository.save (user);
  }

  private static final java.util.concurrent.atomic.AtomicLong PHONE_SEQ =
      new java.util.concurrent.atomic.AtomicLong (0);

  private Sender createSender (String name) {
    District district = districtRepository.findAll ().stream ().findFirst ().orElseThrow ();
    SenderType senderType = senderTypeRepository.findAll ().stream ().findFirst ().orElseThrow ();
    Sender sender = new Sender ();
    sender.setName (name);
    sender.setPhone ("0910-%07d".formatted (PHONE_SEQ.incrementAndGet ()));
    sender.setAddress ("測試路 1 號");
    sender.setDistrict (district);
    sender.setSenderType (senderType);
    return senderRepository.save (sender);
  }

  private Case saveCase (User user, Crop crop, Service service, String senderName,
                        LocalDate receiveDate, CaseStatus status) {
    Case caseEntity = new Case ();
    caseEntity.setReceiveDate (receiveDate);
    caseEntity.setStatus (status);
    Sender sender = createSender (senderName);
    caseEntity.setSender (sender);
    caseEntity.setMethod (methodRepository.findAll ().stream ().findFirst ().orElseThrow ());
    caseEntity.setCrop (crop);
    caseEntity.setService (service);
    caseEntity.setDelivery (deliveryRepository.findAll ().stream ().findFirst ().orElseThrow ());
    // 田區位置與送件人同縣市 (預設同鄉鎮，符合 80% 規則)
    caseEntity.setFieldDistrict (sender.getDistrict ());
    caseEntity.setCreatedBy (user);
    caseEntity.setCreatedAt (LocalDateTime.of (receiveDate, java.time.LocalTime.of (10, 30)));
    caseEntity.setUpdatedAt (LocalDateTime.of (receiveDate, java.time.LocalTime.of (10, 30)));
    return caseRepository.saveAndFlush (caseEntity);
  }
}
