## Context

見 `proposal.md: Why`。現況三處呈現順序與 `docs/diagnoses.typ` 不一致：`CasesView.vue:viewDetail` 彈窗為 `收件日期→作物→送件人→縣市→耕作→被害部位→病蟲害→建議→鑑定者`；`CaseDetailView.vue` 明細卡為 `收件日期/編號→作物/耕作→面積→被害部位→病蟲害→送件人/縣市/田區→服務/交付→建議/簽名人→描述`；`CaseService.toCsv` 表頭為 `案件編號,收件日期,狀態,送件人,電話,縣市鄉鎮,地址,身分別,作物,種植面積,被害面積,被害部位,病蟲害,土壤紀錄,防治建議,簽名人,耕種方式,服務,交付,時間`。約束：保持現有卡片視覺 (Bootstrap `card`/`@media print`) 不改為紙本表格，僅重排欄位；紙本本身有局部順序瑕疵，疑義先提問 (本文件 `Open Questions`)。

## Goals / Non-Goals

**Goals:**
- 三處輸出 (彈窗、明細 `/cases/:id`、CSV) 的欄位順序對齊 `diagnoses.typ` 的紙本邏輯：`收件日期/編號 → 病蟲害發生地點 (田區) → 送件人身分別 → 基本資料 → 耕作方式 → 作物種類/名稱 → 被害部位 → 土壤栽培用藥紀錄 → 栽培與被害面積/被害描述 → 服務類別 → 送件方式 → 鑑定者 → 五類分組 → 建議事項`
- 保持 `VIEWER` 遮蔽、列印僅本體、CSV BOM 與篩選語意不變

**Non-Goals:**
- 不改視覺設計 (不將卡片重做為 `diagnoses.typ` 的兩欄表格)、不改資料模型/API 契約 (僅呈現順序)、不改 `v_case_search` 篩選、不改五類底層 `pest_categories` 定義

## Decisions

### D1. 僅重排，不重構版式

彈窗仍 `Swal.fire html`、明細仍 `card shadow-sm`、列印仍 `@media print .print-area`，僅調整 `html` 內 `<p>`/`<div class="col-...">` 與 `toCsv` 的 `join` 順序。理由：符合「保持目前前端設計不變」且風險最低；替代「重做紙本表格版式」會牽動 RWD 與列印。

### D2. 五類分組以 `pestType` 分組呈現

`pestCategories` 為扁平 `List<IdNameWithNote>`，以 `pestCategory.pestTypeId` 分為 `病害 (1)/蟲害 (2)/有害動物 (3)/生理因子 (4)/其他 (5)` 五段，每段 `GROUP_CONCAT` 頓號，無資料則顯示「無」並附 `pestNote` 為 `名稱 (備註)`。理由：對應表單第二張表的五個 `checkbox` 區塊；替代「維持單一清單」則無法對照表單。

### D3. 面積/描述欄位對應 (依 2026-09-01 定版)

- `cropScale` → 栽培面積 (表單「栽培與被害面積」左)
- `damageScale` → 被害面積/植株數 (右)
- `caseDescription` → 土壤、栽培、用藥紀錄 (表單「被害部位」下方大段)，**與 `pest_note` 共同構成「被害描述」** (Q1 定版：維持現狀，`被害描述 ≒ caseDescription + pest_note`，呈現時以分號/換行串接)
- `hintDescription` → 是否已採取防治措施及其效果 (表單未明示，沿用現行「防治描述」)
理由：與 `CaseFormView.vue` 標籤一致；Q1 確認不新增 `damageDescription` 欄位。

### D4. 鑑定者與建議事項位置 (依 2026-09-01 定版)

`identifiers` 維持現行任意多選、人員變動頻繁故不固定為 A/B/C (Q2 定版)，僅調整位置：置於五類之後、`hints` 之前，與表單「鑑定者 → 五類 → 建議事項」一致。現行明細將 `hints` 置於 `identifiers` 前，需對調。`hints` 第 6 項 Q4 定版更名 `其他 → 其他回覆` (顯示層更名，`hints` 表 `id=6` 的 `hint` 值由「其他」改為「其他回覆」，需 `UPDATE` 種子資料並同步前端 `hintOptions`)。

### D5. CSV 表頭重排為表單順序，標 BREAKING (依 Q5 定版加作物種類)

新表頭：`收件編號,收件日期,病蟲害發生地點_縣市,病蟲害發生地點_鄉鎮,是否同寄件人,送件人身分別,姓名,顯示名稱,電話,住址,耕作方式,**作物種類**,作物名稱,被害部位,土壤栽培用藥紀錄,栽培面積,被害面積,被害描述,服務類別,送件方式,鑑定者,病害,蟲害,有害動物,生理因子,其他,建議事項,防治描述,建立者,建立時間,更新時間`。其中 `作物種類` 為 `crop.cropCategory.cropCategory` (Q5 定版：需要顯示)，`送件方式` 維持現行 `網路諮詢` 不對齊表單 9 項 (Q3 定版)，`基本資料` 是否顯 `senderType` 維持現狀 (Q6)。理由：貼合表單；下游以索引解析者需改以表頭解析，已於 `proposal.md: Impact` 標 BREAKING。

## Risks / Trade-offs

- [CSV 順序變更導致下游解析失敗] → 表頭已具名且含 BOM，文件與 release note 標 BREAKING；保留舊表頭對照表於 `docs/DEPLOY.md`
- [五類分組無資料時版式空洞] → 每類缺省顯示「無」，與現行 `joinWithNote` 一致
- [紙本「電子信箱」無對應欄位] → 省略該欄，`sender.email` 不存在，僅保留電話/住址

## Migration Plan

- 前端僅重排，無資料遷移；`mvn test` 與 `npm run build` 驗證
- CSV 屬呈現層，舊檔無需轉檔；通知下游改以表頭解析

## Open Questions (2026-09-01 已定版)

1. **栽培與被害面積 / 被害描述** → **Q1 定版：維持現狀**，`cropScale/damageScale` 即兩欄，`被害描述 ≒ caseDescription + pest_note`，不新增欄位。
2. **鑑定者 A/B/C 其它** → **Q2 定版：維持現行任意多選**，人員變動頻繁不固定為 A/B/C，僅調位置。
3. **送件方式** → **Q3 定版：維持網路諮詢**，不更名為 Email/FB+Line，不對齊 9 項。
4. **建議事項** → **Q4 定版：更名**，`hints.id=6` 由「其他」改為「其他回覆」。
5. **作物種類 9 類** → **Q5 定版：需要**，明細與 CSV 需額外顯示 `cropCategory` 名稱。
6. **基本資料 senderType** → **Q6 定版：維持現狀**，不額外於首行顯示身分別。
