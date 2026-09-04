## ADDED Requirements

### Requirement: 送件人電話候選 4 碼門檻

送件人候選搜尋中，電話欄位 SHALL 僅在輸入 4 碼以上時觸發後端搜尋；未達門檻 SHALL 不發請求。

#### Scenario: 電話未達門檻不查詢
- **WHEN** 電話輸入少於 4 碼
- **THEN** 系統不呼叫送件人搜尋 API

#### Scenario: 電話達門檻查詢
- **WHEN** 電話輸入 4 碼以上
- **THEN** 系統可呼叫搜尋並回傳候選
