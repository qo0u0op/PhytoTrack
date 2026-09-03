## Context

見 `proposal.md - Why`。第一批後行為已收斂，剩餘為併發、一致性與顯示問題。

## Goals / Non-Goals

**Goals:**
- 併發冪等、正規化比對、綁定一致、重建循環、表單可辨、清空保護。

**Non-Goals:**
- 歷史快照、強制一對一 DB 約束、自動合併同名。

## Decisions

- 正規化函式集中於 `IdentifierNames.normalize`，所有查重共用；DB 索引視 SQLite 能力採函式索引或應用層＋異常轉換兜底。
- 綁定前比對正規化後名稱，不一致即拒；重建前先查同名 `inactive`，有則提示啟用。
- 表單選項顯示沿用管理頁 `身分別＋帳號` 文案，提交維持 `id`。
- 最後 `active` 檢查置於 `updateIdentifierActive(false)` 前，全域計數為 1 即阻擋或要求 `force`。
