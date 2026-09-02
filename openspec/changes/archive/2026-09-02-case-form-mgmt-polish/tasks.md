## 1. 管理頁按鈕

- [x] 1.1 交換 `CropManagementView.vue` / `PestManagementView.vue` 標題列「篩選」「新增」次序為「篩選在前、新增在後」，並驗證 `npm run build` 通過
- [x] 1.2 移除 `SendersView.vue` 標題列「新增」按鈕與 `handleCreate` 入口，僅保留「篩選」按鈕，驗證 STAFF 與 VIEWER 皆不顯示新增且 `npm run build` 通過
- [x] 1.3 交換 `SendersView.vue`（若保留）與 `CasesView.vue` 觸及的管理頁按鈕次序一致性，補齊 `ReferenceDataAdminView` 若有新增區塊的次序，並驗證視覺一致

## 2. 病蟲害排序改升冪

- [x] 2.1 將 `backend/src/main/java/com/d0w0b/phytotrack/service/ReferenceDataService.java:toPestTypeResponse` 改為 `Comparator.comparing(PestCategory::getPestCategoryCode)`（升冪）並驗證 `mvn test` 通過
- [x] 2.2 將 `backend/src/main/java/com/d0w0b/phytotrack/repository/PestCategoryRepository.java` 改為 `findAllByOrderByPestCategoryCodeAsc` 並驗證 `mvn test` 通過

## 3. 案件表單標籤與 کارت重組

- [x] 3.1 更名 `CaseFormView.vue` 中「病蟲害明細」→「診斷結果」並附加灰字 `span.text-muted.small`「(可增刪多列，同分類可多筆)」、「害物類型」→「害物」、「病蟲害分類」→「害物因素」、「新增一列」→「新增因素」，驗證 `npm run build` 通過
- [x] 3.2 移除 `form.caseDescription` textarea 的 ghost placeholder（改為空字串），驗證表單呈現無提示殘留
- [x] 3.3 將卡片標題「作物與診斷資訊」→「作物資訊」，驗證標題更名
- [x] 3.4 將「建議採取措施」(hintDescription) 與「診斷結果」(pestRows) 區塊由上卡搬移至下卡，下卡標題「防治建議與簽名」→「診斷結果與建議」，保持同 `diagnosisVisible` 門檻，驗證表單儲存與顯示正常

## 4. 驗收

- [x] 4.1 執行 `npm run build`、`npm test` 與 `mvn test` 通過，`openspec validate --specs --changes` 通過
