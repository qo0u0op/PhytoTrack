## 1. 依賴與配置基座

- [x] 1.1 新增 `backend/pom.xml` 依賴 `com.bucket4j:bucket4j-core:8.10.1` 與 `com.github.ben-manes.caffeine:caffeine`，執行 `mvn -q -DskipTests package` 驗證下載成功且無版本衝突
- [x] 1.2 新增 `application.yaml` 配置 `app.rate-limit.enabled`、`app.rate-limit.requests-per-minute=10`、`app.rate-limit.window-seconds=60`、`app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:}`、`app.security-headers.enabled`（prod 覆寫為 true），並於 `application-test.yaml` 設 `app.rate-limit.enabled=false`，驗證 `mvn test -Dtest=Dummy` 啟動不報 `BindException`
- [x] 1.3 更新 `backend/.env.example` 與 `docs/DEPLOY.md` 範例加入 `CORS_ALLOWED_ORIGINS` 說明，驗證文件與 `application.yaml` 註解一致

## 2. 認證端點速率限制

- [x] 2.1 實作 `config/RateLimitService.java`（Caffeine `Cache<String,Bucket>` 以 IP 為 key，`expireAfterWrite 65s`、`maximumSize 10k`，`Bandwidth.simple(10, Duration.ofMinutes(1))`），單元測試驗證同一 IP 10 次內 `tryConsume` 成功、第 11 次失敗且不同 IP 互不影響
- [x] 2.2 實作 `config/RateLimitFilter.java`（`OncePerRequestFilter`，僅攔 `POST /api/auth/login|register|abandon-deactivate`，取 `remoteAddr`，命中限制時寫 `429` JSON：`error.code=RATE_LIMITED`、`error.message`、`requestId` 來自 MDC、`Retry-After` 標頭，並 `log.warn` 含 requestId/ip/path），驗證 filter 未匹配路徑直通且不影響 `GET /api/cases`
- [x] 2.3 掛載 Filter 至 `SecurityConfig.securityFilterChain`（`addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)` 確保早於 `JwtAuthenticationFilter`），並確認匿名請求亦被限流，驗證 `SecurityConfig.java:64` 後 endpoint 規則不變且 `mvn test` 不因順序報錯
- [x] 2.4 補 `exception/GlobalExceptionHandler` 或 Filter 內統一 `429` 錯誤形狀（若用例外則拋 `RateLimitedException` 並映射），驗證 `MockMvc` 呼叫 `POST /api/auth/login` 第 11 次回 `429` 且 body 含 `error.code` 與 `requestId`、header 含 `Retry-After`
- [x] 2.5 撰寫整合測試 `RateLimitIntegrationTest`（`@ActiveProfiles("prod-like")` 或 `@TestPropertySource(app.rate-limit.enabled=true)`）：60 秒內 11 次 `POST /api/auth/login` 第 11 次 429、混合 `login+register` 合計 11 次 429、等待視窗重置後恢復 200/401，驗證 `mvn test -Dtest=RateLimitIntegrationTest` 全綠且 `test` profile 預設不啟用限流

## 3. CORS 白名單化

- [x] 3.1 重構 `config/CorsConfig.java`：注入 `app.cors.allowed-origins`，以逗號 split/trim/去重解析為列表；若空且 `dev` profile 則 `setAllowedOriginPatterns(List.of("*"))`，若空且非 dev 則 `setAllowedOrigins(List.of())`（預設拒絕），否則 `setAllowedOrigins(explicitList)`，保留既有 `allowedMethods/Headers/exposedHeaders/allowCredentials`，驗證 Bean 啟動不拋 `IllegalArgumentException`
- [x] 3.2 加入啟動日誌 `log.info("CORS allowed-origins={} (profile={})", effectiveList, env.getActiveProfiles())`，驗證 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 日誌可見且不洩敏感 header
- [x] 3.3 撰寫 `CorsConfigTest` 與 `MockMvc` 測試：白名單內 Origin 回 `Access-Control-Allow-Origin` + `Vary: Origin`、非白名單不回 `Allow-Origin`、prod 空配置拒絕、dev 空配置沿用 `*`、預檢 `OPTIONS` 行為正確，驗證 `mvn test -Dtest=CorsConfigTest` 通過

