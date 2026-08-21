package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Crop;
import com.d0w0b.phytotrack.models.CropCategory;

/**
 * 作物資料存取層（Data Access Layer）
 */
public interface CropRepository extends JpaRepository<Crop, Long> {

  /** 依分類抓取作物，並預先抓取分類關聯 */
  @EntityGraph(attributePaths = "cropCategory")
  java.util.List<Crop> findByCropCategory(CropCategory category);

  boolean existsByCropCategoryCropCategoryId(Long cropCategoryId);
}