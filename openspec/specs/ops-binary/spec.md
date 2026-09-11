# ops-binary Specification

## Purpose
為 Binary 交付提供三模式目錄契約：Windows Portable 與 Linux AppImage 為可攜（同目錄）、Unix deb/brew 為 XDG，並提供單一 `phytotrack.toml`、AI 開關、JWT 首次亂數、防火牆提示與自動開瀏覽器。

## Requirements

### Requirement: 目錄契約與自動生成

系統 SHALL 依執行環境決定 `phytotrack.toml`、`diagnoses.db`、`phytotrack.log` 落點並於缺失時自動生成預設檔與目錄；生成為冪等，後續啟動不覆蓋已存在檔。優先順序：`APPIMAGE` > `isWindows` > XDG。

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

### Requirement: 單一 TOML 配置（移除 env 主路徑）

系統 SHALL 以 `phytotrack.toml` 為單一真相，提供全量可配：`server.port`、`spring.datasource.url`、`logging.file.name`、`app.jwt.secret`、`app.jwt.expiration-ms`、`app.bootstrap.*`、`ai.base-url/model/enabled`、`app.cors.allowed-origins`、`app.rate-limit.*`、`app.security-headers.enabled`、`springdoc`；`backend/.env` 退場，僅 `AI_API_KEY` 仍支援 `AI_API_KEY` env 覆蓋 TOML。

#### Scenario: TOML 驅動 port
- **WHEN** `phytotrack.toml` 設 `server.port=9090`
- **THEN** 服務監聽 `9090`，`http://localhost:9090` 可達，8080 不再監聽

#### Scenario: AI_API_KEY env 覆蓋
- **WHEN** `phytotrack.toml` 設 `ai.api-key="toml-key"` 且 env `AI_API_KEY=env-key`
- **THEN** 實際使用 `env-key`（env 優先）

#### Scenario: 舊 .env 不再主路徑
- **WHEN** 僅存在 `backend/.env` 而無 `phytotrack.toml`
- **THEN** 啟動仍成功但印 `WARN backend/.env 已棄用，請遷至 phytotrack.toml`，`.env` 僅相容一版

### Requirement: AI 總開關

系統 SHALL 提供 `ai.enabled`（預設 `true`），`false` 時後端不註冊 `/api/ai/*` 且前端不顯示 AI 診斷入口。

#### Scenario: AI 關閉時後端
- **WHEN** `phytotrack.toml` 設 `ai.enabled=false` 後以任意角色 `POST /api/ai/diagnose`
- **THEN** 回 `404` 或 `503` 且 `error.code=AI_DISABLED`

#### Scenario: AI 關閉時前端
- **WHEN** `ai.enabled=false` 且以 STAFF 登入
- **THEN** 案件詳情頁不顯示「AI 診斷」按鈕

#### Scenario: AI 開啟時正常
- **WHEN** `ai.enabled=true`（預設）且 llama-server 存活
- **THEN** `POST /api/ai/diagnose` 正常回診斷

### Requirement: JWT 首次亂數與提示

系統 SHALL 於 `phytotrack.toml` 無 `app.jwt.secret` 時亂數 48 bytes（Base64URL）寫入 TOML，並於 console 以中文印「首次啟動已生成亂數密鑰，舊 token 失效請重新登入」；後續啟動沿用，刪 TOML 重生成為預期。

#### Scenario: 首次啟動生成
- **WHEN** 刪 `phytotrack.toml` 後首次啟動
- **THEN** 新 `phytotrack.toml` 含 `app.jwt.secret="<random>"`，console 印提示，舊 token 呼叫 `GET /api/cases` 回 `401`

#### Scenario: 已存在不重生成
- **WHEN** 再次啟動且 `phytotrack.toml` 已有 `app.jwt.secret`
- **THEN** 不重生成，舊 token 仍有效（未過期前提下）

### Requirement: Windows 防火牆提示

Windows 下系統 SHALL 於啟動後偵測 `server.port` 防火牆規則，若未放行則於 console 印 PowerShell 提示指令，不自動提權。

#### Scenario: 未放行提示
- **WHEN** 以 Windows 啟動且 `netsh advfirewall firewall show rule name="PhytoTrack"` 無規則
- **THEN** console 印 `netsh advfirewall firewall add rule name="PhytoTrack" dir=in action=allow protocol=TCP localport=${port}` 提示

#### Scenario: 已放行不提示
- **WHEN** 同上但規則已存在
- **THEN** 不印提示

