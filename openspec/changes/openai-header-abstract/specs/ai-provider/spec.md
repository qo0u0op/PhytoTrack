## MODIFIED Requirements

### Requirement: 外部 Provider 可選

系統 SHALL 支援 `ai.provider` 為任意 OpenAI 相容名稱（`local`/`opencode-go`/`openai` 等自由字串，僅作標記），`base-url` 決定實際連線端點；Header 注入 SHALL 經由通用 `OpenAiHeaderCustomizer` 依 `ai.headers` map 注入，非寫死 `opencode.ai`。

#### Scenario: 自訂標頭透傳
- **WHEN** `phytotrack.toml` 設 `ai.headers.x-opencode-session = "auto"`
- **THEN** 每次 `POST /v1/chat/completions` 自動帶 `x-opencode-session: UUID`，未配置時不注入
