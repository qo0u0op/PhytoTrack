package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Damage;

/**
 * 被害部位資料存取層（Data Access Layer）
 */
public interface DamageRepository extends JpaRepository<Damage, Long> {
}