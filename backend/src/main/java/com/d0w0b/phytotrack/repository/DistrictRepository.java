package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.District;

/**
 * 鄉鎮市區資料存取層 (Data Access Layer)
 */
public interface DistrictRepository extends JpaRepository<District, Long> {

  /** 縣市刪除保護：是否仍有鄉鎮歸屬 */
  boolean existsByCityCityId (Long cityId);

  /** 依縣市列出鄉鎮 */
  java.util.List<District> findByCityCityId (Long cityId);
}