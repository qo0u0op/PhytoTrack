package com.d0w0b.phytotrack.repository;

import org.springframework.data.jpa.domain.Specification;

import com.d0w0b.phytotrack.dto.CaseDtos.CaseFilter;
import com.d0w0b.phytotrack.models.Case;
import com.d0w0b.phytotrack.models.CaseDamage;
import com.d0w0b.phytotrack.models.CaseHint;
import com.d0w0b.phytotrack.models.CasePestCategory;
import com.d0w0b.phytotrack.models.CaseSearchView;
import com.d0w0b.phytotrack.models.CaseStatus;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import java.util.ArrayList;
import java.util.List;

/**
 * 案件查詢條件的組裝 (Case Specifications)
 *
 * 將 CaseFilter 轉換為 JPA Specification：各條件以 AND 組合。
 * senderName 採部分比對 (LIKE %值%)，並跳脫使用者輸入中的 % 與 _
 * (SQL LIKE 萬用字元)，確保按字面比對。
 *
 * 於規格查詢中一併 fetch join 列表所需關聯 (送件人、作物、服務)，
 * 避免 N+1；count 查詢以 resultType 區隔不 fetch。
 *
 * 僅負責純 SQL 條件的組裝；業務對映 (如狀態字串→整數的驗證) 由 Service 層處理。
 */
public final class CaseSpecifications {

  private CaseSpecifications () {
  }

  public static Specification<Case> build (CaseFilter filter, CaseStatus status) {
    return (root, query, cb) -> {
      if (query.getResultType () != Long.class && query.getResultType () != long.class) {
        root.fetch ("sender");
        root.fetch ("crop");
        root.fetch ("service");
      }
      List<Predicate> predicates = new ArrayList<>();
      if (filter.cropId () != null) {
        predicates.add (cb.equal (root.get ("crop").get ("cropId"), filter.cropId ()));
      }
      if (filter.serviceId () != null) {
        predicates.add (cb.equal (root.get ("service").get ("serviceId"), filter.serviceId ()));
      }
      if (filter.senderName () != null && !filter.senderName ().isBlank ()) {
        String escaped = filter.senderName ()
            .replace ("\\", "\\\\")
            .replace ("%", "\\%")
            .replace ("_", "\\_");
        predicates.add (cb.like (root.get ("sender").get ("name"), "%" + escaped + "%", '\\'));
      }
      if (filter.senderTypeId () != null) {
        predicates.add (cb.equal (root.get ("sender").get ("senderType").get ("senderTypeId"), filter.senderTypeId ()));
      }
      if (filter.methodId () != null) {
        predicates.add (cb.equal (root.get ("method").get ("methodId"), filter.methodId ()));
      }
      if (filter.receiveDateFrom () != null) {
        predicates.add (cb.greaterThanOrEqualTo (root.get ("receiveDate"), filter.receiveDateFrom ()));
      }
      if (filter.receiveDateTo () != null) {
        predicates.add (cb.lessThanOrEqualTo (root.get ("receiveDate"), filter.receiveDateTo ()));
      }
      if (status != null) {
        predicates.add (cb.equal (root.get ("status"), status));
      }
      return cb.and (predicates.toArray (new Predicate[0]));
    };
  }

