# 提案：案件表單內新增作物／簽名人的原子性（ACID）

## Why

目前 `CaseFormView.vue:365` 的「＋新增作物」與 `CaseFormView.vue:396` 的「＋新增簽名人」直接呼叫 `POST /admin/ref/crops` / `POST /admin/ref/identifiers` 立即落庫，若使用者隨後放棄新增或取消編輯案件，已建立的作物／簽名人仍殘留為孤兒資料，違背預期「放棄案件即放棄其間新建的參照」且污染下拉選單。隨著 `signer-lifecycle` 引入 `active` 軟刪除，孤兒累積更需治理。

## What Changes

- **案件 API 內聯參照建立（主路徑）**：`POST /api/cases` 與 `PUT /api/cases/{id}` 新增可選內聯欄位 `inlineCrop: {name, cropCategoryId}` 與 `inlineIdentifiers: string[]`（或 `inlineIdentifierNames`），當提供時由 `CaseService` 於同一交易內先建立 `Crop`/`Identifier`（`user_id == null` 的獨立簽名人，`active=true`），取得新 `id` 後再關聯至案件並提交；整筆交易成功才可見，失敗則全回滾。
- **前端暫存（staging）**：`CaseFormView.vue` 的「新增」改為僅寫入本地暫存集合（`pendingCrops` / `pendingIdentifiers`），不立即打 API；於 `caseApi.create/update` 時將暫存以 `inlineCrop` / `inlineIdentifiers` 併入請求。放棄（`取消` 導回 `/cases`、`cancelSenderEdit` 無關）則丟棄暫存，不產生後端副作用；已存在的 `refApi.identifiers` / `cropCategories` 下拉僅在案件提交成功後重新載入。
- **後端保留既有單點管理**：`POST /admin/ref/crops` 與 `POST /admin/ref/identifiers` 仍供 `ReferenceDataAdminView` 獨立管理參照資料（非案件流程），與案件內聯路徑共用同一 `ReferenceDataService` 建表邏輯（去重、名稱 `trim`、查重 `409`）。
- **相容與過濾**：內聯建立的作物／簽名人同受 `active` 與名稱校驗；若同名已存在（`Crop` 同分類同名、`Identifier` 同名 `active=true`）則復用既有 `id` 而非重建，避免併發重複。

**非目標**：不改既有 `DELETE` 保護與 `active` 停用語意；不引入前端本地儲存（localStorage）跨頁暫存，僅記憶體暫存，重新整理即丟棄。

## Capabilities

### New Capabilities

<!-- 無新增能力，僅強化既有交易邊界 -->

### Modified Capabilities

- `case-lifecycle`: 擴充案件建立／更新契約，支援內聯參照原子建立，失敗回滾且放棄不落庫
- `reference-data-admin`: 釐清「案件流程內內聯建立」與「管理頁獨立建立」共用建表邏輯，補充去重／復用規則

## Impact

- **後端**：`dto/CaseDtos.java` 增 `inlineCrop` / `inlineIdentifiers` 欄位，`service/CaseService.java` 交易內先調 `ReferenceDataService.createCrop/createIdentifier`（或抽 `IdentifierService`）再關聯，`controller/CaseController.java` 無需新路由；`service/ReferenceDataService.java` 建表方法抽共用並支援事務內重入。
- **前端**：`CaseFormView.vue` 重構 `handleCreateCrop` / `handleCreateIdentifier` 為暫存、`api/index.ts` `caseApi` 增內聯欄位型別、下拉合併 `pending` 與 `remote` 顯示（待提交標記），`ReferenceDataAdminView.vue` 維持直連管理。
- **資料**：無 schema 遷移，僅交易邊界改變；既有孤兒作物／簽名人可由 `ADMIN` 經管理頁 `active=false` 或刪除（未被引用）手工清理。
- **相容性**：既有 `POST /admin/ref/*` 獨立流程不受影響；`POST /api/cases` 舊客戶端不傳內聯欄位則走原 `cropId`/`identifierIds` 路徑，無破壞。
