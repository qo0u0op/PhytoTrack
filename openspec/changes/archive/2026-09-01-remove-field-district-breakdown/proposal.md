## Why

`align-case-field-order` 於 `CaseService.statistics` 與 `StatisticsDtos.CaseStatisticsResponse` 新增 `fieldCityBreakdown` 與 `fieldDistrictBreakdown` 兩欄，前端 `DashboardView` 僅消費 `fieldCityBreakdown`，`fieldDistrictBreakdown` 從未被使用、無測試覆蓋、亦未在 `case-statistics` spec 中定義，屬 scope creep。審查（review）已標記為 dead code，需移除以收斂 API 契約與降低維護成本。

## What Changes

- 移除 `StatisticsDtos.CaseStatisticsResponse` 的 `fieldDistrictBreakdown` 欄位（保留 `fieldCityBreakdown` 作為田區位置 Top 10 縣市）。
- 移除 `CaseService.statistics` 中 `fieldDistrictBreakdown` 的計算與 `topN` 分組（`District::getDistrict`）。
- 更新相容建構子與測試中對 22 參數建構的 `List.of()` 占位。
- 前端與文件無需改動（已僅用 `fieldCityBreakdown`）。
- **BREAKING**：`GET /api/cases/statistics` 回應將不再包含 `fieldDistrictBreakdown` 欄位（前端未使用，無實際影響；以契約收斂為目的，於 release note 標記）。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `case-statistics`: 移除未定義且未使用的 `fieldDistrictBreakdown` 回應欄位，收斂統計契約為 `fieldCityBreakdown`（Top 10 縣市）。

## Impact

- 後端：`backend/src/main/java/com/d0w0b/phytotrack/dto/StatisticsDtos.java:35`、`backend/src/main/java/com/d0w0b/phytotrack/service/CaseService.java:637`、`backend/src/test/java/com/d0w0b/phytotrack/service/CaseServiceTest.java` 相關斷言。
- API：`GET /api/cases/statistics` 回應 JSON 少一欄位。
- 前端：無影響（`frontend/src/views/DashboardView.vue:259` 已僅依賴 `fieldCityBreakdown`）。
- 文件：`openspec/specs/case-statistics/spec.md` 需同步收斂描述。
