## 1. 分頁

- [x] 1.1 於 `frontend/src/views/SendersView.vue` 新增分頁（>20 顯示，`page/size/pageInput/totalPages`，`filteredSenders` 的 `paged` 計算，與 `CasesView` 同款控制），並驗證 `npm run build` 通過且 >20 時分頁可切換
- [x] 1.2 於 `frontend/src/views/ReferenceDataAdminView.vue` 為各類別新增同款分頁（>20 顯示，前端本地分頁），並驗證 `npm run build` 通過

## 2. 導覽與獨立頁面

- [x] 2.1 新增 `frontend/src/views/CropManagementView.vue` 與 `PestManagementView.vue`（複用參照表格邏輯，僅單一類別），`frontend/src/router/index.ts` 新增 `/admin/crops` 與 `/admin/pest-categories` 路由，並驗證 `npm run build` 通過且路由可達
- [x] 2.2 更新 `frontend/src/App.vue` Navbar 順序為 儀表板/案件管理/送件人管理/作物管理/害物管理/參照資料管理/使用者管理，標籤改為 `作物管理/害物管理`，並驗證 `npm run build` 通過且導覽順序正確
- [x] 2.3 自 `ReferenceDataAdminView` 移除作物與害物 tab（保留其餘類別），並驗證 `npm run build` 與既有類別仍可維護

## 3. 權限

- [x] 3.1 將 `backend/src/main/java/com/d0w0b/phytotrack/controller/ReferenceDataAdminController.java` 作物相關端點（`createCrop/updateCrop`）權限改為 `hasAnyRole('STAFF','ADMIN')`，`deleteCrop` 維持 `ADMIN`，前端 `CropManagementView` 刪除按鈕 `v-if="auth.isAdmin"`，並驗證 `mvn test` 與 `npm run build` 通過

## 4. 驗收

- [x] 4.1 執行 `npm run build`、`npm test`、`mvn test` 與 `openspec validate --specs --changes` 通過
