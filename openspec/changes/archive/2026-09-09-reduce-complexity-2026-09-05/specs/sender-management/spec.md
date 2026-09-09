## ADDED Requirements

### Requirement: 重構不改變行為

重構 SHALL 不改變任何 spec 行為與 API 契約，僅減少分支與重複；既有測試 SHALL 保持通過。

#### Scenario: 測試綠燈
- **WHEN** 執行 `mvn test`
- **THEN** 148 測通過且 API 回應與重構前一致
