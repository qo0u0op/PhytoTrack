package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "crop_categories")
public class CropCategory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cropCategoryId;

  @Column(nullable = false)
  private String cropCategory;

  /** 反向關聯：此分類下的作物清單（配合 @EntityGraph 一次抓取） */
  @OneToMany(mappedBy = "cropCategory", fetch = FetchType.LAZY)
  private List<Crop> crops;
}
