package com.d0w0b.phytotrack.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 速率限制服務（RateLimitService）
 *
 * 以 IP 為 key 的 token bucket，固定視窗 10 次/60 秒，Caffeine 快取自動過期。
 * 單機記憶體方案，無外部依賴，符合 LAN 5 人部署。
 */
@Service
public class RateLimitService {

  private final Cache<String, Bucket> buckets;
  private final int requestsPerMinute;
  private final int windowSeconds;
  private final boolean enabled;

  public RateLimitService (
      @Value ("${app.rate-limit.enabled:true}") boolean enabled,
      @Value ("${app.rate-limit.requests-per-minute:10}") int requestsPerMinute,
      @Value ("${app.rate-limit.window-seconds:60}") int windowSeconds) {
    this.enabled = enabled;
    this.requestsPerMinute = requestsPerMinute;
    this.windowSeconds = windowSeconds;
    // 過期時間比視窗略長，避免視窗內重建桶
    this.buckets = Caffeine.newBuilder ()
        .expireAfterWrite (Duration.ofSeconds (windowSeconds + 5))
        .maximumSize (10_000)
        .build ();
  }

  /**
   * 嘗試消耗 1 個 token。
   * @return true 若未超限（已消耗），false 若已達上限
   */
  public boolean tryConsume (String ip) {
    if (!enabled) {
      return true;
    }
    String key = ip == null ? "unknown" : ip;
    Bucket bucket = buckets.get (key, k -> newBucket ());
    return bucket.tryConsume (1);
  }

  /** 建立新桶：固定視窗 refillGreedy 與數量限制一致 */
  private Bucket newBucket () {
    Bandwidth limit = Bandwidth.simple (requestsPerMinute, Duration.ofSeconds (windowSeconds));
    return Bucket.builder ().addLimit (limit).build ();
  }

  /** 供測試：清除指定 IP 的桶 */
  public void clear (String ip) {
    buckets.invalidate (ip);
  }

  /** 供測試：清除所有桶 */
  public void clearAll () {
    buckets.invalidateAll ();
  }

  public boolean isEnabled () {
    return enabled;
  }
}
