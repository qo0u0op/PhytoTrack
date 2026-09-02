## Context

見 `proposal.md` Why。`openspec/specs` 已於 `2026-09-02-case-display-filter-export` 前完成 5 次 CSV 與顯示/篩選演進，`docs/` 仍引用舊表頭與篩選描述，`diagnoses.typ/manual.typ` 紙本欄位亦未跟隨更名，需以 spec 與實作為單一真相源做文件校對。

## Goals / Non-Goals

**Goals:**
- 使 `docs/REQUIREMENTS.md`、`ARCHITECTURE.md`、`diagnoses.typ`、`manual.typ`、`adr/*` 與 `README.md` 文案與 `case-report`/`case-search` 最新行為一致。
- 保持文件結構不重排，僅更新內容與範例。

**Non-Goals:**
- 不改程式碼與 spec 行為，不新增 ADR 決策，僅補述既有決策影響。

## Decisions

- **對齊策略**：以 `openspec/specs/case-report,spec.md` 與 `case-search,spec.md` 為準，逐份比對 `docs/` 文案，表格與列舉以新表頭（`田區位置,身分別`、17 欄篩選、5 列換行）覆蓋，範例 CSV 亦全欄位引號化。替代：逐文件重寫，但風險高且易遺漏。
- **typ 處理**：`diagnoses.typ` 欄位標籤與 `manual.typ` 操作步驟同步更名，`typst compile` 驗證。替代：僅改 md，typ 不同步則紙本不一致。
- **ADR**：僅在 `ADR-005/007` 等必要處補註「視圖 17 欄、CSV 格式演進」，不新立 ADR。替代：新 ADR 會稀釋決策歷史。

## Risks / Trade-offs

- [遺漏] 文件多處分散易漏 → 以 `grep 病蟲害發生地點/送件人身分別` 全量掃描清單驅動。
- [typ 破版] 版面換行 → `typst compile docs/manual.typ` 與 `diagnoses.typ` 本地驗證。

## Migration Plan

1. 批次更新 `docs/*.md` 表頭與流程描述。
2. 更新 `docs/diagnoses.typ`/`manual.typ` 並編譯。
3. `openspec validate --specs --changes` 與 `grep` 零殘留驗證。

## Open Questions

- 無。
