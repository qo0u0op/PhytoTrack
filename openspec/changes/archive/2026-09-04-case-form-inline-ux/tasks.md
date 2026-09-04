## 1. 作物下拉禁用

- [x] 1.1 於 `CaseFormView.vue` 將作物 `select` 綁定 `:disabled="!selectedCropCategoryId"`，驗證未選分類時禁用、選擇後啟用

## 2. 送件人候選門檻與 inline 化

- [x] 2.1 調整 `CaseFormView.vue` 送件人電話觸發門檻為 4 碼以上，未達門檻不呼叫 `senderApi.search`，驗證輸入 3 碼無請求、4 碼有請求
- [x] 2.2 將 `searchCandidates`/`applyCandidate` 由 `Swal` popup 改為送件人卡內 inline 下拉（與取消沿用同區域），驗證候選顯示於卡內且可選擇
- [x] 2.3 將候選操作按鈕文案由「沿用」改為「使用」，驗證新建與沿用情境文案一致

## 3. 驗證與回歸

- [x] 3.1 執行 `npm run build` 與 `openspec validate --specs --changes --strict`，驗證無錯誤
- [x] 3.2 執行 `mvn test` 相關送件人/案件測試，驗證無回歸
