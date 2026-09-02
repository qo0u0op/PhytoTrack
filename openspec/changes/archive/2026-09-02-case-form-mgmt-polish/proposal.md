## Why

管理頁按鈕次序與送件人新增入口需與最新操作習慣對齊；案件表單中病蟲害選單、卡片標題與欄位 ghost 文案存在命名不一致與視覺干擾，且「診斷結果/建議」資訊分散於兩張卡片，影響填單效率，需一次收斂 8 項微調。

## What Changes

- 管理頁：交換「新增/篩選」按鈕左右位置（作物/害物/送件人）；送件人管理移除「新增」按鈕
- 案件表單：病蟲害分類下拉改升冪（`pest_category_code asc`）；病蟲害明細→診斷結果、害物類型→害物、病蟲害分類→害物因素、新增一列→新增因素；診斷結果後附加灰字「(可增刪多列，同分類可多筆)」；土壤/栽培/用藥紀錄 textarea 移除 ghost placeholder；作物與診斷資訊卡片改名為「作物資訊」；將「建議採取措施」(hintDescription) 與「診斷結果」(pestRows) 由上卡移至下卡，下卡由「防治建議與簽名」改名為「診斷結果與建議」

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `sender-management`: 送件人管理標題列按鈕組合（移除新增、交換次序屬管理頁統一樣式延伸）
- `reference-data-admin`: 作物/害物管理標題列按鈕次序交換；害物分類排序改升冪
- `case-lifecycle`: 案件表單卡片標題、欄位標籤、灰字註解、placeholder 與診斷結果區塊搬移

## Impact

- 前端：`frontend/src/views/SendersView.vue`、`CropManagementView.vue`、`PestManagementView.vue`（按鈕次序/顯隱）、`CaseFormView.vue`（標籤/卡片/欄位搬移/placeholder/排序）、`frontend/src/types/*` 若有生成
- 後端：`backend/src/main/java/com/d0w0b/phytotrack/service/ReferenceDataService.java`、`PestCategoryRepository.java`（排序 asc）、`backend/src/main/resources/schema.sql` 無需異動（僅排序邏輯）
- 風險低，純展示層調整，不影響 API 契約與權限模型
