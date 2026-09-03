## ADDED Requirements

### Requirement: 簽名人停用連動與首筆確定性

系統 SHALL 在使用者停用或降級出 STAFF/ADMIN 時將其名下 `user as signer` 置為 `active=false`；同使用者多筆 `active` 並存時，`ensureForUser` 與同步邏輯 SHALL 以 `identifierId ASC` 首筆為準，其餘不自動更名。

#### Scenario: 停用帳號連動停用簽名人
- **WHEN** STAFF 帳號被停用或降級為 VIEWER
- **THEN** 其名下 `active=true` 的 `user as signer` 全數轉為 `active=false`，歷史案件仍以 id 顯示原名

#### Scenario: 多筆時首筆確定
- **WHEN** 同一使用者存在多筆 `active` 簽名人
- **THEN** 自動帶入與更名只作用於 `identifierId` 最小者，其餘保持不變

### Requirement: 自動帶入全域查重

`ensureForUser` 在新建前 SHALL 檢查全域 `active` 同名；若存在屬他人的 `active` 簽名人（含非使用者），SHALL 走綁定流程或回 `DISPLAY_NAME_EXISTS`，不得靜默新建重複。

#### Scenario: 自動帶入撞名不重複新建
- **WHEN** STAFF 的 `displayName` 已存在屬他人的 `active` 簽名人
- **THEN** 系統不新建第二筆，回 `DISPLAY_NAME_EXISTS` 或導向綁定確認
