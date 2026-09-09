## ADDED Requirements

### Requirement: AI 送審個資隔離

`AIService.analyze` 送審 SHALL 無論呼叫者角色皆依 `VIEWER` 規則遮蔽 `name/phone/address/displayName`（顯示為 `***` 或 `***(***)`），僅保留縣市鄉鎮與 `senderId` 等非個資欄位。

#### Scenario: AI 送審遮蔽
- **WHEN** ADMIN 對含完整個資的案件呼叫 `POST /api/ai/analyze`
- **THEN** 外送 prompt 內個資欄位為 `***`，與 Viewer 檢視 `GET /api/cases/{id}` 的遮蔽結果一致
