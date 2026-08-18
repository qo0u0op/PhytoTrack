package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "cases")
@EntityListeners(AuditingEntityListener.class)
public class Case {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long caseId;

  @Column(nullable = false)
  private LocalDate receiveDate;

  private String cropScale;

  private String damageScale;

  @Column(columnDefinition = "TEXT")
  private String pestDescription;

  @Column(columnDefinition = "TEXT")
  private String hintDescription;

  @Column(nullable = false)
  private int status = 0;

  /** 建立時間：由 JPA 稽核（Auditing）自動填寫 */
  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** 最後修改時間：由 JPA 稽核（Auditing）自動填寫 */
  @LastModifiedDate
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  /** 建立者：由稽核（Auditing）搭配 AuditorAware 自動填寫 */
  @CreatedBy
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private Sender sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "method_id", nullable = false)
  private Method method;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crop_id", nullable = false)
  private Crop crop;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id", nullable = false)
  private Service service;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deliver_id", nullable = false)
  private Delivery delivery;

  /** 案件的被害部位關聯（Cascade：隨案件一起新增/刪除） */
  @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CaseDamage> caseDamages = new ArrayList<>();

  /** 案件的防治建議關聯（Cascade：隨案件一起新增/刪除） */
  @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CaseHint> caseHints = new ArrayList<>();

  /** 案件的病蟲害小分類關聯（Cascade：隨案件一起新增/刪除） */
  @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CasePestCategory> casePestCategories = new ArrayList<>();

  /** 案件的診斷簽名人關聯（Cascade：隨案件一起新增/刪除） */
  @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CaseIdentifier> caseIdentifiers = new ArrayList<>();
}
