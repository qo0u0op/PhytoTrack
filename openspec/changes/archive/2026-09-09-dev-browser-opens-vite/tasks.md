## 1. dev 開啟 vite

- [x] 1.1 於 `config/BrowserOpener.java` 新增 `devFrontendUrl`（`@Value("${app.ui.dev-frontend-url:http://localhost:5173/}")`）與 `dev` profile 判斷（`@Value("${spring.profiles.active:}")` 或 `Environment`），dev 且允許時開啟該位址並印 `Frontend (vite)` 訊息，`mvn test` 綠燈
- [x] 1.2 於 `application-dev.yaml` 新增 `app.ui.dev-frontend-url: ${DEV_FRONTEND_URL:http://localhost:5173/}`，`phytotrack.toml.example` 補 `[app.ui] dev-frontend-url` 註解，`openspec validate --specs --changes` 通過

## 2. 驗證

- [x] 2.1 `mvn test` 綠燈（154 passed, BUILD SUCCESS），`mise run dev` 後瀏覽器開啟 `:5173`（以日誌 `Frontend (vite)` 確認，不實際開瀏覽器時以 `auto-open-browser=false` 驗日誌分支）
