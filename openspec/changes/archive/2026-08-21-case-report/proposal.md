# Proposal: case-report 案件明細、列印診斷單與 CSV 匯出

## Why

目前案件僅有列表與建立／編輯表單，沒有獨立的單案明細檢視，也無法輸出紙本診斷記錄表，工作人員需人工謄寫診斷結果與防治建議，易錯且費時。

## What Changes

- 前端新增**單案明細頁**：顯示送件人、作物／病蟲害、描述、AI 診斷結果與案件時間資訊
- 明細頁支援**列印診斷單**：以 `@media print` 呈現診斷記錄表樣式，列印內容僅含診斷單本體 (隱藏導覽與操作區)
- 後端新增 **CSV 匯出端點** `GET /api/cases/export` (登入即可)：輸出含案件欄位 (含詳細內容) 的 CSV 檔
- 前端明細頁提供「列印」「匯出 CSV」按鈕
- 重新生成 `types/api.ts`、文件 (REQUIREMENTS / ARCHITECTURE / AGENTS / 操作手冊 / notebook) 同步

## Capabilities

### New Capabilities

- `case-report`: 案件明細檢視、列印診斷單與 CSV 匯出 (規格已存在於主規格 `openspec/specs/case-report/spec.md`，本 change 為其實作，故於 `.openspec.yaml` 設 `skip_specs: true`)

## Impact

- 後端：`CaseController` (CSV 匯出端點)、`CaseService` (匯出資料彙整)、`CaseDtos` 或新 DTO、相關測試
- 前端：`views/CaseDetailView.vue` (新)、路由表、`api/index.ts`、`types/api.ts` (重新生成)、CSS (`@media print`)
- 文件：`docs/REQUIREMENTS.md`、`docs/ARCHITECTURE.md`、`AGENTS.md`、操作手冊 `docs/manual.typ`、`docs/notebook/`
- 資料庫：無 schema 變更