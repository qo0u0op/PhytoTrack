package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import com.d0w0b.phytotrack.dto.CaseDtos.CaseFilter;
import com.d0w0b.phytotrack.exception.ApiException;
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
 */
public final class CaseSpecifications {

  private CaseSpecifications() {
  }

  public static Specification<Case> build(CaseFilter filter) {
    // 狀態字串立即對映（fail-fast）：非法值在建構時即拋錯，而非等查詢執行
    Integer statusInt = filter.status() != null ? toStatusInt(filter.status()) : null;
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

  /**
   * 列舉字串對映現有整數狀態。
   *
   * 過渡假設（見 case-search proposal）：case-lifecycle 將欄位遷移為列舉後，
   * 僅需移除本對照，API 契約（列舉字串）不變。
   */
  private static int toStatusInt(String status) {
    return switch (status) {
      case "PENDING" -> 0;
      case "RESOLVED" -> 1;
      case "CLOSED" -> 2;
      default -> throw new ApiException(
          "INVALID_STATUS", HttpStatus.BAD_REQUEST, "無效的狀態：" + status);
    };
  }
}
