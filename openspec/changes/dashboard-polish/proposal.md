## Why

Dashboard 現有文案與版式有五處不一致與易錯：期間顯示英文枚舉、年份 `請選擇年份` 為空值導致後端 400、AI 卡片標題不符「連線情況」語意、近半年趨勢卡片位置與命名不直觀、底部導覽按鈕與頂部導覽重複且佔版面。

## What Changes

- 期間 `期間：xxx` 的 `HISTORICAL/ANNUAL/MONTHLY` 顯示改為中文 `歷史/年度/月度`（`DashboardView.vue:102`）。
- 年份選單移除 `請選擇年份` 空選項，預設選 `availableYears[0]`，年度/月度下年份為必填，避免 400。
- `AI 模型 (llama.cpp)` 改為 `AI 連線情況`（`DashboardView.vue:137`）。
- `近 6 月案件趨勢` 改名 `近半年案件趨勢`，卡片移至耕種方式卡片右側（與 `deliveryBreakdown/methodBreakdown` 同列，三欄改兩欄+趨勢）。
- 移除底部 `建立新診斷案件/案件管理/使用者管理` 三按鈕列（`DashboardView.vue:364`）。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `case-statistics`: Dashboard 呈現文案與版式調整（期間中文化、年份必填、卡片標題與位置、移除底部導覽）。

## Impact

- 前端：`frontend/src/views/DashboardView.vue:89,102,137,344,364` 版型與文案。
- 後端：無（年份空值改前端預設，避免 `GET /api/cases/statistics?year=` 空值）。
- 文件：無。
