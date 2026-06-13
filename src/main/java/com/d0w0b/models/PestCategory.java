package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pest_categories")
public class PestCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long pestCategoryId;

  @Column(nullable = false)
  private String pestCategoryCode;

  @Column(nullable = false)
  private String pestCategory;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pest_type_id", nullable = false)
  private PestType pestType;

  @Column(nullable = false)
  private int sortOrder;
}
