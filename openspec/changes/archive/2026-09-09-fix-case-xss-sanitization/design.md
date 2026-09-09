## Context

前次 `pentest-hardening-2026-09-04` 已於 DTO+Service 雙層阻擋 `Account/Auth/Sender` 三路徑的 `< >`，但 `Case` 內聯送件人屬「案件建立時順手建立送件人」之捷徑，未被覆蓋。審查（Standards hard + Spec 缺漏）判定為**主路徑繞過**，需補齊以達成 spec「所有 `displayName/name/address` 寫入路徑」。

## Goals / Non-Goals

**Goals:**
- 使 `POST /api/cases` 與 `PUT /api/cases/{id}` 的內聯 `sender*` 欄位同受 `< >` 阻擋，錯誤形狀與既有 `SenderService` 一致。
- 保持 `InputSanitizer` 防禦深度，不僅依賴 `@Valid`。

**Non-Goals:**
- 不遷 `InputSanitizer` 至 `util` 套件（Judgement call，留待後續重構）。
- 不改 `AGENTS.md:60` 的 ADR 清單數字（Nit）。
- 不擴大 `deepsec.sarif`（已封存，後續掃描再補）。

## Decisions

- **DTO + Service 雙層**：與既有三路徑一致。DTO 層 `@Pattern` 產生 `details`（`GlobalExceptionHandler.handleValidation`），Service 層 `assertNoHtml` 防直接 `CaseService` 呼叫或 `@Valid` 被跳過；兩層文案統一為「...不可包含 < 或 >」。
- **Service 層 details 一致性**：現 `InputSanitizer` 拋 `details=null`，Spec L27 要求 `details.displayName` 含文案。解法為 `InputSanitizer.assertNoHtml` 改為 `new ApiException(..., Map.of(field, field+"不可包含 < 或 >"))`，或在 `CaseService` 捕獲後轉譯；擇前者以集中邏輯。屬次要偏離，優先修主路徑，次要可分次提交。
- **InlineSigner/InlineCrop**：簽名人與作物名稱亦為寫入路徑，但非 `security-hardening` 原列「`displayName/name/address`」之 `address` 範疇，列為評估項，若風險等價則一併加 `@Pattern`。

## Risks / Trade-offs

- [過度攔截] `senderAddress` 含 `A<B` 數學式亦擋 → 接受（地址不含 `< >`）。
- [重複規則漂移] 三處 DTO + 一處 Service 重複同一正則 → 接受，design 已明載為刻意。

## Migration Plan

1. 補 `CaseDtos` 註解 → 補 `CaseService` 兩處 `assertNoHtml` →（可選）調 `InputSanitizer` 含 `details` → 補測試 → `mvn test` + `openspec validate`。

## Open Questions

- 無。
