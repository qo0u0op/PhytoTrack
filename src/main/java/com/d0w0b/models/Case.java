package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cases")
public class Case {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long caseId;

  @Column(nullable = false)
  private LocalDate receiveDate;

  private String cropScale;

  private String damageScale;

  @Column(columnDefinition = "TEXT")
  private String pestDescription;

  @Column(columnDefinition = "TEXT")
  private String hintDescription;

  @Column(nullable = false)
  private int status = 0;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_id", nullable = false)
  private Sender sender;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "method_id", nullable = false)
  private Method method;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "crop_id", nullable = false)
  private Crop crop;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "service_id", nullable = false)
  private Service service;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "deliver_id", nullable = false)
  private Delivery delivery;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
