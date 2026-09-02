## Context

`docs/REQUIREMENTS.md` 未電子化欄位中「土壤、栽培、用藥紀錄」標註 `pestDescription → caseDescription` 待更名，現行 `Case.caseDescription` 與 `schema.sql:case_description` 已更名，但文件與殘留 `pestDescription` 參照需收斂；案件表單「＋新增因素」按鈕位於同列，視覺擁擠需換行。見 `proposal.md`。

## Goals / Non-Goals

**Goals:**
- 完成 `pestDescription` 殘留更名收斂與文件釐清（其餘 4 項維持現狀/替代說明）。
- 使「＋新增因素」獨立一行，提升可操作性。

**Non-Goals:**
- 不改病蟲害發生地點現有呈現，不新增 email 欄位，不獨立被害描述欄位，不新增 Delivery 類型（以 `網路諮詢` 涵蓋）。

## Decisions

- **更名收斂**：以 `rg pestDescription` 全量掃描，`Case.java`/`CaseDtos`/`schema.sql` 已為 `caseDescription`/`case_description`，僅需更新 `docs/REQUIREMENTS.md` 該條為已更名與 `CaseService` 註解 `pestDescription` 殘留。替代：DB 欄位重建遷移，現已為目標名稱故無需 `ALTER`。
- **按鈕換行**：於 `CaseFormView.vue` 按鈕前插入 `w-100` 空 `div` 或將按鈕改 `d-block mt-2`，使獨立一行。替代：`flex-wrap` 自動換行但不可控。
- **文件釐清**：`docs/REQUIREMENTS.md` 未電子化清單增括號註明替代方案（`displayName` 替代 email、`pestNote` 呈現被害描述、`網路諮詢` 涵蓋 Email/FB/Line）。

## Risks / Trade-offs

- [更名遺漏] 殘留 `pestDescription` 未清 → `grep` 驗證零殘留（排除歷史文件）。
- [版面] 按鈕換行後空隙過大 → 以 `mt-2` 控制。

## Migration Plan

1. 更新 `CaseService` 註解與 `docs/REQUIREMENTS.md` 未電子化清單。
2. 調整 `CaseFormView.vue` 按鈕前換行。
3. `openspec validate` 與 `grep pestDescription` 驗證。

## Open Questions

- 無。
