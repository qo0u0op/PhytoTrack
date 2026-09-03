## MODIFIED Requirements

### Requirement: 診斷簽名人關聯使用者

每位 `ROLE_STAFF` 與 `ROLE_ADMIN` 使用者 SHALL 擁有至少一個以其 `displayName`（若空則 `username`）命名的 `Identifier`（診斷簽名人），`Identifier.user` SHALL 指向該使用者；`VIEWER` 不強制。系統 SHALL 於使用者提權至 `STAFF|ADMIN` 與 `displayName` 變更時同步建立或更名首個 `active` 關聯 Identifier，並於交易內完成；刪除使用者 SHALL 不自動刪除其 Identifier。`Identifier` SHALL 具 `active`（`boolean`，預設 `true`）用於軟停用，停用後 SHALL 從 `GET /api/ref/identifiers` 預設結果中過濾（管理頁以 `?includeInactive=true` 可見），但仍可被既有案件以 `id` 引用（刪除保護不變）。**1.** 種子 `張志明/林雅惠/陳建宏` SHALL 不再預填（`schema.sql` 與 `DataInitializer` 移除，`**BREAKING**`）。**2.** `identifier_id` SHALL 為 `INTEGER PRIMARY KEY` 而非 `AUTOINCREMENT`，空庫首筆為 `1`，重建需 `DELETE FROM identifiers` 後由提權/首建自動重建。**3.** `DELETE /api/admin/ref/identifiers/{id}` SHALL 禁用（回 `405`），僅容 `PATCH .../active` 啟停用；停用後 `GET /api/ref/identifiers` 預設不含該筆，故新增案件對應簽名人自動移除。**6.** 案件表單內新建簽名人 SHALL 默認為 `user IS NULL`（`signer but not user`）。**8.** 簽名人 SHALL 從參照資料管理獨立為 `/signers` 導覽且置於 `參照資料管理` 之前（Navbar 順序：… / 簽名人管理 / 參照資料管理 / …），列表 SHALL 顯示 `active` 與 `類型: user as signer / signer but not user`（`user_id != null` 判斷）。`user as signer`（`user_id != null`）的名稱 SHALL 以 `User.displayName` 為唯一真相源，`PUT /api/admin/ref/identifiers/{id}` 對此類 SHALL 拒絕直改（`409 USER_LINKED_SIGNER_IMMUTABLE`），僅容 `PATCH .../active`；`signer but not user`（`user_id == null`）則 `ADMIN` 可 `PUT` 更名、`PATCH active`。

#### Scenario: Staff 建立時自動建立簽名人
- **WHEN** ADMIN 將新註冊使用者授權為 `ROLE_STAFF`
- **THEN** `identifiers` 新增一筆 `identifier = displayName` 且 `user_id` 指向該使用者

#### Scenario: 修改顯示名稱同步更名簽名人
- **WHEN** STAFF 透過帳號管理將 `displayName` 由「診斷員A」改為「診斷員B」
- **THEN** 其關聯的 `active` 首個 `Identifier.identifier` 同步更新為「診斷員B」

#### Scenario: 已有簽名人不重複建立
- **WHEN** 已擁有簽名人的使用者再次變更非顯示名稱欄位（如 email）
- **THEN** 不新增 `Identifier`，僅在 `displayName` 變更時更名

#### Scenario: 刪除使用者保留簽名人
- **WHEN** ADMIN 刪除一名 STAFF 使用者
- **THEN** 其關聯 `Identifier` 保留，後續仍可被案件引用與刪除保護（`existsByCaseIdentifiersIdentifierIdentifierId`）

#### Scenario: 種子不再預填且 ID 重設
- **WHEN** 空庫首次啟動（`DataInitializer` 已移除 3 筆）
- **THEN** `identifiers` 為空，首個提權建立者 `identifier_id = 1`，不再有 `張志明/林雅惠/陳建宏`

#### Scenario: user as signer 禁直改名稱
- **WHEN** ADMIN 對 `user_id != null` 的簽名人呼叫 `PUT /api/admin/ref/identifiers/{id}` 改名
- **THEN** 回 `409` `USER_LINKED_SIGNER_IMMUTABLE`，`identifier` 保持原值

#### Scenario: signer but not user 可直改名稱
- **WHEN** ADMIN 對 `user_id == null` 的簽名人呼叫 `PUT /api/admin/ref/identifiers/{id}` 改名
- **THEN** 回 `200` 且名稱更新

