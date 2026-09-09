# ai-provider Specification

## Purpose
支援外部 OpenAI 相容 AI provider 作為本機 llama.cpp 的可選替代，同時以 Viewer 權限隔離確保個人資料不外流。

## Requirements

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

### Requirement: Viewer 權限隔離

`AIService.analyze` 組 prompt 前 SHALL 將案件個人資料經 Viewer 過濾（`name/phone/address/displayName` → `***`，僅保留縣市鄉鎮、作物、病蟲害、被害部位等非個資），再送 provider；無論呼叫者為 ADMIN/STAFF，送審內容 SHALL 等同 Viewer 可見範圍。

#### Scenario: STAFF 送審仍遮蔽
- **WHEN** STAFF 對含 `senderPhone=0912345678` 的案件呼叫 `POST /api/ai/analyze`
- **THEN** 送至 provider 的 prompt 內 `phone` 顯示為 `***`，外部日誌與重放亦不含明文

#### Scenario: 本地亦適用
- **WHEN** `provider=local` 時呼叫 AI
- **THEN** 同樣經 Viewer 過濾，避免未來切換至外部時遺漏

### Requirement: 健康檢查與開關

`GET /api/ai/health` SHALL 依 `provider` 檢查對應端點（`local` 查 `http://localhost:11435/health`，`external` 查 `base-url` 的 `/health` 或 `/v1/models`），`ai.enabled=false` 時 `POST /api/ai/analyze` 回 404。

#### Scenario: 外部健康檢查
- **WHEN** `provider=external` 且外部端點可達
- **THEN** `GET /api/ai/health` 回 `{healthy:true, provider:"external"}`
