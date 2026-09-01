## 1. 後端契約收斂

- [x] 1.1 移除 `StatisticsDtos.CaseStatisticsResponse` 的 `fieldDistrictBreakdown` 欄位（保留 `fieldCityBreakdown`），更新相容建構子占位，並驗證編譯通過
- [x] 1.2 移除 `CaseService.statistics` 中 `fieldDistrictBreakdown` 的 `topN` 計算與回傳參數，並驗證 `mvn test -Dtest=CaseServiceTest` 通過
- [x] 1.3 更新 `CaseServiceTest` 斷言（若曾以 `contains("fieldDistrictBreakdown")` 檢查則移除，改為僅斷言 `fieldCityBreakdown` 存在且不再含鄉鎮欄位），並驗證 `mvn test` 全綠

## 2. 驗證與文件

- [x] 2.1 執行 `npm run build` 驗證 Dashboard 仍正常渲染田區位置 Top 10 縣市（無前端改動）
- [x] 2.2 執行 `openspec validate --specs --changes` 與手動呼叫 `GET /api/cases/statistics` 驗證回應不再包含 `fieldDistrictBreakdown`，且 `fieldCityBreakdown` 仍為 Top 10
