## ADDED Requirements

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
