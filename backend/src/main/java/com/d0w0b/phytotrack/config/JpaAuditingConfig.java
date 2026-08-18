package com.d0w0b.phytotrack.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import com.d0w0b.phytotrack.models.User;
import com.d0w0b.phytotrack.repository.UserRepository;
import com.d0w0b.phytotrack.security.UserPrincipal;

/**
 * JPA 稽核（Auditing）設定
 *
 * 讓框架自動填寫實體的 @CreatedDate / @LastModifiedDate / @CreatedBy，
 * 取代每個實體手寫 @PrePersist 的樣板碼。
 *
 * @CreatedBy 由 AuditorAware 從 SecurityContext（安全上下文）取得目前登入使用者。
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

  /** 注入使用者資料存取層（Data Access Layer），供 AuditorAware 查詢目前使用者 */
  private final UserRepository userRepository;

  public JpaAuditingConfig(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * 提供「目前登入使用者」給稽核機制（Auditing）
   *
   * 從 SecurityContext 取出已認證的 UserPrincipal，再用 userId 查詢資料庫，
   * 供 @CreatedBy 欄位自動填入。
   */
  @Bean
  public AuditorAware<User> auditorAware() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()
          || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
        return Optional.empty();
      }
      return userRepository.findById(principal.getUserId());
    };
  }
}
