## Context

見 `proposal.md - Why`：`security-review` B 組待辦中 `security-hardening` Phase 2 已完成可即修 6 項，剩餘 **認證端點無節流、CORS `*`、缺少 CSP/HSTS** 未落實。目前 `CorsConfig.java:27` 寫死 `allowedOriginPatterns=*`、`SecurityConfig` 未注入安全標頭、認證端點無 `429` 分支，LAN 5 人內使用風險尚可但隨 `account-self-service` 自助流程上線需補齊。單機 SQLite、無 Redis、無閘道，須以記憶體內方案收斂；前端 `stores/auth.ts:15` 與 `api/http.ts:12` 維持 `localStorage` token，遷移 `httpOnly` cookie 屬破壞性變更不在本批次。

## Goals / Non-Goals

**Goals:**
- 認證端點每 IP 10/min 限流並回 `429 + Retry-After + error.code=RATE_LIMITED + requestId`，可被 `grep requestId` 追溯
- CORS 由 `CORS_ALLOWED_ORIGINS` 白名單驅動，`dev` 為空相容 `*`，`prod` 為空預設拒絕跨源，前端本地開發零配置
- 非 dev 回應一律帶 `CSP / HSTS / nosniff / DENY`，Swagger inline style 仍可用
- 明確決策維持 `localStorage` 並文件化，後續遷移條件可追溯

**Non-Goals:**
- 分佈式限流（Redis/DB）、帳號維度或滑動窗口精確語意；本批次僅 IP 固定視窗即足夠
- 遷移 `httpOnly` cookie、恢復 CSRF、導入 WAF 或集中式日誌
- 變更既有 `401/403` 語意或既有 CORS 方法／標頭白名單

## Decisions

### D1 速率限制：Bucket4j + Caffeine 記憶體桶，`RateLimitFilter` 優先於 `JwtAuthenticationFilter`

- **選擇**：`com.bucket4j:bucket4j-core:8.10.1` + `com.github.ben-manes.caffeine:caffeine`，`Caffeine` 以 `expireAfterWrite 65s` + `maximumSize 10k` 承載 `IP -> Bucket`，Bucket 為 `Bandwidth.simple(10, Duration.ofMinutes(1))` 固定視窗；`RateLimitFilter extends OncePerRequestFilter` 掛於 `SecurityFilterChain` 最前（`addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)` 且順序上早於 `JwtAuthenticationFilter`），僅對 `POST /api/auth/login|register|abandon-deactivate` 生效，其餘直通。
- **超限處理**：`bucket.tryConsume(1)` 失敗時直接寫 `429` JSON（沿用 `GlobalExceptionHandler` 的 `ApiError` 形狀：`error.code=RATE_LIMITED`、`message=Too many requests, please retry later`、`requestId` 來自 `RequestIdFilter` 的 MDC），Header `Retry-After: <剩餘秒數>`（由 `bucket.getAvailableTokens` 與 refill 計算或固定 `60` 簡化），並以 `log.warn("[{}] rate limited ip={} path={}", requestId, ip, path)` 供追蹤。
- **IP 取得**：LAN 無反向代理，取 `request.getRemoteAddr()`；若未來有代理，再以 `X-Forwarded-For` 首段覆蓋（保留擴充點，不預設信任）。
- **可配置與測試**：`application.yaml` 新增 `app.rate-limit.enabled=true`、`app.rate-limit.requests-per-minute=10`、`app.rate-limit.window-seconds=60`；`application-test.yaml` 設 `app.rate-limit.enabled=false` 以免 `mvn test` 被限流誤殺；亦可在測試中以 `@TestPropertySource` 覆蓋。
- **替代考慮**：Resilience4j RateLimiter 需額外 registry 且主要面向方法級；手寫 `ConcurrentHashMap + AtomicInteger + ScheduledCleaner` 可行但需自理併發與清理，Bucket4j 已完整處理且依賴輕量；Redis 方案過重且與單機 SQLite 部署不一致。

### D2 CORS 白名單：env `CORS_ALLOWED_ORIGINS` → `app.cors.allowed-origins` → `CorsConfiguration`

- **選擇**：`application.yaml` 新增 `app.cors.allowed-origins: ${CORS_ALLOWED_ORIGINS:}`（空代表未配置）；`CorsConfig` 注入 `Environment` 或 `@Value` 解析逗號分隔、trim、去空、去重；若列表為空且 `spring.profiles.active` 含 `dev` 則 `setAllowedOriginPatterns(List.of("*"))`（沿用現行為）、否則 `setAllowedOrigins(explicitList)`（明確 Origin，不用 pattern），`setAllowCredentials(false)` 維持現狀，非 `/api/**` 不套用；`Vary: Origin` 由 Spring 自動處理。
- **暴露標頭與方法**：維持 `Authorization,Content-Disposition,X-Request-Id` 與 `GET,POST,PUT,PATCH,DELETE,OPTIONS`、`AllowedHeaders *` 不變。
- **多環境行為**：`dev` 未配置→ `*`（零配置啟動）；`prod` 未配置→ 空列表 → 無 `Access-Control-Allow-Origin`（同源部署不受影響，跨源預設阻擋，符合最小驚訝）；配置範例 `CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com` 於 `docs/DEPLOY.md` 明示。
- **替代考慮**：維持 `allowedOriginPatterns=*` 並僅在 prod 以 `CorsProcessor` 攔截拒絕，語意較繞；直接改 `allowedOriginPatterns` 為白名單列表在 `*` 混用時不易區分通配與明確，改 `allowedOrigins` 更精確。

