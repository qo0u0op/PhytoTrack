## 1. 表格滾動與固定寬度

- [x] 1.1 為 `frontend/src/views/CasesView.vue` 的 `table-responsive` 加入左右滾動（`overflow-x: auto`，確保 `scrollbar` 可見）並固定部分欄位寬度（ID 60px、操作 120px 等），並驗證 `npm run build` 通過且寬度不足時可左右滾動不換行
- [x] 1.2 為 `frontend/src/views/SendersView.vue`、`CropManagementView.vue`、`PestManagementView.vue` 同步加入左右滾動與固定寬度，並驗證 `npm run build` 通過
- [x] 1.3 為 `frontend/src/views/ReferenceDataAdminView.vue` 與 `UsersView.vue` 加入左右滾動與固定寬度，並驗證 `npm run build` 通過

## 2. 驗收

- [x] 2.1 執行 `npm run build` 與 `openspec validate --specs --changes` 通過，手動驗證所有管理表格在窄寬度下可左右滾動且不換行
