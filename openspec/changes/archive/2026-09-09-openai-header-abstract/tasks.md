## 1. 更名與通用化

- [x] 1.1 將 `config/OpencodeSessionHeaderCustomizer.java` 更名為 `config/OpenAiHeaderCustomizer.java`，移除 `host.contains("opencode.ai")` 判斷，改讀 `ai.headers` map（`auto`→UUID），`mvn test` 綠燈
- [x] 1.2 更新 `phytotrack.toml.example` 與 `docs/ARCHITECTURE.md` 中 `OpencodeSessionHeaderCustomizer` 提及為 `OpenAiHeaderCustomizer`，`openspec validate --specs --changes` 通過
