package com.d0w0b.phytotrack.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 案件篩選視圖 (v_case_search) 的唯讀映射
 *
 * 縣市/鄉鎮以田區位置 (cases.field_district_id → districts) 為準，與送件人地址分離；
 * 以 LEFT OUTER JOIN 涵蓋可空關聯，多對多以 GROUP_CONCAT 頓號聚合供顯示，
 * 篩選仍以 EXISTS 精確匹配。僅供列表篩選與匯出分頁透過 case_id 回補實體。
 * 使用 @Subselect 避免 Hibernate 對視圖進行 DDL 驗證 (SQLite 視圖中繼資料問題)。
 */
@Getter
@Entity
@Immutable
@Subselect ("select * from v_case_search")
@Synchronize ({"cases", "senders", "districts", "crops", "crop_categories", "case_pest_categories", "pest_categories", "case_hints", "hints", "case_damages", "damages"})
public class CaseSearchView {

  @Id
  @Column (name = "case_id")
  private Long caseId;

  @Column (name = "receive_date")
  private LocalDate receiveDate;

  @Column (name = "status")
  private Integer status;

  @Column (name = "created_at")
  private LocalDateTime createdAt;

  @Column (name = "sender_name")
  private String senderName;

  @Column (name = "sender_display_name")
  private String senderDisplayName;

  @Column (name = "sender_phone")
  private String senderPhone;

  @Column (name = "sender_type_id")
  private Long senderTypeId;

  @Column (name = "district_id")
  private Long districtId;

  @Column (name = "city_id")
  private Long cityId;

  @Column (name = "crop_id")
  private Long cropId;

  @Column (name = "crop_category_id")
  private Long cropCategoryId;

  @Column (name = "service_id")
  private Long serviceId;

  @Column (name = "deliver_id")
  private Long deliverId;

  @Column (name = "method_id")
  private Long methodId;

  @Column (name = "pest_category_count")
  private Integer pestCategoryCount;

  @Column (name = "pest_category_names")
  private String pestCategoryNames;

  @Column (name = "hint_names")
  private String hintNames;

  @Column (name = "damage_names")
  private String damageNames;
}
