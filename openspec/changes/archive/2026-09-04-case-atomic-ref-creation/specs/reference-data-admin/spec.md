## ADDED Requirements

### Requirement: 參照資料建表邏輯共用與去重

`POST /admin/ref/crops`、`POST /admin/ref/identifiers`（管理頁獨立路徑）與案件內聯路徑 `POST /api/cases` / `PUT /api/cases/{id}` 的 `inlineCrop` / `inlineIdentifiers` SHALL 共用同一建表邏輯：名稱 `trim` 後去重，同分類同名作物或同名 `active=true` 的 `Identifier` SHALL 回 `409` 或於內聯路徑復用既有 `id`（不重建），未被案件引用的孤兒仍可由 `ADMIN` 經 `DELETE /admin/ref/*` 或 `PATCH .../active=false` 清理。

#### Scenario: 管理頁與案件內聯去重一致
- **WHEN** 管理頁已存 `crop: {name:"柑橘", cropCategoryId:1}` 活躍，案件以 `inlineCrop: {name:"柑橘", cropCategoryId:1}` 提交
- **THEN** 案件內聯不新建而復用該 `cropId`，`POST /admin/ref/crops` 以同參數亦回 `409` 或復用

#### Scenario: 同名簽名人復用
- **WHEN** 已有 `Identifier(name:"張三", active=true)`，案件以 `inlineIdentifiers:["張三"]` 提交
- **THEN** 內聯復用該 `id`，不新增重複

#### Scenario: 孤兒可清理
- **WHEN** 某內聯暫存未提交而被丟棄
- **THEN** 無孤兒產生；若曾經管理頁獨立建立且未被引用，`ADMIN` 可 `DELETE` 成功或 `PATCH active=false`
