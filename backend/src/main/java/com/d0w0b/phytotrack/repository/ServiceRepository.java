package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Service;

/**
 * 服務類別資料存取層（Data Access Layer）
 *
 * 注意：此處的 Service 為領域實體（models.Service），
 * 與 Spring 的 @Service 註解無關。
 */
public interface ServiceRepository extends JpaRepository<Service, Long> {
}