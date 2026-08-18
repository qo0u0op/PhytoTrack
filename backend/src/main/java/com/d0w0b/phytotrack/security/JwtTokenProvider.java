package com.d0w0b.phytotrack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.d0w0b.phytotrack.models.User;

/**
 * JWT（JSON Web Token）產生與驗證元件
 *
 * 職責：
 *   - 產生：登入成功後，將使用者識別資訊簽章為 Token
 *   - 驗證：解析請求攜帶的 Token，取出 subject（使用者名稱）、角色與使用者 ID
 *
 * 設計說明：
 *   - 無狀態（Stateless）：伺服器不儲存 Token，驗證只需檢查簽章與有效期限
 *   - 缺點：Token 在有效期限前無法主動撤銷（以短效 Token 緩解）
 */
@Component
public class JwtTokenProvider {

  /** 簽章密鑰（Signing Key），HS256 至少需 32 bytes */
  private final SecretKey key;

  /** Token 有效期限（毫秒） */
  private final long expirationMs;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-ms}") long expirationMs) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
  }

  /**
   * 產生 Token
   *
   * @param user 已驗證的使用者
   * @return 以使用者名稱、使用者 ID、角色為內容的簽章 Token
   */
  public String generateToken(User user) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .subject(user.getUsername())
        .claim("userId", user.getUserId())
        .claim("role", user.getRole().name())
        .issuedAt(now)
        .expiration(expiry)
        .signWith(key)
        .compact();
  }

  /**
   * 解析並驗證 Token，取出宣告（Claims）
   *
   * @param token 請求攜帶的 JWT
   * @return Token 內容；驗證失敗或已過期回傳 null
   */
  public Claims parseToken(String token) {
    try {
      return Jwts.parser()
          .verifyWith(key)
          .build()
          .parseSignedClaims(token)
          .getPayload();
    } catch (Exception e) {
      // 簽章錯誤、格式錯誤或過期等，一律視為無效
      return null;
    }
  }
}
