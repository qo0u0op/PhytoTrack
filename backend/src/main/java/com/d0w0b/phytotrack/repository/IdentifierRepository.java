package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Identifier;

/**
 * 診斷簽名人資料存取層 (Data Access Layer)
 */
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {
}