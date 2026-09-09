## Why

`OpenAiHeaderCustomizer` 前身 `OpencodeSessionHeaderCustomizer` 內含 `opencode.ai` 與 `x-opencode-session` 寫死，雖透過 `ai.headers` 已可通用，但 class 名與註解仍綁定單一 provider。現行以 OpenAI 相容為主（Anthropic 另有獨立協議），需先將名稱與邏輯抽象為 OpenAI 層級，以免未來支援 Anthropic 時誤用。

## What Changes

- **更名**：`config/OpencodeSessionHeaderCustomizer.java` → `config/OpenAiHeaderCustomizer.java`（`implements OpenAiHttpClientBuilderCustomizer` 維持），內註解與日誌去 `opencode` 字樣
- **通用化**：Header 來源改為讀 `ai.headers` map（`auto` 時每次 `UUID`，固定值原樣送），不再 `host.contains("opencode.ai")` 判斷；未配置 `ai.headers` 時不注入，`local` 的 `llama-server` 不受影響
- **文件**：`phytotrack.toml.example` 範例保留 `opencode-go` 僅作值之一，註解改為「OpenAI 相容 provider 範例」；`docs/ARCHITECTURE.md` 提及 `OpenAiHeaderCustomizer` 時同步更名

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `ai-provider`: Header 注入由 opencode 專屬改為 OpenAI 兼容通用（`ai.headers`）

## Impact

- 後端：`config/OpenAiHeaderCustomizer.java`（更名 1 檔）、`PhytotrackTomlEnvironmentPostProcessor.java` 已透傳 `ai.headers` 無需再動
- 前端：無
- 風險：更名為純搬移，`OpenAiHttpClientBuilderCustomizer` 行為不變，`Anthropic` 另起獨立 `AnthropicHeaderCustomizer` 時不衝突
