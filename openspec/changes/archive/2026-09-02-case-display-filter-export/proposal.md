## Why

預覽/檢視與 CSV 的「病蟲害發生地點」「送件人身分別」用語與表單不一致造成混淆；案件管理篩選欄位現行排序與新增欄位（身分別、耕種方式）需求未對齊；且 CSV 匯出未跟隨當前景篩選結果，使用者需二次篩選。需一次性對齊顯示用語、篩選排序與匯出範圍。

## What Changes

- 顯示用語統一：預覽彈窗（`CasesView.vue`）、檢視頁（`CaseDetailView.vue`）、CSV 表頭/內容的 `病蟲害發生地點` → `田區位置`、`送件人身分別` → `身分別`。**BREAKING** 文案變更。
- 案件管理篩選重排並補齊欄位，依序：`收件日期區間 → 狀態 → 田區縣市 → 田區鄉鎮 → 送件人 → 身分別(新增) → 服務類別 → 送件方式 → 耕種方式(新增) → 作物類別 → 作物 → 被害部位 → 害物 → 害物類別 → 建議類別`。新增 `身分別(senderTypeId)`、`耕種方式(methodId)` 篩選支援（後端 `CaseFilter`/`CaseSpecifications`、前端選單與重置）。
- 篩選結果影響 CSV 匯出：`GET /api/cases/export` SHALL 接受與 `GET /api/cases` 相同篩選參數並沿用當前景篩選條件（前端匯出按鈕以當前 `appliedFilter` 組查詢字串呼叫），匯出範圍即為篩選結果全量（不分頁，仍 `caseId asc`、BOM、全欄位引號）。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `case-report`: 預覽/檢視/CSV 顯示用語更名（田區位置/身分別）與 CSV 表頭同步
- `case-search`: 案件列表篩選新增身分別/耕種方式、篩選版面排序重排、篩選條件穿透至 CSV 匯出

## Impact

- 前端：`CasesView.vue` 篩選版面與 `appliedFilter`、匯出邏輯；`CaseDetailView.vue` 欄位標籤；CSV 表頭文案。
- 後端：`CaseDtos.CaseFilter` 新增 `senderTypeId/methodId`、`CaseSpecifications` 與 `CaseService` 篩選分支；`CaseController` 匯出介面參數擴充；`CaseSearchView` 若缺欄位需檢視是否支援新篩選。
- 測試：`CaseServiceTest`/`CaseControllerTest`/`PhytoTrackIntegrationTest` 篩選與匯出斷言；既有表頭斷言需同步文案。
