package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sender_types")
public class SenderType {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long senderTypeId;

  @Column(nullable = false)
  private String senderType;
}
