## Why

`playwright-cli open http://localhost:5173` 現行因 `.playwright/cli.config.json` 預設 `browserName: terminal-browser` 導致 `createIsolatedBrowser` 以 `executablePath: /usr/bin/terminal-browser` 啟動 daemon 失敗（`Cannot read properties of undefined (reading 'launch')`，pid 169078），而 `terminal-browser action --` 反可正常驅動；造成 `docs/E2E.md` 所述「可重現自動化」路徑不可用，影響 CI 與無頭重現。

## What Changes

- 修正 `.playwright/cli.config.json` 預設瀏覽器為 `chromium`（或移除 `executablePath` 覆寫，改由 `playwright-cli` 自帶瀏覽器），保留 `terminal-browser` 僅供 `terminal-browser` 模式；新增 `cli.config.chromium.json` 供 `playwright-cli` 專用。
- 更新 `docs/E2E.md` §3.2/§6 明確區隔 `playwright-cli`（chromium）與 `terminal-browser action --`（互動式）之啟動方式與 `mise` 版本對齊（`@playwright/cli@0.1.18` + `playwright-core@1.63` 已於 `mise.toml` 鎖定）。
- 補 `mise` 安裝後 `playwright install chromium` 提示（若需自帶瀏覽器）與 `playwright-cli --help` 驗證步驟。

## Capabilities

### New Capabilities
<!-- 無，工具鏈修正 -->

### Modified Capabilities
<!-- 無行為變更，純工具鏈 -->

## Impact

- 工具鏈：`.playwright/cli.config.json`、`docs/E2E.md`、`mise.toml`（若需鎖定瀏覽器版本）；不改 `backend/`/`frontend/` 程式碼與 `openspec/specs` 行為。
- 風險低，`terminal-browser` 互動式仍可用，`playwright-cli` 回歸可重現自動化。
