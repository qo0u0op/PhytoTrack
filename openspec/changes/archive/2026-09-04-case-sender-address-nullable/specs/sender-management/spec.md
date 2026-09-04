## MODIFIED Requirements

### Requirement: 送件人欄位規則

送件人 SHALL 支援 `name`、`displayName`、`phone`、`address`、`district` (含所屬 `city`) 與 `senderType`；`name` MAY 為空，`phone` 與 `displayName` 之間 SHALL 至少一項有值；`address` MAY 為空（空字串或全空白視為 null，解除／未綁定地址）；`displayName` 用於標記來源顯示名稱 (Line/Facebook/Email 暱稱)。

#### Scenario: 有姓名且有顯示名稱
- **WHEN** 送件人同時有 `name` 與 `displayName`
- **THEN** 顯示為 `name (displayName)`

#### Scenario: 只有顯示名稱
- **WHEN** 送件人只有 `displayName`、無 `name`
- **THEN** 直接顯示 `displayName`

#### Scenario: 兩者皆空
- **WHEN** 建立送件人時 `phone` 與 `displayName` 皆未提供
- **THEN** 回應 4xx，且不建立資料

#### Scenario: 無地址可建檔
- **WHEN** 以空地址（未傳、null 或全空白）呼叫 `POST /api/cases` 新建送件人或 `POST|PUT /api/senders`
- **THEN** 回 2xx 且送件人建立／更新成功，地址存為 null，查詢與匯出顯示為空

#### Scenario: 地址仍可正常填寫
- **WHEN** 以非空地址建立或更新送件人
- **THEN** 地址去空白後儲存，行為與既有一致
