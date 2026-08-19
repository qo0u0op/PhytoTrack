## Purpose

提供單一案件的明細檢視、可列印診斷單與 CSV 匯出，作為紙本診斷記錄表的前端對應。

## ADDED Requirements

### Requirement: 案件明細檢視

系統 SHALL 於前端提供單案明細頁，顯示送件人、作物／病蟲害、描述、AI 診斷結果與案件時間資訊。

#### Scenario: 檢視案件明細
- **WHEN** 使用者開啟某一案件
- **THEN** 顯示完整欄位內容

### Requirement: 列印診斷單

明細頁 SHALL 支援列印輸出，以 `@media print` 呈現診斷記錄表樣式，列印內容僅含診斷單本體。

#### Scenario: 觸發列印
- **WHEN** 使用者於明細頁觸發列印
- **THEN** 輸出僅含診斷單內容的版式

### Requirement: CSV 匯出

系統 SHALL 提供案件 CSV 匯出端點，僅限登入使用者存取。

#### Scenario: 匯出案件
- **WHEN** 登入使用者呼叫匯出端點
- **THEN** 下載包含案件欄位的 CSV 檔案