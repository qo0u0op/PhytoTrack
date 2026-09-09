## Why

檢視頁（`CaseDetailView`）顯示「建立者：／建立： ／更新：」三段同行，預覽頁（`CasesView` preview）顯示「建立者：／建立時間：」兩段，兩處時間格式不一致且「更新」與建案資訊擠在同一行，可讀性差。需以檢視頁的時間格式為基準統一，並讓「更新」獨立成行同時釐清標籤語意。

## What Changes

- **時間格式統一**：預覽頁的 `createdAt` 顯示由原始 `esc(data.createdAt)` 改為檢視頁同款 `formatTime`（`T`→空白、`slice(0,19)` 即 `yyyy-MM-dd HH:mm:ss`），兩處 `createdAt`/`updatedAt` 皆一致
- **版面調整**：檢視頁原 `建立者：／建立： ／更新：` 單行改為兩行：
  - 第 1 行：`建立者：{{createdByName}}／建立：{{formatTime(createdAt)}}`
  - 第 2 行：`編輯者：{{updatedByName ?? '—'}}／更新：{{formatTime(updatedAt)}}`（`updatedBy` 若後端未提供則回落 `createdByName`/`'—'`，不破壞顯示）
- **預覽頁對齊**：預覽頁原單行 `建立者：／建立時間：` 改為同檢視頁兩行結構與標籤（`建立者／建立` 與 `編輯者／更新`），`建立` 與 `建立時間` 視為同義標籤統一為 `建立`，無 `updatedAt` 時第二行隱藏或顯示 `—`
- **標籤語意**：`更新` 獨立行時標籤由 `更新：` 擴為 `編輯者：／更新：`，與 `建立者：／建立：` 對稱
- **Progress 配色微調**：Dashboard `DashboardView.vue:211` `progress-bar` 的 `barClass` 三色維持語意（黃/綠/灰）僅微調色調：`PENDING` `bg-warning` → `#f59e0b` (amber-500)、`RESOLVED` `bg-success` → `#10b981` (emerald-500)、`CLOSED` `bg-secondary` → `#64748b` (slate-500)，改由 inline `background-color` 或自訂 class 呈現，提升對比與現代感，不改變狀態語意

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `case-report`: 檢視與預覽的建立/更新中繼資料顯示格式與版面（兩行、時間格式、標籤）
- `case-statistics`: Dashboard `案件狀態比例` 三條 `progress-bar` 配色微調（三色語意維持，僅色調優化）

## Impact

- 前端：`views/CaseDetailView.vue:260`（中繼資料區塊）、`views/CasesView.vue:477`（預覽 `Swal` HTML）、共用 `formatTime`（`replace('T',' ').slice(0,19)`）、`views/DashboardView.vue:80,211`（`barClass` 與 `progress-bar` 配色）
- 後端（可選）：`dto/CaseDtos.CaseResponse` 若需提供 `updatedByName` 則補欄位，否則前端回落；本 change 以前端回落為預設，不強制後端變更
- 文件：`docs/manual.typ` 檢視/預覽說明若提及時間格式需同步（非強制）
