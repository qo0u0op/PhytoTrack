package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "pest_types")
public class PestType {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long pestTypeId;

  @Column(nullable = false)
  private String pestType;

  /** 反向關聯：此類型下的小分類清單（配合 @EntityGraph 一次抓取） */
  @OneToMany(mappedBy = "pestType", fetch = FetchType.LAZY)
  private List<PestCategory> categories;
}
