package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cities")
public class City {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cityId;

  @Column(nullable = false)
  private String city;

  @Column(nullable = false)
  private int sortOrder;
}