  /** 視圖篩選：送件人三欄合一、田區縣市/鄉鎮、作物類別、害物/類別、建議、送件方式，LEFT JOIN 已涵蓋可空 */
  public static Specification<CaseSearchView> buildView (CaseFilter filter, CaseStatus status) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (filter.cropId () != null) {
        predicates.add (cb.equal (root.get ("cropId"), filter.cropId ()));
      }
      if (filter.serviceId () != null) {
        predicates.add (cb.equal (root.get ("serviceId"), filter.serviceId ()));
      }
      if (filter.deliveryId () != null) {
        predicates.add (cb.equal (root.get ("deliverId"), filter.deliveryId ()));
      }
      if (filter.cityId () != null) {
        predicates.add (cb.equal (root.get ("cityId"), filter.cityId ()));
      }
      if (filter.districtId () != null) {
        predicates.add (cb.equal (root.get ("districtId"), filter.districtId ()));
      }
      if (filter.cropCategoryId () != null) {
        predicates.add (cb.equal (root.get ("cropCategoryId"), filter.cropCategoryId ()));
      }
      if (filter.senderTypeId () != null) {
        predicates.add (cb.equal (root.get ("senderTypeId"), filter.senderTypeId ()));
      }
      if (filter.methodId () != null) {
        predicates.add (cb.equal (root.get ("methodId"), filter.methodId ()));
      }
      String senderQuery = filter.senderQuery () != null && !filter.senderQuery ().isBlank ()
          ? filter.senderQuery () : filter.senderName ();
      if (senderQuery != null && !senderQuery.isBlank ()) {
        String escaped = senderQuery.replace ("\\", "\\\\").replace ("%", "\\%").replace ("_", "\\_");
        String pattern = "%" + escaped + "%";
        Predicate p1 = cb.like (root.get ("senderName"), pattern, '\\');
        Predicate p2 = cb.like (root.get ("senderDisplayName"), pattern, '\\');
        Predicate p3 = cb.like (root.get ("senderPhone"), pattern, '\\');
        predicates.add (cb.or (p1, p2, p3));
      }
      if (filter.receiveDateFrom () != null) {
        predicates.add (cb.greaterThanOrEqualTo (root.get ("receiveDate"), filter.receiveDateFrom ()));
      }
      if (filter.receiveDateTo () != null) {
        predicates.add (cb.lessThanOrEqualTo (root.get ("receiveDate"), filter.receiveDateTo ()));
      }
      if (status != null) {
        predicates.add (cb.equal (root.get ("status"), status.ordinal ()));
      }
      if (filter.pestTypeId () != null) {
        predicates.add (pestTypeExists (root, query, cb, filter.pestTypeId ()));
      }
      if (filter.pestCategoryId () != null) {
        predicates.add (pestCategoryExists (root, query, cb, filter.pestCategoryId ()));
      }
      if (filter.hintId () != null) {
        predicates.add (hintExists (root, query, cb, filter.hintId ()));
      }
      if (filter.damageId () != null) {
        predicates.add (damageExists (root, query, cb, filter.damageId ()));
      }
      return cb.and (predicates.toArray (new Predicate[0]));
    };
  }

  private static Predicate pestTypeExists (Root<CaseSearchView> root,
      jakarta.persistence.criteria.CriteriaQuery<?> query,
      jakarta.persistence.criteria.CriteriaBuilder cb, Long pestTypeId) {
    Subquery<Integer> sq = query.subquery (Integer.class);
    Root<CasePestCategory> cpc = sq.from (CasePestCategory.class);
    sq.select (cb.literal (1)).where (cb.equal (cpc.get ("caseEntity").get ("caseId"), root.get ("caseId")),
        cb.equal (cpc.get ("pestCategory").get ("pestType").get ("pestTypeId"), pestTypeId));
    return cb.exists (sq);
  }

  private static Predicate pestCategoryExists (Root<CaseSearchView> root,
      jakarta.persistence.criteria.CriteriaQuery<?> query,
      jakarta.persistence.criteria.CriteriaBuilder cb, Long pestCategoryId) {
    Subquery<Integer> sq = query.subquery (Integer.class);
    Root<CasePestCategory> cpc = sq.from (CasePestCategory.class);
    sq.select (cb.literal (1)).where (cb.equal (cpc.get ("caseEntity").get ("caseId"), root.get ("caseId")),
        cb.equal (cpc.get ("pestCategory").get ("pestCategoryId"), pestCategoryId));
    return cb.exists (sq);
  }

  private static Predicate hintExists (Root<CaseSearchView> root,
      jakarta.persistence.criteria.CriteriaQuery<?> query,
      jakarta.persistence.criteria.CriteriaBuilder cb, Long hintId) {
    Subquery<Integer> sq = query.subquery (Integer.class);
    Root<CaseHint> ch = sq.from (CaseHint.class);
    sq.select (cb.literal (1)).where (cb.equal (ch.get ("caseEntity").get ("caseId"), root.get ("caseId")),
        cb.equal (ch.get ("hint").get ("hintId"), hintId));
    return cb.exists (sq);
  }

  private static Predicate damageExists (Root<CaseSearchView> root,
      jakarta.persistence.criteria.CriteriaQuery<?> query,
      jakarta.persistence.criteria.CriteriaBuilder cb, Long damageId) {
    Subquery<Integer> sq = query.subquery (Integer.class);
    Root<CaseDamage> cd = sq.from (CaseDamage.class);
    sq.select (cb.literal (1)).where (cb.equal (cd.get ("caseEntity").get ("caseId"), root.get ("caseId")),
        cb.equal (cd.get ("damage").get ("damageId"), damageId));
    return cb.exists (sq);
  }
}
