## Why

`docs/REQUIREMENTS.md` 未電子化欄位中「土壤、栽培、用藥紀錄」標註 `pestDescription → caseDescription` 待更名，且案件表單「＋新增因素」按鈕位於同列導致版面擁擠；其餘未電子化欄位經確認可維持現狀或以現有欄位替代，需一併釐清並完成必要更名與換行。

## What Changes

- 資料庫/實體更名：確認 `cases.case_description`（現行）與 `pestDescription` 殘留參照全量更名為 `caseDescription`，`docs/REQUIREMENTS.md` 該條標註為已更名；若仍有 `pest_description` 欄位則以遷移腳本更名（SQLite `ALTER TABLE` 重建）。
- 前端版面：`CaseFormView.vue` 中 `<button>＋新增因素</button>` 前加入換行（`d-block` 或 `<div class="w-100">`），使按鈕獨立一行。
- 文件釐清：`docs/REQUIREMENTS.md` 未電子化欄位更新為：
  - 病蟲害發生地點維持現狀
  - 基本資料 email 以 `displayName` 替代（紙本幾乎未用）
  - 被害描述沿用 `pest_note`（`pestNote`）呈現
  - Email/FB/Line 等同 `網路諮詢`（`Delivery` 名稱）
  - 土壤、栽培、用藥紀錄已更名為 `caseDescription`/`case_description`

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `case-lifecycle`: 案件表單欄位命名與資料庫欄位更名
- `case-report`: 文件釐清不影響 CSV 欄位（`土壤栽培用藥紀錄` 仍由 `caseDescription` 提供）

## Impact

- 後端：`schema.sql`/`Case.java`/`CaseDtos`/`CaseService` 殘留 `pestDescription` 更名；`docs/` 同步。
- 前端：`CaseFormView.vue` 按鈕換行，樣式微調。
- 測試：若有 `pestDescription` 相關測試需同步更名；`migrate` 腳本若需欄位更名則補遷移。
