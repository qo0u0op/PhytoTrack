## 1. 配置載入與 TOML 解析

- [x] 1.1 新增 `config/BinaryPaths.java` 與 `config/PhytotrackTomlProperties.java`（`@ConfigurationProperties(prefix="phytotrack")` 或 `app.*` + `server.port` 等），改 `application.yaml:8` 的 `spring.config.import` 為 `optional:file:${phytotrack.config:./config/phytotrack.toml},optional:file:${XDG_CONFIG_HOME}/phytotrack/phytotrack.toml,optional:file:/etc/phytotrack/phytotrack.toml`，並以 `System.setProperty` 將 `datasource.url`/`logging.file.name` 絕對化，驗證 `openspec validate` 與 `mvn test` 通過
- [x] 1.2 於 `PhytotrackTomlInitializer`（`ApplicationEnvironmentPreparedEvent`）實現首次自動生成：`config/phytotrack.toml`（Win 可攜）與 XDG 三路徑不存在即 `mkdirs` 並寫預設 TOML（含註解、全量鍵、`ai.enabled=true`、`app.jwt.secret=<random>` 若無），二次啟動不覆蓋，驗證刪 `config/` 後重啟自動生成

## 2. AI 開關與環境收斂

- [x] 2.1 新增 `ai.enabled`（預設 `true`）並以 `@ConditionalOnProperty` 守衛 `AIController` / `AIService` / `AiHealthIndicator`，`false` 時 `POST /api/ai/*` 回 `404 AI_DISABLED`，驗證 `phytotrack.toml ai.enabled=false` 後 `curl POST /api/ai/diagnose` 404
- [x] 2.2 移除 `backend/.env` 主路徑，`application.yaml` 內 `${AI_BASE_URL:}` 等改讀 TOML，僅 `AI_API_KEY` 保留 `env AI_API_KEY` 覆蓋 TOML，`.env.example` 標記棄用並印 `WARN backend/.env 已棄用` 相容一版，驗證 `AI_API_KEY=env-key` 優先於 TOML

## 3. 系統整合：防火牆與瀏覽器

- [x] 3.1 新增 `config/FirewallAdvisor.java`（僅 Win 生效，`ApplicationReadyEvent` 後 `netsh advfirewall show rule` 探測，無規則即 console 印 `netsh advfirewall firewall add rule ...` 提示，不提權），驗證 Win 下無規則時 console 出現提示
- [x] 3.2 新增 `config/BrowserOpener.java`（`ApplicationReadyEvent` 後 `Desktop.browse` → `xdg-open`/`open` 回落，受 `app.ui.auto-open-browser` 控制，預設 `true`），驗證啟動後自動開 `http://localhost:${server.port}`，`false` 時僅印 `Server started`

## 4. 打包與文件

- [x] 4.1 提供 `phytotrack.toml.example`（全量鍵含註解）與 `docs/DEPLOY.md` 補 Win Portable（解壓即用、勿放 `Program Files`）與 Unix XDG/包管理器（`/usr/bin/phytotrack`、`~/.config` 覆蓋 `/etc`）章節，並標記 `server.port` 可配，驗證 `phytotrack.toml server.port=9090` 後 `curl :9090` 可達
- [x] 4.2 新增 `config/BinaryPathsTest.java` 驗證 Win（`os.name=Windows 10`）落 `exeDir/config`、Unix（`os.name=Linux` + `XDG_CONFIG_HOME=/tmp/xdg`）落 XDG，且 `mvn test` 148+1 通過，`openspec validate --specs --changes` 13 passed
