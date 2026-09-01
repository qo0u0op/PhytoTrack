# Design: case-report 案件明細、列印診斷單與 CSV 匯出

## Context

見 `proposal.md` (Why)。現況：`CaseResponse` 已含明細全部欄位 (送件人、作物／病蟲害、多對多、描述、狀態、時間)；**AI 診斷結果不持久化** (`Case` 無 ai 欄位，llama.cpp 為即時 `POST /ai/analyze`，STAFF+)；前端已有 `/cases/:id/edit` (CaseFormView 編輯既有案件)，無獨立明細檢視；`/ai/analyze` 已存在 (Spring AI 串接，見 ADR-009)。

## Goals / Non-Goals

- **Goals**：單案明細頁、`@media print` 列印診斷單、`GET /api/cases/export` CSV 匯出 (登入即可)、明細頁即時「AI 診斷」按鈕 (驗證 Spring AI 可行性)
- **Non-Goals**：AI 診斷結果**持久化** (無 schema 變更)；**RAG 歷史 chat／從歷史推測病因防治**——此為使用者期望的後續方向 (STAFF 限定)，不在本 change，另行規劃 (見 Open Questions)

## Decisions

1. **明細頁為獨立視圖** `views/CaseDetailView.vue`，路由 `/cases/:id` (與 `/cases/:id/edit` 並存，列表「檢視」按鈕導向)
   - 理由：檢視與編輯職責分離，列印樣式可獨立於表單；避免把明細塞進編輯頁造成列印含操作元素。
2. **「AI 診斷結果」以即時按鈕呈現**：明細頁對 STAFF+ 顯示「AI 診斷」按鈕，呼叫既有 `POST /ai/analyze` (以 `pestDescription`＋作物／病蟲害為輸入) 即時產生並於頁面顯示結果 (標註「僅供參考，正式診斷由診斷員確認」)，**不持久化**。
   - 理由：spec 明細 SHALL「顯示 AI 診斷結果」之目的即**驗證 Spring AI 可行性**；維持現況架構與零 schema 變更。替代方案均捨棄：持久化欄位 (資料遷移，超本能力範圍)；僅顯示描述欄位 (不符 spec SHALL)。
   - VIEWER 不顯示按鈕 (`/ai/analyze` 為 STAFF+)，僅顯示描述欄位。
3. **CSV 匯出端點** `GET /api/cases/export` (`isAuthenticated ()`)：
   - 沿用 case-search 的 `CaseFilter` (`cropId`／`serviceId`／`senderName`／`receiveDateFrom/To`／`status`)，無 filter＝全量；**不分頁**、依 `receiveDate` 升序
   - 回應：`text/csv; charset=UTF-8` **含 UTF-8 BOM** (Excel 開啟中文不亂碼)、`Content-Disposition: attachment; filename="case-export-YYYYMMDD.csv"`
   - 欄位與明細對應 (案件編號、收件日期、狀態、送件人姓名／電話／縣市鄉鎮／身分別、作物、種植面積、被害面積、被害部位、病蟲害、病害描述、防治建議、簽名人、方法／服務／交付、建立／更新時間)
   - **不引入第三方 CSV 庫**：內部工具資料量小，以 Java 字串組行 (欄位含逗號／引號時以 `"` 包覆、內部 `"` 轉 `""`)
4. **列印採純 CSS**：`@media print` 隱藏導覽、按鈕與無關區塊，診斷單樣式輸出，`window.print ()` 觸發；不使用 jsPDF 等套件。

## Risks / Trade-offs

- LLM 對植物保護幻覺較高、即時分析結果僅供參考 → 明細頁標註免責、需診斷員確認；RAG (後續) 可降低幻覺與提供歷史佐證
- CSV 中文在 Excel 亂碼 → 輸出 UTF-8 BOM
- 全量匯出若案件量大 → 內部工具資料量小，接受；必要時再分頁/上限

## Migration Plan

無資料庫變更。前端新增路由與視圖；`types/api.ts` 重新生成 (`/ai/analyze` 契約應已存在)。

## Open Questions

- **RAG 歷史助理 (後續能力，非本 change)**：使用者期望後續以資料庫案件為 RAG，提供 STAFF「歷史記錄 chat」與「依歷史推測病因／防治建議」 (前期幻覺可能偏高)。屆時需另立 change 評估：AI 結果是否持久化、向量化／檢索策略、VIEWER 不得存取、`/ai/analyze` 是否演化為 chat 型端點。本 change 之明細即時 AI 按鈕可作為該方向的前置驗證。