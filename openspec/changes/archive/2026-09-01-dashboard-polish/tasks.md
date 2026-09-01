## 1. Dashboard 版式與文案

- [x] 1.1 將 `frontend/src/views/DashboardView.vue:102` 期間顯示改為中文映射（HISTORICAL→歷史、ANNUAL→年度、MONTHLY→月度），並驗證切換期別時期間文字為中文
- [x] 1.2 移除 `DashboardView.vue:89` 年份選單的 `請選擇年份` 空選項，改為預設 `availableYears[0]` 且年份必填，驗證年度/月度下不出現空值且不報 400
- [x] 1.3 將 `DashboardView.vue:137` 的 `AI 模型 (llama.cpp)` 改為 `AI 連線情況`，並驗證標題顯示正確
- [x] 1.4 將 `DashboardView.vue:344` 的 `近 6 月案件趨勢` 更名為 `近半年案件趨勢`，並將卡片移至耕種方式右側（三欄：交付、耕種、趨勢），驗證版式與標題正確
- [x] 1.5 移除 `DashboardView.vue:364` 底部三按鈕列（建立新診斷案件/案件管理/使用者管理），並驗證頁面不再顯示該列

## 2. 驗收

- [x] 2.1 執行 `npm run build` 與 `npm test` 驗證無迴歸，並執行 `openspec validate --specs --changes` 通過
