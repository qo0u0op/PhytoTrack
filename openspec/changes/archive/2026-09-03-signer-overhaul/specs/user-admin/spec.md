## MODIFIED Requirements

### Requirement: 管理者調整角色

ADMIN SHALL 可變更使用者角色，變更後該使用者的權限於後續請求生效。提權至 `STAFF|ADMIN` SHALL 觸發 `IdentifierService.ensureForUser` 自動建立 `user as signer`（若無 `active` 同名則建，名稱取 `displayName` 優先否則 `username`），並遵守簽名人綁定規則。

#### Scenario: 變更使用者角色
- **WHEN** ADMIN 將使用者調整為 STAFF
- **THEN** 該使用者於後續請求取得 STAFF 權限且 `GET /api/ref/identifiers/me` 可得其簽名人

#### Scenario: 提權撞名提示綁定
- **WHEN** ADMIN 將 `displayName` 已存在 `signer but not user` 同名的 VIEWER 提權為 STAFF
- **THEN** 回 `409 SIGNER_NAME_CONFLICT` 並附既有 `identifier_id`，前端提示是否綁定

## ADDED Requirements

### Requirement: 提權時簽名人自動註冊與名稱準則

`ROLE_STAFF | ROLE_ADMIN` 使用者 SHALL 於提權當下擁有 `user as signer`，其 `identifier` SHALL 等於 `User.displayName`（`displayName` 空則 `username`），而非角色名。`DATA_INITIALIZER` SHALL 不再建 `張志明/林雅惠/陳建宏` 種子，重建路徑僅由此自動註冊與案件內新建。

#### Scenario: 新 STAFF 自動註冊
- **WHEN** viewer `王小明` 被提權為 STAFF
- **THEN** `identifiers` 新增 `identifier=王小明, user_id=王小明.userId, active=true`，`GET /api/ref/identifiers?includeInactive=false` 可見

#### Scenario: 名稱取帳號而非權限
- **WHEN** 新建 STAFF `displayName=王小明, username=w123`
- **THEN** 簽名人名稱為 `王小明` 而非 `診斷員` 或 `STAFF`

### Requirement: 簽名人綁定端點

`POST /api/admin/ref/identifiers/{id}/bind` SHALL 將 `user IS NULL` 的簽名人綁定至指定 `userId`（限 `ADMIN`），轉為 `user as signer`；若 `user` 已有同名 `active` 簽名人則回 `409`。

#### Scenario: 綁定成功
- **WHEN** ADMIN 呼叫 `POST /api/admin/ref/identifiers/10/bind { userId: 5 }` 且 `10` 為 `user IS NULL`
- **THEN** `10` 的 `user_id` 更新為 `5`，類型轉為 `user as signer`

#### Scenario: 綁定目標已有同名 active
- **WHEN** ADMIN 試圖綁定 `identifier=王小明` 至已有 `王小明` active 的使用者
- **THEN** 回 `409` 提示已存在，無需重複綁定
