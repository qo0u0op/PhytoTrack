package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "crops",
       uniqueConstraints = @UniqueConstraint(columnNames = {"crop_category_id", "crop"}))
public class Crop {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cropId;

  @Column(nullable = false)
  private String crop;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crop_category_id", nullable = false)
  private CropCategory cropCategory;
}
