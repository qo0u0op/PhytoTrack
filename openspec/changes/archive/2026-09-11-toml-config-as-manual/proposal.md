## Why

`phytotrack.toml` 是系統唯一真相（`ops-binary`），但現有 `phytotrack.toml.example` 與 `PhytotrackTomlEnvironmentPostProcessor.generateDefaultToml()` 註釋僅為 1 行提示（例：`port = 8080` 旁「可改 9090」），缺少欄位語意、預設值、取值範圍、生效條件與排錯指引。使用者在 `config/phytotrack.toml`、`~/.config/phytotrack/phytotrack.toml` 與 `/etc/phytotrack/phytotrack.toml` 間無法自給自足，常需跳轉 `docs/DEPLOY.md` 與 `docs/ARCHITECTURE.md`，與「config as a manual」目標不符。需將 `phytotrack.toml` 內註釋提升至手冊等級，使單檔即為可離線閱讀的操作手冊。

## What Changes

- 將 `backend/phytotrack.toml.example` 重寫為手冊式註釋：檔案頭含路徑契約（Windows 可攜 `config/phytotrack.toml` / XDG `~/.config/phytotrack/phytotrack.toml` / AppImage 同目錄 / 系統級 `/etc/phytotrack/phytotrack.toml`）、優先順序與首次生成說明、保存起點指引 `cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml`；每節（`[server]`、`[app.jwt]`、`[app.bootstrap]`、`[ai]`、`[ai.headers]`、`[app.cors]`、`[app.rate-limit]`、`[app.security-headers]`、`[app.tray]`、`[app.ui]`、`[springdoc]`、`spring.datasource/logging`）前加節級說明塊；每欄位加 `用途 | 類型 | 預設 | 取值/範例 | 生效/重啟需求` 五要素，敏感欄位（`app.jwt.secret`）加首次亂數與「刪檔重生成」警告；關聯章節以 `→ docs/DEPLOY.md#...` 註記。
- 同步 `PhytotrackTomlEnvironmentPostProcessor.generateDefaultToml()` 生成內容與 `phytotrack.toml.example` 同源（僅 `app.jwt.secret` 隨機），避免首次生成檔與範例分叉；無效值回落安全預設並印啟動警告。
- 補齊 TOML 可配但之前註釋缺漏的欄位：`app.tray.enabled`、`app.ui.auto-open-browser` / `dev-frontend-url`、`springdoc` 開關、`spring.datasource.url` / `logging.file.name` 覆蓋，並標註 `AI_API_KEY` env 覆蓋規則與 `app.bootstrap` 不可配口令的約束。
- 更新 `docs/DEPLOY.md` 與 `docs/ARCHITECTURE.md` 中「見 phytotrack.toml.example」引用為「以檔內手冊為準」，避免文件與檔內註釋重複分叉；不改執行期行為（僅註釋）。

## Capabilities

### New Capabilities

<!-- 無新增能力，僅提升既有配置文件的文檔等級 -->

### Modified Capabilities

- `ops-binary`: TOML 單一真相的「文件即手冊」要求——`phytotrack.toml` 內註釋 SHALL 達手冊等級（節說明 + 欄位五要素含類型/預設/可選值 + 關聯指引），且 `phytotrack.toml.example` 與首次生成檔二者同源一致

## Impact

- 影響檔案：`backend/phytotrack.toml.example`、`backend/src/main/java/com/d0w0b/phytotrack/config/PhytotrackTomlEnvironmentPostProcessor.java`（`generateDefaultToml` 字串）、`docs/DEPLOY.md`、`docs/ARCHITECTURE.md`（引用語句改為檔內手冊指引）
- 無執行期行為變更、無 API/DB/schema 變更、無新增依賴；僅註釋與文件同步，需通過 `openspec validate --specs --changes` 與 `grep` 檢查生成檔與範例一致性
