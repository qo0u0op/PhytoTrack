# ops-binary Delta

## MODIFIED Requirements

### Requirement: 目錄契約與自動生成

系統 SHALL 依執行環境決定 `phytotrack.toml`、`diagnoses.db`、`phytotrack.log` 落點並於缺失時自動生成預設檔與目錄；生成為冪等，後續啟動不覆蓋已存在檔。優先順序：`APPIMAGE` > `isWindows` > XDG。**當 `spring.profiles.active` 含 `prod`（含所有 binary 交付物）時，首次生成的 `phytotrack.toml` SHALL 以 `backend/phytotrack.toml.example` 手冊模板為基底並以 `isProd patch` 產生 prod 預設（`[app.security-headers] enabled=true`、`[springdoc] api-docs-enabled=false, swagger-ui-enabled=false`，其餘與 example 一致，`app.jwt.secret` 仍亂數）；當 `dev/test` 時 SHALL 直接以 example 原值生成（`security-headers=false, springdoc=true`）。**

#### Scenario: Windows 可攜首次啟動

- **WHEN** 以 Windows 啟動且 `.\config\phytotrack.toml` 不存在
- **THEN** 於 `exe` 所在目錄生成 `config/phytotrack.toml`（含註解與預設 `server.port=8080`、`ai.enabled=true`、`app.jwt.secret=<random>`）、`data/diagnoses.db`（SQLite 建表）、`logs/phytotrack.log` 目錄，並啟動成功

#### Scenario: Windows 已存在不覆蓋

- **WHEN** 再次以 Windows 啟動且 `config/phytotrack.toml` 已存在
- **THEN** 不覆蓋既有 `phytotrack.toml` 與 `data/diagnoses.db`，直接載入

#### Scenario: AppImage 可攜首次啟動

- **WHEN** 以 Linux AppImage 啟動（`APPIMAGE` 環境變數存在）且 `$(dirname $APPIMAGE)/config/phytotrack.toml` 不存在
- **THEN** 於 AppImage 所在目錄生成 `config/phytotrack.toml`、`data/diagnoses.db`、`logs/phytotrack.log`，不走 XDG，並啟動成功

#### Scenario: AppImage 已存在不覆蓋

- **WHEN** 再次以 AppImage 啟動且同目錄 `config/phytotrack.toml` 已存在
- **THEN** 不覆蓋，直接載入

#### Scenario: Unix XDG 首次啟動（deb/brew）

- **WHEN** 以 Unix 非 AppImage 啟動且 `$XDG_CONFIG_HOME/phytotrack/phytotrack.toml`（回落 `~/.config/phytotrack/phytotrack.toml`）不存在
- **THEN** 於 `XDG_CONFIG_HOME` 生成 `phytotrack/phytotrack.toml`，`XDG_DATA_HOME` 生成 `phytotrack/diagnoses.db`，`XDG_STATE_HOME` 生成 `phytotrack/phytotrack.log`，並啟動成功

#### Scenario: Unix 系統級配置

- **WHEN** 以 Unix 非 AppImage 啟動且 `/etc/phytotrack/phytotrack.toml` 存在
- **THEN** 以 `/etc/phytotrack/phytotrack.toml` 為系統預設，家目錄 `~/.config` 覆蓋之（`spring.config.import` 順序：系統級 < 家目錄）

#### Scenario: Binary 首次生成必為 prod

- **WHEN** 以 binary 交付物（`jpackage app-image` 的 `phytotrack.exe` / `AppImage` / 未來 `deb/rpm` 基座，`spring.profiles.active=prod`）啟動且 `config/phytotrack.toml` 不存在
- **THEN** 生成的 `phytotrack.toml` 為 prod 預設（`app.security-headers.enabled=true, springdoc.api-docs-enabled=false, springdoc.swagger-ui-enabled=false`），`app.tray.enabled=true, app.ui.auto-open-browser=true` 保持 GUI 預設

#### Scenario: dev/test 首次生成保持 dev

- **WHEN** 以 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 或 `test` 啟動且對應 `phytotrack.toml` 不存在
- **THEN** 生成的 `phytotrack.toml` 為 dev 預設（`app.security-headers.enabled=false, springdoc.api-docs-enabled=true, springdoc.swagger-ui-enabled=true`）

