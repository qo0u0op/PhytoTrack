## Context

見 `proposal.md` Why。`security-hardening` 僅交付 3 項，`docs/REQUIREMENTS.md` 待辦 A/C 已列 6 項可即修，未改 API 契約，適合 Phase 2 補齊。

## Goals / Non-Goals

**Goals:**
- 使帳號初始化、停用處理、BCrypt/JWT/Swagger/RequestId 符合 OWASP 建議。

**Non-Goals:**
- 不引入 rate limiting、httpOnly cookie 等需大改流程項目（留 ops/security 後續）。

## Decisions

- **Profile 限縮**：`DataInitializer` 加 `@Profile("dev|test")`，`app.bootstrap` 改可 env 覆寫。替代：保留 production 預設但隨機密碼，增加維運負擔。
- **DisabledException**：`GlobalExceptionHandler` 增 `DisabledException` → 403。替代：沿用 500 但誤導為系統錯誤。
- **BCrypt 12**：`new BCryptPasswordEncoder(12)`，既有 10 雜湊仍可驗證。替代：不升級則低於建議。
- **JWT issuer**：`JwtTokenProvider` 簽發與驗證 `issuer`。替代：不校驗則 issuer 無意義。
- **Swagger**：`application.yaml` 非 dev `springdoc.api-docs.enabled=false`。替代：全開放暴露資訊。
- **RequestId**：`RequestIdFilter` 截斷 64 並 `replaceAll("[^\\x20-\\x7E]","")`。替代：原樣回寫易日誌注入。

## Risks / Trade-offs

- [初始化] production 無預設帳號需手動建首帳 → 文件說明。
- [BCrypt 12] 成本增加約 2 倍 → 註冊/登入仍可接受。

## Migration Plan

1. 依序修改上述 6 項，加單元測試。
2. `openspec validate` 與 `mvn test` 通過。

## Open Questions

- 無。
