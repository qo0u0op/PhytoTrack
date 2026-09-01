## Context

見 `proposal.md`。現況 `DashboardView.vue:102` 期間以 `stats.period` 原值顯示（英文枚舉），`DashboardView.vue:89` 年份含 `請選擇年份`（`null`）導致 `GET /api/cases/statistics?period=ANNUAL&year=` 空值報 400，`DashboardView.vue:137` AI 標題為 `AI 模型 (llama.cpp)`，趨勢卡 `DashboardView.vue:344` 標題 `近 6 月案件趨勢` 且獨立一列，底部有三按鈕導覽與頂部重複。

## Goals / Non-Goals

**Goals:**
- 期間中文化、年份預設最新且移除空選項、AI 標題更正、趨勢卡更名並與耕種方式並列、移除底部導覽。

**Non-Goals:**
- 不改統計計算與後端 API（僅前端呈現）。
- 不改其他 Dashboard 卡片內容。

## Decisions

### D1. 期間中文化以前端映射

`periodLabel = { HISTORICAL: '歷史', ANNUAL: '年度', MONTHLY: '月度' }`，模板 `{{ periodLabel[stats.period] ?? '歷史' }}`。替代「後端回中文」影響 API 契約，不採。

### D2. 年份移除空選項並預設最新

`selectedYear` 初始化為 `availableYears[0]`（`loadStats` 後若空則設），模板移除 `<option :value="null">請選擇年份</option>`，改為 `v-for` 僅列年份；`period` 切換時若 `selectedYear` 為空則補最新。替代「保留空選項 + 前端阻擋請求」多餘。

### D3. 趨勢卡與耕種方式並列

原 `delivery/method` 為兩欄 `col-md-6`，改為三欄 `col-md-4`：交付、耕種、近半年趨勢（原獨立 `card shadow-sm mb-4` 移入同 `row g-4`）。替代「保持獨立」佔垂直空間。

### D4. 移除底部導覽

刪除 `DashboardView.vue:364` 的 `row g-3` 三按鈕（`auth.isStaff ? 建立新診斷案件` 等），與頂部/側邊導覽重複，無需遷移。

## Risks / Trade-offs

- [年份無可用值時預設] → `availableYears` 為空時保持 `null`，請求不帶 `year`，後端以 `HISTORICAL` 回退。
- [期間中文化遺漏] → 僅三枚舉，映射窮舉，無風險。

## Migration Plan

- 前端僅改 `DashboardView.vue`，`npm run build` 驗證，`openspec validate` 通過，無遷移。

## Open Questions

- 無。
