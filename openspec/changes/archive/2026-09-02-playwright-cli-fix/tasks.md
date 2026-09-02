## 1. 工具鏈修正

- [x] 1.1 調整 `.playwright/cli.config.json` 預設為 `chromium`（移除 `executablePath` 覆寫）並新增 `cli.config.terminal.json` 供 `terminal-browser` 模式，驗證 `playwright-cli open http://localhost:5173 --browser=chromium` 可開啟且 `terminal-browser action -- snapshot` 仍正常
- [x] 1.2 更新 `docs/E2E.md` §3.2/§6 明確區隔 `playwright-cli`（chromium）與 `terminal-browser action --`（互動式）及 `mise` 版本，驗證文件與 `mise.toml` 鎖定一致

## 2. 驗證

- [x] 2.1 以 `playwright-cli open http://localhost:5173/login` 執行登入→建案→篩選可重現流程，驗證無 daemon 錯誤
