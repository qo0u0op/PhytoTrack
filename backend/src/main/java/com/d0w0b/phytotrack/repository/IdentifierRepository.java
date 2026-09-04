package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * 診斷簽名人資料存取層 (Data Access Layer)
 */
public interface IdentifierRepository extends JpaRepository<Identifier, Long> {

  List<Identifier> findByUserUserId (Long userId);

  Optional<Identifier> findByIdentifier (String identifier);

  List<Identifier> findByIdentifierContainingIgnoreCase (String keyword);

  List<Identifier> findByActiveTrue ();

  List<Identifier> findByUserUserIdAndActiveTrueOrderByIdentifierIdAsc (Long userId);

  List<Identifier> findByActiveTrueOrderByIdentifierIdAsc ();

  Optional<Identifier> findByIdentifierAndActiveTrueAndUserIsNull (String identifier);

  List<Identifier> findByIdentifierAndActiveTrue (String identifier);

  List<Identifier> findByUserIsNull ();

  List<Identifier> findByUserIsNullAndActiveTrue ();

  /** 解綁歷史：曾屬於指定使用者的簽名人（恢復原筆用） */
  List<Identifier> findByFormerUserUserId (Long userId);
}