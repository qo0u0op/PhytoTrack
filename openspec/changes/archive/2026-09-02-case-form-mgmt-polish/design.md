## Context

見 proposal.md Why。案件表單 `CaseFormView.vue` 現以「作物與診斷資訊」與「防治建議與簽名」兩卡承載診斷資訊，病蟲害分類由後端 `ReferenceDataService.toPestTypeResponse` 以 `pest_category_code desc` 排序；管理頁 `Senders/Crop/Pest` 三頁標題列為 `新增 | 篩選`。

## Goals / Non-Goals

**Goals:** 交換管理頁按鈕次序並移除送件人新增；表單病蟲害改升冪、標籤與卡片改名、灰字註解、去 ghost、區塊搬移。

**Non-Goals:** 不改後端 API 欄位與權限；不改送件人新增流程（改由案件表單內儲存送件人）；不改表格排序與分頁邏輯。

## Decisions

- **Decision: 按鈕次序交換以 DOM 順序調整** — `Crop/Pest/Senders` 標題列 `d-flex gap-1` 內改為 `篩選` 在前 `新增` 在後；`SendersView` 僅移除 `v-if="auth.isStaff"` 新增按鈕。替代：CSS order，不選，因可讀性差。
- **Decision: 病蟲害升冪以 `pestCategoryCode asc`** — `ReferenceDataService.toPestTypeResponse` 改 `Comparator.comparing(PestCategory::getPestCategoryCode)`（非 reversed），`PestCategoryRepository.findAllByOrderByPestCategoryCodeDesc` → `Asc`。替代：前端自行排序，不選，後端為單一真相。
- **Decision: 表單改名與搬移保持同一 `diagnosisVisible` 條件** — `作物與診斷資訊` → `作物資訊`；下卡 `防治建議與簽名` → `診斷結果與建議`，並將 `pestRows` 區塊與 `hintDescription` (建議採取措施) 兩欄位由上卡 `<div v-if="diagnosisVisible" class="card">` 搬至下卡同一 `v-if` 內，保持顯示條件一致。替代：拆兩個 v-if，不選，維持原門檻。
- **Decision: 灰字註解以 `span.text-muted.small`** — 「診斷結果」label 後接 `(可增刪多列，同分類可多筆)`。替代：`form-text`，不選，貼近標題更直觀。
- **Decision: 去 ghost 僅清 placeholder** — `form.caseDescription` textarea `placeholder` 由 `對應紙本表單…` 改 `""`。

## Risks / Trade-offs

- [按鈕次序變更影響操作習慣] → 變更範圍小且與 Form 內「新增作物/簽名人」一致
- [升冪改動改變候選預設選取] → `pestRows.push` 首項由 `categories[0]` 改為升冪首項，符合用戶期望
- [卡片搬移可能遺漏 v-if] → 兩區塊共用同一 `diagnosisVisible`，上卡移除後確保下卡仍受控

## Migration Plan

- 純前端/後端展示層變更，無 DB migration；舊 `pest_categories` 排序僅由後端 comparator 決定，止回滾即還原 desc comparator 與按鈕 DOM。

## Open Questions

- 無
