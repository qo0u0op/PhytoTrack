package com.d0w0b.phytotrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * JWT 密鑰啟動期檢查器 (JWT Secret Validator)
 *
 * 目的 (見 ADR-004)：JWT 密鑰若沿用開發預設值，等於所有人都能偽造 Token。
 * 此檢查器在非 dev 環境使用預設密鑰時，於啟動階段直接失敗 (fail-fast)，
 * 避免正式部署時忘了以環境變數 JWT_SECRET 覆蓋。
 */
@Component
public class JwtSecretValidator {

  /** 開發預設密鑰：必須與 application.yaml 的 app.jwt.secret 預設值一致 */
  private static final String DEV_DEFAULT_SECRET = "phytotrack-dev-secret-please-change-in-production-0123456789";

  public JwtSecretValidator (Environment environment, @Value ("${app.jwt.secret}") String secret) {
    boolean isDev = environment.acceptsProfiles (Profiles.of ("dev", "test"));
    if (!isDev && DEV_DEFAULT_SECRET.equals (secret)) {
      throw new IllegalStateException ("JWT 密鑰仍為開發預設值，禁止用於非 dev 環境。請以環境變數 JWT_SECRET 提供正式密鑰。");
    }
  }
}
