## Why

現行 Dashboard 期別僅 `歷史/年度/月度`（`HISTORICAL/ANNUAL/MONTHLY`），無法以半年度（1-6 月/7-12 月）檢視案件分布與趨勢，現場需半年彙整時僅能手動加總，不便對齊半年期報告。

## What Changes

- 後端統計：新增期別 `HALF_YEAR`（中文 `半年度`），參數為 `year` 與 `half`（1=上半年, 2=下半年），`GET /api/cases/statistics?period=HALF_YEAR&year=2026&half=1` 僅統計該年該半年度案件；`availableYears` 與 `period` 列舉同步擴充。
- 前端 Dashboard：期別選單新增 `半年度`，選中時顯示 `年份` 與 `半年度` 下拉（上半年/下半年），呼叫統計時帶對應參數；其餘卡片（總數/趨勢/breakdown）與現行一致。
- **BREAKING**：`period` 新增枚舉值，舊版前端未識別時應忽略或視為 `HISTORICAL`（後端相容）。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `case-statistics`: 期別新增半年度及其 API 與 Dashboard 呈現

## Impact

- 後端：`CaseService.statistics`/`CaseSpecifications` 期別分支、`CaseController` 參數、`CaseStatisticsResponse` 期別欄位；不改資料模型。
- 前端：`DashboardView.vue` 期別選單與參數組裝；`api` 型別更新。
- 測試：統計與 Dashboard 相關單元/整合測試補半年度場景。
