## Why

案件管理的篩選卡片目前僅支援 `cropId/serviceId/senderName/receiveDateFrom/To/status` 6 項，且 `senderName` 僅比對 `senders.name` 單欄，無法滿足實務上以電話/顯示名稱（Line/FB 暱稱）定位送件人的需求。隨著參照維度增加（縣市/鄉鎮、作物類別、害物/害物類別、送件方式、建議類別），`CaseSpecifications` 以 `JOIN/EXISTS` 逐欄疊加的作法將使 `CaseService.list()` 的 `Specification` 複雜度與 N+1 風險升高，且與 `CSV` 匯出、`dashboard` 統計共用的篩選語意難以一致。故需以 `CREATE VIEW` 承接複雜業務，將多表與多對多聚合收斂至單一視圖。

## What Changes

- **篩選維度擴充（`AND` 組合）**：
  - 送件人：`name/displayName/phone` 三欄 `OR LIKE`（沿用 `CaseSpecifications` 跳脫 `%/_` 邏輯）
  - 服務類別：既有 `serviceId` 保留
  - 送件方式：新增 `deliveryId`（`deliveries.deliver_id`）
  - 縣市/鄉鎮：`cityId/districtId`（`senders → districts → cities`，前端必先選縣市才啟用鄉鎮）
  - 作物類別：`cropCategoryId`（`crops → crop_categories`，如 糧食/雜糧）
  - 害物：`pestTypeId`（`pest_types`，如 病害/蟲害，經 `case_pest_categories → pest_categories`）
  - 害物類別：`pestCategoryId`（`pest_categories`，如 真菌/細菌）
  - 建議類別：`hintId`（`hints`，經 `case_hints`，即 `hint_id`）
- **視圖承接**：`CREATE VIEW v_case_search` 以 `LEFT OUTER JOIN` 涵蓋所有可空關聯，單值欄直接投影，多對多以 `GROUP_CONCAT(DISTINCT name, '、')` 頓號聚合供列表顯示，篩選仍以 `EXISTS` 子查詢精確匹配。
- **重用性**：視圖命名 `v_case_search` 獨立，`dashboard` 後續將另案 `propose` 共用或衍生 `v_case_dashboard`，本次不含統計邏輯。

## Capabilities

### New Capabilities

<!-- skip_specs: true，不新增 capability 檔案 -->

### Modified Capabilities

<!-- case-search 能力篩選參數擴充，屬實作層擴充，不另立 delta spec -->

## Impact

- 後端：`schema.sql` 新增 `v_case_search`、`dto/CaseDtos.CaseFilter` 擴 14 欄（`senderQuery/cityId/districtId/cropCategoryId/pestTypeId/pestCategoryId/hintId/deliveryId`）、`models/CaseSearchView`（`@Immutable`）+ `repository/CaseSearchViewRepository`、`CaseSpecifications` 新增 `buildView()`、`CaseService.list/exportCsv` 改先查視圖再回補 `Case` 實體、`CaseController` 新增 8 個 `@RequestParam`
- 前端：`views/CasesView.vue` 篩選卡擴 4 列、縣市→鄉鎮聯動、`loadFilterOptions()` 併取 `cities/cropCategories/pestTypes/hints/deliveries`、`caseApi.list/exportCsv` 同步參數
- 文件：`docs/ARCHITECTURE.md` 視圖說明、`openspec` 歷史保留
