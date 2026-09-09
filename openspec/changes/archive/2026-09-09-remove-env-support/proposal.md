## Why

目前 `application.yaml` 大量使用 `${ENV_VAR:default}`（`JWT_SECRET`、`ADMIN_PASSWORD`、`CORS_ALLOWED_ORIGINS`、`AI_API_KEY`、`JWT_REMEMBER_ME_EXPIRATION_MS` 等），`PhytotrackTomlEnvironmentPostProcessor` 亦以 `System.getenv("AI_API_KEY")` 覆蓋 TOML，且文件廣泛提及 `JWT_SECRET` / `CORS_ALLOWED_ORIGINS` 等環境變數，導致設定分散於 env 與 TOML 兩處，易遺漏且與「單一 `phytotrack.toml` 配置」目標衝突。需收斂為僅 TOML + `application.yaml` 內建預設，移除所有應用層 env 解析與文件。

## What Changes

- **後端 env 解析移除**：`application.yaml` 內所有 `${ENV_VAR:default}` 改為純預設值（`app.jwt.secret` 回開發預設明文、`app.bootstrap.*`、`spring.ai.openai.*`、`app.cors.allowed-origins`、`app.jwt.remember-me-expiration-ms` 等不再含 `${...}`）；`PhytotrackTomlEnvironmentPostProcessor` 移除 `System.getenv("AI_API_KEY")` 覆蓋與 `.env` WARN 分支；`JwtSecretValidator` 提示改為「請於 `phytotrack.toml` 的 `app.jwt.secret` 設定正式密鑰」；`CorsConfig` 等不再提 env
- **XDG/AppImage 保留**：`BinaryPaths` 的 `System.getenv("XDG_*"/"APPIMAGE"/"OWD")` 屬作業系統路徑契約，不屬應用 env，予以保留並於規格中註記屬例外
- **.env 完全退役**：`backend/.env.example` 標示刪除或清空為註解範例，啟動不再檢查 `backend/.env`；相關載入分支刪除
- **文件同步**：`docs/ARCHITECTURE.md`、`docs/DEPLOY.md`、`README.md`、`docs/adr/ADR-012`、`docs/notebook/*`、`application.yaml` 註解等移除 `JWT_SECRET` / `CORS_ALLOWED_ORIGINS` / `AI_API_KEY` 等環境變數說明，改為指向 `phytotrack.toml` 對應鍵（`app.jwt.secret`、`app.cors.allowed-origins`、`ai.api-key` 等）

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `security-hardening`: JWT 密鑰與帳密不再由環境變數提供，改由 `phytotrack.toml` 與 `application.yaml` 內建預設；fail-fast 提示改 TOML 路徑

## Impact

- 後端：`resources/application.yaml`、`config/PhytotrackTomlEnvironmentPostProcessor.java`、`config/JwtSecretValidator.java`、`config/CorsConfig.java`、`backend/.env.example`（刪除/留空）、`service/DataInitializer.java` 註解
- 文件：`docs/ARCHITECTURE.md`、`docs/DEPLOY.md`、`README.md`、`docs/adr/ADR-012`、`docs/notebook/security/*`
- 部署：**BREAKING** — 既有依賴 `JWT_SECRET` 等環境變數的部署需改寫 `phytotrack.toml` 對應鍵；無資料庫遷移
