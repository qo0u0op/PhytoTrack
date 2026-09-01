# API Observability Specification

## Purpose

統一錯誤回應契約並提供伺服器端可追蹤性 (requestId 串接日誌)，滿足除錯與監控需求。

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