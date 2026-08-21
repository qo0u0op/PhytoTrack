package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.d0w0b.phytotrack.models.Sender;

import java.util.List;
import java.util.Optional;

/**
 * 送件人資料存取層（Data Access Layer）
 */
public interface SenderRepository extends JpaRepository<Sender, Long> {

  /** 依姓名與電話查詢送件人（schema 中此組合為唯一鍵） */
  Optional<Sender> findByNameAndPhone(String name, String phone);

  boolean existsBySenderTypeSenderTypeId(Long senderTypeId);

  @Query("SELECT s FROM Sender s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%')) "
      + "OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :q, '%')) "
      + "OR LOWER(s.displayName) LIKE LOWER(CONCAT('%', :q, '%'))")
  List<Sender> search(@Param("q") String q);
}