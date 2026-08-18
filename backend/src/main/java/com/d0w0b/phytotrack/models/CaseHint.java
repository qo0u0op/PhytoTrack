package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "case_hints",
       uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "hint_id"}))
public class CaseHint {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long chId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private Case caseEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hint_id", nullable = false)
  private Hint hint;
}
