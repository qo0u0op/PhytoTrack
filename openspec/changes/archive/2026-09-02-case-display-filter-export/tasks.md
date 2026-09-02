## 1. 顯示用語統一

- [x] 1.1 將 `CaseDetailView.vue`、`CasesView.vue` 預覽彈窗與 `CaseService.toCsv` 表頭的 `病蟲害發生地點`/`病蟲害發生地` 更名為 `田區位置`，`送件人身分別` 更名為 `身分別`，並驗證 `CaseServiceTest` 與預覽/檢視手測文案正確
- [x] 1.2 驗證 `openspec validate --specs --changes` 通過且搜尋舊字串無殘留

## 2. 篩選擴充與排序

- [x] 2.1 後端 `CaseDtos.CaseFilter` 新增 `senderTypeId/methodId`，`CaseSpecifications` 增加對應等值條件（含 `buildView`），`CaseController` 列表與匯出介面同步新參數，驗證 `CaseServiceTest` 以 `senderTypeId/methodId` 篩選回分頁正確
- [x] 2.2 前端 `CasesView.vue` 篩選版面依 `收件日期區間 → 狀態 → 田區縣市 → 田區鄉鎮 → 送件人 → 身分別 → 服務類別 → 送件方式 → 耕種方式 → 作物類別 → 作物 → 被害部位 → 害物 → 害物類別 → 建議類別` 重排，新增身分別/耕種方式選單與重置，並驗證展開順序與篩選生效
- [x] 2.3 後端與前端參照載入補 `senderTypes/methods`，驗證選單選項顯示

## 3. 篩選穿透至 CSV 匯出

- [x] 3.1 前端匯出按鈕以 `appliedFilter` 組 `URLSearchParams` 呼叫 `GET /api/cases/export`，後端 `exportCsv` 沿用相同 `CaseFilter` 查詢全量（`caseId asc`、BOM、引號），驗證套用篩選後匯出僅含符合案件
- [x] 3.2 更新 `CaseControllerTest` 與 `PhytoTrackIntegrationTest` 匯出篩選穿透斷言，並執行 `rm -f backend/target/phytotrack-test.db && mvn test` 通過
