# Security Hardening Specification

## Purpose

消除已知的網頁安全缺口：AI 輸出注入、OSIV 開啟、JWT 開發預設密鑰落入正式環境。

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