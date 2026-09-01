## Context

見 `proposal.md`。現況 `StatisticsDtos.CaseStatisticsResponse` 為 22 欄（末兩欄 `fieldCityBreakdown/fieldDistrictBreakdown`），`CaseService.statistics` 同時計算兩者，但 `frontend/src/views/DashboardView.vue:259` 僅渲染 `fieldCityBreakdown`。`fieldDistrictBreakdown` 無 spec、無測試、無消費，屬 `align-case-field-order` 的多餘新增。

## Goals / Non-Goals

**Goals:**
- 移除 `fieldDistrictBreakdown` 欄位與計算，收斂 `GET /api/cases/statistics` 契約為 `fieldCityBreakdown`（Top 10 縣市）。
- 保持 `fieldCityBreakdown` 邏輯與 Dashboard 現有行為不變。

**Non-Goals:**
- 不更動其他統計欄位（topN/breakdown/period）與 Dashboard 版式。
- 不新增替代的鄉鎮層級統計（若未來需要，以新 change 提出）。

## Decisions

### D1. 僅刪鄉鎮 breakdown，保留縣市

`fieldCityBreakdown` 已被 Dashboard 使用且符合「田區位置 Top 10」需求，刪除 `fieldDistrictBreakdown` 即可消除 dead code。替代「全刪」會失去已交付的縣市卡；替代「保留」則維持 scope creep。選「僅刪鄉鎮」。

### D2. 後端契約收斂為 21 欄

更新 `StatisticsDtos` record 定義為 21 欄（末欄僅 `fieldCityBreakdown`），`CaseService` 移除 `District::getDistrict` 分組與 `fieldDistrictBreakdown` 變數，回傳建構對應減少一參數。相容建構子（8 參數）仍保留，僅調整內部 `List.of()` 占位數量。理由：最少改動收斂，避免影響 `CaseControllerTest` 的 mock。

### D3. 不改前端

`DashboardView.vue` 已 `slice(0,10)` 並僅讀 `fieldCityBreakdown`，無需改動。理由：零風險。

## Risks / Trade-offs

- [外部呼叫端曾依賴 `fieldDistrictBreakdown`] → 機率極低（前端未用、spec 未定義），於 proposal 標 BREAKING 並於 release note 說明，遷移為改用 `fieldCityBreakdown` 或改以 `GET /api/cases` 聚合。
- [測試以位置索引依賴欄位順序] → 僅 `CaseServiceTest` 以 `contains` 斷言 breakdown，不以索引，無風險。

## Migration Plan

- 後端刪除欄位後重跑 `mvn test`（`CaseServiceTest` 需同步更新斷言）與 `npm run build`；驗證 `GET /api/cases/statistics` 回應不再含 `fieldDistrictBreakdown`。
- 無需資料遷移。

## Open Questions

- 無。
