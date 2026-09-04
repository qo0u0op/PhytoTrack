## 1. 模型與資料層

- [x] 1.1 `models/Identifier.java` 增 `boolean active=true`（`@Column(nullable=false)`），`schema.sql` 增 `active BOOLEAN NOT NULL DEFAULT 1`，`repository/IdentifierRepository` 增 `findByActiveTrue` 與 `findByUserUserIdAndActiveTrue` 等，驗證 `mvn test -Dtest=IdentifierRepositoryTest` 或啟動後 `SELECT active FROM identifiers` 預設 `1`
- [x] 1.2 `DataInitializer` 與 `IdentifierService` 補 `active` 初始化（新建時 `true`，既有補建亦 `true`），驗證舊庫啟動後 `active` 全為 `true` 且 `GET /ref/identifiers` 仍全量（未過濾前）

## 2. 後端阻擋與軟停用

- [x] 2.1 `ReferenceDataService.updateIdentifier` 對 `user_id != null` 拋 `409 USER_LINKED_SIGNER_IMMUTABLE`（提示改個人檔案），`user_id == null` 照常更名，驗證 `MockMvc PUT /admin/ref/identifiers/{linkedId}` 回 `409` 且 `PUT {unlinkedId}` 回 `200`
- [x] 2.2 `ReferenceDataService.updateIdentifierActive` 與 `IdentifierService` 增 `active` 切換，`ReferenceDataAdminController PATCH /admin/ref/identifiers/{id}/active`（`ADMIN`）與 `ReferenceDataController PATCH /ref/identifiers/{id}/active`（`STAFF` 限己，`ADMIN` 全量，否則 `403`），驗證 `STAFF` 對己首個可 `PATCH false`，對他人回 `403`，`ADMIN` 對任意回 `200`
- [x] 2.3 `ReferenceDataService.identifiers` 與 `GET /ref/identifiers` 加 `@RequestParam includeInactive=false`，預設回 `findByActiveTrue()`，`includeInactive=true` 回全量，`GET /ref/identifiers/me` 僅回自身首個 `active`（若全停用回 `404` 指引），驗證 `GET /ref/identifiers` 不含已停用，`?includeInactive=true` 含

## 3. 前端管理與選單

- [x] 3.1 `ReferenceDataAdminView.vue` 簽名人頁增「顯示已停用」切換（調 `includeInactive`）與「停用/啟用」按鈕（`PUT` 對 `user as signer` 禁用，`PATCH active` 放行），驗證 `ADMIN` 停用後列表預設消失、勾選「顯示已停用」可見且可啟用
- [x] 3.2 `CaseFormView.vue` 簽名人清單無需改（後端已過濾），驗證新增案件時已停用簽名人不出現，舊案件 `GET /cases/{id}` 仍顯示已停用之歷史 `id` 的名稱

## 4. 驗證與回歸

- [x] 4.1 撰寫整合測試 `SignerLifecycleTest`：`user as signer` 直改 `409`、`signer but not user` 直改 `200`、停用後預設過濾、`STAFF` 自助停用 typo 與不可停他人，驗證 `mvn test -Dtest=SignerLifecycleTest` 全綠
- [x] 4.2 執行 `cd backend && mvn test` 全回歸與 `cd frontend && npm run build`（含 `vue-tsc`），驗證既有 `CaseSignerAutoFillTest` 等不受 `active` 預設過濾影響
- [x] 4.3 執行 `openspec validate --specs --changes --strict` 與 `openspec status --change signer-lifecycle`，驗證無錯誤且四件製品皆 `done`，`logs/` 仍 gitignore
