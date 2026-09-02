## Context

見 `proposal.md`。現況 `CasesView` 與 `SendersView` 的篩選卡片常駐，表格僅固定排序，窄螢幕佔位且無法彈性排序。

## Goals / Non-Goals

**Goals:**
- 篩選卡片抽屜化（預設收合）。
- 表格除操作外皆可排序，前端本地排序。

**Non-Goals:**
- 不改後端篩選與排序 API。

## Decisions

### D1. 抽屜以 `v-if` + `collapse` 實作

篩選卡片外層以 `showFilter` 布林控制 `v-show`，按鈕 `抽屜` 切換。替代「常駐」佔位。

### D2. 前端本地排序

表頭 `click` 切換 `sortKey/sortOrder`，`computed sortedList = [...filtered].sort(...)`，依欄位類型（文字/日期/數字）比較。替代「後端排序」需改 API。

## Risks / Trade-offs

- [大量資料排序效能] → 前端 <1k 筆可接受。

## Migration Plan

- 前端僅改兩視圖，`npm run build` 驗證。

## Open Questions

- 無。
