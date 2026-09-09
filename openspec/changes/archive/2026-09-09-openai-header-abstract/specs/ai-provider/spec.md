## MODIFIED Requirements

### Requirement: 外部 Provider 可選

系統 SHALL 支援 `ai.provider` 為任意 OpenAI 相容名稱（`local`/`opencode-go`/`openai` 等自由字串，僅作標記），`base-url` 決定實際連線端點；Header 注入 SHALL 經由通用 `OpenAiHeaderCustomizer` 依 `ai.headers` map 注入，非寫死 `opencode.ai`。

#### Scenario: 預設本地
- **WHEN** `phytotrack.toml` 未設定 `ai.provider` 或為 `local`
- **THEN** `AIService` 仍以 `http://localhost:11435/v1` 呼叫，行為與現行一致

#### Scenario: 切換外部
- **WHEN** 設 `ai.provider = "external"`、`ai.base-url = "https://api.openai.com/v1"`、`ai.model = "gpt-4o"`、`ai.api-key = "sk-..."`
- **THEN** `POST /api/ai/analyze` 轉送至外部端點並回傳同格式建議，日誌標記 `provider=external`

#### Scenario: 自訂標頭透傳
- **WHEN** `phytotrack.toml` 設 `ai.headers.x-opencode-session = "auto"`
- **THEN** 每次 `POST /v1/chat/completions` 自動帶 `x-opencode-session: UUID`，未配置時不注入
