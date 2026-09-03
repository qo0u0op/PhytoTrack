## 1. 模型與資料層

- [x] 1.1 移除 `schema.sql` 3 筆 `identifiers` 種子並將 `identifier_id` 改 `INTEGER PRIMARY KEY`（去 `AUTOINCREMENT`），`models/Identifier.java` 同步，`DataInitializer` 刪 3 筆並加 `ensureGenericSigner` 遷移為 `user=null active=true` 或 `active=false`，驗證 `sqlite3 diagnoses.db "SELECT sql FROM sqlite_master WHERE name='identifiers'"` 無 `AUTOINCREMENT` 且空庫首筆 `id=1`
- [x] 1.2 `repository/IdentifierRepository` 增 `findByIdentifierAndActiveTrueAndUserIsNull` 與 `findByUserIsNull` 等，`IdentifierService` 調整 `ensureForUser` 以 `displayName/username` 為準且不以角色名，驗證 `mvn test -Dtest=IdentifierServiceTest` 或手動 `ensureForUser` 以帳號名建

## 2. 生命週期與綁定

- [x] 2.1 `service/ReferenceDataService` 禁 `DELETE`（改 `405`）並保留 `createIdentifier` 默認 `user=null active=true`，`updateIdentifier` 對 `user!=null` 拋 `409 USER_LINKED_SIGNER_IMMUTABLE`，驗證 `MockMvc DELETE` 回 `405`、`PUT user as signer` 回 `409`、`PUT signer but not user` 回 `200`
- [x] 2.2 `service/IdentifierService.bindToUser` 與 `controller/ReferenceDataAdminController POST /admin/ref/identifiers/{id}/bind`（限 `ADMIN`）實現 `user IS NULL → user_id=userId`，驗證 `MockMvc POST bind` 後 `GET ?includeInactive=true` 類型轉 `user as signer`
- [x] 2.3 `service/AuthService` 與 `UserAdminController` 提權時檢查 `displayName` 撞 `signer but not user` (`active=1`)，撞名回 `409 SIGNER_NAME_CONFLICT` 帶 `existingIdentifierId`，否則自動建 `user as signer`，驗證 `PATCH /admin/users/{id}/role` 撞名回 `409` 且前端可綁定
- [x] 2.4 `service/CaseService` 支援 `inlineIdentifiers` 同交易原子建 `user IS NULL`（復用同名 `active`），`GET /ref/identifiers` 已過濾停用，驗證 `POST /cases` 含 `inlineIdentifiers` 建非 user 且放棄不落庫、停用後不現於 `CaseFormView`

## 3. 導覽與前端

- [x] 3.1 新 `frontend/src/views/SignersView.vue` 獨立頁，`router` 與 `Navbar.vue` 將 `簽名人管理` 置於 `參照資料管理` 之前，列表欄含 `ID/名稱/類型/狀態/操作`（類型由 `userId != null` 判斷），複用 `ReferenceDataService` 的 `includeInactive`，驗證 `npm run dev` 導覽順序正確且 `SignersView` 可見類型欄
- [x] 3.2 調整 `ReferenceDataAdminView.vue` 隱藏或導向簽名人籤頁、`CaseFormView.vue` 內建新增走 `user=null`、`UsersView.vue` 提權 `409` 彈「綁定」確認並調 `bind`，驗證 `includeInactive` 切換、`PATCH active` 與綁定彈窗

## 4. 驗證與回歸

- [x] 4.1 撰寫整合測試 `SignerOverhaulTest`：移除種子、`id` 重設、`DELETE 405`、停用後不現、提權自動建帳號名、`inline` 非 user、撞名 `409` 與綁定，驗證 `mvn test -Dtest=SignerOverhaulTest` 全綠
- [x] 4.2 執行 `cd backend && mvn test` 全回歸與 `cd frontend && npm run build`（含 `vue-tsc`），驗證既有 `SignerLifecycleTest`、`CaseSignerAutoFillTest` 不受影響
- [x] 4.3 執行 `openspec validate --specs --changes --strict` 與 `openspec status --change signer-overhaul`，驗證無錯誤且四件製品皆 `done`，`logs/` 仍 gitignore
