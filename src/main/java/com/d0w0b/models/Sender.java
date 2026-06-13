package com.d0w0b.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "senders")
public class Sender {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long senderId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String phone;

  @Column(nullable = false)
  private String address;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "district_id", nullable = false)
  private District district;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "sender_type_id", nullable = false)
  private SenderType senderType;
}
