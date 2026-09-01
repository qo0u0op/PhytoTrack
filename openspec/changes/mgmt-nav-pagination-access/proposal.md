## Why

送件人與參照資料管理頁在資料超過 20 筆時缺乏分頁，瀏覽效率差；作物與病蟲害分類混於單一「參照資料管理」頁內，導覽與職責不清；作物管理僅 ADMIN 可存取，但實務上 STAFF 需維護作物資料，權限過嚴。

## What Changes

- 送件人管理（`SendersView`）與參照資料管理（`ReferenceDataAdminView`）超過 20 筆時顯示與案件管理同款分頁控制（頁碼按鈕、頁碼輸入、每頁筆數 [10,20,50,100]，預設 10，`total>20` 才顯示分頁），前端本地分頁（後端仍全量回傳，量大再獨立後端分頁）。
- 將參照資料管理中的「作物」與「病蟲害分類」獨立為兩個管理頁，Navbar 置於送件人管理之後、參照資料管理之前，標籤分別為 `作物管理` 與 `害物管理`；原參照資料管理頁保留其餘類別（damages/hints/methods/deliveries/services/identifiers/senderTypes/cropCategories 等）。
- 導覽與路由更新：`App.vue` 與 `router/index.ts` 新增 `/admin/crops` 與 `/admin/pest-categories`（或複用既有元件但分流），Navbar 順序：儀表板 / 案件管理 / 送件人管理 / 作物管理 / 害物管理 / 參照資料管理 / 使用者管理。
- 作物管理權限下放至 STAFF 可檢視與新增/編輯，但刪除按鈕僅 ADMIN 可見（`v-if="auth.isAdmin"`），後端 `ReferenceDataAdminController` 作物相關端點 `@PreAuthorize` 改為 `hasAnyRole('STAFF','ADMIN')` 供寫入，刪除仍 `hasRole('ADMIN')`。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `sender-management`: 送件人管理頁分頁（>20 筆顯示）。
- `reference-data-admin`: 參照資料管理頁分頁；作物與害物分類獨立為作物管理/害物管理並調整 Navbar 順序與標籤；作物管理權限下放至 STAFF（刪除除外）。

## Impact

- 前端：`frontend/src/views/SendersView.vue`、`frontend/src/views/ReferenceDataAdminView.vue`（分頁）、新增 `frontend/src/views/CropManagementView.vue` 與 `PestManagementView.vue` 或複用並分流、`frontend/src/router/index.ts`、`frontend/src/App.vue`（Navbar 標籤與順序）。
- 後端：`backend/src/main/java/com/d0w0b/phytotrack/controller/ReferenceDataAdminController.java` 作物端點權限註解（`createCrop/updateCrop` 改 STAFF+），其餘刪除權限維持 ADMIN。
- 文件：`docs/manual.typ` 導覽說明視需要更新。
