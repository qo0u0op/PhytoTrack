package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}
