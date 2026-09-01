package com.d0w0b.phytotrack.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.d0w0b.phytotrack.models.User;

/**
 * 目前登入使用者的安全主體 (Principal)
 *
 * 包裝 User 實體，實作 Spring Security 的 UserDetails 介面，
 * 讓 SecurityContext 能以統一方式取得使用者資訊與角色。
 */
public class UserPrincipal implements UserDetails {

  private final Long userId;
  private final String username;
  private final String password;
  private final String role;
  private final boolean active;

  public UserPrincipal (Long userId, String username, String password, String role, boolean active) {
    this.userId = userId;
    this.username = username;
    this.password = password;
    this.role = role;
    this.active = active;
  }

  /** 從 User 實體建立安全主體 */
  public static UserPrincipal from (User user) {
    return new UserPrincipal (user.getUserId (),
        user.getUsername (),
        user.getPassword (),
        user.getRole ().name (),
        user.isActive ());
  }

  public Long getUserId () {
    return userId;
  }

  public String getRole () {
    return role;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities () {
    // 角色需以 "ROLE_XXX" 形式宣告，才能用 hasRole () 判斷
    return List.of (new SimpleGrantedAuthority (role));
  }

  @Override
  public String getPassword () {
    return password;
  }

  @Override
  public String getUsername () {
    return username;
  }

  @Override
  public boolean isAccountNonExpired () {
    return true;
  }

  @Override
  public boolean isAccountNonLocked () {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired () {
    return true;
  }

  @Override
  public boolean isEnabled () {
    return active;
  }
}
