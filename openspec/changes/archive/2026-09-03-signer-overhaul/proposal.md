# 提案：簽名人重構 — 移除預設、重設 ID、停用為主與自動綁定

## Why

`signer-lifecycle` 已引入 `active` 與 `user as signer / signer but not user` 分流，但仍遺留 8 項不一致：預設 3 筆 `張志明/林雅惠/陳建宏` 佔 `1..3` 且 `DATA_INITIALIZER` 仍依賴、 `identifier_id` `AUTOINCREMENT` 導致停用後 ID 空洞、刪除仍開放致歷史可被硬刪、提權未自動建 `user as signer` 且名稱以權限名而非帳號名、案件內新建簽名人誤綁 `user`、提權時 `displayName` 撞 `signer but not user` 無綁定提示，且簽名人仍寄生於參照資料管理無獨立導覽。需一次重構使簽名人生命週期、ID 策略與綁定語意一致，避免名單膨脹與 `id—name` 漂移。

## What Changes

- **1. 移除預設 3 筆**：**BREAKING** `schema.sql` 初始 `INSERT` 與 `service/DataInitializer.java` 3 筆種子（`張志明/林雅惠/陳建宏`）全量移除，`schema.sql` 不再預填 `identifiers`，既有庫以遷移刪或置 `active=false`（見設計）。
- **2. ID 不再 AUTOINCREMENT、重設**：**BREAKING** `identifiers.identifier_id` 改 `INTEGER PRIMARY KEY`（移除 `AUTOINCREMENT`），空庫重建後首筆為 `1`；提供遷移 `VACUUM` / `DELETE FROM sqlite_sequence` 並建議 `TRUNCATE` 後由提權/首建案件重建，文件標註 `id` 不可作穩定外部引用。
- **3. 禁刪除、僅停用**：`DELETE /api/admin/ref/identifiers/{id}` 改為 `405 Method Not Allowed` 或 Service 拋 `410 USER_LINKED_SIGNER_IMMUTABLE` 替代，僅保留 `PATCH .../active`（`ADMIN` 全量、`STAFF` 僅己），停用後 `GET /api/ref/identifiers` 預設（`includeInactive=false`）不出現，故「停用後移除新增案件的對應簽名人」由前端過濾自然達成，歷史 `case_identifiers` 仍可引用已停用。
- **4. 提權自動註冊**：`user-admin` 提權至 `STAFF|ADMIN`（`PATCH /api/admin/users/{id}/role` 與 `AccountService`）時 `IdentifierService.ensureForUser` 自動建 `user as signer`（若無 `active` 同名則建），`DATA_INITIALIZER` 與註冊流程不再依賴種子。
- **5. 對應帳號名稱而非權限名稱**：`user as signer` 的 `identifier` 取 `User.displayName`（若空則 `username`），不再取 `角色名`（`管理員/診斷員`）；既有以角色名建立者於遷移時更正為當前 `displayName`（若衝突走 7 綁定）。
- **6. 案件內新建默認非 user**：`CaseFormView` 內 `POST /api/cases` 的 `inlineIdentifiers`（`case-atomic-ref-creation`）與 `POST /api/admin/ref/identifiers` 經案件入口者，一律 `user=null`（`signer but not user`），僅提權路徑建 `user as signer`。
- **7. 提權時重名綁定提示**：`PATCH /api/admin/users/{id}/role` 提權前檢查 `User.displayName == identifiers.identifier WHERE user IS NULL AND active=1`，若重名則回 `409 SIGNER_NAME_CONFLICT { existingIdentifierId, displayName }` 並附 `hint: 是否綁定`，前端 `UsersView` 彈「綁定既有簽名人」確認，確認則 `PATCH /api/admin/ref/identifiers/{existingId}/bind { userId }`（新端點）將該 `signer but not user` 轉為 `user as signer`（設 `user_id`、保持 `active`），取消則走 4 建新（允許同名多筆，後續由 `active` 收斂）。
- **8. 獨立導覽與欄位**：簽名人從參照資料管理獨立為 `/signers` 導覽，**Navbar 順序置於「參照資料管理」之前**（即：… / `簽名人管理` / `參照資料管理` / …，`ReferenceData` 與 `Navbar` 解耦），列表增欄位 `類型: user as signer / signer but not user`（`user_id != null` 判斷）與 `active` 狀態，前端 `ReferenceDataAdminView` 拆分子視圖或復用但隱藏於原籤頁，新增 `SignersView.vue` 獨立頁。

**非目標**：不改 `case_identifiers` 歷史快照、不引入 `AUTOINCREMENT` 回退、不開放 `STAFF` 改他人 `user as signer` 名稱。

## Capabilities

### New Capabilities
<!-- 無新增，皆為既有能力重構 -->

### Modified Capabilities
- `reference-data-admin`: 移除預設 3 筆、ID 策略、禁刪僅停用、獨立導覽與 `類型` 欄位、`bind` 端點與 `active` 過濾
- `user-admin`: 提權自動建 `user as signer`、帳號名為準、重名綁定提示
- `case-lifecycle`: 案件內新建簽名人默認 `signer but not user`（原子交易內）

## Impact

- **後端**：`models/Identifier.java`（`active` 已有，`id` 去 `AUTOINCREMENT`）、`resources/schema.sql`（刪 3 筆、改 `identifier_id` 定義）、`repository/IdentifierRepository`（`findByIdentifier` 需處理同名多筆、新增 `findByIdentifierAndActiveTrue`）、`service/DataInitializer.java`（刪種子、加遷移）、`service/IdentifierService.java`（`ensureForUser` 以 `displayName/username` 為準、`bind` 方法）、`service/ReferenceDataService.java`（`createIdentifier` 默認 `user=null`、禁刪、綁定）、`controller/ReferenceDataAdminController.java`（`DELETE` 改 405、`PATCH active` 已有、`POST bind` 新增）、`controller/UserAdminController.java`/`service/AuthService.java`（提權衝突檢查）、`controller/CaseController.java`/`service/CaseService.java`（`inlineIdentifiers` 走 `user=null`）。
- **前端**：`views/ReferenceDataAdminView.vue` 拆分或隱藏簽名人籤頁、`views/SignersView.vue` 新獨立頁（含類型欄位）、`views/CaseFormView.vue` 內建新增走 `user=null`、`views/UsersView.vue` 提權時處理 `409` 綁定彈窗、`api/index.ts` 新增 `bindSigner` 與 `signers` 獨立 API。
- **資料**：**BREAKING** 空洞 ID 不回收，需 `DELETE FROM identifiers` + `DELETE FROM sqlite_sequence WHERE name='identifiers'`（文件化）；既有 3 筆若已使用建議轉 `active=false` 而非硬刪，歷史案件以 `id` 引用不受影響。
- **相容性**：`DELETE` 禁用為 **BREAKING**（前端已切 `PATCH`）；`id` 重設僅影響空庫或手動清空後；提權 `409` 為新增分支，需前端配合。
