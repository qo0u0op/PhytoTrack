package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "methods")
public class Method {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long methodId;

  @Column(nullable = false)
  private String method;
}
