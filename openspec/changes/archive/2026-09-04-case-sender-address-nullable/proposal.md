## Why

現場送件常無完整地址（如 Line 轉介僅有暱稱與電話），但 `POST /api/cases` 新建送件人、`POST|PUT /api/senders` 與前端表單皆以 `@NotBlank`/必填強制要求 `senderAddress`，導致無地址案件無法建檔，只能填假地址污染資料。放寬地址為選填，使無地址送件可正常建檔與編輯。

## What Changes

- `CaseDtos.CaseCreateRequest.senderAddress` 移除 `@NotBlank`（改為可空）；`findOrCreateSender` 以 `blankToNull` 語意存入（全空白視為 null）。
- `SenderDtos.SenderUpsertRequest.address` 移除 `@NotBlank`（改為可空）；`SenderService.apply` 同以 `blankToNull` 存入。送件人查詢 API（`SenderService`）維持既有遮蔽與查詢行為。
- `Sender.address` 改 `@Column(nullable = true)`；`schema.sql` 同步改可空；既有資料庫因 SQLite `ddl-auto: update` 無法放寬既有欄位，需手動遷移（見 Migration Plan）。
- 前端案件表單與送件人編輯的地址欄移除 `required`，空值送 `undefined`（後端收 null）；CSV／詳情／列表的地址顯示維持（空值顯示為空字串，既有 `Optional.orElse("")` 已容忍）。
- 案件更新路徑（`senderAddress=null` 沿用舊值）行為不變。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `sender-management`: 送件人地址改為選填（建立、更新、案件內新建）

## Impact

- 後端：`dto/CaseDtos.java`、`dto/SenderDtos.java`（驗證放寬）、`models/Sender.java`＋`schema.sql`（可空）、`service/CaseService.java`（`findOrCreateSender` 正規化）、`service/SenderService.java`（`apply` 正規化）。
- 前端：`CaseFormView.vue`（地址欄去必填、空值送 `undefined`）、送件人編輯流程（`SendersView.vue` 若有同款必填一併去除）。
- 相容性：舊客戶端照填地址不受影響；空地址送件人可建檔；`VIEWER` 遮蔽與去重鍵不受影響（地址非去重鍵）。
