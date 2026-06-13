package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "case_identifiers",
       uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "identifier_id"}))
public class CaseIdentifier {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long ciId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "case_id", nullable = false)
  private Case caseEntity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "identifier_id", nullable = false)
  private Identifier identifier;
}
