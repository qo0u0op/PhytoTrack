# Security Hardening Specification

## Purpose

消除已知的網頁安全缺口：AI 輸出注入、OSIV 開啟、JWT 開發預設密鑰落入正式環境，確保正式環境部署符合基本安全基線。

## Requirements

### Requirement: AI 診斷建議輸出安全渲染

前端 SHALL 將 AI 診斷建議視為不可信內容，任何渲染皆 SHALL 轉義 HTML 標籤，不得將模型輸出當作 HTML 執行。

#### Scenario: 模型輸出含 HTML
- **WHEN** AI 建議內容包含 `<img onerror=...>` 等 HTML
- **THEN** 前端以純文字顯示該內容，且不執行任何標籤

### Requirement: 關閉 OSIV (Open Session in View)

後端 SHALL 關閉 open-in-view，任何 API 回應不得因自動保持資料庫 session 而依賴交易外的延遲載入。

#### Scenario: 交易外序列化
- **WHEN** API 於交易外回應含 Lazy 關聯的資料
- **THEN** 系統不自動開啟資料庫 session，回應內容由 DTO 承載且行為穩定

### Requirement: JWT 密鑰 fail-fast

非 dev profile 啟動時若仍使用開發預設密鑰，應用程式 SHALL 於啟動階段失敗並提示需於 `phytotrack.toml` 的 `app.jwt.secret` 提供正式密鑰（不再經由環境變數 `JWT_SECRET`）。

#### Scenario: 以預設密鑰啟動非 dev 環境
- **WHEN** 以 production profile 啟動且 `phytotrack.toml` 的 `app.jwt.secret` 仍為開發預設
- **THEN** 應用程式啟動失敗，並明確提示於 `phytotrack.toml` 設定正式密鑰

### Requirement: 帳號初始化

系統 SHALL 依 profile 決定初始帳號：`dev`/`test` 建立 `admin`/`staff`/`viewer` 三帳號，`prod`（含 binary）僅建立 `admin` 單一帳號與其簽名人；`staff`/`viewer` 於 prod 不自動建立。`prod` 首次啟動資料庫為空時 SHALL 仍可使用內建預設 `admin/admin123` 建號並以 `BCrypt(12)` 儲存（行為不變），但該預設帳密 SHALL 不以可配置項出現在任何 `phytotrack.toml` 設定檔（僅以註釋提醒）。

#### Scenario: dev/test 三帳號
- **WHEN** 以 `dev` 或 `test` 啟動且無既有帳號
- **THEN** `admin`、`staff`、`viewer` 皆自動建立

#### Scenario: production 僅 admin
- **WHEN** 以 `prod` 啟動且無既有帳號
- **THEN** 僅 `admin` 自動建立（`staff`/`viewer` 不建立），且僅 `admin` 擁有簽名人

### Requirement: 停用帳號登入處理

停用帳號登入 SHALL 回 403 `ACCOUNT_DISABLED`，不落 500。

#### Scenario: 停用帳號登入
- **WHEN** 已停用使用者以正確密碼登入
- **THEN** 回 403 且 `error.code=ACCOUNT_DISABLED`

### Requirement: 密碼強度與 JWT 簽章

密碼編碼 SHALL 使用 `BCrypt(12)`；JWT SHALL 包含 `issuer=phytotrack` 並於驗證時校驗。

#### Scenario: BCrypt 成本
- **WHEN** 建立使用者
- **THEN** 雜湊成本為 12

#### Scenario: JWT issuer
- **WHEN** 驗證 token
- **THEN** 需 `issuer` 為 `phytotrack` 否則拒絕

### Requirement: 文件與請求識別安全

非 dev 環境 SHALL 關閉 `springdoc`，`RequestId` 需限長度 ≤64 且過濾不可列印字元。

#### Scenario: 非 dev 文件關閉
- **WHEN** 以 `prod` 取得 `/v3/api-docs`
- **THEN** 回 404 或 403

#### Scenario: RequestId 過濾
- **WHEN** 客戶端帶入過長或含控制字元的 `X-Request-Id`
- **THEN** 伺服器以過濾後值寫入日誌與回應 header

### Requirement: 輸入消毒阻擋 Stored XSS

系統 SHALL 於所有 `displayName/name/address` 寫入路徑阻擋 HTML 標籤字元：DTO 層以 `@Pattern(regexp="^[^<>]*$")` 驗證，Service 層以 `InputSanitizer.assertNoHtml` 二次檢查；含 `<` 或 `>` 的請求 SHALL 回 `400 VALIDATION_ERROR` 且不持久化。

#### Scenario: 個人檔案顯示名稱含 script
- **WHEN** 已登入使用者以 `PUT /api/account/profile` 送 `{"displayName":"<script>alert(1)</script>"}`
- **THEN** 回 `400` 且 `error.code=VALIDATION_ERROR`、`details.displayName` 含「不可包含 < 或 >」，資料庫 `users.display_name` 不變更

#### Scenario: 註冊顯示名稱含 HTML
- **WHEN** 以 `POST /api/auth/register` 送 `{"displayName":"<img onerror=...>"}` 
- **THEN** 回 `400 VALIDATION_ERROR`，不建立帳號

#### Scenario: 送件人姓名或地址含 HTML
- **WHEN** 以 `POST /api/senders` 送 `{"name":"<b>test</b>"}` 或 `{"address":"<svg>"}`
- **THEN** 回 `400 VALIDATION_ERROR` 對應欄位

#### Scenario: 正常中文名稱通過
- **WHEN** 以 `PUT /api/account/profile` 送 `{"displayName":"診斷員"}`
- **THEN** 回 `200` 且持久化為該值

