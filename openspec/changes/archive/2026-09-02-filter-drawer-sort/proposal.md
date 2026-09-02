## Why

案件與送件人管理的篩選卡片常駐佔版面，且表格僅支援固定排序，無法依欄位彈性檢視，影響窄螢幕與大量資料時的可用性。

## What Changes

- 案件管理（`CasesView`）與送件人管理（`SendersView`）的篩選卡片改為抽屜式：預設收合隱藏，點擊「篩選」按鈕展開/收合（`v-if` + `collapse`），展開時顯示原有四欄/多欄篩選控制。
- 表格排序：案件與送件人表格的所有欄位（除 `操作` 外）支援點擊表頭依 `asc`/`desc` 切換排序，前端本地排序（基於當前頁面或全量篩選後結果），預設依 `收件日期` 或 `ID` 降冪；排序狀態以表頭箭頭 `↑/↓` 指示。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `case-search`: 案件管理篩選抽屜與表格排序。
- `sender-management`: 送件人管理篩選抽屜與表格排序。

## Impact

- 前端：`frontend/src/views/CasesView.vue`、`frontend/src/views/SendersView.vue` 篩選卡片與表格表頭排序邏輯。
- 後端：無（前端本地排序與篩選）。
