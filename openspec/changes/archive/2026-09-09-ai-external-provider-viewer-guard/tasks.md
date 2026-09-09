## 1. Provider 可選與 Viewer 隔離

- [x] 1.1 於 `phytotrack.toml.example` 補 `[ai]` 的 `provider = "local"` 註解（可選 `external` + `base-url/model/api-key`），`application.yaml` 增 `ai.provider=local` 預設，`PhytotrackTomlEnvironmentPostProcessor.java` 依 `provider` 映射 `spring.ai.openai.base-url/api-key/model`，`mvn test` 綠燈
- [x] 1.2 於 `service/AIService.java` 注入 Viewer 過濾（新增 `util/ViewerFilter`，`analyze` 前 `filterForViewer`），`analyze` 組 prompt 前將 `name/phone/address/displayName` 視為 `***`（現行 AnalyzeRequest 本身不含個資，結構化保障），外部與本地皆適用，`mvn test` 綠燈
- [x] 1.3 於 `util/ViewerFilter` 確保 Viewer 規則可被 AI 重用，前端 `CaseFormView.vue` AI 區在 `provider=external` 時顯示「外部模式：僅送 Viewer 可見資料」提示（`aiProvider` 來自 `GET /api/ai/health` 的 `provider`），`npm run build` 通過

## 2. 文件與驗證

- [x] 2.1 更新 `docs/ARCHITECTURE.md` AI 章節與 `docs/DEPLOY.md` AI 設定，說明 `ai.provider` 雙模式與 Viewer 隔離，`docs/adr/ADR-009-llama-proxy.md` 補修訂段落，`openspec validate --specs --changes` 16 passed
