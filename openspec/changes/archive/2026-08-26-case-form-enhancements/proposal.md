## Why

紙本表單的「土壤、栽培、用藥紀錄」與診斷結果的「分類勾選＋名稱」在現行數位表單中語意混淆：`pestDescription` 同時承載害物描述與土壤紀錄，且作物與病蟲害的級聯選擇未對應紙本表單的分類勾選結構。調查 `docs/diagnoses.typ` 確認紙本表單以分類為主、名稱為輔，且開發初期可忽略既有資料遷移，故需正名欄位並補齊級聯與多筆害物支援。

## What Changes

- **正名**：`cases.pest_description` 更名為 `case_description`（土壤、栽培、用藥紀錄），對應紙本「土壤、栽培、用藥紀錄」欄位；`Case` 實體/`CaseCreateRequest`/`CaseResponse`/`StatisticsDtos`/`AiDtos`/`AIService`（prompt 參數 `pestDescription` 同步更名為 `caseDescription`）、CSV 欄位「病害描述」改為「土壤栽培用藥紀錄」、測試 fixture 同步，舊 `pestDescription` 移除**（BREAKING：開發初期直接重建 DB，無遷移）**
- **作物級聯**：建案表單作物區改為「作物別下拉 → 依所選別過濾作物下拉」；無已知作物時提供 `STAFF` inline 新增（需放寬 `POST /api/admin/ref/crops` 至 `STAFF`）
- **簽名人 STAFF 可新增**：建案表單簽名人區已支援，後端 `POST /api/admin/ref/identifiers` 需放寬至 `STAFF`（方法層 `hasAnyRole('STAFF','ADMIN')`），僅文件化
- **參照資料篩選**：`ReferenceDataAdminView` 的作物與病蟲害頁籤已加入名稱/代碼與類型篩選（前一修復已完成），本次僅文件同步
- **害物三段式**：`case_pest_categories` 移除 `UNIQUE(case_id, pest_category_id)`、新增 `pest_note TEXT` 欄位；建案表單害物區改「害物類型 → 病蟲害分類 → 學名：描述」列編輯（可增刪多列，同分類多筆合法），每列對應一 junction 列（含 `pestNote`）；`CaseResponse.pestCategories` 由 `List<IdName>` 擴為 `List<IdNameWithNote>`（`id/name/note`），`caseDescription` 不再承載害物文字
- **未電子化欄位**：病蟲害發生地點、電子信箱、被害描述、送件方式 Email/FB/Line 等記入 `docs/REQUIREMENTS.md` backlog

## Capabilities

### New Capabilities

<!-- 本 change 採 skip_specs: true，僅正名與級聯，無需新增 capability 檔案 -->

### Modified Capabilities

<!-- 正名與害物明細屬非相容變更，但開發初期直接重建 DB，視為實作層正名，不另立 delta spec -->

## Impact

- 後端：`models/Case.java`（`pestDescription`→`caseDescription`）、`models/CasePestCategory.java`（新增 `pestNote`）、`repository/CaseRepository.java` 無變、`dto/CaseDtos.java`/`StatisticsDtos.java`/`AiDtos.java` 更名、`service/CaseService.java`（建案/更新/CSV/統計/`hasContentUpdate` 對應）、`service/AIService.java`（prompt 參數對應）、`schema.sql`（更名與 junction 變動）、`controller/CaseController.java` 無變、`controller/ReferenceDataAdminController.java`（放寬 `POST crops/identifiers` 至 STAFF）、CSV 與統計的 `topPestCategories` 維持分類聚合（不含 `pest_note`）
- 前端：`views/CaseFormView.vue`（作物級聯、害物三段式列編輯、土壤紀錄標籤）、`views/CaseDetailView.vue`/`CasesView.vue`（`List<IdNameWithNote>` 顯示與列印）、`views/ReferenceDataAdminView.vue` 無變（已含篩選）、`types/api.ts`（重新生成）
- 文件：`docs/ARCHITECTURE.md`（欄位語意）、`docs/REQUIREMENTS.md`（backlog 與 `caseDescription`）、`docs/manual.typ`（欄位更名與紙本對應）、`docs/adr/ADR-011`（如需）
