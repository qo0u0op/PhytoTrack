## Why

現行 `GET /api/cases/export` 欄位順序、狀態語系與地點呈現與紙本 `diagnoses.typ` 及使用者匯出需求不一致（狀態為英文、縣市/鄉鎮分欄、含冗餘「是否同寄件人」、欄序與表單不符、被害描述命名不直覺），需對齊本次 7 項 CSV 格式調整以提升可讀性與實際作業對應。

## What Changes

- 匯出預設排序改為 `收件編號 asc`（現行 `receiveDate asc`）；`BREAKING` 變更預設排序行為。
- `狀態` 欄位輸出中文（PENDING → 待處理、IN_PROGRESS → 已處理、CLOSED → 已結案 等）。
- `病蟲害發生地點_縣市` / `病蟲害發生地點_鄉鎮` 合併為單欄 `病蟲害發生地`，值為 `縣市+鄉鎮` 串接（例 `xx縣xx鎮`）。
- 移除 `是否同寄件人` 欄位（計算 `fieldDistrict == sender.district`）。
- 欄序調整：`服務類別`、`送件方式` 移動至 `耕作方式` 之前。
- 欄序調整：`鑑定者` 移動至 `建立者` 之前。
- `被害描述` 更名為 `診斷結果`，且位置調整至 `建議事項` 之前。
- 欄序調整：`栽培面積`、`被害面積` 移動至 `土壤栽培用藥紀錄` 之前。
- 全欄位強制以 `"` 包覆（`BREAKING`：原僅含逗號/引號/換行才引號），`電話` 與中文欄位皆以字串形式輸出，避免 Excel 前導零與科學記號轉換。

## Capabilities

### New Capabilities
<!-- 無新增能力 -->

### Modified Capabilities
- `case-report`: CSV 匯出表頭、欄序、欄位合併/移除、狀態中文轉換與預設排序調整

## Impact

- 後端：`CaseService.exportCsv` / `toCsv` 表頭與列對應、`csvEscape` 改全欄位加引號、`CaseController` 匯出排序、`CaseServiceTest` / `PhytoTrackIntegrationTest` 斷言更新；前端無需變更（僅呼叫匯出端點）。
- 既有匯出依賴需同步更新試算表模板；未登入 401 與 UTF-8 BOM 行為不變。
