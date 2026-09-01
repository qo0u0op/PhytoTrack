## Context

見 `proposal.md`。現況管理表格在窄寬度下換行，`table-responsive` 雖存在但未固定欄寬，部分欄位因內容長度不一導致分頁時寬度抖動。

## Goals / Non-Goals

**Goals:**
- 所有管理表格左右可滾動且不換行，ID/操作等固定寬度。

**Non-Goals:**
- 不改資料與 API。

## Decisions

### D1. 固定寬度與滾動

`table-responsive` 加 `overflow-x:auto`，`table` 加 `min-width` 與 `table-layout:fixed`，`th` 設 `width/min-width`，`td` 加 `text-truncate` 與 `title`。替代「自動寬度」會抖動。

## Risks / Trade-offs

- [窄螢幕仍需滾動] → 預期行為，優於換行。

## Migration Plan

- 前端僅改樣式，`npm run build` 驗證。

## Open Questions

- 無。
