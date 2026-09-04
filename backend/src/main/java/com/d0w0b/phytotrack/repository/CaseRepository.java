package com.d0w0b.phytotrack.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.d0w0b.phytotrack.models.Case;
import com.d0w0b.phytotrack.models.CaseStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 案件資料存取層 (Data Access Layer)
 *
 * 以 @EntityGraph 預先抓取 (Eager Fetch) 列表所需關聯，
 * 避免查詢列表時對每個案件逐筆查詢關聯的 N+1 問題。
 */
public interface CaseRepository
    extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

  /** 分頁查詢全部案件，並一次抓取送件人、作物與服務類別 */
  @Override
  @EntityGraph (attributePaths = {"sender", "crop", "service"})
  Page<Case> findAll (Pageable pageable);

  /** 統計用：一次抓取全部案件，預抓作物與病蟲害關聯 (Java 聚合避免逐案 N+1) */
  @Override
  @EntityGraph (attributePaths = {"crop", "casePestCategories.pestCategory"})
  List<Case> findAll ();

  /** CSV 匯出用：依規格查詢全部案件並預抓非 collection 關聯 (Hibernate 不允許同時
   * 抓取多個 List collection，被害部位／病蟲害等 collection 於交易內 Lazy 載入) */
  @Override
  @EntityGraph (attributePaths = {
      "sender", "sender.district", "sender.senderType",
      "crop", "method", "service", "delivery"})
  List<Case> findAll (Specification<Case> spec, Sort sort);

  /** 依狀態計數 (統計：待處理數等) */
  long countByStatus (CaseStatus status);

  /** 依收件日期 ≥ 指定日計數 (統計：本月新增) */
  long countByReceiveDateGreaterThanEqual (LocalDate date);

  // 參照資料刪除保護：檢查是否被案件引用
  boolean existsByCropCropId (Long cropId);
  boolean existsByCropCropCategoryCropCategoryId (Long cropCategoryId);
  boolean existsByMethodMethodId (Long methodId);
  boolean existsByServiceServiceId (Long serviceId);
  boolean existsByDeliveryDeliverId (Long deliveryId);
  boolean existsByCaseDamagesDamageDamageId (Long damageId);
  boolean existsByCaseHintsHintHintId (Long hintId);
  boolean existsByCasePestCategoriesPestCategoryPestCategoryId (Long pestCategoryId);
  boolean existsByCaseIdentifiersIdentifierIdentifierId (Long identifierId);
  boolean existsByFieldDistrictDistrictId (Long districtId);
  boolean existsBySenderDistrictDistrictId (Long districtId);

  // 送件人刪除保護
  boolean existsBySenderSenderId (Long senderId);
}