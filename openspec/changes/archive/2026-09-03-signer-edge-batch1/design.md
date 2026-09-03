## Context

見 `proposal.md - Why`。`CaseService` 僅驗存在性、`ensureForUser` 只查自身、帳號狀態與簽名人狀態脫鉤、`get(0)` 無排序。

## Goals / Non-Goals

**Goals:**
- 新建擋 `inactive`、歷史放行；自動帶入查全域；帳號異動連動；首筆 `ORDER BY identifierId ASC`。

**Non-Goals:**
- DB 唯一約束與正規化比對（第二批）；案件表單顯示改造（第二批）。

## Decisions

- `CaseService.create` 內對 `identifierIds` 逐筆查 `active`，`false` 即拋 `409 SIGNER_INACTIVE`；`update` 保留原語意（`null` 不動、`[]` 帶入），`[]` 帶入結果必為 `active`。
- `IdentifierService.ensureForUser` 新建前查 `findByIdentifierAndActiveTrue`，撞他人即拋 `DISPLAY_NAME_EXISTS`（呼叫方轉綁定確認）。
- `AuthService.updateActive` / 降級處呼叫 `deactivateUserSigners(userId)` 將其 `active` 全置 `false`。
- `findByUserUserIdAndActiveTrue` 改為 `...OrderByIdentifierIdAsc`，呼叫方首筆即確定。

## Risks / Trade-offs

- 新建擋 `inactive` 使舊前端重送含停用 id 的草稿失敗 → 以 409 明示，前端需引導重選。
- 連動停用後該使用者 `me` 端點觸發重建 → 需配合「停用重建循環」在第二批改為提示啟用舊筆。
