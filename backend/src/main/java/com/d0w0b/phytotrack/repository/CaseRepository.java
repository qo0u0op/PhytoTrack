package com.d0w0b.phytotrack.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Case;

/**
 * 案件資料存取層（Data Access Layer）
 *
 * 以 @EntityGraph 預先抓取（Eager Fetch）列表所需關聯，
 * 避免查詢列表時對每個案件逐筆查詢關聯的 N+1 問題。
 */
public interface CaseRepository extends JpaRepository<Case, Long> {

  /** 分頁查詢全部案件，並一次抓取送件人、作物與服務類別 */
  @Override
  @EntityGraph(attributePaths = {"sender", "crop", "service"})
  Page<Case> findAll(Pageable pageable);
}