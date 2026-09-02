## 1. 後端 CSV 表頭與排序

- [x] 1.1 調整 `CaseService.exportCsv` 排序鍵為 `caseId asc`（`findAll(Sort.by("caseId"))` 與 `view → findAllById → sort by caseId`），並驗證 `CaseServiceTest.exportCsv_shouldPassFilterAndSortAscending` 改斷言 `Sort.by("caseId")`
- [x] 1.2 重建 `CaseService.toCsv` 表頭與列順序：合併 `病蟲害發生地點_縣市/鄉鎮→病蟲害發生地`（`city+district`）、移除 `是否同寄件人` 與 `isSame` 計算、將 `服務類別/送件方式` 移至 `耕作方式` 前、`栽培面積/被害面積` 移至 `土壤栽培用藥紀錄` 前、`診斷結果`（原 `被害描述`）移至 `建議事項` 前、`鑑定者` 移至 `建立者` 前，並修改 `csvEscape` 為全欄位以 `"` 包覆且 `""` 轉義，驗證 `CaseServiceTest.exportCsv_shouldBuildRowsWithBomAndEscape` 表頭與欄位皆為引號字串（含 `電話` 如 `"0912345678"` 且 `被害部位,栽培面積,被害面積,土壤栽培用藥紀錄` 順序）

## 2. 狀態中文轉換

- [x] 2.1 新增 `statusDisplay(CaseStatus)` 映射 `PENDING/RESOLVED/CLOSED→待處理/已處理/已結案` 並於 `toCsv` 取代 `status.name()`，驗證匯出 CSV 中狀態為中文

## 3. 測試與驗證

- [x] 3.1 更新 `CaseControllerTest` 匯出斷言（`startsWith` 含 BOM 與 `"`，補 `病蟲害發生地/診斷結果` 存在且 `是否同寄件人` 不存在，電話為引號字串）與 `PhytoTrackIntegrationTest` CSV 相關斷言
- [x] 3.2 執行 `rm -f backend/target/phytotrack-test.db && mvn test` 與 `openspec validate --specs --changes` 通過
