package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.SenderType;

/**
 * 送件人身分別資料存取層（Data Access Layer）
 */
public interface SenderTypeRepository extends JpaRepository<SenderType, Long> {
}