#### Scenario: 案件內聯送件人名稱含 HTML
- **WHEN** 以 `POST /api/cases` 送 `{"senderDisplayName":"<script>alert(1)</script>"}` 或 `{"senderName":"<img onerror=...>"}`
- **THEN** 回 `400` 且 `error.code=VALIDATION_ERROR`，`details.senderDisplayName` 或 `details.senderName` 含「不可包含 < 或 >」，不建立 `senders` 亦不建立 `cases`

#### Scenario: 案件更新內聯送件人地址含 HTML
- **WHEN** 以 `PUT /api/cases/{id}` 送 `{"senderAddress":"<svg>"}`
- **THEN** 回 `400 VALIDATION_ERROR`，原案件與送件人不變更

#### Scenario: Service 層直調亦阻擋
- **WHEN** 以 Service 直調 `findOrCreateSender` 傳 `displayName="<b>"`
- **THEN** 拋 `ApiException VALIDATION_ERROR` 且 `details` 含對應欄位（防 `@Valid` 旁路）

### Requirement: 生產環境預設帳號覆寫

生產部署 SHALL 於 `phytotrack.toml` 的 `app.bootstrap` 與 `app.jwt.secret` 覆寫開發預設，不再經由環境變數 `ADMIN_USERNAME/ADMIN_PASSWORD/JWT_SECRET` 覆寫；啟動期若仍為預設密鑰 SHALL fail-fast。

#### Scenario: 生產以環境變數覆寫
- **WHEN** 於 `phytotrack.toml` 設定 `app.jwt.secret` 與 `app.bootstrap.admin-username` 等
- **THEN** 預設 `admin:admin123` 與開發密鑰不再生效，新設定值方可登入與簽章

### Requirement: 生產文件端點關閉

`springdoc` 文件端點 SHALL 於非 dev 環境關閉：`prod` profile 下 `springdoc.api-docs.enabled=false` 與 `swagger-ui.enabled=false`，`GET /v3/api-docs` 與 `/swagger-ui.html` 不暴露；`dev` 保留 `permitAll` 便利。

#### Scenario: prod 文件關閉
- **WHEN** 以 `prod` 取得 `GET /v3/api-docs`
- **THEN** 回 `404` 或 `403`，不洩露完整 OpenAPI

#### Scenario: dev 文件可訪問
- **WHEN** 以 `dev` 取得 `GET /v3/api-docs`
- **THEN** 回 `200` 且含完整規格（供開發檢視）

### Requirement: 記住我雙時效 JWT

系統 SHALL 支援 `rememberMe` 分支的 JWT 時效：`POST /api/auth/login` 帶 `rememberMe=true` 時簽發長效 token（`app.jwt.remember-me-expiration-ms`，預設 7 天 = 604800000 ms），未帶或 `false` 時維持 `app.jwt.expiration-ms`（預設 1 小時 = 3600000 ms）。兩時效 SHALL 皆符合 `issuer=phytotrack` 簽章與 `BCrypt(12)` 密碼校驗既有約束，長效時間 MUST 可由 `JWT_REMEMBER_ME_EXPIRATION_MS` / `phytotrack.toml` 的 `app.jwt.remember-me-expiration-ms` 覆蓋。

#### Scenario: 勾選記住我取得長效 token
- **WHEN** 使用者以正確帳密與 `{"rememberMe": true}` 呼叫 `POST /api/auth/login`
- **THEN** 回 `200` 且 `token` 的 `exp - iat` 約為 7 天（容差 ±60s），且 `GET /api/cases` 等受保護 API 於 6 天後仍可驗證通過

#### Scenario: 未勾選維持短效
- **WHEN** 使用者以 `{"rememberMe": false}` 或省略該欄位呼叫 `POST /api/auth/login`
- **THEN** `token` 的 `exp - iat` 約為 1 小時，且 1 小時後驗證失敗（401）

#### Scenario: 長效可由設定覆蓋
- **WHEN** 以 `app.jwt.remember-me-expiration-ms=259200000`（3 天）啟動並以 `rememberMe=true` 登入
- **THEN** `token` 時效為 3 天而非 7 天

#### Scenario: 錯誤帳密仍拒絕
- **WHEN** 以錯誤密碼與 `rememberMe=true` 呼叫登入
- **THEN** 回 `401 BAD_CREDENTIALS`，不簽發 token

### Requirement: 設定檔不得含帳密配置

`phytotrack.toml.example` 與首次自動生成的 `phytotrack.toml` SHALL 不包含任何 `app.bootstrap` 可配置項（`admin-username` / `*-password` 等皆不以有效配置行出現）；`[app.bootstrap]` 段落 SHALL 僅以註釋提醒預設帳密（admin/admin123 等由程式內建）與「首次登入後請立即修改」。`application.yaml` 的內建預設值可保留供後端內部使用，但 TOML 層 SHALL 不提供帳密配置行。既有 TOML 含舊帳密者 SHALL 仍可讀取（相容），但新生成檔 SHALL 不含任何有效帳密配置。

#### Scenario: 範例檔僅註釋提醒
- **WHEN** 檢視 `backend/phytotrack.toml.example`
- **THEN** `grep -E "^admin-username|^admin-password|^staff-username|^viewer-username" backend/phytotrack.toml.example` 無結果，且含「帳號密碼不可在設定檔配置」註釋

#### Scenario: 自動生成僅註釋
- **WHEN** 刪除既有 `phytotrack.toml` 後首次啟動
- **THEN** 生成檔含亂數 `app.jwt.secret` 但 `[app.bootstrap]` 段落不含任何有效 `*-username`/`*-password` 行（僅註釋）
