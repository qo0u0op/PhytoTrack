package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "identifiers")
public class Identifier {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long identifierId;

  @Column(nullable = false)
  private String identifier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;
}
