package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
}
