## 1. 安全加固

- [x] 1.1 於 `DataInitializer` 增 `@Profile("dev|test")` 並使 `app.bootstrap.*` 可 env 覆寫，驗證 `prod` 啟動不建預設帳號
- [x] 1.2 於 `GlobalExceptionHandler` 新增 `DisabledException` → 403 `ACCOUNT_DISABLED`，驗證停用帳號登入回 403
- [x] 1.3 於 `SecurityConfig` 改 `BCryptPasswordEncoder(12)`，驗證既有 hash 仍可登入且新註冊成本為 12
- [x] 1.4 於 `JwtTokenProvider` 增 `issuer("phytotrack")` 與驗證，驗證無 issuer token 被拒
- [x] 1.5 非 dev 關閉 `springdoc`，於 `RequestIdFilter` 限 64 與過濾，驗證 `prod` 下 `/v3/api-docs` 不可達且長 `X-Request-Id` 被截斷
- [x] 1.6 執行 `mvn test` 與 `openspec validate --specs --changes` 通過
