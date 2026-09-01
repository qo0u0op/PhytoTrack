## 1. 後端正名與結構

- [x] 1.1 `cases.pest_description` 更名為 `case_description`：更新 `schema.sql`、`models/Case.java` (欄位與 `@Column (name="case_description")`)、`dto/CaseDtos.java` (`CaseCreateRequest`/`CaseUpdateRequest`/`CaseResponse`/`CaseSummaryResponse`)、`dto/AiDtos.java` 與 `service/AIService.java` (prompt 參數 `pestDescription`→`caseDescription`)、`service/CaseService.java` (`hasContentUpdate`/`toCsv`/`toDetail`/`findOrCreate` 對應)、`controller/CaseControllerTest` 與 `PhytoTrackIntegrationTest` fixture，驗證 `./mvnw compile` 通過
- [x] 1.2 `case_pest_categories` 移除 `UNIQUE (case_id, pest_category_id)`、新增 `pest_note TEXT` 欄位與實體 `CasePestCategory.pestNote` (`@Column`)，更新 `schema.sql` 與 `models/CasePestCategory.java`，驗證 `./mvnw test -Dtest=CaseRepositoryTest` 同分類多筆可並存

## 2. 後端害物明細

- [x] 2.1 `CaseDtos` 新增害物附註結構：`Record PestCategoryNote (Long pestCategoryId, String pestNote @Size (max=500))`，`CaseCreateRequest`/`CaseUpdateRequest` 新增 `List<PestCategoryNote> pestCategoryWithNotes` (同時保留 `pestCategoryIds` 過渡期或標 `@Deprecated`，若保留需明確優先序)，驗證 `openapi-typescript` 產物中 `PestCategoryNote` 與 `pestCategoryWithNotes` 可見
- [x] 2.2 `CaseService` 調整建案/更新：依 `pestCategoryWithNotes` 以 `clear+add` 整組替換 `case_pest_categories` (含 `pestNote`，不再以 `Set<Long>` 去重，同分類多筆合法)，`caseDescription` 獨立儲存，驗證 `CaseServiceTest` 同分類多筆不同 note 的替換與 `IdNameWithNote` 回顯通過

## 3. 前端作物級聯

- [x] 3.1 `CaseFormView.vue` 作物區改級聯：新增 `selectedCropCategoryId` 下拉 (`null` 為全部時不過濾)，過濾 `crops` 選項；無已知作物時「＋新增作物」彈窗 (名稱＋當前分類) 呼叫 `refAdminApi.createCrop`，成功後刷新 `cropCategories` 並自動選用新 `cropId`，驗證手動建案可新增並選用
- [x] 3.2 放寬 `ReferenceDataAdminController` 權限：`POST /api/admin/ref/crops` 與 `POST /api/admin/ref/identifiers` 方法層改 `hasAnyRole ('STAFF','ADMIN')` (`PUT/DELETE` 維持 `ADMIN`)，驗證 `STAFF` 200、`VIEWER` 403、`ADMIN` 200

## 4. 前端害物三段式

- [x] 4.1 `CaseFormView.vue` 害物區改三段式列編輯：害物類型下拉 → 過濾分類下拉 → 學名：描述輸入 (`@Size (max=500)` 提示與 `escapeHtml`)，可增刪多列 (含同分類多筆，`pestNote` 獨立)，送 `pestCategoryWithNotes` 至後端，`caseDescription` 輸入框標籤改為「土壤、栽培、用藥紀錄」，`loadCase` 需將 `case.pestCategories` 的 `note` 回填列編輯，`submit` 同時送 `pestCategoryWithNotes` 與 `caseDescription`，`runAi` 參數改送 `caseDescription` + `pestNotes` 拼接，驗證手動新增兩筆同分類害物可儲存與回顯
- [x] 4.2 `CaseDetailView.vue`/`CasesView.vue` 更新顯示：害物分類改逐列顯示 `pestNote` (`List<IdNameWithNote>`)，`caseDescription` 顯示為土壤紀錄，`CasesView` 列表摘要可選顯示首條 `pestNote`，驗證 `VIEWER` 仍遮蔽個資但可見害物明細

## 5. 測試與文件

- [x] 5.1 後端切片與整合：`CaseRepositoryTest` 同分類多筆害物可並存；`CaseServiceTest` 同分類多筆不同 `pestNote` 的 `clear+add` 替換；`CaseControllerTest` (VIEWER 遮蔽仍保留 `pestNotes`)；`ReferenceDataAdminControllerTest` 權限放寬；`CsvExportTest` 新欄位 `caseDescription` 與 `pest_note` 轉義；`PhytoTrackIntegrationTest` 同分類兩害物各帶 note 可建立與編輯正確回顯，驗證 `./mvnw test` 全量通過
- [x] 5.2 前端驗證：`npm run build` 與 `npm test` 通過
- [x] 5.3 重新生成 `frontend/src/types/api.ts` (`npx openapi-typescript ...`)，驗證 `caseDescription` 與 `pestCategoryWithNotes`/`PestCategoryNote` 出現在型別檔
- [x] 5.4 同步 `docs/ARCHITECTURE.md` (欄位語意與害物明細 `pest_note`)、`docs/REQUIREMENTS.md` (backlog 已於前一波記錄，正名註記 `pestDescription`→`caseDescription`)、`docs/manual.typ` (欄位更名與紙本對應)、`docs/adr/ADR-011` (送件人去重與害物多筆語意) 與 `openspec validate --specs --changes` 通過
