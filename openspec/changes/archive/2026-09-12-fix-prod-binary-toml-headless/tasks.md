# Tasks: fix-prod-binary-toml-headless

## 1. Workflow binary 必為 prod

- [x] 1.1 修改 `.github/workflows/release.yml`：將 `EXTRA_JAVA="--java-options -Dspring.profiles.active=prod"` 從僅 Windows 改為全平台（`windows-latest` 與 `ubuntu-latest` 皆注入），驗證 `grep -n "EXTRA_JAVA" .github/workflows/release.yml` 兩平台皆有且 `jpackage` 行含 `$EXTRA_JAVA`
- [x] 1.2 驗證 `AppRun` 透傳邏輯保留（`grep spring.profiles.active` 仍可覆蓋 prod），且 `dist/phytotrack/bin/phytotrack --help` 直跑預設為 prod（文件化 prod-only，不提供切 dev 路徑）

## 2. TOML 首次生成 isProd patch

- [x] 2.1 修改 `backend/src/main/java/com/d0w0b/phytotrack/config/TomlGenerator.java`：`generateDefaultToml(path, isProd)` 以 example 模板為基底，`isProd=true` 時 patch `app.security-headers.enabled false→true`、`springdoc.api-docs-enabled/swag true→false`，`isProd=false` 保持原值，驗證 `mvn test -Dtest=TomlGeneratorTest` 或手動 `rm -rf /tmp/cfg && java -Dspring.profiles.active=prod -jar` 生成檔 `grep` prod 鍵
- [x] 2.2 確認 `PhytotrackTomlEnvironmentPostProcessor.java` 已透傳 `isProd`（`TomlLoader.isProdProfile(env)`），無需另改，驗證 `grep -n "generateDefaultToml" backend/src/main/java/com/d0w0b/phytotrack/config/PhytotrackTomlEnvironmentPostProcessor.java` 含 `isProd`

## 3. 有頭→無頭臨時回落（含 Windows SSH）

- [x] 3.1 修改 `backend/src/main/java/com/d0w0b/phytotrack/util/DesktopEnvironment.java`：註解與邏輯明確 `isSsh` 優先於 `isWindows`，`hasDisplay()` 的 Windows true 僅非 SSH 時視為有頭，`isHeadlessOrSsh()` 順序 `isSsh → (!hasDisplay && !isWindows) → isHeadless`，驗證 `mvn test -Dtest=DesktopEnvironmentTest` 或手動 `SSH_CONNECTION=1` 啟動日誌含 `SSH=true`
- [x] 3.2 修改 `backend/src/main/java/com/d0w0b/phytotrack/PhytoTrackApplication.java`：保持 `isSsh` 先行設 `headless=true`，Windows 非 SSH 才 `headless=false`，驗證 `grep -n "isSsh\|hasDisplay" PhytoTrackApplication.java`
- [x] 3.3 修改 `backend/src/main/java/com/d0w0b/phytotrack/config/BrowserOpener.java` 與 `SystemTrayManager.java`：統一以 `isSsh()` / `isHeadlessOrSsh()` 先行 return，僅 log 不改 TOML，下次 GUI 仍有頭，驗證 `SSH_CONNECTION=1 mvn spring-boot:run -Dspring-boot.run.profiles=prod` 的 `log.info` 含 `SSH=true, headlessProp=true` 且不創托盤/不開瀏覽器
- [x] 3.4 補 `linux→windows server` SSH 場景的 log 診斷（`os / isSsh / hasDisplay / headlessProp / DISPLAY / WAYLAND_DISPLAY`），驗證 Windows 上 `SSH_CONNECTION=1` 啟動仍判定 headless

## 4. 文件與驗證

- [x] 4.1 同步 `backend/phytotrack.toml.example` 註釋（標明 prod patch 鍵的 `isProd` 語意）、`docs/DEPLOY.md#12` 與 `docs/ARCHITECTURE.md#6`（交付物必為 prod、臨時回落不寫盤），驗證 `grep -rn "isProd\|臨時回落" docs/ backend/phytotrack.toml.example`
- [x] 4.2 執行 `openspec validate --specs --changes` 與 `mvn test`（`backend`）與 `npm run build`（`frontend`）與 `typst compile docs/manual.typ /tmp/manual.pdf` 皆通過，驗證 `ls /tmp/manual.pdf`
