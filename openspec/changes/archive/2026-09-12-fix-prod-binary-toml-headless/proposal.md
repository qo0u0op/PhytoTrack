# Proposal: fix-prod-binary-toml-headless

## Why

Workflow 產生的 binary（AppImage / win exe / 未來 deb/rpm）與其首次自動生成的 `phytotrack.toml` 出現回歸：binary 不保證 prod、生成檔混種 dev 值；且有頭配置（`app.tray`/`app.ui`）在 SSH（含 linux→windows server）時未明確臨時回落 headless，導致托盤/自動開瀏覽器誤判或啟動噪音。

## What Changes

- **Binary 必為 prod**：所有 `jpackage --type app-image` 交付物（`windows-latest` 與 `ubuntu-latest`，未來 deb/rpm 同理）皆在 jpackage 階段嵌入 `--java-options -Dspring.profiles.active=prod`，`AppRun` 仍保留透傳覆蓋 `prod` 的邏輯但不再是唯一 prod 來源；直接執行 `dist/phytotrack/bin/phytotrack` 亦為 prod。
- **首次生成 TOML 必為 prod（isProd patch）**：`PhytotrackTomlEnvironmentPostProcessor` 計算 `isProd` 並傳入 `TomlGenerator.generateDefaultToml(path, isProd)`；`isProd=true` 時在 `phytotrack.toml.example` 手冊模板基礎上 patch prod 敏感鍵：`[app.security-headers] enabled false→true`、`[springdoc] api-docs-enabled/swagger-ui-enabled true→false`（其餘 `tray/ui` 已為 true 保持），`isProd=false`（`spring-boot:run dev/test`）保持 example 原值。
- **有頭→無頭臨時回落，不寫盤**：`phytotrack.toml` 保持 GUI 預設（`tray=true, ui=true`）；當 `DesktopEnvironment.isSsh()` 或（非 Windows 且 `!hasDisplay`）或 `GraphicsEnvironment.isHeadless()` 為真時，`PhytoTrackApplication` 設 `java.awt.headless=true`、`BrowserOpener`/`SystemTrayManager` 跳過 Desktop/Tray 初始化，僅 `log.info`，不修改 TOML，下次 GUI 環境仍有頭。明確覆蓋 `linux→windows server` 的 SSH 場景（`isSsh` 優先於 `isWindows`）。
- **BREAKING**: Binary 交付物不再提供切回 `dev` 的方法（移除透過配置或啟動參數回 `dev` 的文件化路徑，僅工程上可透傳覆蓋但不予支援）。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `ops-binary`: 首次生成 TOML 的 prod 語意與有頭→無頭臨時回落（含 Windows SSH）
- `ops-binary-packaging`: jpackage 矩陣的 prod 嵌入契約（全平台）

## Impact

- `backend/src/main/java/com/d0w0b/phytotrack/config/TomlGenerator.java`：新增 `isProd` 分支 patch
- `backend/src/main/java/com/d0w0b/phytotrack/config/PhytotrackTomlEnvironmentPostProcessor.java`：確保 `isProd` 透傳
- `backend/src/main/java/com/d0w0b/phytotrack/util/DesktopEnvironment.java`：釐清 `isSsh` 優先於 `isWindows`，`hasDisplay` 語意
- `backend/src/main/java/com/d0w0b/phytotrack/PhytoTrackApplication.java`、`config/BrowserOpener.java`、`config/SystemTrayManager.java`：統一以 `isHeadlessOrSsh()` / `isSsh` 先行判斷
- `.github/workflows/release.yml`：Linux app-image 補 `EXTRA_JAVA` prod 注入
- `backend/phytotrack.toml.example`：註釋需標明 prod patch 鍵
- `openspec/specs/ops-binary/spec.md`、`ops-binary-packaging/spec.md`：新增/修改 Requirement 與 Scenario
- `docs/DEPLOY.md`、`docs/ARCHITECTURE.md`：同步交付物必為 prod 與臨時回落說明
