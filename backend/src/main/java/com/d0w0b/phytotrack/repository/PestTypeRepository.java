package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.PestType;

import java.util.List;

/**
 * 害物類型資料存取層 (Data Access Layer)
 */
public interface PestTypeRepository extends JpaRepository<PestType, Long> {

  /** 查詢所有害物類型並預先抓取底下小分類，避免 N+1 */
  @EntityGraph (attributePaths = "categories")
  List<PestType> findAllByOrderByPestTypeIdAsc ();
}