## Context

`case-statistics` 現行僅 `HISTORICAL/ANNUAL/MONTHLY` 三期別，`docs/REQUIREMENTS.md` 與 `DashboardView` 皆以此為準，後端 `CaseService.statistics` 以 `period/year/month` 分支。需新增 `HALF_YEAR` 以支援半年報告，見 `proposal.md` Why。

## Goals / Non-Goals

**Goals:**
- 後端支援 `HALF_YEAR` 篩選（`year`+`half` 1/2），前端選單可切並正確帶參。
- 保持既有三期別與統計卡片邏輯不變。

**Non-Goals:**
- 不改 `Case` 模型與 `v_case_search` 視圖，僅於查詢層以日期區間過濾。
- 不做 `half` 的 i18n 僅中文化 `半年度/上半年/下半年`。

## Decisions

- **API 參數**：`GET /api/cases/statistics?period=HALF_YEAR&year=2026&half=1`（1=1-6, 2=7-12），`half` 必填，驗證失敗 400。替代：以 `month` 範圍但語意不清。
- **查詢實作**：`CaseService` 期別分支新增 `HALF_YEAR`，以 `receiveDate` 介於 `year-01-01`/`year-06-30` 或 `year-07-01`/`year-12-31` 過濾；`availableYears` 與既有一致。替代：新增資料庫欄位，無必要。
- **前端**：`DashboardView.vue` 期別 `select` 新增 `HALF_YEAR` 選項，`watch(period)` 時若為 `HALF_YEAR` 顯示 `half` 下拉（預設 1），`load()` 組參時帶 `half`。替代：獨立頁面，增加維護成本。

## Risks / Trade-offs

- [參數驗證] 缺 `half` 需 400 → 前端預設 1 避免空值。
- [舊前端相容] 舊版不識別 `HALF_YEAR` → 後端仍接受，舊版僅不顯示新選項。

## Migration Plan

1. 後端擴充期別與參數驗證，補測試。
2. 前端擴充選單與呼叫，補 `openapi-typescript` 型別。
3. 文件 `docs/REQUIREMENTS.md` 與 `ARCHITECTURE.md` 若涉期別則同步。

## Open Questions

- 無。
