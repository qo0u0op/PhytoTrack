package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "case_damages",
       uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "damage_id"}))
public class CaseDamage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long cdId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private Case caseEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "damage_id", nullable = false)
  private Damage damage;
}