### Requirement: 自動開瀏覽器

系統 SHALL 於 `ApplicationReadyEvent` 後自動以系統預設瀏覽器開前端 `http://localhost:${server.port}/`（binary 已內嵌前端 dist，/ 與 /api 同 port），可由 `app.ui.auto-open-browser=false` 關閉；失敗僅 log。

#### Scenario: 預設自動開
- **WHEN** 以預設 `phytotrack.toml` 啟動（`auto-open-browser=true`）
- **THEN** 系統瀏覽器自動開前端 `http://localhost:8080/`（或實際 `server.port` 的 `/`），console 同步印 `Server started at ... (前端)` 與 `API: .../api`

#### Scenario: 關閉自動開
- **WHEN** `phytotrack.toml` 設 `app.ui.auto-open-browser=false`
- **THEN** 不開瀏覽器，僅 console 印 `Server started at http://localhost:${port}/ (auto-open disabled)`

#### Scenario: 無桌面環境回落
- **WHEN** 以 Unix 無 `Desktop` 支援啟動
- **THEN** 回落 `xdg-open` / `open`，失敗僅 `log.warn`，不影響服務啟動

### Requirement: 配置文件即手冊

`phytotrack.toml` 與其範例 `phytotrack.toml.example` SHALL 達到「config as a manual」等級：單檔即可離線指導部署，無需跳轉外部文件；每鍵註釋含類型/預設/可選值。首次啟動生成的 `phytotrack.toml` 與倉庫 `phytotrack.toml.example` 的註釋 SHALL 同源一致。

#### Scenario: 範例檔含手冊頭與節說明

- **WHEN** 檢視 `backend/phytotrack.toml.example`
- **THEN** 檔頭含路徑契約（Windows `config/phytotrack.toml` / AppImage 同目錄 / XDG `~/.config/phytotrack/phytotrack.toml` / 系統級 `/etc/phytotrack/phytotrack.toml`）與優先順序、首次生成與冪等說明、保存起點指引 `cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml`，且每節（`[server]`、`[app.jwt]`、`[app.bootstrap]`、`[ai]`、`[ai.headers]`、`[app.cors]`、`[app.rate-limit]`、`[app.security-headers]`、`[app.tray]`、`[app.ui]`、`[springdoc]`）前有節級說明塊

#### Scenario: 欄位五要素註釋

- **WHEN** 檢視任意已暴露欄位（如 `server.port`、`app.jwt.secret`、`app.jwt.expiration-ms`、`ai.provider/base-url/model/api-key/temperature/max-tokens/max-context-tokens/reasoning-effort`、`app.cors.allowed-origins`、`app.rate-limit.*`、`app.tray.enabled`、`app.ui.auto-open-browser`、`springdoc`、`spring.datasource.url`/`logging.file.name`）
- **THEN** 其相鄰註釋含 `用途 | 類型 | 預設 | 取值/範例 | 生效/重啟需求` 五要素，敏感欄位標註首次亂數與「刪檔重生成」警告，`ai` 節標註 `AI_API_KEY` env 覆蓋規則，`app.bootstrap` 標註「帳號口令不可配」約束

#### Scenario: 範例與生成檔同源

- **WHEN** 比較 `backend/phytotrack.toml.example` 與 `PhytotrackTomlEnvironmentPostProcessor.generateDefaultToml()` 生成的預設檔（刪空 `config/phytotrack.toml` 後首次啟動產物）
- **THEN** 二者節順序、欄位集合與手冊級註釋一致（僅 `app.jwt.secret` 隨機值不同），`grep -c "^#"` 差異小於 5 行且無欄位缺漏，無效值回落安全預設並印啟動警告

#### Scenario: 關聯指引不分叉

- **WHEN** 檢視 `docs/DEPLOY.md` 與 `docs/ARCHITECTURE.md` 中對 `phytotrack.toml` 的引用
- **THEN** 引用語為「以檔內手冊為準」或指向節錨點，不重複羅列欄位表，避免文件與檔內註釋分叉

#### Scenario: 離線可讀性

- **WHEN** 在無網路環境僅持 `phytotrack.toml.example` 部署
- **THEN** 依檔內註釋可完成 `port` 修改、`ai.enabled` 關閉、`provider` 切 `external`、`tray/ui` 開關與 `CORS/rate-limit` 調整，無需查閱外部連結

#### Scenario: 保存起點指引

- **WHEN** 檢視範例檔頭
- **THEN** 含保存起點指引 `cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml`，說明無需手寫即可得完整起點
