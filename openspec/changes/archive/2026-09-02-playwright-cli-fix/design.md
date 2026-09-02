## Context

見 `proposal.md` Why。`mise.toml` 已鎖定 `@playwright/cli@0.1.18` 與 `playwright-core@1.63`，`.playwright/cli.config.json:2` 強制 `browserName: terminal-browser` 致 `playwright-cli` 嘗試 `createIsolatedBrowser` 經 `executablePath: /usr/bin/terminal-browser` 失敗；`terminal-browser action --` 直連 `terminal-browser` daemon 則正常。

## Goals / Non-Goals

**Goals:**
- 使 `playwright-cli open` 可無頭重現（chromium 自帶瀏覽器），保留 `terminal-browser` 互動式。

**Non-Goals:**
- 不改 `backend/`/`frontend/` 行為，不引入新 E2E 框架。

## Decisions

- **預設瀏覽器**：`cli.config.json` 改 `browserName: chromium` 並移除 `executablePath` 覆寫，新增 `cli.config.terminal.json` 供 `terminal-browser` 模式（`--config` 指定）。替代：持續共用單一 config 需手動 `--browser=chromium` 易忘。
- **文件**：`docs/E2E.md` 明確 `playwright-cli` 用 chromium、`terminal-browser action --` 用 `terminal-browser`，並補 `mise exec -- playwright-cli --help` 驗證。替代：不分流則再現失敗。
- **安裝**：補 `playwright install chromium` 提示（若 `playwright-core` 未含瀏覽器）。替代：依賴系統 chromium 版本分散。

## Risks / Trade-offs

- [瀏覽器下載] 需額外 `playwright install` → `mise` 可快取，影響可控。
- [雙 config] 維護成本微增 → 以命名區隔，風險低。

## Migration Plan

1. 調整 `.playwright/cli.config.json` 與新增 `cli.config.terminal.json`。
2. 更新 `docs/E2E.md`。
3. 以 `playwright-cli open http://localhost:5173 --browser=chromium` 與 `terminal-browser action -- snapshot` 雙路驗證。

## Open Questions

- 無。
