## ADDED Requirements

### Requirement: 案件表單內內聯參照原子建立

`POST /api/cases` 與 `PUT /api/cases/{id}` 在建立或更新案件時，對於在表單內現場新增的作物與簽名人 SHALL 以內聯欄位於同一交易內原子建立並關聯，交易失敗 SHALL 全回滾且放棄案件時 SHALL 不產生任何參照殘留。具體為：`inlineCrop: {name, cropCategoryId}` 若提供 SHALL 於交易內建立 `Crop`（同分類同名 `409` 去重，成功則復用或新建），`inlineIdentifiers: string[]` 若提供 SHALL 於交易內建立 `Identifier`（`user_id == null`、`active=true`，同名 `active=true` 則復用）；建立後取得之 `id` SHALL 自動加入案件的 `cropId` / `identifierIds` 關聯並與案件一併提交。前端「新增作物／簽名人」在案件提交前 SHALL 僅為本地暫存，不呼叫 `POST /admin/ref/*`，放棄（取消、導回列表、重新整理） SHALL 丟棄暫存且不呼叫後端。

#### Scenario: 空表單新增作物後放棄不落庫
- **WHEN** STAFF 在新增案件表單內以 `inlineCrop: {name:"新作物A", cropCategoryId:1}` 暫存作物後，未提交而按取消返回列表
- **THEN** 後端 `crops` 不含「新作物A」，重新進入表單下拉亦無該項

#### Scenario: 內聯作物隨案件一併提交才可見
- **WHEN** STAFF 以 `inlineCrop: {name:"新作物B", cropCategoryId:1}` 併入 `POST /api/cases` 提交
- **THEN** 回應 `201` 且 `GET /api/ref/crop-categories` 可見該作物，案件 `cropName` 為「新作物B」，交易內去重生效

#### Scenario: 內聯簽名人原子建立
- **WHEN** STAFF 以 `inlineIdentifiers: ["新簽名人X"]` 併入 `POST /api/cases` 提交
- **THEN** `GET /api/ref/identifiers` 可見該簽名人且案件 `identifiers` 含其 `id`，若同名 `active=true` 已存在則復用既有 `id`

#### Scenario: 內聯與既有 id 併用
- **WHEN** 請求同時含 `cropId: 36` 與 `inlineCrop: {name:"新作物C", ...}` 或同時含 `identifierIds:[1]` 與 `inlineIdentifiers:["新簽名人Y"]`
- **THEN** 系統 SHALL 以顯式 `cropId`/`identifierIds` 為準或合併兩者（`crop` 以 `inline` 覆蓋顯式，`identifiers` 為並集），文件化其一且不產生重複

#### Scenario: 交易失敗全回滾
- **WHEN** `POST /api/cases` 含 `inlineCrop` 但案件必填 `receiveDate` 缺失而回 `400`
- **THEN** `inlineCrop` 對應的作物亦未落庫，`GET /api/ref/crop-categories` 無該項

#### Scenario: 編輯時內聯新增亦原子
- **WHEN** STAFF 編輯既有案件時以 `inlineIdentifiers: ["編輯新增簽名人"]` 併入 `PUT /api/cases/{id}`
- **THEN** 僅在更新成功時該簽名人可見，放棄編輯則不產生

#### Scenario: 放棄編輯不殘留
- **WHEN** STAFF 在編輯頁暫存 `inlineCrop` 後未提交而取消
- **THEN** 該暫存作物不落庫，列表與管理頁均不可見