## 4. 安全回應標頭（CSP/HSTS）

- [x] 4.1 新增 `config/SecurityHeadersFilter.java`（`OncePerRequestFilter`，非 dev 或 `app.security-headers.enabled=true` 時為所有回應設 `Content-Security-Policy: default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; img-src 'self' data:; font-src 'self' data:`、`Strict-Transport-Security: max-age=31536000; includeSubDomains`、`X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`，已存在標頭不覆蓋），驗證 Filter 僅在 prod 啟用且不影響 `dev` 回應
- [x] 4.2 驗證與既有 `SecurityConfig` header 配置無衝突（若 Spring Security 已默認 `nosniff/DENY` 則避免重複），並以 `WebMvcTest` 或 `MockMvc` 斷言 `prod` 請求 `GET /api/cases` 回應含上述四標頭、`dev` 請求可不含 `HSTS`，驗證 `mvn test -Dtest=SecurityHeadersFilterTest` 通過
- [x] 4.3 手測 Swagger 相容：`prod` 下 `GET /swagger-ui/index.html` 仍可載入樣式（`style-src 'unsafe-inline'`），驗證瀏覽器 console 無 CSP 阻擋錯誤

## 5. Token 儲存決策與前端文件

- [x] 5.1 維持 `frontend/src/stores/auth.ts` 與 `frontend/src/api/http.ts` 的 `localStorage` 實作不變，於 `api/http.ts` 可選新增 `429` 分支 `toast("請求過於頻繁，請稍後再試")`，驗證 `npm run build`（含 `vue-tsc`）通過且 11 次 429 時前端不進入無限重試
- [x] 5.2 新增 `docs/adr/ADR-0XX-token-storage-and-cors.md` 記錄決策：維持 `localStorage` 理由（無 XSS 注入點、遷移需 CSRF 恢復屬破壞性）、`CORS_ALLOWED_ORIGINS` 收斂理由與 prod 空白預設拒絕策略，驗證 ADR 標題與 `docs/adr/README.md` 索引一致
- [x] 5.3 更新 `docs/ARCHITECTURE.md` §認證授權（CORS 白名單、限流、安全標頭）與 `docs/DEPLOY.md` §環境變數與監控（`CORS_ALLOWED_ORIGINS`、`app.rate-limit.*`、`app.security-headers.*` 範例與 `curl` 驗證指引），驗證 `grep -rn CORS_ALLOWED_ORIGINS docs` 與 `application.yaml` 一致且 `typst compile docs/manual.typ /tmp/manual.pdf` 仍 exit 0（若提及部署步驟）

## 6. 驗證與回歸

- [x] 6.1 執行 `cd backend && mvn test` 全量回歸，驗證既有 `CaseControllerTest` 等不受 `test` 關閉限流影響且新增測試全綠
- [x] 6.2 執行 `openspec validate --specs --changes --strict` 驗證 0 error，`openspec status --change security-review-phase2` 顯示四件 artifacts 皆 done，且 `logs/` 不進版控（已 `.gitignore`）
- [x] 6.3 手動 `prod` 啟動驗收：`CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com mvn spring-boot:run -Dspring-boot.run.profiles=prod` 後 `curl -i -H "Origin: https://evil.com" http://localhost:8080/api/cases` 無 `Allow-Origin`、`curl -i -H "Origin: https://app.example.com" http://localhost:8080/api/cases` 有 `Allow-Origin`、`curl -i http://localhost:8080/api/cases` 有 `CSP/HSTS/nosniff/DENY`，且 `POST /api/auth/login` 連 11 次第 11 次 429
