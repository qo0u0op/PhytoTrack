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

### Requirement: Actuator 健康檢查端點

系統 SHALL 透過 Spring Boot Actuator 暴露 `health` 與 `info` 端點，供監控探測與部署檢查使用；其他 Actuator 端點預設不暴露。

#### Scenario: 匿名探測健康狀態
- **WHEN** 未認證使用者呼叫 `GET /actuator/health`
- **THEN** 回應 200 且 body 含 `{"status":"UP"}`，不洩漏細節 (`showDetails=never`)

#### Scenario: 匿名探測資訊端點
- **WHEN** 未認證使用者呼叫 `GET /actuator/info`
- **THEN** 回應 200 (內容可為空物件)，不需認證

#### Scenario: 其他 Actuator 端點預設關閉
- **WHEN** 未認證使用者呼叫 `GET /actuator/env` 或 `GET /actuator/beans`
- **THEN** 回應 404 或 403，不暴露內部資訊

#### Scenario: 敏感端點需授權
- **WHEN** 已登入但非 ADMIN 使用者嘗試存取未暴露的 Actuator 端點 (若曾臨時開啟)
- **THEN** 系統拒絕存取 (403 或 404)，不洩漏配置

### Requirement: logback 滾動日誌含 requestId

系統 SHALL 以 `logback-spring.xml` 提供滾動檔案日誌，按日與大小滾動、保留 30 天、總量 1GB 上限，且每行日誌 SHALL 包含與錯誤回應相同的 `requestId` (由 `RequestIdFilter` 以 MDC 注入)。

#### Scenario: 日誌同時輸出至 console 與檔案
- **WHEN** 應用程式啟動且產生日誌
- **THEN** 日誌同時出現在 console 與 `logs/phytotrack.log`，檔案依 `logs/phytotrack.%d{yyyy-MM-dd}.%i.log` 滾動

#### Scenario: requestId 關聯查詢
- **WHEN** 發生 4xx 業務錯誤並回應 `requestId: abc-123`
- **THEN** 同一 `requestId` 以 ` [abc-123]` 形式出現在日誌行 (含 `%X{requestId}`)，可 `grep abc-123 logs/phytotrack.log` 追溯

#### Scenario: 滾動策略生效
- **WHEN** 單日日誌超過 10MB 或跨日
- **THEN** 產生新滾動檔，舊檔依 `TimeBasedRollingPolicy` 保留 30 天、總量不超過 1GB

#### Scenario: 日誌檔案不進版控
- **WHEN** 執行 `git status`
- **THEN** `logs/` 下的 `*.log` 不顯示為未追蹤檔案 (已於 `.gitignore` 忽略)

### Requirement: 錯誤狀態碼精確映射

系統 SHALL 將常見 Web 層例外映射為精確 HTTP 狀態碼，而非一律 `500 INTERNAL_ERROR`：`NoResourceFoundException→404 NOT_FOUND`、`HttpRequestMethodNotSupportedException→405 METHOD_NOT_ALLOWED`、`HttpMediaTypeNotSupportedException→415 UNSUPPORTED_MEDIA_TYPE`、`HttpMessageNotReadableException→400 VALIDATION_ERROR`；其他未預期例外仍回 `500` 泛化。

#### Scenario: 未定義路徑回 404
- **WHEN** 以 `GET /api/ref/districts`（不存在）請求
- **THEN** 回 `404` 且 `error.code=NOT_FOUND`，日誌記 `WARN 資源不存在`

#### Scenario: 方法不支援回 405
- **WHEN** 以 `GET /api/account/profile`（僅支援 PUT）請求
- **THEN** 回 `405` 且 `error.code=METHOD_NOT_ALLOWED`

#### Scenario: 空或畸形 JSON 回 400
- **WHEN** 以 `POST /api/auth/login` 送空 body 或 `Content-Type: application/octet-stream`
- **THEN** 回 `400 VALIDATION_ERROR` 或 `415 UNSUPPORTED_MEDIA_TYPE`，不落 500

#### Scenario: 未預期例外仍 500
- **WHEN** 發生未捕捉例外
- **THEN** 回 `500 INTERNAL_ERROR` 泛化訊息，日誌以 `requestId` 記錄堆疊
