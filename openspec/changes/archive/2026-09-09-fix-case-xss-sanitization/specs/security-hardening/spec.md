## MODIFIED Requirements

### Requirement: 輸入消毒阻擋 Stored XSS

系統 SHALL 於所有 `displayName/name/address` 寫入路徑阻擋 HTML 標籤字元：DTO 層以 `@Pattern(regexp="^[^<>]*$")` 驗證，Service 層以 `InputSanitizer.assertNoHtml` 二次檢查；含 `<` 或 `>` 的請求 SHALL 回 `400 VALIDATION_ERROR` 且不持久化。

#### Scenario: 個人檔案顯示名稱含 script
- **WHEN** 已登入使用者以 `PUT /api/account/profile` 送 `{"displayName":"<script>alert(1)</script>"}`
- **THEN** 回 `400` 且 `error.code=VALIDATION_ERROR`、`details.displayName` 含「不可包含 < 或 >」，資料庫 `users.display_name` 不變更

#### Scenario: 註冊顯示名稱含 HTML
- **WHEN** 以 `POST /api/auth/register` 送 `{"displayName":"<img onerror=...>"}` 
- **THEN** 回 `400 VALIDATION_ERROR`，不建立帳號

#### Scenario: 送件人姓名或地址含 HTML
- **WHEN** 以 `POST /api/senders` 送 `{"name":"<b>test</b>"}` 或 `{"address":"<svg>"}`
- **THEN** 回 `400 VALIDATION_ERROR` 對應欄位

#### Scenario: 正常中文名稱通過
- **WHEN** 以 `PUT /api/account/profile` 送 `{"displayName":"診斷員"}`
- **THEN** 回 `200` 且持久化為該值

#### Scenario: 案件內聯送件人名稱含 HTML
- **WHEN** 以 `POST /api/cases` 送 `{"senderDisplayName":"<script>alert(1)</script>"}` 或 `{"senderName":"<img onerror=...>"}`
- **THEN** 回 `400` 且 `error.code=VALIDATION_ERROR`，`details.senderDisplayName` 或 `details.senderName` 含「不可包含 < 或 >」，不建立 `senders` 亦不建立 `cases`

#### Scenario: 案件更新內聯送件人地址含 HTML
- **WHEN** 以 `PUT /api/cases/{id}` 送 `{"senderAddress":"<svg>"}`
- **THEN** 回 `400 VALIDATION_ERROR`，原案件與送件人不變更

#### Scenario: Service 層直調亦阻擋
- **WHEN** 以 Service 直調 `findOrCreateSender` 傳 `displayName="<b>"`
- **THEN** 拋 `ApiException VALIDATION_ERROR` 且 `details` 含對應欄位（防 `@Valid` 旁路）
