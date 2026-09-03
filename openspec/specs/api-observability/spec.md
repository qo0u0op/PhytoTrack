# API Observability Specification

## Purpose

統一錯誤回應契約並提供伺服器端可追蹤性 (requestId 串接日誌)，滿足除錯與監控需求，並為前端全域錯誤處理提供穩定依據。

## Requirements

### Requirement: 錯誤回應含 details 欄位

所有錯誤回應 SHALL 使用統一形狀：`error.code`、`error.message`、`error.details` 與 `requestId`；`details` 在無補充資訊時可為空。

#### Scenario: 參數驗證失敗
- **WHEN** 請求體通過 Bean Validation 失敗
- **THEN** 回應含 `error.code=VALIDATION_ERROR`、`message` 與列出各欄位錯誤的 `details`

### Requirement: requestId 寫入伺服器日誌

系統 SHALL 為每個錯誤回應產生 requestId，並將相同 requestId 寫入伺服器日誌 (含業務錯誤與系統錯誤)。

#### Scenario: 4xx 業務錯誤
- **WHEN** 服務層拋出業務例外
- **THEN** 伺服器日誌記錄與回應相同的 requestId 與例外訊息

#### Scenario: 5xx 系統錯誤
- **WHEN** 發生未預期例外
- **THEN** 回應為泛化訊息，伺服器日誌以同一 requestId 記錄完整堆疊

### Requirement: Actuator 健康與指標端點

系統 SHALL 暴露 Actuator 精簡端點：`GET /actuator/health`（含應用與資料庫狀態）、`GET /actuator/info` 與 `GET /actuator/metrics`（JVM/HTTP 指標），非 dev 環境下僅允許內網或 `ROLE_ADMIN` 存取，敏感端點（`env`/`beans`）不得暴露。

#### Scenario: 健康檢查
- **WHEN** 呼叫 `GET /actuator/health`
- **THEN** 回傳 `{"status":"UP"}` 且包含 `db` 與 `llama` 組件狀態

#### Scenario: 指標查詢
- **WHEN** 呼叫 `GET /actuator/metrics/http.server.requests`
- **THEN** 回傳 HTTP 指標且僅 ADMIN 或內網可存取

### Requirement: 日誌滾動與保留策略

系統 SHALL 以 logback 依日與大小滾動寫入 `logs/phytotrack-%d{yyyy-MM-dd}.%i.log.gz`，單檔上限 10MB、保留 30 日、總量 500MB，格式包含 `timestamp`、`level`、`requestId`、`thread` 與 `message`，並於 `application.yaml` 可調整。

#### Scenario: 日誌滾動
- **WHEN** 日誌達 10MB 或跨日
- **THEN** 自動滾動為新檔並壓縮舊檔，舊檔依保留策略清理

#### Scenario: requestId 於日誌可追蹤
- **WHEN** 產生錯誤回應
- **THEN** 同一 `requestId` 出現於對應日誌行
