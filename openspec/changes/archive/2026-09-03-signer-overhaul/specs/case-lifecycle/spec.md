## MODIFIED Requirements

### Requirement: 建案時診斷簽名人自動帶入

`POST /api/cases` 與 `PUT /api/cases/{id}` 在 `identifierIds` 為空、未傳或空陣列時 SHALL 自動帶入當前登入使用者的關聯 `Identifier`（以 `Identifier.user.userId = 當前使用者` 且 `active=true` 查找；若無則以當前 `displayName` 依 `IdentifierService.ensureForUser` 建立並使用）；若請求已含 `identifierIds` 則 SHALL 原樣採用，不覆蓋或增補。前述自動帶入 SHALL 於交易內完成且回應的 `identifiers` 陣列 SHALL 包含該簽名人。

#### Scenario: 建案未選簽名人自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 建立案件
- **THEN** 案件 `identifiers` 含一筆其 `displayName` 對應且 `active=true` 的簽名人，且 `GET /api/cases/{id}` 可見

#### Scenario: 建案已選簽名人不覆蓋
- **WHEN** STAFF 以 `identifierIds: [2,3]` 建立案件
- **THEN** 案件 `identifiers` 恰為 `[2,3]`，不額外加入當前使用者簽名人

#### Scenario: 無關聯簽名人時即時建立
- **WHEN** 新 STAFF（尚無 `Identifier`）以空清單建案
- **THEN** 系統先建立 `Identifier(identifier=displayName, user=currentUser, active=true)` 再關聯至案件，後續 `GET /api/identifiers` 可見該筆

#### Scenario: 更新時空清單亦自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 更新既有案件
- **THEN** 案件簽名人更新為僅含當前使用者簽名人（整組替換語意同既有多對多）

#### Scenario: 更新時未傳欄位不變
- **WHEN** STAFF 更新案件但未傳 `identifierIds`（null）
- **THEN** 保留原簽名人

#### Scenario: 前端預選當前使用者簽名人
- **WHEN** STAFF 開啟案件新增表單
- **THEN** `GET /api/identifiers/me` 回傳的當前使用者簽名人預設勾選，仍可手動增刪多簽名

## ADDED Requirements

### Requirement: 案件表單內內聯簽名人原子建立且默認為非使用者

`POST /api/cases` 與 `PUT /api/cases/{id}` 支援 `inlineIdentifiers: [{ name }]` 與 `identifierIds` 併用，`inlineIdentifiers` 內每筆 SHALL 於同一交易內建立 `Identifier(user IS NULL, active=true)`，名稱去空白重複時復用既有 `active` 同名簽名人（`signer but not user` 優先），建立後與案件以 `case_identifiers` 關聯；若未傳 `identifierIds` 且未傳 `inlineIdentifiers` 則走自動帶入。`STAFF|ADMIN` 於案件內新建者一律 `user IS NULL`，僅提權路徑建 `user as signer`。放棄新增/編輯（前端不提交）時 `inlineIdentifiers` SHALL 不落庫；交易失敗 SHALL 全回滾（含內聯簽名人）。

#### Scenario: 內聯新建默認為非 user
- **WHEN** STAFF 以 `inlineIdentifiers: [{ name: "外聘專家" }]` 建案
- **THEN** 新建 `identifiers` 其 `user_id IS NULL` 且 `active=true`，案件關聯該 `id`

#### Scenario: 併用併去重
- **WHEN** 同時傳 `identifierIds=[1]` 與 `inlineIdentifiers=[{ name: "外聘專家" }]`
- **THEN** 案件最終簽名人為 `1` 加新建之外聘，名稱重複時復用既有 `active` 同名而非新建

#### Scenario: 放棄不落庫
- **WHEN** STAFF 於表單內新增外聘簽名人後取消未提交
- **THEN** 資料庫 `identifiers` 無新增，`GET /api/ref/identifiers` 不可見

#### Scenario: 交易失敗全回滾
- **WHEN** `inlineIdentifiers` 建成但後續 `pestCategoryIds` 校驗失敗致 `400`
- **THEN** 內聯簽名人亦回滾，不殘留

### Requirement: 停用簽名人於新增案件時隱藏

`GET /api/ref/identifiers` 預設僅回 `active=true`，故 `active=false` 的簽名人 SHALL 不出現於 `CaseFormView` 的候選清單；但 `PUT /api/cases/{id}` 仍可引用已停用之歷史 `id`（刪除保護不變）。

#### Scenario: 已停用不出現於新建
- **WHEN** 某簽名人已 `active=false`
- **THEN** `GET /api/ref/identifiers` 不含該筆，`CaseFormView` 不可勾選，但 `GET /api/cases/{歷史id}` 仍顯示其名
