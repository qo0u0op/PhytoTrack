package com.d0w0b.phytotrack.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.d0w0b.phytotrack.models.Case;
import com.d0w0b.phytotrack.models.CaseStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 案件資料存取層（Data Access Layer）
 *
 * 以 @EntityGraph 預先抓取（Eager Fetch）列表所需關聯，
 * 避免查詢列表時對每個案件逐筆查詢關聯的 N+1 問題。
 */
public interface CaseRepository
    extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

  /** 分頁查詢全部案件，並一次抓取送件人、作物與服務類別 */
  @Override
  @EntityGraph(attributePaths = {"sender", "crop", "service"})
  Page<Case> findAll(Pageable pageable);

  /** 統計用：一次抓取全部案件，預抓作物與病蟲害關聯（Java 聚合避免逐案 N+1） */
  @Override
  @EntityGraph(attributePaths = {"crop", "casePestCategories.pestCategory"})
  List<Case> findAll();

  /** 依狀態計數（統計：待處理數等） */
  long countByStatus(CaseStatus status);

  /** 依收件日期 ≥ 指定日計數（統計：本月新增） */
  long countByReceiveDateGreaterThanEqual(LocalDate date);
}