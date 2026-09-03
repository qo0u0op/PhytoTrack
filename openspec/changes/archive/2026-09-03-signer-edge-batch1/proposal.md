## Why

簽名人停用後仍可被新建案件直接以 id 引用，且自動帶入、停用連動與首筆選取語意不確定，導致停用形同虛設與責任歸屬漂移。需先收斂第一批高風險邊界。

## What Changes

- 新建案件時拒絕 `active=false` 的 `identifierIds`（歷史案件更新時放行以保留顯示）。
- `ensureForUser` 自動帶入前先查全域 `active` 同名；撞非使用者簽名人時走綁定或報 `DISPLAY_NAME_EXISTS`，不靜默重複。
- 使用者停用帳號或降級出 STAFF/ADMIN 時，連動將其 `user as signer` 置 `active=false`（歷史引用保留）。
- 同使用者多筆 `active` 時取首筆改為確定性排序（`ORDER BY identifierId ASC`），並收斂同步邏輯只動首筆。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `reference-data-admin`: 新增建案引用 active 檢查、自動帶入全域查重、停用連動、首筆確定性排序。
- `case-lifecycle`: 新建案件拒 inactive 簽名人。

## Impact

- 後端：`service/CaseService.java`、`service/IdentifierService.java`、`service/AccountService.java`、`service/AuthService.java`、`repository/IdentifierRepository.java`。
- 前端：`CaseFormView.vue` 錯誤提示沿用 `DISPLAY_NAME_EXISTS`。
- 資料：無遷移，僅行為收斂。
