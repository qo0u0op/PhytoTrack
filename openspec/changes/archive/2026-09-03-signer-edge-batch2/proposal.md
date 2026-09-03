## Why

第一批收斂行為後，仍有併發重複、綁定名實不符、停用重建循環、比對寬鬆、案件表單難辨同名與清空保護六項中風險邊界。需第二批補齊以防資料發散。

## What Changes

- 併發重複改由 DB 層部分唯一索引（`active + identifier`）或捕 `DataIntegrityViolationException` 轉 `409 DISPLAY_NAME_EXISTS`。
- `bindToUser` 要求同名或同步改 `displayName`，綁定名實不符時拒絕並提示。
- 使用者唯一 `active` 被停用後，下次 `ensureForUser` 優先提示啟用舊筆而非直接新建。
- 名稱比對改為正規化＋大小寫不敏感（trim、連續空白摺疊、全半形統一、NFC、英文小寫）。
- `CaseFormView` 簽名人勾選同步顯示 `身分別＋帳號`，與管理頁一致。
- 停用前檢查是否為最後一個全域 `active`，是則需二次確認或阻擋。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `reference-data-admin`: 併發冪等、綁定一致性、重建循環、比對正規化、最後 active 保護。
- `case-lifecycle`: 案件表單簽名人顯示帳號以辨同名。

## Impact

- 後端：`repository/IdentifierRepository.java`、`service/IdentifierService.java`、`service/ReferenceDataService.java`、遷移索引。
- 前端：`CaseFormView.vue` 簽名人選項顯示。
- 資料：視DB而定新增部分唯一索引；無破壞性行為變更。
