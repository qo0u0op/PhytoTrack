package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hints")
public class Hint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long hintId;

  @Column(nullable = false)
  private String hint;
}
