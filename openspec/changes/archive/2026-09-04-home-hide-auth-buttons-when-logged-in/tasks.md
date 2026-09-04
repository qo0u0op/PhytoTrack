## 1. 首頁按鈕條件顯示

- [x] 1.1 於 `frontend/src/views/HomeView.vue` 引入 `useAuthStore` 並以 `v-if="!auth.isAuthenticated"` 控制「立即登入/建立帳號」按鈕組顯示，驗證未登入顯示、已登入隱藏且重新整理保持

## 2. 驗證

- [x] 2.1 執行 `cd frontend && npm run build`（含 `vue-tsc`）與 `openspec validate --specs --changes --strict`，驗證通過
