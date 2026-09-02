## Why

近月連續交付已改變對外可見行為（CSV 表頭 `田區位置/身分別` 與全欄位引號、篩選新增 `身分別/耕種方式` 並依 5 列換行重排且穿透至匯出、`caseSearch` 15→17 欄、帳號自助停用、案件明細/預覽用語統一等），但 `docs/*.md` 與 `docs/*.typ` 仍停留於 2026-08 中期（舊表頭、舊篩選順序、舊狀態描述、缺帳號管理），導致文件與程式不一致，需一次性同步至目前 `openspec/specs` 與實作。

## What Changes

- 同步 `docs/REQUIREMENTS.md` 能力狀態與 Phase 範圍至最新 archive（`2026-09-02-case-display-filter-export` 等）。
- 同步 `docs/ARCHITECTURE.md` 後端結構/資料模型/篩選視圖（`v_case_search` 17 欄、`sender_type_id`、全欄位引號、狀態中文等）與前端篩選版面（5 列換行）說明。
- 同步 `docs/DEPLOY.md` / `docs/E2E.md` 若涉篩選/匯出流程。
- 同步 `docs/diagnoses.typ` 與 `docs/manual.typ` 紙本欄位、表頭用語與操作手冊步驟至 `case-report`/`case-search` 最新行為。
- 同步 `docs/adr/*` 必要補充（如篩選視圖演進、CSV 格式變更）。
- 校對 `README.md` 若引用舊表頭/篩選說明。
- **BREAKING**：僅文件變更，無 API/DB 行為變更；`skip_specs: true`。

## Capabilities

### New Capabilities
<!-- 無新增能力，純文件同步 -->

### Modified Capabilities
<!-- 無行為變更，skip_specs: true -->

## Impact

- 文件：`docs/**/*.md`、`docs/*.typ`、`docs/adr/*`；不改 `backend/`/`frontend/` 程式碼與 `openspec/specs` 行為。
- 風險低，需 `typst compile` 驗證 PDF 產物未破版。
