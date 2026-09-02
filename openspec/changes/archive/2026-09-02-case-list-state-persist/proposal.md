## Why

案件管理現行進入「檢視/編輯」後按瀏覽器上一頁或頁面「返回」會重置為預設篩選與分頁，導致已套用的篩選、每頁筆數與頁碼遺失，需重複操作，影響 5 人現場作業效率。

## What Changes

- 瀏覽狀態保持：案件管理頁的篩選條件（17 欄）、每頁筆數（`size`）、頁碼（`page`）與排序（`sortStates`）在進入 `/cases/:id`（檢視）或 `/cases/:id/edit`（編輯）後，按「上一頁」返回 `/cases` 時 SHALL 保持原狀態（不重置為預設）。
- 實作以 URL 查詢參數為單一真相源：進入檢視/編輯時攜帶當前 `query`，返回時以 `router.push` 回寫；重新整理後亦可由 URL 還原狀態。`size`/`page` 預設值僅在無 query 時使用。
- 返回按鈕（檢視頁的「返回案件列表」與瀏覽器上一頁）皆需觸發保持邏輯。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `case-search`: 案件列表狀態保持（篩選/分頁/排序在導覽後保持）

## Impact

- 前端：`CasesView.vue`（`filters`/`page`/`size`/`sortStates` 與 `router` query 雙向同步、`viewDetail`/`edit` 導覽附帶 query）、`CaseDetailView.vue`/`CaseFormView.vue` 返回邏輯；`router/index.ts` 支援 `/cases` query 參數。
- 後端：無變更。
- 測試：新增 `CasesView` 狀態保持相關單元測試；既有 `mvn test` 不受影響。
