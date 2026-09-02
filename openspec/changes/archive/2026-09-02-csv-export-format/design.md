## Context

現行 `CaseService.exportCsv` / `toCsv` 以 `receiveDate asc` 查詢排序、狀態輸出 `name()` 英文、表頭含 `病蟲害發生地點_縣市/鄉鎮` 分欄與 `是否同寄件人` 計算、欄序為住址後接耕作方式且 `被害描述` 位於服務類別前、鑑定者位於五類後但建立者前僅間隔建議事項。`diagnoses.typ` 與本次 7 項需求要求改為編號排序、中文狀態、合併地點、移除同寄件人、服務/送件前移、鑑定者前移、被害描述更名並前移至建議事項前。見 `proposal.md` 動機。

## Goals / Non-Goals

**Goals:**
- 調整 CSV 表頭與資料列對應新欄序與命名，預設排序改 `caseId asc`，狀態轉中文，保持 UTF-8 BOM 與轉義邏輯。
- 全欄位強制 `"` 包覆，`電話` 與中文皆以字串輸出，避免 Excel 前導零遺失。
- 最小改動 `CaseService` 單一服務，不影響篩選語意與權限。

**Non-Goals:**
- 不改變筛选參數、權限（登入可匯出）、前端下載方式。
- 不引入新欄位或改變 `pestNote` / 五類分組語意，僅更名與位置調整。
- 不做國際化框架，僅硬編碼三狀態對應中文。

## Decisions

- **排序鍵**：`exportCsv` 兩分支（`findAll` 與 `view → findAllById`）皆改 `Sort.by("caseId")` / `Comparator.comparing(Case::getCaseId)`，而非保留 `receiveDate`。替代：增加 `orderBy` 參數，但需求為固定預設，故直接改預設鍵最簡。
- **狀態中文**：新增 `private static String statusDisplay(CaseStatus)` 映射 `PENDING→待處理` 等，`toCsv` 呼叫取代 `name()`。替代：Enum 新增 `displayName` 欄位，但僅匯出使用，工具方法足夠且不改模型。
- **地點合併**：`fieldCity + fieldDistrict` 直接串接為 `病蟲害發生地`，移除 `fieldCity/fieldDistrict` 雙欄與 `isSame` 計算。替代：保留雙欄但隱藏，不符合「移除」需求。
- **欄序重排**：`toCsv` 首列 `join(...)` 與每列 `join(..., pestByType, hints...)` 同步依新順序重建：`住址 → 服務/送件 → 耕作 → ... → 被害部位 → 栽培面積 → 被害面積 → 土壤栽培用藥紀錄 → 五類 → 診斷結果 → 建議事項 → 防治描述 → 鑑定者 → 建立者`。替代：抽取欄定義表，但現有 30 欄固定，字串清單直觀可審。
- **更名**：表頭 `被害描述→診斷結果`，變數 `pestNotes` 改語意註解為診斷結果，但資料來源不變。
- **全欄位引號**：`csvEscape` 改為恆以 `"` 包覆並 `replace("\"","\"\"")`，取代原僅含 `,`/`"`/換行才包覆的條件分支。替代：僅 `電話` 加引號，但需求為全部中文與電話為字串，且全欄位引號實作最簡、相容性最高。

## Risks / Trade-offs

- [BREAKING 排序] 依賴 `receiveDate asc` 的外部彙整需更新 → 文件與 changelog 註明。
- [BREAKING 表頭] 依賴舊表頭名稱的試算表巨集失效 → 遷移指引提供新舊對照表。
- [BREAKING 引號] 全欄位加引號使檔案略增、舊解析器若以無引號分割需更新 → 標準 CSV 解析器相容，影響可控。
- 狀態硬編碼中文若新增狀態需同步更新 → 風險低，枚舉固定 3 值；可加 `default → name()` 後備。
- 地點合併後無法單獨篩選縣市（僅顯示）→ 接受，匯出僅展現非篩選。

## Migration Plan

1. 更新 `CaseService.toCsv` 表頭與列順序（`栽培面積/被害面積` 移至 `土壤栽培用藥紀錄` 前）、排序鍵、狀態轉換、移除 `isSame` 計算，並將 `csvEscape` 改為全欄位引號。
2. 更新 `CaseServiceTest.exportCsv_shouldBuildRowsWithBomAndEscape` / `exportCsv_shouldPassFilterAndSortAscending` 與 `CaseControllerTest` 斷言（表頭、狀態、排序驗證）。
3. 更新 `PhytoTrackIntegrationTest` CSV 斷言（若檢表頭）。
4. 部署無資料遷移；回滾僅 revert 表頭與排序。

## Open Questions

- 無。
