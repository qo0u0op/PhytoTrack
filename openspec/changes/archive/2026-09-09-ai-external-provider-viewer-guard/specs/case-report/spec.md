## ADDED Requirements

### Requirement: AI 診斷外部 Provider 提示

案件表單的 AI 診斷 SHALL 支援 `local` 與 `external` 雙 provider（由 `phytotrack.toml:ai.provider` 決定），外部模式下 SHALL 於 UI 提示「將外送去識別化資料（不含姓名/電話/地址）」。

#### Scenario: 外部模式提示
- **WHEN** `ai.provider=external` 時開啟案件表單的 AI 診斷區
- **THEN** 按鈕旁顯示「外部模式：僅送 Viewer 可見資料」提示
