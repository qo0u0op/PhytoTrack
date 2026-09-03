## MODIFIED Requirements

### Requirement: 登入與註冊速率限制

系統 SHALL 對公開認證端點施加每 IP 速率限制：`POST /api/auth/login`、`POST /api/auth/register` 與 `POST /api/auth/abandon-deactivate` 共享同一限制，預設每 IP 每 60 秒最多 10 次；超限時 SHALL 回 `429 Too Many Requests`，回應 SHALL 含 `Retry-After`（秒）與統一錯誤形狀 `error.code=RATE_LIMITED`、`requestId`，且 SHALL 寫入含 `requestId` 的警告日誌；限制 SHALL 以固定視窗（或滑動視窗等價）與記憶體儲存實作，單機 LAN 部署無外部依賴；`test` profile SHALL 可透過設定關閉或放寬限制以免影響自動化測試。

#### Scenario: 暴力嘗試受限
- **WHEN** 同一 IP 於 60 秒內連續 11 次呼叫 `POST /api/auth/login`
- **THEN** 第 11 次回 `429`，body 含 `error.code=RATE_LIMITED` 與 `requestId`，header 含 `Retry-After`

#### Scenario: 註冊同樣受限
- **WHEN** 同一 IP 於 60 秒內連續 11 次呼叫 `POST /api/auth/register`
- **THEN** 第 11 次回 `429` 且附 `Retry-After`

#### Scenario: 混合計數
- **WHEN** 同一 IP 於 60 秒內合計呼叫 `login` 5 次與 `register` 6 次
- **THEN** 第 11 次（無論哪個端點）回 `429`，因三端點共享同一 IP 桶

#### Scenario: 不同 IP 獨立計數
- **WHEN** IP A 已達 10 次上限，IP B 呼叫 `POST /api/auth/login`
- **THEN** IP B 的請求仍回正常狀態（200 或 401 依憑證），不受 A 限制影響

#### Scenario: 視窗重置後恢復
- **WHEN** 超限後等待 `Retry-After` 秒數（視窗重置）
- **THEN** 後續請求不再回 `429`，恢復正常處理

#### Scenario: 測試環境可放寬
- **WHEN** 以 `test` profile 啟動且 `app.rate-limit.enabled=false`（或等價放寬設定）
- **THEN** 同一 IP 連續 11 次呼叫 `POST /api/auth/login` 仍依業務邏輯回 200/401，不回 429

### Requirement: Token 儲存與 CORS 強化

系統 SHALL 將 CORS 由 `*` 收斂為 env 白名單，並於非 dev 環境啟用 `Content-Security-Policy` 與 `Strict-Transport-Security`；前端 token 儲存 SHALL 維持 `localStorage`（不遷移至 `httpOnly` cookie，本次僅文件化決策），`CSRF` 保持關閉與無狀態登出不變。

CORS SHALL 由 `app.cors.allowed-origins`（env `CORS_ALLOWED_ORIGINS`，逗號分隔）驅動：`dev` profile 若未配置 SHALL 沿用 `*` 以相容 `http://localhost:5173`；`prod` profile 若未配置 SHALL 預設拒絕跨源（不回 `Access-Control-Allow-Origin`）；僅白名單 Origin 得回對應 `Access-Control-Allow-Origin` 與 `Vary: Origin`，方法限 `GET/POST/PUT/PATCH/DELETE/OPTIONS`、標頭限 `*`、暴露標頭為 `Authorization,Content-Disposition,X-Request-Id`。

安全標頭 SHALL 於非 dev（`prod` 或 `app.security-headers.enabled=true`）對所有回應注入：`Content-Security-Policy`（兼顧 Swagger：`default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'` 等）、`Strict-Transport-Security: max-age=31536000; includeSubDomains`、`X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`；`dev` 不強制。

#### Scenario: CORS 白名單
- **WHEN** `CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com` 且請求 `Origin: https://app.example.com` 呼叫 `GET /api/cases`
- **THEN** 回應含 `Access-Control-Allow-Origin: https://app.example.com` 與 `Vary: Origin`

#### Scenario: CORS 非白名單拒絕
- **WHEN** 同上白名單下請求 `Origin: https://evil.com` 呼叫 `GET /api/cases`
- **THEN** 回應不含 `Access-Control-Allow-Origin`（瀏覽器阻擋），或後端回 `403`，不洩白名單內容

#### Scenario: prod 未配置預設拒絕
- **WHEN** 以 `prod` 啟動且未設定 `CORS_ALLOWED_ORIGINS`
- **THEN** 任意跨源 `Origin` 呼叫 `/api/**` 皆不回 `Access-Control-Allow-Origin`

#### Scenario: dev 未配置沿用通配
- **WHEN** 以 `dev` 啟動且未設定 `CORS_ALLOWED_ORIGINS`
- **THEN** 任意 `Origin` 呼叫 `/api/**` 仍回 `Access-Control-Allow-Origin: *` 或對應 Origin（依實作），保持本地開發相容

#### Scenario: CORS 預檢請求
- **WHEN** 瀏覽器發送 `OPTIONS /api/auth/login` 並帶 `Origin: https://app.example.com`、`Access-Control-Request-Method: POST`
- **THEN** 若 Origin 在白名單，回 `200` 且含 `Access-Control-Allow-Methods` 與 `Access-Control-Allow-Origin`；否則不含 `Allow-Origin`

#### Scenario: CSP/HSTS 標頭
- **WHEN** 以 `prod` 取得任意 API 回應（例如 `GET /api/cases`）
- **THEN** 回應含 `Content-Security-Policy`（含 `default-src 'self'` 與 `style-src 'self' 'unsafe-inline'` 以相容 Swagger）、`Strict-Transport-Security: max-age=31536000; includeSubDomains`、`X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`

#### Scenario: CSP/HSTS 於 dev 不強制
- **WHEN** 以 `dev` 取得任意 API 回應
- **THEN** 允許不含 `Strict-Transport-Security`（避免本地 http 誤導），`Content-Security-Policy` 可選

#### Scenario: Token 儲存維持現狀
- **WHEN** 檢視前端 `stores/auth.ts` 與 `api/http.ts` 的 token 儲存實作
- **THEN** 仍使用 `localStorage.getItem('token')` / `setItem`，不改為 `httpOnly` cookie，且文件（ADR/ARCHITECTURE）已記錄維持理由與未來遷移條件（需伴隨 CSRF 恢復）
