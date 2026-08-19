package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.City;

import java.util.List;

/**
 * 縣市資料存取層（Data Access Layer）
 */
public interface CityRepository extends JpaRepository<City, Long> {

  /** 查詢所有縣市並預先抓取底下鄉鎮市區，避免 N+1 */
  @EntityGraph(attributePaths = "districts")
  List<City> findAllByOrderBySortOrderAsc();
}