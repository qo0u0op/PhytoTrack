## ADDED Requirements

### Requirement: 新建案件拒絕停用簽名人

`POST /api/cases` SHALL 拒絕 `identifierIds` 內含 `active=false` 者（回 `409` 或 `400`），`PUT /api/cases/{id}` 更新歷史案件時 SHALL 放行已引用之 `inactive` 以保留顯示。

#### Scenario: 新建引用停用簽名人被拒
- **WHEN** STAFF 以 `identifierIds: [已停用id]` 建立案件
- **THEN** 回 4xx 且案件未建立，簽名人清單仍僅顯示 `active` 候選

#### Scenario: 歷史案件仍顯示停用簽名人
- **WHEN** 檢視已引用停用簽名人的舊案件
- **THEN** 詳情仍以 id 顯示原名，不因停用消失
