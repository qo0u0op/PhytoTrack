## 1. 資料層與簽名人服務基座

- [x] 1.1 新增 `IdentifierRepository.findByUserUserId` 與 `findByIdentifier` 方法並以 `Caffeine` 快取無需，驗證 `mvn test -Dtest=IdentifierRepositoryTest` 或手動 `findByUserUserId` 回傳正確且空時為空集合
- [x] 1.2 實作 `service/IdentifierService.java`（或擴充 `ReferenceDataService`）之 `ensureForUser(User)`：`findByUserUserId` 有則取首筆（多筆時首筆為主），`displayName` 變更則同步更名，無則以 `displayName` 新建並 `save`，交易內完成，驗證單元測：無簽名人時新建、已有時不重建、更名時同步

## 2. 使用者—簽名人同步點

- [x] 2.1 於 `AccountService.updateProfile` 內於 `user.setDisplayName` 後呼叫 `identifierService.ensureForUser` 並同步更名，驗證 `MockMvc PUT /api/account/profile` 改名後 `GET /api/identifiers` 含新名
- [x] 2.2 於 `UserAdminService`（或 `service/UserService`）之「調整角色/啟用」與 `AuthService` 相關授權路徑中，當角色變為 `STAFF/ADMIN` 且無簽名人時呼叫 `ensureForUser`，驗證 `PATCH /api/admin/users/{id}/role` 將 VIEWER 升 STAFF 後 `findByUserUserId` 有值
- [x] 2.3 擴充 `DataInitializer` 啟動補建：對所有 `ROLE_STAFF/ADMIN` 使用者若 `findByUserUserId` 空則各補一筆，驗證 `dev` 啟動日誌或 `GET /api/identifiers` 數量不少於 STAFF/ADMIN 人數

## 3. 案件自動帶入簽名人

- [x] 3.1 修改 `CaseService.createCase` 與 `updateCase`：於 `@AuthenticationPrincipal` 取得 `currentUser` 後，若 `request.identifierIds()==null || isEmpty()` 則取 `auto = identifierService.ensureForUser(currentUser)` 並以 `List.of(auto.getIdentifierId())` 替換請求（有值則原樣），驗證整合測：STAFF 以 `identifierIds: []` 建案後 `GET /api/cases/{id}` 的 `identifiers` 含己名
- [x] 3.2 補 `updateCase` 空陣列語意：`identifierIds != null && isEmpty()` 觸發自動帶入，`null` 保留原值（同 `damageIds` 判斷），驗證 `PUT /api/cases/{id} identifierIds: []` 自動替換且 `null` 保留
- [x] 3.3 保留多簽名人能力：請求已含 `[2,3]` 時不增補當前使用者，驗證 `POST /api/cases identifierIds:[2,3]` 仍恰為 2 筆且不含自動簽名人

## 4. API 與前端預選

- [x] 4.1 新增 `GET /api/identifiers/me`（`ReferenceDataController`，需 `STAFF+`），回當前使用者關聯簽名人（`ensureForUser` 後），若 VIEWER 無簽名人回 `404`，驗證 `MockMvc GET /api/identifiers/me` 含己名且未登入 `401`
- [x] 4.2 修改 `CaseFormView.vue`：`onMounted` 先調 `GET /api/identifiers/me` 失敗回退 `GET /api/identifiers` 並以 `userId` 或 `displayName` 比對預選，若 `identifierIds` 模型為空則賦值為 `[myIdentifierId]`，仍允許手動增刪，驗證前端 `npm run dev` 開啟新增頁預設勾選當前使用者且可取消

## 5. 管理與文件

- [x] 5.1 `ReferenceDataAdminView` 顯示 Identifier 關聯使用者（若 API 回 `userId/displayName` 則表格新增欄位），驗證 ADMIN 進入識別簽名人管理頁可見關聯欄位
- [x] 5.2 更新 `docs/ARCHITECTURE.md` §資料模型與 §認證授權（Identifier—User 關聯與自動帶入）與 `docs/DEPLOY.md`（若有簽名人同步說明），驗證 `grep -rn Identifier docs` 與 `application.yaml` 一致

## 6. 驗證與回歸

- [x] 6.1 撰寫整合測試 `CaseSignerAutoFillTest`：STAFF 空清單建案自動帶入、已選清單不覆蓋、無簽名人即時建立、更新空清單自動帶入，驗證 `mvn test -Dtest=CaseSignerAutoFillTest` 全綠
- [x] 6.2 執行 `cd backend && mvn test` 全回歸與 `cd frontend && npm run build`（含 `vue-tsc`），驗證既有 `CaseControllerTest` / `ReferenceDataAdminControllerTest` 不因簽名人補建或自動帶入而失敗
- [x] 6.3 執行 `openspec validate --specs --changes --strict` 與 `openspec status --change case-signer-auto-fill`，驗證無錯誤且四件製品皆 `done`，`logs/` 仍 gitignore
