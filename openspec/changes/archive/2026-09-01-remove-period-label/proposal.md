## Why

Dashboard 期別選擇器右側的 `期間：xxx` 小字與期別下拉重複，且先前 `AI 連線情況 → 模型狀態` 文案調整已於 `6cacd3b` 完成，兩者合併為單次版式清理可減少變更噪音。

## What Changes

- 已完成（`6cacd3b`）：`frontend/src/views/DashboardView.vue:139` `AI 連線情況 → 模型狀態`。
- 本變更：移除 `frontend/src/views/DashboardView.vue:102` 的 `<div class="small text-muted">期間：{{ periodLabel(...) }} ...</div>`（期間中文顯示已由下拉與 `期別案件數` 涵蓋，無需重複）。
- Dashboard 百分比：`frontend/src/views/DashboardView.vue` 的 `percent` 與表格佔比以 `toFixed(1)` 四捨五入至小數後一位（例如 `27% → 27.1%`），涵蓋狀態比例、breakdown（作物類別/害物/防治建議/交付/耕種）與期別相關佔比。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `case-statistics`: Dashboard 版式（移除期間小字，模型狀態標題已於前置 chore 完成）。

## Impact

- 前端：`frontend/src/views/DashboardView.vue:102,139`。
- 後端：無。
