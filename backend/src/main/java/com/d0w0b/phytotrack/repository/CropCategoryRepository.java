package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.CropCategory;

import java.util.List;

/**
 * 作物分類資料存取層 (Data Access Layer)
 */
public interface CropCategoryRepository extends JpaRepository<CropCategory, Long> {

  /** 查詢所有分類並預先抓取分類下所有作物，避免 N+1 */
  @EntityGraph (attributePaths = "crops")
  List<CropCategory> findAllByOrderByCropCategoryIdAsc ();
}