# Security Review Pending Specification

## Purpose

彙整 2026-08-20 OWASP 安全審查待辦（B/C 組），作為後續加固的追蹤清單，與 `security-hardening` 已交付項目區隔。

## Requirements

### Requirement: 登入與註冊速率限制

系統 SHALL 對 `/api/auth/login` 與 `/api/auth/register` 施加請求速率限制（例每 IP 每分鐘 10 次），超過時回 429 並附 `Retry-After`。

#### Scenario: 暴力嘗試受限
- **WHEN** 同一 IP 於 1 分鐘內連續 11 次呼叫 `/api/auth/login`
- **THEN** 第 11 次回 429

### Requirement: Token 儲存與 CORS 強化

系統 SHALL 將前端 token 儲存遷移評估（`localStorage` → `httpOnly` cookie 需伴隨 CSRF 恢復），並將 CORS 由 `*` 收斂為 env 白名單；`Content-Security-Policy` 與 `Strict-Transport-Security` SHALL 於非 dev 啟用（注意 Swagger inline style 相容）。

#### Scenario: CORS 白名單
- **WHEN** 非白名單 Origin 呼叫 API
- **THEN** 回應不含 `Access-Control-Allow-Origin` 或為 403

#### Scenario: CSP/HSTS 標頭
- **WHEN** 以 `prod` 取得任意 API 回應
- **THEN** 回應含 `Content-Security-Policy` 與 `Strict-Transport-Security`

### Requirement: 維持現狀項目

下列項目 SHALL 維持現狀不另行處置：`CSRF` 關閉（Bearer 無 cookie 面）、無狀態登出、登入錯誤訊息統一、`HS256` 固定 HMAC、`JWT_SECRET` fail-fast、500 泛化與 `npm audit` 0 漏洞。

#### Scenario: 維持現狀
- **WHEN** 檢視上述設定
- **THEN** 保持現行實作不變
