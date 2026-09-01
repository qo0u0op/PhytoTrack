## Why

目前案件預覽 popup (`CasesView.vue:viewDetail`)、檢視頁 `/cases/:id` (`CaseDetailView.vue`) 與 CSV 匯出 (`CaseService.toCsv`) 的欄位順序與實體紙本表單 `docs/diagnoses.typ` 不一致，且紙本表單本身亦有局部順序疑義 (例如「栽培與被害面積 / 被害描述」「建議事項」位置、五類病蟲害分組與單一 `pestCategories` 的對應)。在**保持現有前端卡片設計不變** (不改為紙本表格版式) 的前提下對齊欄位順序，可降低紙本/數位對照成本並讓列印與匯出更易核對。

## What Changes

- **對齊 `diagnoses.typ` 的邏輯順序，僅重排欄位** (不改變卡片視覺框架與欄位內容)：
  - **預覽 popup** (SweetAlert，`CasesView.vue:viewDetail`)：依表單 `收件日期 → 收件編號 → 病蟲害發生地點 → 送件人身分別 → 基本資料 (姓名/電話/住址)→ 耕作方式 → 作物種類/名稱 → 被害部位 → 栽培與被害面積/被害描述 → 服務類別 → 送件方式 → 診斷結果 (鑑定者 → 五類分組 → 建議事項)` 重排
  - **檢視頁** `/cases/:id` (`CaseDetailView.vue`)：同上順序重排卡片內欄位，保留 `列印` 的 `@media print` 僅輸出診斷單本體
  - **CSV 匯出** (`CaseService.toCsv`)：重排欄位順序與表頭，使 `收件日期/編號 → 田區位置 → 送件人/身分別 → 基本資料 → 耕作方式 → 作物 → 被害部位 → 面積/描述 → 服務/送件 → 鑑定者/五類 → 建議事項 → 時間` 與表單一致
- **明確疑義先提問後定版**：紙本「栽培與被害面積 / 被害描述」與現行 `cropScale/damageScale/caseDescription` 的對應、「鑑定者 A/B/C 其它」與 `identifiers` 的對應、五類 (病害/蟲害/有害動物/生理因子/其他) 與單一 `pestCategories` 的分組顯示、以及 `pest_note` 的位置，因表單順序本身有瑕疵，實作前於 design 階段以提問確認 (見 `design.md: Open Questions`)
- **不改變**：不改卡片/表格視覺設計、不改資料模型與 API 契約 (僅 presentation 順序)、不改 `v_case_search` 篩選邏輯

## Capabilities

### New Capabilities
<!-- 無新增能力，屬呈現層對齊 -->

### Modified Capabilities
- `case-report`: 案件明細檢視、列印診斷單與 CSV 匯出的欄位呈現順序對齊 `diagnoses.typ` (不改列印僅本體與登入存取規則，僅改順序)

## Impact

- 前端：`frontend/src/views/CasesView.vue` (`viewDetail` popup HTML)、`frontend/src/views/CaseDetailView.vue` (明細卡片順序)、間接 `frontend/src/api/index.ts` 無需改
- 後端：`backend/src/main/java/com/d0w0b/phytotrack/service/CaseService.java` (`toCsv` 欄位與表頭順序、`toDetail` 無需改但順序文件化)
- 文件：`docs/diagnoses.typ` 為對照基準，`docs/ARCHITECTURE.md` 無需改
- 風險：CSV 欄位順序變更為 **BREAKING** (下游以欄位索引解析者需配合表頭)，需於 release note 標註
