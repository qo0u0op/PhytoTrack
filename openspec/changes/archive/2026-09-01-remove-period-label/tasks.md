## 1. 版式清理

- [x] 1.1 移除 `frontend/src/views/DashboardView.vue:102` 的 `<div class="small text-muted">期間：...</div>`，並驗證 `npm run build` 通過且期別選擇器區域不再顯示期間小字
- [x] 1.2 確認 `frontend/src/views/DashboardView.vue:139` 已為 `模型狀態`（`6cacd3b` 已完成），若未套用則補改，並驗證標題顯示正確
- [x] 1.3 將 `frontend/src/views/DashboardView.vue` 的百分比計算改為一位小數（`toFixed(1)` 四捨五入，例如 `27% → 27.1%`），涵蓋 `percent` 函式與表格 `{{ percent(...) }}%` 顯示，並驗證 `npm run build` 通過且各百分比為一位小數

## 2. 驗收

- [x] 2.1 執行 `npm run build` 與 `openspec validate --specs --changes` 通過
