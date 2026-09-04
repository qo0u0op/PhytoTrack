## 1. 診斷簽名人獨立卡片

- [x] 1.1 將 `CaseFormView.vue` 診斷簽名人抽為獨立卡片並以 `signerCardVisible` 條件顯示（僅診斷有編輯時顯示，預設不勾選），驗證未編輯時隱藏、編輯後顯示且可勾選
- [x] 1.2 移除或調整 `loadRefs` 中編輯頁預設勾選簽名人的邏輯，驗證新增案件預設無勾選

## 2. 防治建議橫式

- [x] 2.1 將 `CaseFormView.vue` 防治建議由直式改為橫式 `flex-wrap` 排列，驗證版面與被害部位一致且多選正常

## 3. 驗證與回歸

- [x] 3.1 執行 `npm run build` 與 `openspec validate --specs --changes --strict`，驗證無錯誤
- [x] 3.2 執行 `mvn test` 相關案件測試，驗證無回歸
