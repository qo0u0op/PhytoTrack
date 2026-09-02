## Why

`security-hardening` Phase 0 已交付 OSIV/轉義/JWT fail-fast，但 `docs/REQUIREMENTS.md` 待辦仍列 6 項可即修與部署層加固（`DataInitializer` 預設密碼於 production 仍建立、`DisabledException` 500、BCrypt 10、JWT 無 issuer、Swagger 全環境暴露、`RequestId` 可控）；需於 Phase 2 補齊以降低帳號與日誌風險。

## What Changes

- 帳號初始化僅於 `dev|test` profile 建立（`@Profile`），production 無預設 `admin/staff/viewer`，`app.bootstrap.*` 可 env 覆寫。
- 停用帳號登入由 `DisabledException` → 403 `ACCOUNT_DISABLED`，避免落通用 500。
- `SecurityConfig` `BCryptPasswordEncoder(12)`（既有 hash 相容）。
- `JwtTokenProvider` 增 `issuer("phytotrack")` 與驗證。
- 非 dev 關閉 `springdoc.api-docs/swagger-ui`。
- `RequestIdFilter` 限長度 ≤64 並過濾不可列印字元，避免 header 污染日誌。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `security-hardening`: 擴充帳號初始化、停用處理、BCrypt/JWT/Swagger/RequestId 加固

## Impact

- 後端：`DataInitializer`、`GlobalExceptionHandler`、`SecurityConfig`、`JwtTokenProvider`、`RequestIdFilter`、`application.yaml`；不改 API 契約，僅提升強度與日誌安全。
- 測試：新增對應例外與配置測試；不影響既有 `mvn test`。
