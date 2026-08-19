package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "case_pest_categories",
       uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "pest_category_id"}))
public class CasePestCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cpcId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private Case caseEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pest_category_id", nullable = false)
  private PestCategory pestCategory;
}
