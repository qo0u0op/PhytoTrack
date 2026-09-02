## Context

見 `proposal.md`。現況送件人無新增入口、案件「建立案件」命名不一致、按鈕樣式散亂、作物/害物篩選常駐佔位。

## Goals / Non-Goals

**Goals:**
- 送件人新增入口、案件更名、按鈕統一樣式與位置、作物/害物抽屜。
- 移除害物分類 `sort_order`，改以 `code desc` 排序。

**Non-Goals:**
- 不改其他參照類別邏輯。

## Decisions

### D1. 送件人新增按鈕

`SendersView` 標題列右側加 `新增`（`v-if="auth.isStaff"`，`btn-sm btn-success`，呼叫既有 `handleCreate` 或導向），樣式與 `CasesView` 一致。替代「無入口」需經參照頁。

### D2. 案件更名

`CasesView.vue:398` 文字 `建立案件 → 新增`，路由與 `@PreAuthorize` 不變。

### D3. 按鈕統一樣式

所有管理頁標題列 `d-flex justify-content-between`，右側 `新增` + `篩選` 同列，`新增` 為 `btn-sm btn-success`。替代各頁散亂樣式。

### D4. 作物/害物抽屜

`CropManagementView` 篩選（分類）與 `PestManagementView` 篩選（類型+關鍵字）外層以 `showFilter` 控制 `v-show`，預設 `false`，按鈕 `aria-expanded` 切換。作物主要篩選為分類，如同案件/送件人。

### D5. 移除 sort_order

後端 `PestCategory.sortOrder`、`pest_categories.sort_order`、`ReferenceDtos.PestCategoryCreateRequest.sortOrder` 移除，`ReferenceDataService` 排序改 `ORDER BY pest_category_code DESC`，前端表格與表單移除 `sortOrder` 欄位與 `swal-pc-order` 輸入。替代「保留欄位」冗餘。

## Risks / Trade-offs

- [抽屜隱藏導致找不到篩選] → 按鈕常駐可見，預設收合節省空間。

## Migration Plan

- 前端移除排序欄位/輸入，後端移除欄位與參數，`schema.sql` 移除 `sort_order` 並重建 `idx`，資料以 `code desc` 重排；`mvn test` 與 `npm run build` 驗證。

## Open Questions

- 無。
