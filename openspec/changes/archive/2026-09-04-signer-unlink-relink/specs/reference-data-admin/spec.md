## MODIFIED Requirements

### Requirement: 簽名人停用連動與首筆確定性

系統 SHALL 在使用者停用時將其名下 `user as signer` 解綁（`user_id = null`、`former_user_id` 記為該使用者）並置為 `active=false`；在使用者降權出 STAFF/ADMIN（至 VIEWER）時 SHALL 解綁但維持 `active=true`（`id` 不變，仍為新案件候選）；同使用者多筆 `active` 並存時，`ensureForUser` 與同步邏輯 SHALL 以 `identifierId ASC` 首筆為準，其餘不自動更名。

#### Scenario: 停用帳號連動停用簽名人
- **WHEN** STAFF 帳號被停用
- **THEN** 其名下 `active=true` 的 `user as signer` 全數解綁並轉為 `active=false`（`former_user_id` 留存），歷史案件仍以 id 顯示原名

#### Scenario: 降權解綁保留可見
- **WHEN** STAFF 帳號被降級為 VIEWER
- **THEN** 其名下 `active=true` 的 `user as signer` 全數解綁但維持 `active=true`（`former_user_id` 留存），新案件候選仍可見（非使用者）

#### Scenario: 多筆時首筆確定
- **WHEN** 同一使用者存在多筆 `active` 簽名人
- **THEN** 自動帶入與更名只作用於 `identifierId` 最小者，其餘保持不變

## ADDED Requirements

### Requirement: 簽名人 former_user_id 歷史欄位

`identifiers` SHALL 具 `former_user_id`（可空，外鍵指向 `users`，預設 null），記錄解綁前最後所屬使用者；綁定至使用者時 SHALL 清空（已有所屬無需歷史）。既有未綁定簽名人的 `former_user_id` 為 null，恢復流程 SHALL 找不到原筆而走既有行為。

#### Scenario: 解綁留存歷史
- **WHEN** 使用者被降權或停用而簽名人解綁
- **THEN** 該簽名人 `former_user_id` 為該使用者 `userId`

#### Scenario: 綁定清空歷史
- **WHEN** 非使用者簽名人被綁定至使用者
- **THEN** 其 `former_user_id` 清空

### Requirement: ensureForUser 重鏈同名未綁定舊筆

`IdentifierService.ensureForUser` 在使用者無連結 `active` 簽名人時，除既有同名 inactive 連結筆外，SHALL 亦搜尋 `former_user_id` 為該使用者、同名（正規化比對）的未綁定舊筆（`id` 最小優先），命中 SHALL 重新連結至該使用者並 `active=true`，不得直接新建重複。他人同名簽名人（`former_user_id` 不同或為 null）SHALL 不被重鏈，維持既有撞名檢查。使用者已有同名 active 連結筆時維持原行為（更名同步）。

#### Scenario: 重鏈解綁舊筆不新建
- **WHEN** 使用者無連結 active 簽名人，但存在 `former_user_id` 為其、同名的未綁定舊筆
- **THEN** 系統將該舊筆 `user_id` 回填並 `active=true`，不新增簽名人

#### Scenario: 無舊筆才新建
- **WHEN** 使用者無連結 active 簽名人，且無同名舊筆（連結或未綁定皆無）
- **THEN** 走既有新建流程（含全域非使用者同名檢查）
