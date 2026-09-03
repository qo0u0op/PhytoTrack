## ADDED Requirements

### Requirement: 建案時診斷簽名人自動帶入

`POST /api/cases` 與 `PUT /api/cases/{id}` 在 `identifierIds` 為空、未傳或空陣列時 SHALL 自動帶入當前登入使用者的關聯 `Identifier`（以 `Identifier.user.userId = 當前使用者` 查找；若無則以當前 `displayName` 即時建立並使用）；若請求已含 `identifierIds` 則 SHALL 原樣採用，不覆蓋或增補。前述自動帶入 SHALL 於交易內完成且回應的 `identifiers` 陣列 SHALL 包含該簽名人。

#### Scenario: 建案未選簽名人自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 建立案件
- **THEN** 案件 `identifiers` 含一筆其 `displayName` 對應的簽名人，且 `GET /api/cases/{id}` 可見

#### Scenario: 建案已選簽名人不覆蓋
- **WHEN** STAFF 以 `identifierIds: [2,3]` 建立案件
- **THEN** 案件 `identifiers` 恰為 `[2,3]`，不額外加入當前使用者簽名人

#### Scenario: 無關聯簽名人時即時建立
- **WHEN** 新 STAFF（尚無 `Identifier`）以空清單建案
- **THEN** 系統先建立 `Identifier(identifier=displayName, user=currentUser)` 再關聯至案件，後續 `GET /api/identifiers` 可見該筆

#### Scenario: 更新時空清單亦自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 更新既有案件
- **THEN** 案件簽名人更新為僅含當前使用者簽名人（整組替換語意同既有多對多）

#### Scenario: 更新時未傳欄位不變
- **WHEN** STAFF 更新案件但未傳 `identifierIds`（null）
- **THEN** 若原語意為「未傳即不更動」，則保留原簽名人；若本 change 定義「未傳視為空」則自動帶入（實作以 `CaseService` 現有 `identifierIds != null` 判斷為準，此情境驗證保留原值）

#### Scenario: 前端預選當前使用者簽名人
- **WHEN** STAFF 開啟案件新增表單
- **THEN** `GET /api/identifiers`（或 `GET /api/identifiers/me`）回傳的當前使用者簽名人預設勾選，仍可手動增刪多簽名
