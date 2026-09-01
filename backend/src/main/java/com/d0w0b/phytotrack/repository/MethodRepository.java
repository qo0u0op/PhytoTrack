package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Method;

/**
 * 耕種方式資料存取層 (Data Access Layer)
 */
public interface MethodRepository extends JpaRepository<Method, Long> {
}