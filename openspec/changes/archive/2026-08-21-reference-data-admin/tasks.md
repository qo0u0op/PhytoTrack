## 1. 後端契約（DTO 與驗證）

- [x] 1.1 新增參照資料寫入請求 DTO：`IdNameCreateRequest`（`name` `@NotBlank`）、`IdNameUpdateRequest`、`CropCreateRequest`（`name` + `cropCategoryId @NotNull`）、`PestCategoryCreateRequest` 等，驗證以 `@Valid` 400 為準
- [x] 1.2 擴充 `ReferenceDtos` 或新增 `ReferenceAdminDtos`，並確認 `GlobalExceptionHandler` 對 `VALIDATION_ERROR` 已覆蓋，驗證以 `openapi-typescript` 產物可見為準

## 2. 後端寫入邏輯（Service）

- [x] 2.1 `ReferenceDataService` 新增 `IdName` 通用方法：`createDamage/updateDamage/deleteDamage`、`createHint/updateHint/deleteHint`、`createMethod/updateMethod/deleteMethod`、`createDelivery/updateDelivery/deleteDelivery`、`createService/updateService/deleteService`、`createIdentifier/updateIdentifier/deleteIdentifier`、`createSenderType/updateSenderType/deleteSenderType`，皆 `@Transactional`，`delete` 前查 `Case*Repository.existsBy*` 或 `CaseRepository.existsByCropId` 等，若被引用拋 `REFERENCE_IN_USE` 409，驗證以 `ReferenceDataServiceTest` 或整合查詢為準
- [x] 2.2 實作階層式參照：`createCrop/updateCrop/deleteCrop`（含 `cropCategoryId` 關聯）與 `createCropCategory/updateCropCategory/deleteCropCategory`、`createPestCategory/updatePestCategory/deletePestCategory`，刪除前檢查子層與案件引用，驗證以對應 repository 存在性查詢為準

## 3. 後端控制器

- [x] 3.1 新增 `ReferenceDataAdminController`（`@RequestMapping("/api/admin/ref")`、`@PreAuthorize("hasRole('ADMIN')")` 於類別層），提供各類型的 `POST`/`PUT /{id}`/`DELETE /{id}` 端點，`POST` 回 201、`PUT`/`DELETE` 回 200/204，驗證以 `MockMvc` 401/403/400/409/200 為準
- [x] 3.2 補齊錯誤語意：名稱空白 400、不存在 404、被引用刪除 409、非法 ID 格式 400，驗證以 `ReferenceDataAdminControllerTest` slice test 為準

## 4. 前端（管理頁與 API）

- [x] 4.1 `frontend/src/api/index.ts` 新增 `refAdminApi`（各類型的 `create/update/delete`，路徑與後端一致），驗證以 `npm run build`（`vue-tsc`）通過為準
- [x] 4.2 新增 `frontend/src/views/ReferenceDataAdminView.vue`（路由 `/admin/reference-data`，`meta.requiresAdmin`），以頁籤切換類型，表格 + 新增/編輯彈窗 + 刪除確認，操作後刷新列表，驗證以 `admin` 登入可完成各類型的新增/修改/刪除且 `VIEWER` 不可見為準
- [x] 4.3 更新 `frontend/src/router/index.ts` 與導覽列（`App.vue` 或 `NavBar`），對 `ADMIN` 顯示「參照資料管理」入口，驗證以路由守衛 403/重導為準
- [x] 4.4 重新生成 `frontend/src/types/api.ts`（`npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.ts`），驗證寫入端點與 DTO 出現在型別檔

## 5. 測試

- [x] 5.1 後端切片：`ReferenceDataAdminControllerTest` 覆蓋 ADMIN 200、非 ADMIN 403、未登入 401、名稱空白 400、被引用刪除 409、不存在 404，驗證 `cd backend && ./mvnw test -Dtest=ReferenceDataAdminControllerTest` 通過
- [x] 5.2 後端整合測試：在 `PhytoTrackIntegrationTest`（或新增 `ReferenceDataAdminIntegrationTest`）覆蓋「新增作物後可於表單選用」「刪除被引用的被害部位 409」「刪除未被引用的可成功」，驗證 `./mvnw test` 全量通過
- [x] 5.3 前端驗證：`cd frontend && npm run build`（含 `vue-tsc`）與 `npm test` 通過

## 6. 文件與收尾

- [x] 6.1 同步 `docs/ARCHITECTURE.md`（新增 admin 參照資料端點至 API 一覽）與 `AGENTS.md`/`docs/REQUIREMENTS.md` 標記（僅於 apply 階段），驗證 `openspec validate --specs --changes` 通過
