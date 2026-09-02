## Context

`CasesView.vue` 現行以 `filters/page/size/sortStates` 記憶體狀態驅動列表，進入 `/cases/:id` 或 `/cases/:id/edit` 後返回會重新 `onMounted` 並初始化為預設值，導致篩選、分頁與排序遺失。見 `proposal.md` Why。

## Goals / Non-Goals

**Goals:**
- 使篩選/分頁/排序於檢視/編輯返回後保持，且重新整理可還原。
- 以 URL query 為單一真相源，避免額外儲存分歧。

**Non-Goals:**
- 不改後端分頁/篩選語意與權限。
- 不引入全域 Pinia 持久化，僅在路由層處理。

## Decisions

- **URL Query 雙向同步**：`CasesView.vue` 以 `watch(filters/page/size/sortStates)` 同步至 `router.replace({ query })`，`onMounted` 優先以 `route.query` 還原狀態，無 query 時才用預設。返回時 `CaseDetailView.vue`/`CaseFormView.vue` 的「返回」按鈕以 `router.push({ path: '/cases', query: route.query })` 或 `router.back()` 搭配 `history` 保持。替代：`sessionStorage` 需額外清理且重新整理不一致；Pinia 持久化增加複雜度。
- **攜帶 Query 進入詳情**：列表的「檢視/預覽→檢視」與「編輯」導覽皆以 `router.push({ name, params, query: currentQuery })` 攜帶當前篩選/分頁/排序，避免返回時遺失。替代：僅靠 `back()` 依賴瀏覽器歷史，重新整理後失效。
- **Sort 序列化**：`sortStates` 以 `sort=field,asc|desc` 多值 query 序列化，解析時還原多欄排序。替代：僅保留單欄，無法支援現行多欄。

## Risks / Trade-offs

- [URL 過長] 17 欄篩選全帶 query 可能過長 → 僅保留非空值，長度可控。
- [初始載入閃動] 還原 query 後觸發兩次 `load` → 以 `watch` 合併與 `onMounted` 單次還原避免重複。

## Migration Plan

1. `CasesView.vue` 增加 query 同步與還原邏輯，導覽附帶 query。
2. `CaseDetailView.vue`/`CaseFormView.vue` 返回按鈕改為攜帶 query。
3. 補 `CasesView` 單元測試驗證返回後狀態保持。

## Open Questions

- 無。