#### Scenario: 刪除禁用僅停用
- **WHEN** ADMIN 對任意簽名人呼叫 `DELETE /api/admin/ref/identifiers/{id}`
- **THEN** 回 `405 Method Not Allowed`，需改 `PATCH .../active { active:false }`

#### Scenario: 停用後預設過濾
- **WHEN** ADMIN 將某簽名人 `PATCH .../active` 設 `active=false`
- **THEN** `GET /api/ref/identifiers` 預設不再含該筆，`?includeInactive=true` 才可見

#### Scenario: 獨立導覽與類型欄位
- **WHEN** 使用者開啟簽名人管理頁 `/signers`
- **THEN** 見獨立導覽（置於 `參照資料管理` 之前）且列表含 `類型` 欄顯示 `user as signer` 或 `signer but not user` 與 `active` 狀態

#### Scenario: 識別簽名人清單可見關聯
- **WHEN** 以 `STAFF` 身分呼叫 `GET /api/identifiers`
- **THEN** 回傳清單中每筆含 `identifier` 名稱且後端可透過 `findByUserUserId` 定位當前使用者之簽名人

#### Scenario: Staff 自助停用 typo
- **WHEN** STAFF 對自身首個 `user as signer` 呼叫 `PATCH /api/ref/identifiers/{id}/active` 設 `false`（typo 自清）
- **THEN** 回 `200` 且該筆轉為停用，後續建案預選不再出現

#### Scenario: Staff 不可停用他人簽名人
- **WHEN** STAFF 對非自身的 `user as signer` 呼叫 `PATCH .../active`
- **THEN** 回 `403`，僅 `ADMIN` 可停用他人

#### Scenario: 案件內新建默認為非 user
- **WHEN** STAFF 於 `POST /api/cases` 以 `inlineIdentifiers: [{ name: "新簽名人" }]` 提交
- **THEN** 新建 `identifiers` 之 `user_id` 為 `null` 且 `active=true`，可被停用

## ADDED Requirements

### Requirement: 簽名人綁定與同名去重

當 `PATCH /api/admin/users/{id}/role` 提權 `VIEWER → STAFF|ADMIN` 且 `User.displayName` 已存在 `active` 的 `signer but not user`（`user IS NULL` 且 `identifier == displayName`）時，系統 SHALL 回 `409 SIGNER_NAME_CONFLICT { existingIdentifierId }` 並提示是否綁定；呼叫 `POST /api/admin/ref/identifiers/{existingId}/bind { userId }` SHALL 將該簽名人 `user_id` 更新為該使用者（轉為 `user as signer`），否則走自動新建允許同名多筆，後續以 `active` 收斂。

#### Scenario: 提權撞名提示綁定
- **WHEN** ADMIN 將 `displayName=王小明` 的 VIEWER 提權為 STAFF 且庫中已有一筆 `user IS NULL, identifier=王小明, active=true`
- **THEN** 提權回 `409 SIGNER_NAME_CONFLICT` 並附 `existingIdentifierId`

#### Scenario: 綁定既有簽名人
- **WHEN** ADMIN 確認綁定並呼叫 `POST /api/admin/ref/identifiers/{existingId}/bind { userId }`
- **THEN** 該簽名人 `user_id` 更新為 `userId`，`GET /api/ref/identifiers` 類型轉為 `user as signer`

#### Scenario: 不綁定允許新建
- **WHEN** ADMIN 於撞名提示選取消
- **THEN** 走自動新建，庫中產生第二筆同名 `identifier=王小明` 但 `user_id` 指向新使用者，兩筆並存

### Requirement: 簽名人 ID 非自動遞增

`identifiers.identifier_id` SHALL 為 `INTEGER PRIMARY KEY`（移除 `AUTOINCREMENT`），重建前 SHALL 允許 `DELETE FROM identifiers` 後首筆重回 `1`（透過 `DELETE FROM sqlite_sequence`），文件 SHALL 標註 `id` 非穩定外部引用。

#### Scenario: 重設後首筆為 1
- **WHEN** 管理員清空 `identifiers` 並 `DELETE FROM sqlite_sequence WHERE name='identifiers'`
- **THEN** 下一筆新建簽名人 `identifier_id = 1`
