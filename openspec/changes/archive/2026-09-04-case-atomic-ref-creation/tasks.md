## 1. 後端 DTO 與交易內建表

- [x] 1.1 `dto/CaseDtos.java` 為 `CaseCreateRequest` / `CaseUpdateRequest` 新增可選 `InlineCrop inlineCrop`（`String name`, `Long cropCategoryId`）與 `List<String> inlineIdentifiers`（`@Size` 校驗，`trim` 非空），並更新相容建構，驗證 `mvn test -Dtest=CaseDtosTest` 或編譯通過且 `openapi-typescript` 重生後型別含 `inline*`
- [x] 1.2 `service/ReferenceDataService.java` 抽 `findOrCreateCrop` / `findOrCreateIdentifier` 共用去重邏輯（同分類同名 `active=true` 復用否則新建），驗證單元測 `ReferenceDataServiceTest` 仍綠且 `POST /admin/ref/crops` 同名 `409` 與案件內聯復用一致
- [x] 1.3 `service/CaseService.java` `create/update` 開頭於 `@Transactional` 內處理 `inlineCrop` / `inlineIdentifiers`：先建表取得 `id`（或復用），併入 `cropId` / `identifierIds`（`inline` 覆蓋顯式 `cropId`，`identifiers` 為並集去重），再走原參照校驗與 `add*`，驗證整合測 `POST /api/cases` 含 `inlineCrop` 成功且 `GET /ref/crop-categories` 可見，失敗交易無殘留

## 2. 前端暫存不落庫

- [x] 2.1 `frontend/src/views/CaseFormView.vue` 重構 `handleCreateCrop` / `handleCreateIdentifier` 為 `pendingCrops` / `pendingIdentifiers` 記憶體暫存（不呼叫 `refAdminApi`），下拉由 `computed mergedCrops/mergedIdentifiers` 合併 `remote` 與 `pending`（標記 `（待提交）`），驗證 `npm run dev` 開啟表單新增作物／簽名人後未提交不見於管理頁
- [x] 2.2 `CaseFormView.vue:submit` 將暫存映射為 `inlineCrop` / `inlineIdentifiers` 併入 `caseApi.create/update` 請求體，放棄（`取消`、`router.push /cases`、重新整理）丟棄暫存，成功後清空並 `await refApi.*` 重載，驗證放棄後 `GET /ref/crop-categories` 無該暫存，提交後有且案件關聯正確
- [x] 2.3 `frontend/src/api/index.ts` `caseApi` 型別補 `inlineCrop` / `inlineIdentifiers`，驗證 `npm run build`（含 `vue-tsc`）通過

## 3. 驗證與回歸

- [x] 3.1 撰寫整合測試 `CaseAtomicRefTest`：放棄不落庫、提交才可見、`inline` 與顯式併用、交易失敗全回滾、編輯內聯原子，驗證 `mvn test -Dtest=CaseAtomicRefTest` 全綠
- [x] 3.2 執行 `cd backend && mvn test` 全回歸與 `cd frontend && npm run build`，驗證既有 `CaseControllerTest` / `ReferenceDataAdminControllerTest` / `CaseSignerAutoFillTest` 不因 `inline*` 而失敗
- [x] 3.3 執行 `openspec validate --specs --changes --strict` 與 `openspec status --change case-atomic-ref-creation`，驗證無錯誤且四件製品皆 `done`，`logs/` 仍 gitignore
