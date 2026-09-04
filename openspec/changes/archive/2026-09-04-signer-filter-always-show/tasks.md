## 1. 簽名人篩選常顯與身分別下拉

- [x] 1.1 `SignersView.vue` 移除篩選按鈕與 collapse 抽屜、篩選卡常顯，驗證進入頁面即見篩選列且無按鈕
- [x] 1.2 `SignersView.vue` 新增身分別下拉（全部／使用者／非使用者，預設全部）併入 `filtered` 與既有條件疊加，驗證選使用者僅列有關聯者、與關鍵字疊加正確

## 2. 驗證與回歸

- [x] 2.1 執行 `cd frontend && npm run build`（含 `vue-tsc`），驗證建置通過
- [x] 2.2 執行 `openspec validate --specs --changes --strict`，驗證無錯誤
