## 1. 新增按鈕與更名統一樣式

- [x] 1.1 於 `frontend/src/views/SendersView.vue` 標題列新增「新增」按鈕（`v-if="auth.isStaff"`，`btn-sm btn-success`，右上角與篩選同列），並驗證 `npm run build` 通過且 STAFF 可見、VIEWER 不可見
- [x] 1.2 將 `frontend/src/views/CasesView.vue:398` 的「建立案件」更名為「新增」，並驗證按鈕文字與樣式與其他管理一致
- [x] 1.3 統一所有管理頁（`CasesView`/`SendersView`/`CropManagementView`/`PestManagementView`/`ReferenceDataAdminView`/`UsersView`）的新增區塊按鈕大小顏色位置（標題列右上角 `d-flex`，`新增` 為 `btn-sm btn-success`），並驗證 `npm run build` 通過

## 2. 篩選抽屜

- [x] 2.1 為 `frontend/src/views/CropManagementView.vue` 加入篩選抽屜（`showFilter` 預設收合，`分類` 單欄，`v-show` + `aria-expanded`），如同案件/送件人，並驗證 `npm run build` 通過
- [x] 2.2 為 `frontend/src/views/PestManagementView.vue` 加入篩選抽屜（`showFilter` 預設收合，`類型` 與 `名稱/代碼`），並驗證 `npm run build` 通過

## 3. 移除 sort_order

- [x] 3.1 移除前端 `PestManagementView.vue` 與 `ReferenceDataAdminView.vue` 的 `sortOrder` 欄位與 `swal-pc-order` 輸入，並驗證 `npm run build` 通過且表格不再顯示排序
- [x] 3.2 移除後端 `PestCategory.sortOrder`、`ReferenceDtos` 與 `pest_categories.sort_order` 欄位，`ReferenceDataService` 改以 `pest_category_code desc` 排序，並驗證 `mvn test` 通過
- [x] 3.3 同步資料庫：`schema.sql` 移除 `sort_order` 與 `idx_pest_categories_sort` 重建，並驗證 `mvn test` 與 `npm run build` 通過

## 4. 驗收

- [x] 4.1 執行 `npm run build`、`npm test` 與 `openspec validate --specs --changes` 通過
