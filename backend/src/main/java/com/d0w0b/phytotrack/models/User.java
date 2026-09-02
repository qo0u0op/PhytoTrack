package com.d0w0b.phytotrack.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
  public enum Role {
    ROLE_VIEWER, ROLE_STAFF, ROLE_ADMIN
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long userId;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false)
  private String displayName;

  @Column(nullable = false, length = 60)
  private String password;

  @Column
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Role role = Role.ROLE_VIEWER;

  @Column(nullable = false)
  private boolean active = true;
}
