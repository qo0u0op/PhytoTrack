## 1. 後端去 env

- [x] 1.1 移除 `resources/application.yaml` 內所有 `${ENV_VAR:default}`（`app.jwt.secret`、`app.jwt.remember-me-expiration-ms`、`app.bootstrap.*`、`spring.ai.openai.*`、`app.cors.allowed-origins` 等改為純預設），`mvn test` 綠燈（`phytotrack.*` 內部占位保留，非 env）
- [x] 1.2 刪除 `config/PhytotrackTomlEnvironmentPostProcessor.java` 的 `System.getenv("AI_API_KEY")` 覆蓋與 `.env` WARN 分支（保留 `BinaryPaths` XDG/AppImage 例外），`backend/.env.example` 已改為歷史註解，`mvn test` 綠燈
- [x] 1.3 `config/JwtSecretValidator.java` 提示改為 `phytotrack.toml:app.jwt.secret`，`config/CorsConfig.java` 註解去 env 字樣，`mvn test` 綠燈

## 2. 文件去 env

- [x] 2.1 批次替換 `docs/ARCHITECTURE.md`、`docs/DEPLOY.md`、`README.md`、`docs/adr/ADR-012` 等 `JWT_SECRET`/`CORS_ALLOWED_ORIGINS`/`AI_API_KEY` 字樣為對應 `phytotrack.toml` 鍵，主文件已為 0（`docs/notebook` 歷史保留）
- [x] 2.2 執行 `openspec validate --specs --changes` 21 passed，`grep -r "System.getenv" backend/src/main --include="*.java" | grep -v BinaryPaths` 為 0
