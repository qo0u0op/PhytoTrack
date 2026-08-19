package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.domain.Specification;

import com.d0w0b.phytotrack.dto.CaseDtos.CaseFilter;
import com.d0w0b.phytotrack.models.Case;

import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

/**
 * 案件查詢條件的組裝（Case Specifications）
 *
 * 將 CaseFilter 轉換為 JPA Specification：各條件以 AND 組合。
 * senderName 採部分比對（LIKE %值%），並跳脫使用者輸入中的 % 與 _
 * （SQL LIKE 萬用字元），確保按字面比對。
 *
 * 於規格查詢中一併 fetch join 列表所需關聯（送件人、作物、服務），
 * 避免 N+1；count 查詢以 resultType 區隔不 fetch。
 *
 * 僅負責純 SQL 條件的組裝；業務對映（如狀態字串→整數的驗證）由 Service 層處理。
 */
public final class CaseSpecifications {

  private CaseSpecifications() {
  }

  public static Specification<Case> build(CaseFilter filter, Integer statusInt) {
    return (root, query, cb) -> {
      if (query.getResultType() != Long.class && query.getResultType() != long.class) {
        root.fetch("sender");
        root.fetch("crop");
        root.fetch("service");
      }
      List<Predicate> predicates = new ArrayList<>();
      if (filter.cropId() != null) {
        predicates.add(cb.equal(root.get("crop").get("cropId"), filter.cropId()));
      }
      if (filter.serviceId() != null) {
        predicates.add(cb.equal(root.get("service").get("serviceId"), filter.serviceId()));
      }
      if (filter.senderName() != null && !filter.senderName().isBlank()) {
        String escaped = filter.senderName()
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
        predicates.add(cb.like(root.get("sender").get("name"), "%" + escaped + "%", '\\'));
      }
      if (filter.receiveDateFrom() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("receiveDate"), filter.receiveDateFrom()));
      }
      if (filter.receiveDateTo() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("receiveDate"), filter.receiveDateTo()));
      }
      if (statusInt != null) {
        predicates.add(cb.equal(root.get("status"), statusInt));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
