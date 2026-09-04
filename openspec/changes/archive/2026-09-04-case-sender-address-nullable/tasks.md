## 1. 後端放寬地址必填

- [x] 1.1 `CaseDtos.CaseCreateRequest.senderAddress` 與 `SenderDtos.SenderUpsertRequest.address` 移除 `@NotBlank`（改可空），`Sender.address` 改 `@Column(nullable = true)`，`schema.sql` 同步可空，驗證編譯通過
- [x] 1.2 `CaseService.findOrCreateSender` 與 `SenderService.apply` 以 `blankToNull` 存入地址（空／全空白→null），驗證空地址建檔回 2xx 且查詢顯示為空
- [x] 1.3 `docs/DEPLOY.md` 加註既有 SQLite 庫手動遷移步驟，驗證文件有遷移說明

## 2. 前端去必填

- [x] 2.1 `CaseFormView.vue` 地址欄去 `required`、空值送 `undefined`，`SendersView.vue` 同款必填一併去除，驗證 `npm run build`（含 `vue-tsc`）通過且空地址可提交

## 3. 測試與回歸

- [x] 3.1 新增 `SenderAddressNullableTest`（案件內空地址建檔、送件人獨立空地址新增／更新、全空白視為 null、非空地址行為不變），驗證全綠
- [x] 3.2 執行 `cd backend && mvn test` 全回歸，驗證既有送件人／案件測試通過
- [x] 3.3 執行 `openspec validate --specs --changes --strict`，驗證無錯誤
