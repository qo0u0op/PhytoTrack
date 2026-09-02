## 1. 更名與文件釐清

- [x] 1.1 全量 `pestDescription` 更名收斂（`CaseService` 註解與 `docs/REQUIREMENTS.md` 該條改為已更名，確認 `schema.sql`/`Case.java` 為 `case_description`/`caseDescription`），驗證 `grep -rn pestDescription backend --include="*.java" | grep -v spec` 無殘留

## 2. 版面

- [x] 2.1 於 `CaseFormView.vue` 將 `＋新增因素` 按鈕前加入換行（`d-block` 或 `w-100`），驗證按鈕獨立一行且不影響下拉排版

## 3. 驗證

- [x] 3.1 執行 `openspec validate --specs --changes` 與 `grep -rn 新增因素` 版面確認
