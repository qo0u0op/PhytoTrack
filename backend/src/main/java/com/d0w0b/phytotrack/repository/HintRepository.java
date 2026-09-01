package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Hint;

/**
 * 防治建議資料存取層 (Data Access Layer)
 */
public interface HintRepository extends JpaRepository<Hint, Long> {
}