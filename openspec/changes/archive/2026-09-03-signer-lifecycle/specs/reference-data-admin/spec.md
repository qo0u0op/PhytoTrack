## MODIFIED Requirements

### Requirement: 診斷簽名人關聯使用者

每位 `ROLE_STAFF` 與 `ROLE_ADMIN` 使用者 SHALL 擁有至少一個以其 `displayName` 命名的 `Identifier`（診斷簽名人），`Identifier.user` SHALL 指向該使用者；`VIEWER` 不強制。系統 SHALL 於使用者建立與 `displayName` 變更時同步建立或更名首個關聯 Identifier，並於交易內完成；刪除使用者 SHALL 不自動刪除其 Identifier。`Identifier` SHALL 具 `active`（`boolean`，預設 `true`）用於軟停用，停用後 SHALL 從 `GET /api/ref/identifiers` 預設結果中過濾（管理頁以 `?includeInactive=true` 可見），但仍可被既有案件以 `id` 引用（刪除保護不變）。`user as signer`（`user_id != null`）的 `identifier` 名稱 SHALL 以 `User.displayName` 為唯一真相源，`PUT /api/admin/ref/identifiers/{id}` 對此類 SHALL 拒絕直改（`409 USER_LINKED_SIGNER_IMMUTABLE`，提示「請改個人檔案顯示名稱」），僅容 `PATCH .../active` 啟停用；`signer but not user`（`user_id == null`）則 `ADMIN` 可 `PUT` 更名、`PATCH active` 與 `DELETE`（未被引用時）全量管理。`STAFF` 對 `user as signer` 僅容自助 `PATCH` 其自身首個簽名人的 `active`（用於 typo 自清），`ADMIN` 可停用任意。

#### Scenario: Staff 建立時自動建立簽名人
- **WHEN** ADMIN 將新註冊使用者授權為 `ROLE_STAFF`（或系統透過 `DataInitializer` 建立 staff）
- **THEN** `identifiers` 新增一筆 `identifier = displayName` 且 `user_id` 指向該使用者

#### Scenario: 修改顯示名稱同步更名簽名人
- **WHEN** STAFF 透過帳號管理將 `displayName` 由「診斷員A」改為「診斷員B」
- **THEN** 其關聯的 `Identifier.identifier` 同步更新為「診斷員B」，案件詳情中歷史簽名仍顯示原字串（若需追溯以案件快照為準）

#### Scenario: 已有簽名人不重複建立
- **WHEN** 已擁有簽名人的使用者再次變更非顯示名稱欄位（如 email）
- **THEN** 不新增 `Identifier`，僅在 `displayName` 變更時更名

#### Scenario: 刪除使用者保留簽名人
- **WHEN** ADMIN 刪除一名 STAFF 使用者
- **THEN** 其關聯 `Identifier` 保留，後續仍可被案件引用與刪除保護（`existsByCaseIdentifiersIdentifierIdentifierId`）

#### Scenario: 識別簽名人清單可見關聯
- **WHEN** 以 `STAFF` 身分呼叫 `GET /api/identifiers`
- **THEN** 回傳清單中每筆含 `identifier` 名稱且後端可透過 `findByUserUserId` 定位當前使用者之簽名人

#### Scenario: user as signer 禁直改名稱
- **WHEN** ADMIN 對 `user_id != null` 的簽名人呼叫 `PUT /api/admin/ref/identifiers/{id}` 改名
- **THEN** 回 `409` `USER_LINKED_SIGNER_IMMUTABLE`，`identifier` 保持原值，提示改個人檔案

#### Scenario: signer but not user 可直改名稱
- **WHEN** ADMIN 對 `user_id == null` 的簽名人呼叫 `PUT /api/admin/ref/identifiers/{id}` 改名
- **THEN** 回 `200` 且名稱更新，後續 `GET /ref/identifiers` 可見新名

#### Scenario: 停用後預設過濾
- **WHEN** ADMIN 將某簽名人 `PATCH .../active` 設 `active=false`
- **THEN** `GET /api/ref/identifiers` 預設不再含該筆，`GET /api/ref/identifiers?includeInactive=true` 才可見，已引用該 `id` 的舊案件仍顯示其名

#### Scenario: Staff 自助停用 typo
- **WHEN** STAFF 對自身首個 `user as signer` 呼叫 `PATCH /api/ref/identifiers/{id}/active` 設 `false`（typo 自清）
- **THEN** 回 `200` 且該筆轉為停用，後續建案預選不再出現

#### Scenario: Staff 不可停用他人簽名人
- **WHEN** STAFF 對非自身的 `user as signer` 呼叫 `PATCH .../active`
- **THEN** 回 `403`，僅 `ADMIN` 可停用他人
