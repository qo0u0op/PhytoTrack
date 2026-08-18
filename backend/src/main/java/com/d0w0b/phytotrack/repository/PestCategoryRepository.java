package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.PestCategory;

import java.util.List;

/**
 * 病蟲害小分類資料存取層（Data Access Layer）
 */
public interface PestCategoryRepository extends JpaRepository<PestCategory, Long> {

  /** 查詢全部小分類並預先抓取所屬害物類型 */
  @EntityGraph(attributePaths = "pestType")
  List<PestCategory> findAllByOrderBySortOrderAsc();
}