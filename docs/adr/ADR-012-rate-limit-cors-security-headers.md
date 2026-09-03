# ADR-012: 認證限流、CORS 白名單與安全標頭

**日期**: 2026-09-03

**狀態**: 已實作

**背景**:

2026-08-20 OWASP 審查 B 組指出三項待辦：登入/註冊無速率限制（暴力破解、大量註冊）、CORS `*` 通配、缺少 `Content-Security-Policy` / `Strict-Transport-Security`。Phase 0–1 已補前 6 項可即修（BCrypt 12、JWT issuer、Swagger 關閉等），本批為 Phase 2 最小可部署加固。部署型態為 LAN 單機 SQLite、無 Redis/閘道，5 人規模。

**選項**:

**限流**
1. Bucket4j + Caffeine 記憶體桶（IP 10/min，固定視窗，65s 過期，10k 上限）
2. 手寫 ConcurrentHashMap + AtomicInteger + 定時清理
3. Resilience4j RateLimiter / Spring Cloud Gateway
4. Redis 分佈式限流

**CORS**
1. env `CORS_ALLOWED_ORIGINS` 逗號白名單，dev 空→`*`、prod 空→拒絕（不回 Allow-Origin），明確 Origin 才回 `Vary: Origin`
2. 維持 `allowedOriginPatterns=*`
3. Nginx 層白名單

**安全標頭**
1. `SecurityHeadersFilter`（非 dev 注入 `CSP / HSTS / nosniff / DENY`，Swagger 需 `style-src 'unsafe-inline'`）
2. `HttpSecurity.headers` 配置
3. Nginx 層注入

**Token 儲存**
1. 維持 `localStorage`（現無 XSS 注入點，遷移 `httpOnly` cookie 需恢復 CSRF，屬破壞性）
2. 遷移 `httpOnly` + `SameSite=Strict` + `CSRF`

**決策**:

- **限流**：採 1。`RateLimitService` 以 Caffeine `IP -> Bucket`（`expireAfterWrite 65s`、`maximumSize 10k`），Bucket 為 `Bandwidth.simple(10, Duration.ofMinutes(1))`；`RateLimitFilter` 僅攔 `POST /api/auth/login|register|abandon-deactivate`，`tryConsume` 失敗回 `429` JSON（`error.code=RATE_LIMITED`、`requestId` 來自 MDC、`Retry-After: 60`）並 `log.warn`。`test` profile 預設 `app.rate-limit.enabled=false`，單測不經限流。
- **CORS**：採 1。`CorsConfig` 注入 `app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:}`，以逗號 split/trim/去重；空且 dev（含無 profile）→ `allowedOriginPatterns *`，空且 prod → `allowedOrigins []`（預設拒絕，同源不受影響）。保留方法/標頭白名單與 `exposedHeaders`，啟動 `log.info` 白名單。
- **安全標頭**：採 1（Filter 形式，易測且與 `RequestIdFilter` 同管 MDC）。`SecurityHeadersFilter` 於非 dev 或 `enabled=true` 時設 `Content-Security-Policy: default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; font-src 'self' data:`、`Strict-Transport-Security: max-age=31536000; includeSubDomains`、`X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`，已存在不覆蓋。
- **Token**：採 1。維持 `stores/auth.ts` 與 `api/http.ts` 的 `localStorage`，前端僅補 `429` toast（不重試、不清 token）；遷移條件記錄：出現 XSS 面或合規要求時，改 `httpOnly` + `CookieCsrfTokenRepository` + 前端 `X-XSRF-TOKEN`。

**原因**:

- 單機 5 人無需 Redis，記憶體桶重啟丟失可接受，Caffeine 自動清理避免洩漏
- LAN 無代理，取 `remoteAddr`，不信任 `X-Forwarded-For`
- `dev` 零配置相容 `localhost:5173`，`prod` 預設拒絕符合最小驚訝
- `style-src 'unsafe-inline'` 為 Swagger inline style 相容，`script-src` 仍嚴格；prod 已關 `springdoc`，暴露面低
- 維持 `localStorage` 避免 CSRF 回歸與 `Bearer` 契約破壞

**取捨**:

- 固定視窗非精確滑動，10/min 對 5 人寬鬆，NAT 共用亦可接受
- 記憶體限流跨重啟重置，屬可接受
- `prod` 未配 `CORS_ALLOWED_ORIGINS` 將阻擋跨源，需文件與啟動日誌明示
- CSP 含 `unsafe-inline` 略削弱，後續 Swagger 關閉可再收斂
- HSTS 於 http 下無效，僅作聲明