### D3 安全標頭：`SecurityHeadersFilter`（非 dev）或 `HttpSecurity.headers` 配置

- **選擇**：新增 `config/SecurityHeadersFilter extends OncePerRequestFilter` 以 `@ConditionalOnProperty("app.security-headers.enabled")` 或 `Environment.acceptsProfiles(Profiles.of("prod"))` 判斷啟用，亦可在 `SecurityConfig` 以 `http.headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; font-src 'self' data:")) .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000)) .contentTypeOptions(Customizer.withDefaults()) .frameOptions(f -> f.deny()))` 達成；本設計採 Filter 以便與 `RequestIdFilter` 同管 MDC 且易於單元測試無需啟動完整 Security chain。
- **CSP 取捨**：`style-src 'unsafe-inline'` 為遷就 Swagger UI inline style（`docs/REQUIREMENTS.md` 已註記），`script-src 'self'` 不含 `unsafe-inline`；後續若 Swagger 於 prod 關閉可再收斂。
- **HSTS**：僅在 `X-Forwarded-Proto=https` 或 prod profile 下有意義，本地 `http` 不影響；LAN 內 http 亦無副作用（瀏覽器僅在 https 下遵守）。
- **替代考慮**：於 `application.yaml` 以 `server.servlet.session.cookie.secure` 控制非本需求範圍；以 Nginx 層加標頭雖可但本專案為單機 Spring Boot 直出，應用層加標頭更可攜。

### D4 Token 儲存：維持 `localStorage`，文件化不遷移

- **選擇**：本次不改 `stores/auth.ts` 與 `api/http.ts`，於 `docs/ARCHITECTURE.md:認證授權` 與 `docs/adr/ADR-0xx-token-storage-cors.md` 明確記錄：現無 XSS 注入點（AI 輸出已轉義、`v-html` 禁用）、遷移 `httpOnly` cookie 需恢復 CSRF（`SecurityConfig.csrf(Customizer.withDefaults())` + `CookieCsrfTokenRepository` + 前端 `X-XSRF-TOKEN` 讀寫）屬流程破壞性改動，待有明確 XSS 面或合規要求再評估。
- **替代考慮**：立即遷移 `httpOnly` + `SameSite=Strict` 可降低竊取面，但需前後端同步改動且與現有 `Bearer` 契約衝突，本批次不承擔。

## Risks / Trade-offs

- [單機記憶體限流跨重啟丟失] → 接受：LAN 單機、5 人規模、重啟即重置視窗屬可接受； mitigation：`Caffeine` 自動過期避免洩漏
- [IP 偽造與 NAT 共用] → `X-Forwarded-For` 不信任、僅 `remoteAddr`，LAN 內 NAT 共用 10/min 對 5 人仍寬鬆；後續若人數增可改帳號維度
- [測試被限流誤殺] → `test` profile 預設 `enabled=false`，單測以 `MockMvc` 不經 Filter 或以 `@TestPropertySource` 明確覆蓋；整合測試對 `429` 的用例獨立文件標記需啟用限流
- [CORS 收斂導致 prod 跨源失效] → `docs/DEPLOY.md` 與 `application.yaml` 註解明示必填 `CORS_ALLOWED_ORIGINS`，同源部署不受影響； mitigation：啟動日誌 `INFO` 印出有效白名單（不含敏感）
- [CSP `unsafe-inline` 削弱] → 為 Swagger 相容的權衡，`script-src` 仍嚴格； mitigation：prod 已關閉 `springdoc`（`application.yaml` `prod` 段），實際暴露面低
- [HSTS 在 http 下無效] → 接受：標頭僅在 https 被遵守，http 下僅為聲明，無負面影響

## Migration Plan

1. **DB**：無 schema 變更，無遷移
2. **後端部署**：`mvn test`（含 `test` 關閉限流）、`mvn spring-boot:run -Dspring-boot.run.profiles=dev` 手測 `curl -i -H "Origin: https://evil.com" http://localhost:8080/api/cases` 驗 CORS；`curl -i http://localhost:8080/api/auth/login` 連 11 次驗 `429` 與 `Retry-After`；`curl -i -H "Origin: https://app.example.com" http://localhost:8080/api/cases` 驗放行；`curl -i http://localhost:8080/api/cases` 驗 `CSP/HSTS` 僅 `prod` 帶
3. **Env**：`CORS_ALLOWED_ORIGINS` 於 `backend/.env` 或 systemd/env 注入，`APP_RATE_LIMIT_ENABLED` 等可選
4. **前端**：無改動，僅 `api/http.ts` 可選補 `429` 提示（`toast: 請稍後再試`）
5. **文件**：更新 `docs/ARCHITECTURE.md`、`docs/DEPLOY.md`、`docs/adr/ADR-0xx`、操作手冊若涉及部署步驟
6. **Rollback**：`app.rate-limit.enabled=false` 或移除 Filter Bean 即關閉限流；`CORS_ALLOWED_ORIGINS=*` 僅在 `dev` 生效，`prod` 回滾需填回 `*`（不建議）或直接重啟舊版 jar；安全標頭 Filter 以 profile 開關控制

## Open Questions

- 速率限制是否需對 `GET /api/auth/me` 等已認證端點擴大？目前僅公開端點，後續依濫用觀察再擴。
- `Retry-After` 採用固定 60 秒或精確剩餘秒數，實作時以後者為佳但允許前者簡化。
