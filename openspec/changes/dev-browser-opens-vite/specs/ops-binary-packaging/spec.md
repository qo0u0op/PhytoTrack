## ADDED Requirements

### Requirement: dev 模式自動開啟 vite 前端

`dev` profile 啟動且 `app.ui.auto-open-browser=true` 時，`BrowserOpener` SHALL 開啟 `app.ui.dev-frontend-url`（預設 `http://localhost:5173/`），而非後端同 port `/`；`prod`（含 binary）行為維持不變。`auto-open-browser=false` 時 SHALL 不開啟任何瀏覽器。

#### Scenario: dev 自動開啟 vite
- **WHEN** 以 `mise run dev`（`dev` profile，預設設定）啟動後端
- **THEN** 瀏覽器開啟 `http://localhost:5173/`，且終端印出 `Frontend (vite)` 與 `API: http://localhost:8080/api`

#### Scenario: prod 維持同 port
- **WHEN** 以 `prod`（`AppImage` / `phytotrack.exe`）啟動
- **THEN** 瀏覽器开启 `http://localhost:${port}/`（內嵌前端），行為與此前一致

#### Scenario: 可關閉與可覆蓋
- **WHEN** 設 `app.ui.auto-open-browser=false` 或 `DEV_FRONTEND_URL=http://localhost:5174/`
- **THEN** 前者不開啟瀏覽器，後者開啟覆蓋後位址