### Requirement: 自動開瀏覽器

系統 SHALL 於 `ApplicationReadyEvent` 後自動以系統預設瀏覽器開前端 `http://localhost:${server.port}/`（binary 已內嵌前端 dist，/ 與 /api 同 port），可由 `app.ui.auto-open-browser=false` 關閉；失敗僅 log。**當 `phytotrack.toml` 為有頭配置（`auto-open-browser=true`）但執行環境為 SSH（含 `linux→windows server` 的 `SSH_CONNECTION/SSH_CLIENT/SSH_TTY`）或無 `DISPLAY`/`WAYLAND_DISPLAY`（非 Windows）或 `GraphicsEnvironment.isHeadless()` 時，系統 SHALL 臨時回落為 headless（跳過 `Desktop.browse` / `xdg-open` / `rundll32`），僅 `log.info`，不修改 `phytotrack.toml`，下次 GUI 環境仍有頭。**

#### Scenario: 預設自動開

- **WHEN** 以預設 `phytotrack.toml` 啟動（`auto-open-browser=true`）且為 GUI 環境
- **THEN** 系統瀏覽器自動開前端 `http://localhost:8080/`（或實際 `server.port` 的 `/`），console 同步印 `Server started at ... (前端)` 與 `API: .../api`

#### Scenario: 關閉自動開

- **WHEN** `phytotrack.toml` 設 `app.ui.auto-open-browser=false`
- **THEN** 不開瀏覽器，僅 console 印 `Server started at http://localhost:${port}/ (auto-open disabled)`

#### Scenario: 無桌面環境回落

- **WHEN** 以 Unix 無 `Desktop` 支援啟動
- **THEN** 回落 `xdg-open` / `open`，失敗僅 `log.warn`，不影響服務啟動

#### Scenario: SSH 臨時回落不寫盤

- **WHEN** 以 `SSH_CONNECTION=1`（或 `SSH_CLIENT` / `SSH_TTY`，含 `linux → windows server` 的 Windows SSH 會話）啟動且 `phytotrack.toml` 為 `auto-open-browser=true`
- **THEN** 跳過自動開瀏覽器，`log.info` 含 `SSH=true`，`phytotrack.toml` 保持 `auto-open-browser=true`，下次 GUI 啟動仍自動開

#### Scenario: Windows SSH 優先於 isWindows

- **WHEN** 以 Windows 啟動且 `SSH_CONNECTION` 存在
- **THEN** 判定為 headless（`isSsh` 優先於 `isWindows` 的 `hasDisplay=true`），跳過 `Desktop.browse` 與 `rundll32`，不因 `isWindows` 誤判為有頭

### Requirement: 配置文件即手冊

`phytotrack.toml` 與其範例 `phytotrack.toml.example` SHALL 達到「config as a manual」等級：單檔即可離線指導部署，無需跳轉外部文件；每鍵註釋含類型/預設/可選值。**首次啟動生成的 `phytotrack.toml` SHALL 以 `backend/phytotrack.toml.example` 為基底，僅當 `isProd=true` 時對 prod 敏感鍵打 patch（`security-headers true / springdoc false`），其餘節順序、欄位集合與手冊級註釋保持一致；`dev/test` 生成時與範例同源一致。**

#### Scenario: 範例檔含手冊頭與節說明

- **WHEN** 檢視 `backend/phytotrack.toml.example`
- **THEN** 檔頭含路徑契約（Windows `config/phytotrack.toml` / AppImage 同目錄 / XDG `~/.config/phytotrack/phytotrack.toml` / 系統級 `/etc/phytotrack/phytotrack.toml`）與優先順序、首次生成與冪等說明、保存起點指引 `cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml`，且每節（`[server]`、`[app.jwt]`、`[app.bootstrap]`、`[ai]`、`[ai.headers]`、`[app.cors]`、`[app.rate-limit]`、`[app.security-headers]`、`[app.tray]`、`[app.ui]`、`[springdoc]`）前有節級說明塊

#### Scenario: 欄位五要素註釋

