## Context

見 `proposal.md`。現況 `SendersView` 與 `ReferenceDataAdminView` 無分頁，作物/病蟲害分類混於單一參照頁，`ReferenceDataAdminController` 作物端點僅 ADMIN。

## Goals / Non-Goals

**Goals:**
- 超過 20 筆顯示與案件管理同款分頁。
- 作物/害物獨立頁與 Navbar 重排，標籤 `作物管理/害物管理`。
- 作物管理下放 STAFF（刪除外）。

**Non-Goals:**
- 不改後端分頁 API（前端本地分頁，沿用 `senderApi.list` 與 `refAdminApi` 全量）。
- 不改其他參照類別權限。

## Decisions

### D1. 前端本地分頁（>20 才顯示）

`SendersView` 與 `ReferenceDataAdminView` 各維護 `page/size/pageInput/totalPages`，`filteredSenders` 或類別 `items` 經 `computed pagedItems = filtered.slice((page-1)*size, page*size)`，模板 `v-if="total>20"` 顯示 Controls（同 `CasesView` 的 `onSizeChange/onPageInputConfirm`）。替代「後端 Pageable」需改 Controller/Service，量小不需。

### D2. 獨立頁面複用元件

新增 `CropManagementView.vue` 與 `PestManagementView.vue`，複用 `ReferenceDataAdminView` 的表格/表單邏輯但僅處理單一類別；`ReferenceDataAdminView` 保留其餘類別並移除作物/害物 tab。替代「共用單頁以參數切換」導覽與權限判斷較複雜。

### D3. Navbar 順序與標籤

`App.vue` 順序改為 儀表板/案件管理/送件人管理/作物管理/害物管理/參照資料管理/使用者管理；`router/index.ts` 新增 `/admin/crops`、`/admin/pest-categories` 對應新視圖。標籤 `作物管理/害物管理` 取代原 `作物/病蟲害分類` tab 名。

### D4. 作物權限下放但刪除外

`ReferenceDataAdminController` 的 `createCrop/updateCrop`（及新 `CropManagement` 對應）`@PreAuthorize("hasAnyRole('STAFF','ADMIN')")`，`deleteCrop` 維持 `hasRole('ADMIN')`；前端 `CropManagementView` 的刪除按鈕 `v-if="auth.isAdmin"`。替代「全放 STAFF 含刪除」不符合僅 ADMIN 可刪原則。

## Risks / Trade-offs

- [前端分頁效能] → <1k 筆可接受，後續量大再後端分頁。
- [獨立頁重複程式碼] → 抽 `useReferenceTable` composable 降低重複。

## Migration Plan

- 前端新增路由與視圖，後端改註解，`npm run build` 與 `mvn test` 驗證，`openspec validate` 通過。

## Open Questions

- 無。
