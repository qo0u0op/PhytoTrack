## Context

See proposal.md Why. 現制 `application.yaml:8` 以 `optional:file:.env` 疊加，未參數化 `server.port`/`datasource.url`/`logging.file.name`；`.env` 需手動放 `backend/` 且與 `application.yaml` 兩處改同一變數。Binary 需單 jar 同時滿足 Win 可攜與 Unix XDG。

## Goals / Non-Goals

**Goals:**
- 單一 `phytotrack.toml` 為真相，OS 自適應落點，無需使用者手動選 profile
- 未存在即生成預設 TOML/目錄，後續啟動冪等
- `ai.enabled` 可關閉 AI 能力，`JWT_SECRET` 首次亂數可落檔

**Non-Goals:**
- 不引入外部秘鑰庫（Vault/KMS），`AI_API_KEY` 以 env 覆蓋即足
- 不做 Win 安裝包簽署/自動更新
- 不改 `application-test.yaml` 的測試路徑（仍用 `target`）

## Decisions

- **TOML 庫**：`com.moandjiez:toml4j` 或 `org.tomlj:toml4j` 解析 `phytotrack.toml` 為 `PhytotrackTomlProperties`（`@ConfigurationProperties`），`spring.config.import: optional:file:${phytotrack.config:./config/phytotrack.toml}`。替代：YAML 亦可，但 TOML 對 Win 記事本更友善（無縮排敏感），且已約定 `phytotrack.toml`。
- **OS 偵測與路徑**：`BinaryPaths.java` 優先檢查 `APPIMAGE`（AppImage 可攜）→ `isWindows` → XDG。AppImage `base = Paths.get(APPIMAGE).getParent()`（`dirname $APPIMAGE`，回落 `OWD`），Win `base = CodeSource.getLocation().getParent()`，Unix `base = XDG_*`；`LOG_FILE` 與 `datasource.url` 以絕對路徑注入 `System.setProperty`。優先鏈 `APPIMAGE > isWindows > XDG > /etc`，AppImage 不走 XDG 以保可攜。
- **自動生成**：`PhytotrackTomlInitializer` 於 `ApplicationListener<ApplicationEnvironmentPreparedEvent>` 檢查 `config/phytotrack.toml` 不存在即 `Files.createDirectories` + 寫預設 TOML（內含註解、預設 `server.port=8080`、`ai.enabled=true`、`app.jwt.secret=<random>` 若無）。替代：啟動時 `CommandLineRunner` 生成，需 Spring 已啟動，失敗時無法回滾 → 前者更早。
- **JWT 亂數**：`SecureRandom 48 bytes → Base64URL`，寫入 TOML 的 `app.jwt.secret`，console 以 `AnsiOutput` 印提示。舊 token 失效為預期，不做遷移。
- **AI 開關**：`@ConditionalOnProperty("ai.enabled", havingValue="true")` 於 `AIController` 與 `AIService`，`AiHealthIndicator` 同條件；前端 `src/config/features.ts` 打包時注入 `VITE_AI_ENABLED` 或運行時 `GET /api/ai/health` 探測後隱藏。替代：後端 503，前端仍顯示按鈕易混淆 → 後端不註冊更乾淨。
- **防火牆提示**：`FirewallAdvisor.java` 僅 Win 且 `isWindows` 時，於 `ApplicationReadyEvent` 以 `netsh advfirewall firewall show rule name="PhytoTrack"` 探測，無規則即印 `PowerShell -Command Start-Process netsh -ArgumentList '...' -Verb RunAs` 提示，不自動提權。替代：自動提權需 UAC，易被防毒阻擋 → 提示即可。
- **自動開瀏覽器**：`BrowserOpener.java` 於 `ApplicationReadyEvent` 後 `Desktop.isDesktopSupported() ? Desktop.browse(URI) : Runtime.exec("xdg-open ...")`，`app.ui.auto-open-browser` 預設 `true`，Win/Unix 皆支援，失敗僅 log。替代：前端 `window.open` 需使用者先開瀏覽器 → 後端開更直接。

## Risks / Trade-offs

- [TOML 解析] 引入新依賴 → 鎖 `toml4j` 版本於 `pom.xml:properties`，無則退化為 `application.yaml` 外置。
- [路徑權限] Win `Program Files` 無寫入 → Portable 明確要求解壓至使用者可寫目錄（如 `C:\Tools\PhytoTrack` 或桌面），文件強調勿放 `Program Files`。
- [XDG 家目錄] 無 `HOME` 時回落 `user.dir` → 極少，僅容器無 `HOME`，log 警告。
- [JWT 覆寫] TOML 已有 `secret` 後 env `AI_API_KEY` 仍可覆蓋，`JWT_SECRET` 若同時在 TOML 與 env，env 優先 → 文件明載優先鏈。

## Migration Plan

1. 新增 `BinaryPaths`、`PhytotrackTomlProperties`、`FirewallAdvisor`、`BrowserOpener` 與 `phytotrack.toml.example`
2. 改 `application.yaml:8` 的 `spring.config.import` 與各 `${VAR:default}` 指向 TOML 屬性
3. `AIController` 加 `ai.enabled` 條件，`DataInitializer` 改讀 TOML bootstrap
4. 更新 `docs/DEPLOY.md` 與 `README.md`，標記 `backend/.env.example` 棄用但保留相容一版
5. 以 Win zip 與 deb 包驗證：刪 `config/` 後重啟自動生成，`curl` 與瀏覽器自動開驗證

## Open Questions

- 無 — `ai.enabled` 預設 `true` 保持現行可診斷，關閉為 opt-in。
