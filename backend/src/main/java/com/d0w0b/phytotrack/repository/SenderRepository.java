package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.d0w0b.phytotrack.models.Sender;

import java.util.Optional;

/**
 * 送件人資料存取層（Data Access Layer）
 */
public interface SenderRepository extends JpaRepository<Sender, Long> {

  /** 依姓名與電話查詢送件人（schema 中此組合為唯一鍵） */
  Optional<Sender> findByNameAndPhone(String name, String phone);

  boolean existsBySenderTypeSenderTypeId(Long senderTypeId);
}