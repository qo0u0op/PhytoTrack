## Why

`mise run dev` 同時啟動後端（`:8080`）與 vite 前端（`:5173`），但後端 `BrowserOpener` 固定自動開啟 `:8080`，該位址在 dev 下僅有陳舊打包 bundle（或無 bundle），導致開發者第一眼看到的是過期頁面，與 vite 即時前端脫節。dev 模式應直接開啟 `:5173`。

## What Changes

- **dev profile 開啟 vite**：`BrowserOpener` 新增 `app.ui.dev-frontend-url` 設定（預設 `http://localhost:5173/`），僅於 `dev` profile 且 `auto-open-browser=true` 時開啟該位址；`prod`/binary 維持開啟 `http://localhost:${port}/`
- **設定**：`application-dev.yaml` 新增 `app.ui.dev-frontend-url: ${DEV_FRONTEND_URL:http://localhost:5173/}`，`phytotrack.toml.example` 補註解；預設關閉不影響（`auto-open-browser=false` 仍不開啟）
- **日誌**：dev 啟動訊息改印 `Frontend (vite): http://localhost:5173/` + `API: http://localhost:8080/api`，prod 維持原訊息

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `ops-binary-packaging`: `BrowserOpener` 開啟位址依 profile 分流（dev → vite `:5173`，prod → 同 port `/`）

## Impact

- 後端：`config/BrowserOpener.java`、`application-dev.yaml`、`phytotrack.toml.example`
- 無 DB/API/前端變更；`mvn test` 綠燈
