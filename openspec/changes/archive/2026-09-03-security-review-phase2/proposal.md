# 提案：security-review Phase 2（速率限制 / CORS 白名單 / 安全標頭）

## Why

`security-review` 規格彙整 2026-08-20 OWASP 審查 B 組待辦，`security-hardening` Phase 2 已補齊 6 項可即修項目（BCrypt 12、JWT issuer、Swagger 關閉等），但 **登入／註冊無速率限制（暴力破解／大量註冊）、CORS `*` 通配、缺少 CSP/HSTS** 仍列為待實作。隨 LAN 部署與帳號自助流程（`account-self-service`）上線，認證端點暴露風險升高：無 429 節流、非白名單 Origin 仍可取 `*`、prod 回應缺少 `Content-Security-Policy` / `Strict-Transport-Security`，需在 Phase 2 補齊最小可用加固。

## What Changes

- **登入／註冊速率限制**：對 `POST /api/auth/login`、`POST /api/auth/register`（含 `POST /api/auth/abandon-deactivate` 視為同級）施加每 IP 每分鐘 10 次限制，超出回 `429 Too Many Requests` 並附 `Retry-After` 秒數與統一錯誤形狀（`error.code=RATE_LIMITED`）；以記憶體桶（Bucket4j 或等價 in-memory token bucket + Caffeine/ConcurrentHashMap 清理）實作，無外部 Redis，LAN 單機足夠；`test` profile 可透過設定關閉或放寬以免影響 `mvn test`。
- **CORS 白名單化**：`CorsConfig` 由 `allowedOriginPatterns=*` 改為 env `CORS_ALLOWED_ORIGINS`（逗號分隔）驅動；`dev` 為空時沿用 `*` 相容本地 `localhost:5173`，`prod` 為空時預設拒絕非同源（不回 `Access-Control-Allow-Origin`），明確白名單才放行；保留 `Authorization/Content-Disposition/X-Request-Id` 暴露標頭與方法／標頭白名單，說明文件同步。
- **安全標頭（CSP/HSTS）**：新增 `SecurityHeadersFilter`（或於 `SecurityConfig` header 配置）於非 dev profile 為所有回應補 `Content-Security-Policy`（兼顧 Swagger inline style：`style-src 'self' 'unsafe-inline'`）與 `Strict-Transport-Security: max-age=31536000; includeSubDomains`；`X-Content-Type-Options: nosniff` 與 `X-Frame-Options: DENY` 若 Spring Security 尚未默認亦一併補齊；`dev` 不強制以免影響本地 http。
- **Token 儲存評估定案**：維持 `localStorage`（現無 XSS 注入點，遷移 `httpOnly` cookie 需恢復 CSRF 且為認證流程破壞性變更），於 ADR / `docs/ARCHITECTURE.md` 明確記錄決策與後續遷移條件，不於本 change 改動前端 `stores/auth.ts` 儲存機制。

**非目標**：不引入分佈式限流（Redis）、不遷移 `httpOnly` cookie、不導入 WAF／集中式日誌；僅完成 OWASP B 組最小可部署加固。

## Capabilities

### New Capabilities

<!-- 無新增能力，僅落實既有 security-review 待辦 -->

### Modified Capabilities

- `security-review`: 落實 Requirement「登入與註冊速率限制」與「Token 儲存與 CORS 強化（CORS 白名單、CSP/HSTS）」；原「維持現狀」條目保留 CSRF off／無狀態登出等項目，新增速率限制與標頭的可驗證 Scenario。

## Impact

- **後端**：`pom.xml` 新增 `bucket4j-core`（或 `com.github.vladimir-bukhtoyarov:bucket4j-core`）與 `caffeine`（若選）；新增 `config/RateLimitFilter.java` + `config/RateLimitService.java`（或合一）、`config/SecurityHeadersFilter.java`；修改 `config/CorsConfig.java`（讀 `app.cors.allowed-origins`）、`config/SecurityConfig.java`（掛 filter 順序）、`application.yaml`（`app.cors.allowed-origins`、`app.rate-limit.*`、`app.security-headers.*` 與 prod profile 覆寫）、`exception/GlobalExceptionHandler.java`（429 映射）。
- **前端**：不改 `localStorage`，僅於 `api/http.ts` 補 429 提示（可選）；文件層記錄決策。
- **文件**：`docs/ARCHITECTURE.md` §安全、`docs/DEPLOY.md` §CORS／速率限制／安全標頭、`docs/adr/ADR-0xx`（CORS 白名單與 token 儲存決策）。
- **相容性**：公開端點行為新增 429 分支，其餘契約不變；CORS 收斂僅影響 prod 非白名單 Origin，LAN 部署需配置 `CORS_ALLOWED_ORIGINS`，否則預設拒絕跨源（同源部署不受影響）。