- **WHEN** 檢視任意已暴露欄位（如 `server.port`、`app.jwt.secret`、`app.jwt.expiration-ms`、`ai.provider/base-url/model/api-key/temperature/max-tokens/max-context-tokens/reasoning-effort`、`app.cors.allowed-origins`、`app.rate-limit.*`、`app.tray.enabled`、`app.ui.auto-open-browser`、`springdoc`、`spring.datasource.url`/`logging.file.name`）
- **THEN** 其相鄰註釋含 `用途 | 類型 | 預設 | 取值/範例 | 生效/重啟需求` 五要素，敏感欄位標註首次亂數與「刪檔重生成」警告，`ai` 節標註 `AI_API_KEY` env 覆蓋規則，`app.bootstrap` 標註「帳號口令不可配」約束

#### Scenario: 範例與生成檔同源

- **WHEN** 比較 `backend/phytotrack.toml.example` 與 `TomlGenerator.generateDefaultToml(path, false)` 生成的 dev 預設檔（刪空 `config/phytotrack.toml` 後以 `isProd=false` 首次啟動產物）
- **THEN** 二者節順序、欄位集合與手冊級註釋一致（僅 `app.jwt.secret` 隨機值不同），`grep -c "^#"` 差異小於 5 行且無欄位缺漏，無效值回落安全預設並印啟動警告

#### Scenario: 範例與生成檔 prod patch

- **WHEN** 比較 `backend/phytotrack.toml.example` 與 `TomlGenerator.generateDefaultToml(path, true)` 生成的 prod 預設檔（`isProd=true`）
- **THEN** 生成檔除 `app.security-headers.enabled=true` 與 `springdoc.api-docs-enabled=false, springdoc.swagger-ui-enabled=false` 的 prod patch 外，其餘節順序、欄位集合與手冊級註釋與範例一致

#### Scenario: 關聯指引不分叉

- **WHEN** 檢視 `docs/DEPLOY.md` 與 `docs/ARCHITECTURE.md` 中對 `phytotrack.toml` 的引用
- **THEN** 引用語為「以檔內手冊為準」或指向節錨點，不重複羅列欄位表，避免文件與檔內註釋分叉

#### Scenario: 離線可讀性

- **WHEN** 在無網路環境僅持 `phytotrack.toml.example` 部署
- **THEN** 依檔內註釋可完成 `port` 修改、`ai.enabled` 關閉、`provider` 切 `external`、`tray/ui` 開關與 `CORS/rate-limit` 調整，無需查閱外部連結

#### Scenario: 保存起點指引

- **WHEN** 檢視範例檔頭
- **THEN** 含保存起點指引 `cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml`，說明無需手寫即可得完整起點

## ADDED Requirements

### Requirement: 系統托盤有頭→無頭臨時回落

系統托盤（`SystemTrayManager`，`app.tray.enabled=true` 為 GUI 預設）當執行環境為 SSH（含 `linux→windows server`）或無 `DISPLAY`/`WAYLAND_DISPLAY`（非 Windows）或 `GraphicsEnvironment.isHeadless()` 時 SHALL 臨時回落為 headless（跳過 `dorkbox SystemTray` 與 Swing 備用視窗），僅 `log.info`，不修改 `phytotrack.toml`，下次 GUI 環境仍有頭；`isSsh` 判定優先於 `isWindows` 的 `hasDisplay=true`。

#### Scenario: GUI 有托盤

- **WHEN** 以 GUI 環境啟動且 `app.tray.enabled=true`
- **THEN** 顯示 `PhytoTrack` 托盤（或 Swing 備用視窗），含五選單

#### Scenario: SSH 臨時無托盤不寫盤

- **WHEN** 以 `SSH_CONNECTION=1` 啟動（含 Windows SSH）且 `phytotrack.toml` 為 `app.tray.enabled=true`
- **THEN** 跳過系統托盤初始化，`log.info` 含 `SSH=true`，`phytotrack.toml` 保持 `enabled=true`

#### Scenario: Windows SSH 不誤判有頭

- **WHEN** 以 Windows 啟動且 `SSH_CONNECTION` 存在
- **THEN** 即使 `hasDisplay` 為 true，仍判定為 headless，跳過托盤，不因 `isWindows` 誤判
