## Why

所有管理頁（案件、送件人、作物、害物、參照資料、使用者）的表格在寬度不足時換行，導致版面錯亂與可讀性差，需改為左右滾動且部分欄位固定寬度。

## What Changes

- 所有管理表格的 `table-responsive` 容器確保左右可滾動（`overflow-x: auto`，必要時 `scrollbar-width` 與 `-webkit-scrollbar` 可見），避免寬度不足時換行。
- 固定部分欄位寬度（例如 ID 60px、操作列 120px、名稱/作物 150px、身分別/分類 100px），以 `style="min-width"` 或 `table-layout: fixed` 防止擠壓。
- 不改資料與 API，僅前端呈現。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- 無（純呈現調整，無 spec 行為變更）

## Impact

- 前端：`frontend/src/views/CasesView.vue`、`SendersView.vue`、`CropManagementView.vue`、`PestManagementView.vue`、`ReferenceDataAdminView.vue`、`UsersView.vue` 等管理頁表格與樣式。
- 後端：無。
