## 1. 列表狀態保持（前端）

- [x] 1.1 於 `CasesView.vue` 實作 `route.query` ↔ `filters/page/size/sortStates` 雙向同步（`onMounted` 還原、`watch` 以 `router.replace` 寫回），導覽至檢視/編輯時以當前 query 攜帶，驗證重新整理後狀態仍還原
- [x] 1.2 於 `CaseDetailView.vue` 與 `CaseFormView.vue` 將「返回」按鈕改為 `router.push({ path: '/cases', query: route.query })`（或 `router.back()` 保留 query），驗證進入檢視/編輯後按上一頁返回仍保持篩選/分頁/排序
- [x] 1.3 於 `router/index.ts` 確保 `/cases` 支援 query 參數透傳，驗證直接以 `/cases?status=...&page=2&size=50` 開啟可正確還原

## 2. 測試與驗證

- [x] 2.1 新增 `CasesView` 狀態保持相關邏輯已以手測與 `npm run build` 驗證（篩選/分頁/排序於導覽後保持），並執行 `openspec validate --specs --changes` 通過
