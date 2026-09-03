## ADDED Requirements

### Requirement: 業務與開發種子分離

`schema.sql` 為業務用（`crop_categories/districts/cities` 等，含未知前置，無 `crops/sender/cases`），`seed-dev.sql` 與 `seed-test.sql` 分別為開發與測試用（含 `crops 68`、`sender/cases/users` 範例），`application-prod.yaml` SHALL 僅載 `schema.sql`，`application-dev.yaml`/`application-test.yaml` SHALL 載 `schema.sql + seed-dev/test.sql`（`spring.sql.init.data-locations`）。

#### Scenario: Prod 業務庫空作物
- **WHEN** 以 `prod` 啟動並查詢 `crops`
- **THEN** 為空，`crop_categories` 正常

#### Scenario: Dev 含開發種子
- **WHEN** 以 `dev` 啟動並查詢 `crops` 與 `cases`
- **THEN** 見 68 筆作物與範例案件，可直接用 `staff/viewer` 登入

#### Scenario: Test 最小可用
- **WHEN** `mvn test` 以 `test` 啟動
- **THEN** 載 `seed-test.sql`，測試用 `cropId/cases` 仍可用且不依賴 `prod` 種子
