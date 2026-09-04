package com.d0w0b.phytotrack.models;

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

  /**
   * 解綁前最後所屬使用者（升權／啟用時恢復原筆的依據）。
   * 綁定至使用者時清空；既有未綁定簽名人為 null。
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "former_user_id")
  private User formerUser;

  @Column(nullable = false)
  private boolean active = true;
}
