# Design: reference-data-admin 參照資料維護

## Context

見 `proposal.md`（Why）。現況：`ReferenceDataController` 僅 `GET /api/ref/*` 唯讀，資料來自 `ReferenceDataService` 對 `Crop`/`PestCategory`/`Damage` 等表的直接映射；種子資料由 `DataInitializer` 寫入。`Case` 透過 FK 與 junction 表引用參照資料（`cases.crop_id`、`case_damages` 等），刪除被引用資料會破壞案件完整性，故需阻擋。

本 change 需在不破壞既有唯讀契約的前提下，補齊 ADMIN 寫入能力，並滿足「被引用不可刪」。

## Goals / Non-Goals

**Goals：**
- ADMIN 可對主要參照資料（作物/作物分類、病蟲害小分類、被害部位、防治建議、耕種方式、送達方式、服務類別、診斷簽名人、送件人身分別）執行新增/修改/刪除，刪除被引用時 409
- 前端提供 ADMIN 專用管理頁（列表 + 表單）

**Non-Goals：**
- 縣市/鄉鎮市區（`cities`/`districts`）的寫入（地理主檔，異動極少，本次不做）
- 參照資料的排序/啟停用、匯入匯出、批次操作
- 參照資料的版本化或稽核日誌

## Decisions

1. **控制器職責分離**
   - 新增 `ReferenceDataAdminController`（`@RequestMapping("/api/admin/ref")`）承載寫入，`ReferenceDataController` 續留唯讀。共用 `ReferenceDataService`。
   - 理由：讀寫權限分離（`GET` 登入即可，寫入限 ADMIN），`@PreAuthorize` 在類別層統一，避免在同一控制器混合兩種安全語意。

2. **端點收斂 vs 分散**
   - 採分散式明確端點：`POST /api/admin/ref/damages`、`PUT /api/admin/ref/damages/{id}`、`DELETE /api/admin/ref/damages/{id}`（餘類推），而非 `POST /{type}` 通用型別參數。
   - 理由：型別明確利於 OpenAPI 型別生成與 Bean Validation，且各表的 DTO 欄位略有差異（`Crop` 需 `cropCategoryId` 等）。通用 `{type}` 需執行期分派與大量 `switch`，可讀性與型別安全較差。量少（約 9 類）下重複可接受。

3. **刪除保護實作**
   - `ReferenceDataService.delete*` 內先查引用：`CaseRepository.existsByCropId` / `CaseDamageRepository.existsByDamageId` 等（或 `countBy*`），若存在拋 `ApiException("REFERENCE_IN_USE", CONFLICT, "已被案件引用，無法刪除")`。
   - 替代方案捨棄：依賴 DB FK `RESTRICT` 拋 `DataIntegrityViolationException` 再轉 409 —— 錯誤訊息不友善且需解析方言；先查後刪可回明確 409。
   - 對 `CropCategory` 刪除需檢查其下 `Crops` 是否存在或是否被引用，採取同樣阻擋。

4. **DTO 與驗證**
   - 新增 `CreateDamagedRequest` 等或共用 `IdNameCreateRequest(@NotBlank name)`、`IdNameUpdateRequest`；`CropCreateRequest` 附加 `cropCategoryId(@NotNull)`。由 `GlobalExceptionHandler` 統一轉 `VALIDATION_ERROR` 400。

5. **前端**
   - 新增 `views/ReferenceDataAdminView.vue`，路由 `/admin/reference-data`（`meta: { requiresAdmin: true }`），以頁籤切換類型（Damages/Hints/Methods...），每類為表格 + 新增/編輯彈窗 + 刪除確認。
   - `api/index.ts` 新增 `refAdminApi`（`createDamage`/`updateDamage`/`deleteDamage` 等），與 `refApi` 讀取分離。
   - 導覽列對 ADMIN 顯示「參照資料管理」入口。

## Risks / Trade-offs

- 多類型重複 CRUD 樣板 → 以共用 `IdName` 服務方法抽取 `createIdName`/`updateIdName`/`deleteIdName` 降低重複，`Crop`/`PestCategory` 保留獨立方法
- 被引用檢查的競態（檢查後、刪除前被新案件引用）→ 以 DB 事務 + FK `RESTRICT` 作為最終保障，例外再轉 409
- `CropCategory`/`PestCategory` 屬階層式，刪除需連帶檢查子層 → 文件註明「需先清空子層方可刪父層」

## Migration Plan

- 無 schema 變更（沿用既有表與 `UNIQUE` 約束）
- 部署：後端先上線（唯讀端點不受影響）；前端隨後發布
- 回滾：移除 admin 寫入路由即可，唯讀功能不受影響

## Open Questions

- 縣市/鄉鎮是否納入管理？（本次不做，維持唯讀；若需，另立案）
- `Crop` 的 `name` 是否跨分類唯一？（依現有 DB `UNIQUE` 為準，本次不改）
