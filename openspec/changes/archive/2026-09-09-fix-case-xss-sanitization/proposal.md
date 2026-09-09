## Why

`0b10d6a` 的 Standards/Spec 雙軸審查指出 Stored XSS 修復存在繞過：`CaseDtos.CaseCreateRequest / CaseUpdateRequest` 的 `senderName / senderDisplayName / senderAddress` 未加 `@Pattern`，且 `CaseService.findOrCreateSender` 與 `update` 內聯送件人路徑未呼叫 `InputSanitizer`，攻擊者仍可經 `POST /api/cases` 內建送件人欄位持久化 `<script>`，規格「所有 `displayName/name/address` 寫入路徑」未達成。

## What Changes

- **DTO 層**：`CaseDtos.CaseCreateRequest` 與 `CaseUpdateRequest` 的 `senderName / senderDisplayName / senderAddress` 補 `@Pattern(regexp="^[^<>]*$", message=...)`，與 `SenderDtos` 保持一致；`InlineSigner.name` 與 `InlineCrop.name` 評估是否納入（屬次要）。
- **Service 層**：`CaseService.findOrCreateSender` 與 `CaseService` 內聯更新（`update` 的 `sender.setName/DisplayName/Address`）前補 `InputSanitizer.assertNoHtml` 二次檢查，覆蓋直接持久化繞過。
- **錯誤細節一致性（次要）**：`InputSanitizer.assertNoHtml` 改拋含 `details` 的 `ApiException`，使 Service 層觸發時 `details.<field>` 與 DTO 路徑一致，滿足 spec「`details.displayName` 含不可包含」。
- **不變**：`InputSanitizer` 仍置 `service` 套件（後續 `fix-case-xss-sanitization` 後再評估遷 `util`），`@Size(max=50)` 已存在者保留。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `security-hardening`: 補齊案件內聯送件人路徑的輸入消毒

## Impact

- 後端：`dto/CaseDtos.java:31`、`service/CaseService.java:594`/`1025`、`service/InputSanitizer.java:12`
- 測試：補 `CaseControllerTest` / `CaseServiceTest` 針對 `senderDisplayName="<script>"` 回 400 的用例
- 文件：無需 ADR 增量，`pentest-2026-09-04.md` 已載明此為已知漏網，修復後更新備註
