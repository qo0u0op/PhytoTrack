## Why

現有參照資料（作物、病蟲害、服務方式等）僅能透過種子資料或直接改 DB 維護，管理者無法在線上新增/修改/刪除，導致試驗新作物或調整選項需工程介入。依 `openspec/specs/reference-data-admin/spec.md` 需補齊管理者在線上維護參照資料的能力，並保障「被案件引用者不可刪」。

## What Changes

- 後端新增 ADMIN 專用參照資料寫入端點（`@PreAuthorize("hasRole('ADMIN')")`）：
  - `POST /api/admin/ref/{type}` 新增、`PUT /api/admin/ref/{type}/{id}` 修改、`DELETE /api/admin/ref/{type}/{id}` 刪除
  - 涵蓋：`crops`/`crop-categories`、`pest-categories`、`damages`、`hints`、`methods`、`deliveries`、`services`、`identifiers`、`sender-types` 等（與 `ReferenceDataService` 讀取面對應）
  - 刪除前檢查是否被 `cases` 相關表引用（`case_damages`、`case_hints` 等及 `cases.crop_id` 等 FK），已被引用回 409 `REFERENCE_IN_USE`
- `ReferenceDataService` 新增 `create/update/delete` 方法與 `IdName` 類 DTO 驗證（名稱必填、唯一性依現有 DB 約束）
- `ReferenceDataAdminController`（或擴充 `ReferenceDataController`）承載寫入端點，與唯讀 `ReferenceDataController` 職責分離
- 前端新增 `frontend/src/views/ReferenceDataAdminView.vue`（路由 `/admin/reference-data`，限 ADMIN），提供分頁籤列表與新增/編輯/刪除表單；`frontend/src/api/index.ts` 補 `refAdminApi`
- `frontend/src/types/api.ts` 重新生成

## Capabilities

### New Capabilities

<!-- 主規格已含 reference-data-admin，本 change 採 skip_specs: true，不新增 capability 檔案 -->

### Modified Capabilities

<!-- 本 change 不修改 spec 需求，僅實作主規格已定義的 reference-data-admin 需求 -->

## Impact

- 後端：`controller/ReferenceDataAdminController.java`、`service/ReferenceDataService.java`、`repository/*`（既有）、`dto/ReferenceDtos.java`（新增 Create/Update 請求 DTO）、`models/*`（既有）、`exception/ApiException`（新增 `REFERENCE_IN_USE`）
- 前端：`views/ReferenceDataAdminView.vue`、`router/index.ts`（新增 admin 路由與守衛）、`api/index.ts`、`types/api.ts`
- 文件與測試：`docs/ARCHITECTURE.md`（補端點）、`openspec validate`、新增 `ReferenceDataAdminControllerTest` 與整合測試
