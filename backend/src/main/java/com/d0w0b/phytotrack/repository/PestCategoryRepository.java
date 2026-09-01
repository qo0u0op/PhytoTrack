package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.PestCategory;

import java.util.List;
import java.util.Optional;

/**
 * 病蟲害小分類資料存取層 (Data Access Layer)
 */
public interface PestCategoryRepository extends JpaRepository<PestCategory, Long> {

  /** 查詢全部小分類並預先抓取所屬害物類型 */
  @EntityGraph (attributePaths = "pestType")
  List<PestCategory> findAllByOrderBySortOrderAsc ();

  /** 同類型下同代碼 (composite unique 前置檢查) */
  boolean existsByPestTypePestTypeIdAndPestCategoryCodeIgnoreCase (Long pestTypeId, String code);

  /** 同類型下同名稱 (composite unique 前置檢查) */
  boolean existsByPestTypePestTypeIdAndPestCategoryIgnoreCase (Long pestTypeId, String name);

  /** 同類型下同代碼查詢實體 (更新時排除自身用) */
  Optional<PestCategory> findByPestTypePestTypeIdAndPestCategoryCodeIgnoreCase (Long pestTypeId, String code);

  /** 同類型下同名稱查詢實體 (更新時排除自身用) */
  Optional<PestCategory> findByPestTypePestTypeIdAndPestCategoryIgnoreCase (Long pestTypeId, String name);
}