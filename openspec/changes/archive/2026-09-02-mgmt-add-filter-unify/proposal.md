## Why

管理頁的新增入口與篩選呈現不一致：送件人缺少新增按鈕、案件「建立案件」命名與其他管理不一致、按鈕樣式與位置不統一、作物/害物篩選缺抽屜，影響操作一致性與可用性。

## What Changes

- 送件人管理頁面標題列新增「新增」按鈕（`v-if="auth.isStaff"`，`btn-success`），與案件管理一致。
- 案件管理「建立案件」更名為「新增」（`CasesView.vue:398` 按鈕文字，路由與權限不變）。
- 所有管理頁新增區塊按鈕統一樣式與位置：右上角 `d-flex justify-content-between` 標題列，`新增` 為 `btn-sm btn-success`，與篩選按鈕同列，大小顏色一致。
- 作物管理加入篩選抽屜（`showFilter` 預設收合，`分類` 單欄篩選，`v-show` + `aria-expanded`），如同案件/送件人。
- 害物管理加入篩選抽屜（`showFilter` 預設收合，`類型` 與 `名稱/代碼` 篩選，`v-show`）。
- 移除害物分類 `sort_order` 欄位（前端表格與表單移除排序輸入，後端 `PestCategory` 實體、`pest_categories.sort_order` 欄位與 `sortOrder` 參數同步移除，改以 `pestCategoryCode desc` 排序）。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `sender-management`: 送件人管理新增按鈕與篩選抽屜統整。
- `case-search`: 案件管理按鈕更名與抽屜一致性。
- `reference-data-admin`: 作物/害物篩選抽屜與按鈕統整（作物/害物獨立頁）。

## Impact

- 前端：`frontend/src/views/SendersView.vue`（新增按鈕）、`CasesView.vue`（更名）、`CropManagementView.vue`/`PestManagementView.vue`（抽屜，害物移除排序欄位/輸入）、`ReferenceDataAdminView.vue`/`UsersView.vue`（按鈕樣式對齊）。
- 後端：`backend/src/main/java/com/d0w0b/phytotrack/models/PestCategory.java`（移除 `sortOrder`）、`ReferenceDtos`/`ReferenceDataService`/`ReferenceDataAdminController`（移除 `sortOrder` 參數）、`schema.sql` 與 `PestCategory` 資料表 `sort_order` 欄位，改以 `pest_category_code desc` 排序。
