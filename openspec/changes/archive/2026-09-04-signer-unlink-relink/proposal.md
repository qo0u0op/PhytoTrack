## Why

現行降權（STAFF/ADMIN → VIEWER）完全不處理簽名人：被降權者的簽名人仍掛「使用者」標籤，與其現況不符。停用雖連動停用簽名人但保留 `user_id` 連結，重啟或升權後能否回到原筆全憑 `displayName` 巧合，且降權情境會因連結尚在而被 `ensureForUser` 視為已有。本 change 明確解綁／恢復語意：降權或停用即解綁為非使用者簽名人並保留 `id`；升權或重新啟用即恢復原筆連結與可見，不再新建。

## What Changes

- `AuthService.updateRole` 降權至 `VIEWER` 時：將其名下 `active` 的 `user as signer` 解綁（`user_id = null`，`active` 維持 `true`，`id` 不變），轉為非使用者簽名人並繼續顯示於新案件候選。
- `AuthService.updateActive(false)` 停用時：除既有連動停用外，一併解綁（`user_id = null`）；停用簽名人維持現行可見性（新案件候選隱藏，管理頁可見）。
- `AuthService.updateRole` 升權至 `STAFF|ADMIN`、`updateActive(true)` 重新啟用時：優先恢復原筆（曾屬於該使用者、同名非使用中籤名人的 `id` 最小者，重新連結並 `active=true`），而非新建；無原筆才走既有 `ensureForUser` 新建／啟用流程。
- `IdentifierService.ensureForUser` 擴充：無連結 active 時，除既有同名 inactive 連結筆外，亦搜尋同名 `user IS NULL` 的 inactive 舊筆（正規化比對，`id` 最小優先），命中則重新連結並啟用，避免重複建立。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `user-admin`: 降權／停用解綁簽名人、升權／啟用恢復原筆
- `reference-data-admin`: `ensureForUser` 重鏈同名未綁定舊筆

## Impact

- 後端：`AuthService.updateRole/updateActive`（解綁／恢復）、`IdentifierService.ensureForUser`（重鏈）；無 schema 遷移（`user_id` nullable 已支援）。
- 前端：無需更動（身分別徽章與候選清單依既有 `userId`／`active` 渲染）。
- 相容性：歷史案件仍以 `id` 顯示原名；解綁後的非使用者簽名人仍可被既有案件引用與刪除保護。
