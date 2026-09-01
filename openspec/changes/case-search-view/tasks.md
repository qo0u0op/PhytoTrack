## 1. 後端視圖與 DTO

- [x] 1.1 `schema.sql` 新增 `DROP VIEW IF EXISTS v_case_search` + `CREATE VIEW v_case_search`（`LEFT OUTER JOIN` 全表，單值直投，多對多 `GROUP_CONCAT(DISTINCT name, '、')` 頓號聚合，`GROUP BY c.case_id`），驗證 `sqlite3 diagnoses.db "SELECT sql FROM sqlite_master WHERE type='view'"` 可見
- [x] 1.2 `dto/CaseDtos.CaseFilter` 擴 14 欄：`senderQuery`（取代 `senderName` 保留相容）、`cityId/districtId/cropCategoryId/pestTypeId/pestCategoryId/hintId/deliveryId`（`Long` 可空）+ `isEmpty()/empty()` 同步，驗證 `mvn compile` 通過
- [x] 1.3 新增 `models/CaseSearchView`（`@Entity @Immutable @Table("v_case_search")`，`case_id` 為 `@Id`）與 `repository/CaseSearchViewRepository`（`JpaSpecificationExecutor<CaseSearchView>`），驗證 `CaseRepositoryTest` 可 `findAll` 視圖

## 2. 後端篩選與控制器

- [x] 2.1 `repository/CaseSpecifications` 新增 `buildView(CaseFilter, CaseStatus)`：`senderQuery` 對 `sender_name/display_name/phone` 三欄 `OR LIKE`（跳脫 `%/_`）、`cityId/districtId/cropCategoryId/serviceId/deliveryId` 為 `equal`、`pestTypeId/pestCategoryId/hintId` 為 `EXISTS` 子查詢，驗證 `CaseServiceTest` 模擬 `viewRepo` 分頁
- [x] 2.2 `service/CaseService.list()` 改先查視圖分頁取 `case_id` 再 `caseRepository.findAllById` 回補 `toSummary`（保留 `isViewer()` 遮蔽），`exportCsv` 同切視圖，驗證 `view` 分頁 `totalElements` 與實表一致
- [x] 2.3 `controller/CaseController.list/export` 新增 `@RequestParam(required=false) Long cityId, districtId, cropCategoryId, pestTypeId, pestCategoryId, hintId, deliveryId, String senderQuery`（`senderName` 保留相容轉 `senderQuery`），組新 `CaseFilter`，驗證 `CaseControllerTest` 透傳 14 參數

## 3. 前端篩選卡

- [x] 3.1 `views/CasesView.vue` `CaseFilters` 擴 14 欄、`filters` 新增 7 欄、`loadFilterOptions()` 併取 `refApi.cities()/cropCategories()/pestTypes()/hints()/deliveries()`，`caseApi.list/exportCsv` 同步參數，`clearFilters()` 重置 14 欄
- [x] 3.2 篩選卡 4 列重排：`送件人(三欄合一)`/`服務`/`送件方式`/`作物類別`/`作物`；`縣市→鄉鎮`（鄉鎮 `disabled` 直至縣市選定，`watch` 聯動同 `CaseFormView.vue:149`，**必先選縣市**）；`害物→害物類別`（類別依害物聯動）；`建議類別`（`hintId`）/日期/狀態，驗證 `VIEWER` 仍禁用送件人
- [x] 3.3 多對多顯示沿用頓號聚合欄（視圖 `pest_category_names/hint_names` 備用），列表 `senderLabel` 與 `VIEWER` 遮蔽不變

## 4. 測試與文件

- [x] 4.1 後端：`CaseSearchViewRepositoryTest` 驗 `LEFT JOIN` 空值回 `null`、`GROUP_CONCAT` 頓號；`CaseServiceTest` 覆蓋 `senderQuery=phone/displayName`、`cityId→district`、`pestType→pestCategory`、`hintId/deliveryId` 組合；`PhytoTrackIntegrationTest` 端到端 `senderQuery`/`deliveryId`/`district` 必選
- [x] 4.2 前端：`vitest` 覆蓋篩選卡縣市必選與參數透傳，`npm run build` 通過
- [x] 4.3 `docs/ARCHITECTURE.md` 補視圖說明、`openspec validate --specs --changes` 通過；`dashboard` 視圖另案 `propose`（本次僅保留 `v_case_search` 命名與重用性）
