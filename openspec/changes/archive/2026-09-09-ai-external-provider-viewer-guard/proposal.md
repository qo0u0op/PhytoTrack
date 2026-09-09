## Why

現行 AI 僅支援本機 `llama.cpp`（`lllama-server :11435`），雖資料不出機但模型能力受限；產品仍以本地資料主權為核心，需在不外洩個人資料前提下，開放任意 OpenAI 相容 provider（本機或外部）作為可選，提升診斷品質同時守住個資邊界。

## What Changes

- **Provider 可選化**：`phytotrack.toml` 的 `[ai]` 段落由單一 `base-url` 擴為 `provider = "local" | "external"` + `base-url`/`model`/`api-key` 組，預設 `local`（`http://localhost:11435`），切換 `external` 時指向外部 OpenAI 相容端點（如 `https://api.openai.com/v1`）；`PhytotrackTomlEnvironmentPostProcessor` 與 `AIService` 據此組出 `ChatClient`，文件標明外部模式下資料將外送
- **Viewer 權限隔離**：`AIService.analyze` 組 prompt 前，先將案件資料經 `VIEWER` 視角過濾（`SenderService`/`CaseService` 的個資遮蔽：`name/phone/address` → `***`，僅保留縣市鄉鎮、作物、病蟲害等非個資欄位），再送 provider；確保外部 provider 僅見 Viewer 可見範圍，與 `sender-management` 已有 `VIEWER 個資遮蔽` 一致
- **可觀測與開關**：`ai.enabled` 仍為總開關，外部模式下 `GET /api/ai/health` 檢查外部端點可達性；日誌標記 `provider=external` 以利稽核

## Capabilities

### New Capabilities
- `ai-provider`: 外部 OpenAI 相容 provider 支援與 Viewer 權限隔離

### Modified Capabilities
- `sender-management`: AI 送審資料適用 Viewer 遮蔽規則（個資不進 prompt）
- `case-report`: AI 診斷可選外部 provider（文件與健康檢查同步）

## Impact

- 後端：`config/PhytotrackTomlEnvironmentPostProcessor.java`、`service/AIService.java`、`service/CaseService.java`（或抽 `ViewerFilter`）、`phytotrack.toml.example`、`application.yaml`（`ai.provider` 預設 `local`）
- 前端：`views/CaseFormView.vue` AI 診斷提示補「外部模式將外送去識別化資料」文案（可選）
- 文件：`docs/ARCHITECTURE.md` AI 章節、`docs/DEPLOY.md` AI 設定、`docs/adr/ADR-009-llama-proxy.md` 補修訂段落
- 風險：外部模式需管理 `api-key`，預設 `local` 不影響現行離線使用者
