## ADDED Requirements

### Requirement: 降權解綁簽名人保留可見

`PATCH /api/admin/users/{id}/role` 將 `STAFF|ADMIN` 降權至 `VIEWER` 時，系統 SHALL 將其名下 `active` 的 `user as signer` 解綁為非使用者（`user_id = null`、`former_user_id` 記為該使用者、`active` 維持 `true`、`id` 不變）；解綁後 SHALL 繼續顯示於新案件簽名人候選（身分別為非使用者），歷史案件仍以 `id` 顯示原名。

#### Scenario: 降權轉非使用者仍可選
- **WHEN** ADMIN 將 STAFF 王小明降權為 VIEWER
- **THEN** 其簽名人 `user_id` 清空、`active` 維持 `true`、`id` 不變，新案件候選仍可見（非使用者）

### Requirement: 停用解綁簽名人

`PATCH /api/admin/users/{id}/active` 設 `active=false` 時，除既有連動停用外，系統 SHALL 一併將其名下簽名人解綁（`user_id = null`、`former_user_id` 記為該使用者）；可見性維持現行（新案件候選隱藏，管理頁 `?includeInactive=true` 可見）。

#### Scenario: 停用解綁且候選隱藏
- **WHEN** ADMIN 停用 STAFF 王小明
- **THEN** 其簽名人 `user_id` 清空且 `active=false`，新案件候選不可見，管理頁可見停用狀態

### Requirement: 升權或啟用恢復原簽名人

升權至 `STAFF|ADMIN` 或重新啟用（`active=true`）時，系統 SHALL 優先恢復原筆簽名人：以 `former_user_id` 為該使用者、同名（正規化比對）的未綁定舊筆取 `id` 最小者，重新連結（`user_id` 回填）並 `active=true`；無原筆才走既有新建／啟用流程，不得在存在可恢復舊筆時新建重複。他人同名非使用者簽名人（`former_user_id` 不同）SHALL 不被恢復，維持既有撞名綁定流程。

#### Scenario: 升權恢復原筆不新建
- **WHEN** 被降權的王小明（其原簽名人已解綁為非使用者 active）重新升權為 STAFF
- **THEN** 原筆 `id` 重新連結該使用者，不新增第二筆同名簽名人

#### Scenario: 啟用恢復停用舊筆
- **WHEN** 被停用的王小明重新啟用
- **THEN** 其原停用簽名人重新連結並 `active=true`，新案件候選可見
