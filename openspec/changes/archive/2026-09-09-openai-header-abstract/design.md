## Context

`ai.headers` 通用化已落地，但 class 名仍為 `Opencode*`，Anthropic 與 OpenAI 協議不同，需先於 OpenAI 層級抽象。

## Goals / Non-Goals

**Goals:**
- Class 名去 `opencode`，改 `OpenAiHeaderCustomizer`
- 邏輯改讀 `ai.headers` map，不判斷 host

**Non-Goals:**
- 不新增 Anthropic 支援（另案）

## Decisions

- **更名而非刪除**：保留 `OpenAiHttpClientBuilderCustomizer` 實作，僅更名與去寫死判斷。替代：保留 `Opencode` 名 → 與通用化目標衝突 → 捨棄。
- **Anthropic 另起**：未來 `Anthropic` 需 `x-api-key` 與不同路徑，另建 `AnthropicHeaderCustomizer`，不與此共用。

## Risks / Trade-offs

- [更名搬移] 需同步更新文件提及處 → Mitigation：僅 1 檔，`grep` 可全量替換

## Migration Plan

1. 更名檔案 → 替換註解/日誌 → `mvn test` 綠燈
2. `openspec validate` 通過

## Open Questions

- 無
