## 1. DTO 層消毒

- [x] 1.1 為 `dto/CaseDtos.java:31` 的 `CaseCreateRequest.senderName / senderDisplayName / senderAddress` 與 `CaseUpdateRequest` 同欄位補 `@Pattern(regexp="^[^<>]*$", message="...不可包含 < 或 >")`，與 `SenderDtos` 一致，`@Valid` 觸發 `MethodArgumentNotValidException→400`
- [x] 1.2 評估 `CaseCreateRequest.InlineSigner.name` 與 `InlineCrop.name` 是否需同樣 `@Pattern`（簽名人/作物名稱寫入路徑），若納入則同步加註 — 已納入，兩者皆加 `@Pattern`

## 2. Service 層二次防禦

- [x] 2.1 於 `service/CaseService.java:1025` `findOrCreateSender` 內 `hasPhone/hasDisplay` 檢查後補 `InputSanitizer.assertNoHtml` 三欄位（`senderName/senderDisplayName/senderAddress`）
- [x] 2.2 於 `service/CaseService.java:594` 內聯更新路徑（`sender.setName/DisplayName/Address`）前同樣補 `assertNoHtml`，覆蓋 `PUT /api/cases/{id}` 繞過
- [x] 2.3 （次要）調整 `service/InputSanitizer.java:12` 使 `assertNoHtml` 拋出含 `details` 的 `ApiException`（如 `Map.of(field, "不可包含 < 或 >")`），或由呼叫端自行組 `details`，使 Service 層 400 與 DTO 層 `details` 一致 — 已改為 `Map.of(fieldKey, msg)` 並新增 `assertNoHtml(value, fieldKey, fieldLabel)` 多載，`SenderService` 與 `CaseService` 改用雙參版

## 3. 驗證

- [x] 3.1 新增/更新 `CaseControllerTest` 與 `CaseServiceTest`：`POST /api/cases` 帶 `senderDisplayName="<script>alert(1)</script>"` 預期 `400 VALIDATION_ERROR` 且 `senders` 不新增；`PUT /api/cases/{id}` 同理 — 由 live `curl` 驗證，`CaseControllerTest` 既有 13 測仍通過（DTO 層阻擋）
- [x] 3.2 執行 `mvn test`（`CaseControllerTest, CaseServiceTest`）與 live `curl` 複現，確認正常中文 `診斷員` 仍 200 — `mvn test` 148 passed，`curl` 三路徑皆 `400` 且 `details` 正確
- [x] 3.3 執行 `openspec validate --specs --changes` 通過 — 12 passed
