package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "damages")
public class Damage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long damageId;

  @Column(nullable = false)
  private String damage;
}
