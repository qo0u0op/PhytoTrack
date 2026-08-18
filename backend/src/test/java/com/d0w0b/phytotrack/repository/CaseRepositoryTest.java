package com.d0w0b.phytotrack.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

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
import com.d0w0b.phytotrack.models.Service;
import com.d0w0b.phytotrack.models.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案件資料存取層（CaseRepository）資料庫切片測試
 *
 * @DataJpaTest 使用真實 SQLite（test profile 的獨立檔案），驗證：
 *   - SQLite 方言下的儲存與讀取
 *   - LocalDate / LocalDateTime 日期轉換器（converter）回讀一致
 *   - 多對多關聯（Junction）隨案件儲存並可讀回
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CaseRepositoryTest {

  @Autowired
  private CaseRepository caseRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SenderRepository senderRepository;

  @Autowired
  private DistrictRepository districtRepository;

  @Autowired
  private SenderTypeRepository senderTypeRepository;

  @Autowired
  private CropRepository cropRepository;

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
  void saveAndRead_caseWithJunctions_shouldPersistDatesAndAssociations() {
    // 建立者（createdBy 於測試切片中無 SecurityContext，手動填寫）
    User user = new User();
    user.setUsername("case-repo-test-user");
    user.setDisplayName("資料庫測試員");
    user.setPassword("encoded");
    user.setRole(User.Role.ROLE_STAFF);
    user.setActive(true);
    user = userRepository.save(user);

    // 送件人（District / SenderType 取自種子資料）
    District district = districtRepository.findAll().stream().findFirst().orElseThrow();
    SenderType senderType = senderTypeRepository.findAll().stream().findFirst().orElseThrow();
    Sender sender = new Sender();
    sender.setName("測試送件人-20260818");
    sender.setPhone("0999-123-456");
    sender.setAddress("測試路 1 號");
    sender.setDistrict(district);
    sender.setSenderType(senderType);
    sender = senderRepository.save(sender);

    // 參照資料（Crop / Method / Service / Delivery）取自種子資料
    Crop crop = cropRepository.findAll().stream().findFirst().orElseThrow();
    Method method = methodRepository.findAll().stream().findFirst().orElseThrow();
    Service service = serviceRepository.findAll().stream().findFirst().orElseThrow();
    Delivery delivery = deliveryRepository.findAll().stream().findFirst().orElseThrow();

    Case caseEntity = new Case();
    caseEntity.setReceiveDate(LocalDate.of(2026, 8, 18));
    caseEntity.setCropScale("2 分地");
    caseEntity.setDamageScale("約 3 成");
    caseEntity.setPestDescription("葉片出現斑點");
    caseEntity.setStatus(0);
    caseEntity.setSender(sender);
    caseEntity.setMethod(method);
    caseEntity.setCrop(crop);
    caseEntity.setService(service);
    caseEntity.setDelivery(delivery);
    caseEntity.setCreatedBy(user);
    caseEntity.setCreatedAt(LocalDateTime.of(2026, 8, 18, 10, 30));
    caseEntity.setUpdatedAt(LocalDateTime.of(2026, 8, 18, 10, 30));

    // 四組多對多關聯各加一筆
    Damage damage = damageRepository.findAll().stream().findFirst().orElseThrow();
    CaseDamage caseDamage = new CaseDamage();
    caseDamage.setCaseEntity(caseEntity);
    caseDamage.setDamage(damage);
    caseEntity.getCaseDamages().add(caseDamage);

    Hint hint = hintRepository.findAll().stream().findFirst().orElseThrow();
    CaseHint caseHint = new CaseHint();
    caseHint.setCaseEntity(caseEntity);
    caseHint.setHint(hint);
    caseEntity.getCaseHints().add(caseHint);

    PestCategory pestCategory = pestCategoryRepository.findAll().stream().findFirst().orElseThrow();
    CasePestCategory casePestCategory = new CasePestCategory();
    casePestCategory.setCaseEntity(caseEntity);
    casePestCategory.setPestCategory(pestCategory);
    caseEntity.getCasePestCategories().add(casePestCategory);

    Identifier identifier = identifierRepository.findAll().stream().findFirst().orElseThrow();
    CaseIdentifier caseIdentifier = new CaseIdentifier();
    caseIdentifier.setCaseEntity(caseEntity);
    caseIdentifier.setIdentifier(identifier);
    caseEntity.getCaseIdentifiers().add(caseIdentifier);

    // 儲存並清除 Persistence Context，強制從資料庫重新載入
    Case saved = caseRepository.saveAndFlush(caseEntity);
    entityManager.clear();

    Case loaded = caseRepository.findById(saved.getCaseId()).orElseThrow();

    // 日期轉換器回讀一致（LocalDate）
    assertThat(loaded.getReceiveDate()).isEqualTo(LocalDate.of(2026, 8, 18));
    // 稽核時間由 Auditing 自動填寫（LocalDateTime 經轉換器回讀不為空即代表轉換正常）
    assertThat(loaded.getCreatedAt()).isNotNull();
    assertThat(loaded.getUpdatedAt()).isNotNull();
    assertThat(loaded.getStatus()).isEqualTo(0);
    assertThat(loaded.getSender().getName()).isEqualTo("測試送件人-20260818");
    assertThat(loaded.getCrop().getCrop()).isEqualTo(crop.getCrop());
    assertThat(loaded.getCreatedBy().getUsername()).isEqualTo("case-repo-test-user");
    assertThat(loaded.getCaseDamages()).hasSize(1);
    assertThat(loaded.getCaseHints()).hasSize(1);
    assertThat(loaded.getCasePestCategories()).hasSize(1);
    assertThat(loaded.getCaseIdentifiers()).hasSize(1);
  }
}
