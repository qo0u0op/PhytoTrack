# Tasks: case-report 案件明細、列印診斷單與 CSV 匯出

## 1. 後端：CSV 匯出

- [x] 1.1 `CaseService` 新增匯出方法：依 `CaseFilter` 查詢全部案件（不分頁，依 `receiveDate` 升序），以 Java 組 CSV 行（欄位含逗號／引號以 `"` 包覆、內部 `"` 轉 `""`），輸出含 UTF-8 BOM
- [x] 1.2 `CaseController` 新增 `GET /api/cases/export`（`isAuthenticated()`）：回 `text/csv; charset=UTF-8`，`Content-Disposition: attachment; filename="case-export-YYYYMMDD.csv"`

## 2. 後端：測試

- [x] 2.1 `CaseServiceTest`：匯出行組字（BOM 前綴、逗號／引號轉義、篩選與排序正確）
- [x] 2.2 `CaseControllerTest`：`GET /api/cases/export` 登入回 200 且 header（Content-Type、Content-Disposition）正確；未登入 401
- [x] 2.3 整合測試：登入後呼叫 export，回應內容含案件編號與 BOM

## 3. 前端：明細頁

- [x] 3.1 路由新增 `/cases/:id` → `views/CaseDetailView.vue`
- [x] 3.2 `CaseDetailView`：載入 `caseApi.detail(id)` 呈現全欄位（送件人、作物／病蟲害、被害部位、描述、狀態、時間、多對多）
- [x] 3.3 明細頁「AI 診斷」按鈕（僅 STAFF+ 顯示）：呼叫 `aiApi.analyze`（以 `pestDescription`＋作物／病蟲害為輸入）即時顯示結果並標註「僅供參考，正式診斷由診斷員確認」；VIEWER 不顯示按鈕
- [x] 3.4 明細頁「列印」按鈕：`window.print()`；`@media print` CSS 隱藏導覽／按鈕，僅輸出診斷單樣式
- [x] 3.5 明細頁「匯出 CSV」按鈕：導向 `/api/cases/export`（下載全量 CSV）

## 4. 前端：列表連結與 API

- [x] 4.1 `CasesView` 每列新增「檢視」連結 → `/cases/:id`
- [x] 4.2 重新生成 `types/api.ts`（確認 export 端點契約）；`api/index.ts` 加 `caseApi.exportCsv()`

## 5. 驗證與文件同步

- [x] 5.1 全量驗證：`cd backend && ./mvnw test`、`cd frontend && npm run build && npm test`、`openspec validate --specs` / `--changes`
- [x] 5.2 同步 `docs/REQUIREMENTS.md`（case-report 標實作）、`docs/ARCHITECTURE.md`（export 端點、明細頁）、`AGENTS.md`、操作手冊 `docs/manual.typ`（明細／列印／匯出）、`docs/notebook/` 筆記（含 RAG 後續方向紀錄）