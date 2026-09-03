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

非 dev profile 啟動時若仍使用開發預設密鑰，應用程式 SHALL 於啟動階段失敗並提示需提供正式密鑰。

#### Scenario: 以預設密鑰啟動非 dev 環境
- **WHEN** 以 production profile 啟動且未提供 JWT_SECRET
- **THEN** 應用程式啟動失敗，並明確提示設定密鑰

### Requirement: 帳號初始化限 dev/test

系統 SHALL 僅於 `dev` 或 `test` profile 建立 `app.bootstrap.*` 預設帳號，`production` 不自動建立任何帳號。

#### Scenario: production 無預設帳號
- **WHEN** 以 `prod` 啟動且無手動帳號
- **THEN** `admin` 帳號不存在，需由外部初始化

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
