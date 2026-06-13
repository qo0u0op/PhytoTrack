package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "districts")
public class District {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long districtId;

  @Column(nullable = false)
  private String district;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id", nullable = false)
  private City city;

  @Column(nullable = false)
  private int sortOrder;
}
