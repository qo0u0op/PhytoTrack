## Why

dev 以 `backend/.env` + `application.yaml:8 optional:file:.env` 疊加，`diagnoses.db` 與 `logs/` 相對 `cwd` 寫死，`server.port` 未參數化，無法直接打包為 binary 交付。Windows 需「可攜（portable）解壓即用、資料跟著 exe 走」，Unix 需「符合 XDG Base Dir、可由包管理器安裝、資料跟著家目錄走」，兩者落點與權限模型衝突，現制不具備。

## What Changes

- **BREAKING: 單一真相 `phytotrack.toml`**：全量可配收斂至 `phytotrack.toml`（TOML），移除 `backend/.env` 與 `optional:file:.env` 主路徑；僅 `AI_API_KEY` 保留 `AI_API_KEY` env 覆蓋（敏感）。其餘（`server.port`、`spring.datasource.url`、`logging.file.name`、`app.jwt.secret/expiration-ms`、`app.bootstrap.*`、`ai.base-url/model/enabled`、`cors/rate-limit/security-headers/springdoc`）皆由 TOML 驅動，`${VAR:default}` 改為 TOML 預設。
- **BREAKING: 目錄契約與自動生成**：Win 偵測 `os.name` 含 `win` 即 `base=exeDir`，`config/phytotrack.toml`、`data/diagnoses.db`、`logs/phytotrack.log` 相對 exe，未存在則首次啟動 `mkdirs` 並生成預設 TOML；Unix 遵循 XDG（`XDG_CONFIG_HOME:-~/.config/phytotrack/phytotrack.toml`、`XDG_DATA_HOME:-~/.local/share/phytotrack/diagnoses.db`、`XDG_STATE_HOME:-~/.local/state/phytotrack/phytotrack.log`，`/etc/phytotrack/phytotrack.toml` 作為系統級），包管理器安裝時 binary 落 `/usr/bin/phytotrack`。
- **AI 總開關**：新增 `ai.enabled`（或 `app.ai.enabled`）控制後端 `/api/ai/*` 是否註冊與前端是否顯示 AI 診斷入口（`false` 時後端 404/503，前端隱藏）。
- **JWT 首次亂數**：Win 可攜與 Unix 首次皆在無 `app.jwt.secret` 時亂數 48 bytes（`SecureRandom` + base64）寫入 TOML，並於 console 印「首次啟動已生成亂數密鑰，舊 token 失效請重新登入」；後續啟動沿用，刪 TOML 重生成即舊 token 失效為預期。
- **Win 防火牆提示**：首次啟動若 `server.port` 監聽失敗或 `netsh advfirewall` 偵測未放行，於 console 印 PowerShell 指令提示（`netsh advfirewall firewall add rule ...`），不自動提權。
- **自動開瀏覽器**：啟動成功後（`ApplicationReadyEvent`）以 `Desktop.browse` 開 `http://localhost:${server.port}`，失敗回落 `BareBrowser`/`xdg-open`/`open`，可由 `app.ui.auto-open-browser=false` 關閉。

## Capabilities

### New Capabilities
- `ops-binary`: Binary 交付的目錄契約、TOML 配置、AI 開關、JWT 生成、防火牆提示與自動開瀏覽器

### Modified Capabilities
<!-- 無 — 新增能力，現有 spec 不改行為，僅新增 ops-binary -->

## Impact

- 後端：`src/main/resources/application.yaml:8` 改 `spring.config.import` 指向 TOML 與 XDG、`config/BinaryPaths.java`（新增，OS 偵測與路徑解析）、`config/PhytotrackTomlProperties.java`、`service/DataInitializer` 與 `JwtSecretValidator` 改讀 TOML、`AIController` 受 `ai.enabled` 守衛、`ApplicationReady` 開瀏覽器與防火牆提示
- 前端：`src/api` 與 `stores/auth` 依 `ai.enabled` 隱藏 AI 入口（可選）
- 部署：`docs/DEPLOY.md` 補 Win Portable 與 Unix XDG/包管理器章節、`phytotrack.toml.example` 新增、`backend/.env.example` 標記棄用
- 測試：`application-test.yaml` 改用記憶體或 `target` 但不走 XDG，新增 `BinaryPathsTest`
