# E2E 測試指引（terminal-browser / playwright-cli）

> 本文件說明 PhytoTrack 以 `zenbu-labs/terminal-browser` 與 `microsoft/playwright-cli`（`@playwright/cli`）作為 E2E 工具的使用方式。兩者皆為 **MCP / CLI 驅動的瀏覽器自動化**，可由 agent 在 terminal 內直接操作真實瀏覽器，驗證前後端整合流程。單元測試（後端 `mvn test`、前端 `vitest`）仍為第一道防線，E2E 僅用於關鍵路徑的端到端驗收。

## 1. 定位與選擇

| 工具 | 來源 | 角色 | 何時用 |
|------|------|------|--------|
| **terminal-browser** | `zenbu-labs/terminal-browser`（Rust + Electron offscreen rendering，依賴 kitty graphics protocol） | **互動式預覽**：在 terminal 內直接顯示 Chromium 畫面，與 agent 共用同一 terminal tab；支援 SSH 透傳 | 需要「人也在 terminal 看見畫面」時：手動驗收、與 agent 協作除錯、預覽遠端 `localhost` 頁面 |
| **playwright-cli** | `microsoft/playwright-cli`（`npm:@playwright/cli@0.1.18`，由 `mise.toml` 鎖定） | **可重現自動化**：以 ref / snapshot 為核心的 CLI 指令，適合寫成可重跑的驗證腳本 | 需要「agent 無頭重現步驟」時：登入→建案→篩選→權限檢查等關鍵路徑自動化 |

兩者皆相容 **agent-browser** 指令集（`snapshot` / `click @eXX` / `fill @eXX` 等），可在同一專案混用：用 `terminal-browser` 眼見為憑、用 `playwright-cli` 留下可重現紀錄。

> 註：`mise.toml` 另鎖定 `npm:@playwright/test@1.62.1`，若需 `npx playwright test` 的測試框架，請另建 `playwright.config.ts`；本文的 `playwright-cli` 指 `@playwright/cli` 的 **MCP CLI**，非 `playwright test` runner。

## 2. 環境需求

| 需求 | 說明 |
|------|------|
| Terminal | 支援 kitty graphics protocol 者效果最佳：`ghostty` / `kitty` / `WezTerm` / VS Code 內建終端機；不支援時仍可跑 `playwright-cli`（無畫面版） |
| Node.js | `mise.toml` 以 `node = "lts"` 鎖定 24 LTS；`playwright-cli` 由 mise 提供，無需全域安裝 |
| 後端 / 前端 | E2E 前需先啟動後端 `:8080` 與前端 `:5173`（見 §3） |
| terminal-browser | 透過 `https://terminal-browser.sh/install` 安裝（二進位，非 npm） |

## 3. 安裝

### 3.1 terminal-browser（zenbu-labs）

```bash
# macOS / Linux 一鍵安裝（官方腳本）
curl -fsSL https://terminal-browser.sh/install | bash

# 驗證
terminal-browser --version
which terminal-browser  # 應為 /usr/bin/terminal-browser 或 ~/.local/bin/terminal-browser

# 終端機最佳化（選用，設定 kitty/ghostty 等）
terminal-browser setup
```

> `terminal-browser` 為獨立二進位（非 `npm:terminal-browser@1.0.2` 的舊版 `puppeteer/react-ink` 套件），勿以 `npm install terminal-browser` 安裝。

### 3.2 playwright-cli（microsoft）

本專案已由 `mise` 鎖定，無需手動安裝：

```bash
mise install          # 依 mise.toml 安裝 node / @playwright/cli / @playwright/test
mise exec -- playwright-cli --help   # 或直接 rtk playwright-cli --help
```

若未安裝 mise，亦可 fallback：

```bash
npx --yes @playwright/cli@0.1.18 --help
# 或全域安裝
npm install -g @playwright/cli@0.1.18
```

> 安裝後 `playwright-cli` 與 `npx playwright cli` 為同一工具，參數完全互通。
> 本專案以 **terminal-browser** 作為測試用瀏覽器（見 `.playwright/cli.config.json:2`），由 `terminal-browser` 自帶的 Electron/Chromium 提供，無需額外執行 `playwright install` 或 `install-browser` 下載瀏覽器。

## 4. 啟動待測系統

E2E 皆以前後端皆就緒為前提：

```bash
# 同時啟動（推薦，需 mise）
mise run dev
# 等待就緒：後端 http://localhost:8080/api/ai/health、前端 http://localhost:5173

# 或分開啟動
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev  # 或 ./mvnw ...
cd frontend && npm run dev -- --host 0.0.0.0
```

健康檢查：

```bash
curl -sf http://localhost:8080/api/ai/health | head
curl -sf http://localhost:5173 | head
```

預設測試帳號（`application.yaml` 的 `app.bootstrap.*`）：

| 帳號 | 密碼 | 角色 |
|------|------|------|
| admin | admin123 | ADMIN |
| staff | staff123 | STAFF |
| viewer | viewer123 | VIEWER |

## 5. terminal-browser 使用方式

### 5.1 基本指令

```bash
# 在當前 pane 開啟瀏覽器（預設空白頁）
terminal-browser open

# 直接開啟指定頁面
terminal-browser open http://localhost:5173
terminal-browser open http://localhost:5173/login --split right --size 0.5

# 列出所有瀏覽器與分頁
terminal-browser ls

# 在新分頁開啟
terminal-browser new-tab http://localhost:5173/cases

# 關閉 daemon
terminal-browser shutdown
```

常用快捷鍵（瀏覽器獲得焦點時）：`Ctrl+Q` 離開、`Cmd/Ctrl+L` 編輯網址、`Cmd/Ctrl+R` 重整、`Cmd/Ctrl+[ / ]` 上下頁、`Cmd+P` / `Ctrl+K` 命令面板、`F12` DevTools。

### 5.2 以 agent 驅動已開啟的瀏覽器

`terminal-browser action` 為 **agent-browser 相容 CLI**，`--` 後的所有指令皆轉送至當前瀏覽器：

```bash
# 對當前 pane 的瀏覽器 / 當前 tab 操作
terminal-browser action -- snapshot
terminal-browser action -- click @e14
terminal-browser action -- fill @e3 "staff"
terminal-browser action -- eval "document.title"

# 指定瀏覽器與分頁（key / id 來自 terminal-browser ls）
terminal-browser action --browser 90107-1 --tab 2 -- snapshot

# 強制將 tab 帶至前景
terminal-browser action --follow -- snapshot
```

可用指令與 `playwright-cli` 完全一致（見 §6.2），包含 `snapshot` / `click` / `fill` / `select` / `hover` / `eval` / `console` / `requests` 等。

### 5.3 SSH 透傳（預覽遠端 localhost）

```bash
# 讓瀏覽器在本地渲染，但所有網路請求經由 SSH 遠端代理
terminal-browser --ssh user@remote open http://localhost:5173
terminal-browser open --ssh user@remote http://localhost:8080/swagger-ui/index.html
```

此模式僅代理網路請求，不需將每一幀畫面經由 SSH 傳回，較「在遠端直接跑 terminal-browser」更流暢。

### 5.4 App Mode（選用）

用於以瀏覽器技術在 terminal 內建應用（見上游 `terminal-code` 範例）：

```bash
terminal-browser open ./report.html --app-mode
# 等價於 --no-toolbar --no-shortcuts --no-context-menu --no-overlays --no-frame
```

## 6. playwright-cli 使用方式

> 完整指令參考：`~/.config/opencode/skills/playwright-cli/SKILL.md` 或 `playwright-cli --help`

### 6.1 會話（session）模型

```bash
# 開啟新瀏覽器（預設 Chromium，in-memory profile）
playwright-cli open http://localhost:5173
# 以下參數會覆蓋 .playwright/cli.config.json 的 browserName（預設 terminal-browser）
playwright-cli open --browser=webkit http://localhost:5173
playwright-cli open --browser=firefox --mobile http://localhost:5173

# 具名會話（可在多 terminal 間共享）
playwright-cli -s=e2e open http://localhost:5173 --persistent
playwright-cli -s=e2e click e12
playwright-cli -s=e2e close

# 常用會話管理
playwright-cli tab-list
playwright-cli tab-new http://localhost:5173/cases
playwright-cli tab-select 0
playwright-cli close        # 關閉當前會話
playwright-cli close-all    # 關閉全部
playwright-cli delete-data  # 清除當前會話資料
```

### 6.2 核心互動（以 snapshot ref 為主）

```bash
playwright-cli open http://localhost:5173/login
playwright-cli snapshot                          # 取得含 ref 的可存取樹（YAML）
playwright-cli find "登入"                       # 在快照中搜尋文字，回傳含上下文的節點
playwright-cli find --regex "/staff|admin/i"

# 互動皆以 ref 為目標（e12 來自 snapshot）
playwright-cli click e12
playwright-cli dblclick e7
playwright-cli fill e3 "staff" --submit          # --submit 自動按 Enter
playwright-cli type "搜尋關鍵字"
playwright-cli press Enter
playwright-cli select e9 "option-value"
playwright-cli check e12 / uncheck e12
playwright-cli hover e4
playwright-cli drag e2 e8
playwright-cli upload ./document.pdf

# CSS / locator 亦可（不推薦，ref 更穩定）
playwright-cli click "#main > button.submit"
playwright-cli click "getByRole('button', { name: '登入' })"

# 對話框
playwright-cli dialog-accept
playwright-cli dialog-accept "確認文字"
playwright-cli dialog-dismiss

# 視窗
playwright-cli resize 1920 1080
playwright-cli reload
playwright-cli go-back / go-forward
```

### 6.3 斷言與除錯

```bash
# 快照深度 / 範圍
playwright-cli snapshot --depth=4
playwright-cli snapshot "#main"
playwright-cli snapshot --boxes                 # 含 bounding box
playwright-cli snapshot --filename=after.yml

# JS 求值
playwright-cli eval "document.title"
playwright-cli eval "el => el.textContent" e5
playwright-cli eval "el => el.getAttribute('data-testid')" e5
playwright-cli --raw eval "JSON.stringify([...document.querySelectorAll('a')].map(a => a.href))" > links.json

# DevTools
playwright-cli console                 # 全部
playwright-cli console warning         # 僅 warning+
playwright-cli requests                # 列出所有網路請求（編號）
playwright-cli request 5               # 單筆詳情（headers / body / response）
playwright-cli run-code "async page => await page.context().grantPermissions(['geolocation'])"
playwright-cli tracing-start / tracing-stop
playwright-cli video-start video.webm / video-stop

# 截圖 / PDF（少用，優先 snapshot）
playwright-cli screenshot --filename=page.png
playwright-cli pdf --filename=page.pdf
```

### 6.4 狀態保存（認證）

```bash
# 登入後保存 storage state（含 localStorage 的 JWT）
playwright-cli state-save auth.json
# 下次直接載入，免重複登入
playwright-cli state-load auth.json

# 細部操作
playwright-cli cookie-list / cookie-get session_id
playwright-cli localstorage-list / localstorage-get theme
playwright-cli localstorage-set theme dark
playwright-cli sessionstorage-list
```

### 6.5 網路 Mock

```bash
playwright-cli route "**/*.jpg" --status=404
playwright-cli route "https://api.example.com/**" --body='{"mock": true}'
playwright-cli route-list
playwright-cli unroute "**/*.jpg"
playwright-cli unroute   # 清除全部
```

## 7. 典型 E2E 流程（PhytoTrack）

以下範例皆假設前後端已啟動於 `http://localhost:5173` / `http://localhost:8080`。

### 7.1 以 playwright-cli 驗證「登入 → 建立案件 → 列表篩選」（可重現）

```bash
# 1. 開啟並登入（STAFF）
playwright-cli open http://localhost:5173/login
playwright-cli snapshot
# 假設帳號欄位為 e3、密碼 e5、登入按鈕 e12（以實際 snapshot 為準）
playwright-cli fill e3 "staff"
playwright-cli fill e5 "staff123"
playwright-cli click e12
playwright-cli snapshot  # 應導向 /dashboard 或 /cases

# 2. 導向建案頁
playwright-cli goto http://localhost:5173/cases/new
playwright-cli snapshot

# 3. 填寫必填欄位（作物、服務類別、收件日期等，依實際表單 ref 調整）
playwright-cli select e20 "1"          # 作物
playwright-cli fill e30 "測試案件 E2E"
playwright-cli click e45               # 送出

# 4. 驗證列表與篩選
playwright-cli goto "http://localhost:5173/cases?senderName=測試"
playwright-cli find "測試案件"
playwright-cli console                 # 確認無非預期錯誤
playwright-cli requests                # 確認 /api/cases 請求為 200

# 5. 儲存狀態供後續用例
playwright-cli state-save /tmp/phytotrack-staff.json
playwright-cli close
```

### 7.2 以 terminal-browser 做同流程的互動式驗證

```bash
# 人在 terminal 看見畫面，agent 以 action 驅動
terminal-browser open http://localhost:5173/login --split right

terminal-browser action -- snapshot
terminal-browser action -- fill @e3 "staff"
terminal-browser action -- fill @e5 "staff123"
terminal-browser action -- click @e12

terminal-browser action -- snapshot
terminal-browser ls   # 確認 browser key / tab id
```

### 7.3 權限與錯誤情境（建議納入 E2E）

- `VIEWER` 登入後嘗試 `POST /api/cases` 應 403；UI 上「新增案件」按鈕不可見或導向 403 頁。
- 未登入直接 `goto /cases` 應導向 `/login`（`401 UNAUTHORIZED` 由 `RestAuthenticationEntryPoint` 回傳，前端攔截器清除 token）。
- 已結案案件（`CLOSED`）以 `STAFF` 嘗試修改內容應 `403 CLOSED_CASE_READONLY`。
- 送件人去重：建案時輸入部分姓名應出現候選彈窗（`GET /api/senders/search?q=`）。

> 以上 ref（`e3` / `e12` 等）皆為範例，實務上每次 `snapshot` 後以 `find` 定位正確 ref。

## 8. 與專案整合建議

- **Base URL**：前端 dev 為 `http://localhost:5173`，API 為 `http://localhost:8080`（Vite 已代理 `/api`，E2E 直接測前端即可）。
- **資料隔離**：E2E 建議走獨立 DB（`application-test.yaml` 的 `./target/phytotrack-test.db`）或在測試前後以 `scripts/backup.sh` / `DELETE /api/cases/{id}` 清理，避免污染開發資料。
- **CI**：`playwright-cli` 為 CLI，可在 CI 中以 `mise install && mise run dev &` 後執行 `playwright-cli` 指令；`terminal-browser` 需圖形終端，不適合 headless CI，僅作本地互動驗收。
- **產物忽略**：`.playwright-cli/` 為產物、`*.db`、`backups/` 已於 `.gitignore` 忽略；`.playwright/cli.config.json` 為需版控的設定檔（見 `.gitignore` 的 `!.playwright/cli.config.json`），E2E 產生的截圖 / trace / `auth.json` 請置於 `.playwright/`（非設定檔）或 `/tmp`，勿提交。

## 9. 常見問題

| 現象 | 排查 |
|------|------|
| `terminal-browser: no terminal browsers running` | 需先 `terminal-browser open` 啟動瀏覽器，再用 `terminal-browser action -- <cmd>` |
| `playwright-cli: command not found` | `mise install` 後以 `mise exec -- playwright-cli --help` 或 `npx @playwright/cli --help` 執行 |
| `snapshot` 為空或 ref 對不上 | 先 `reload`，再 `snapshot --depth=4` / `find "關鍵字"` 縮小範圍；避免用 CSS selector 硬寫 |
| 登入後仍導回 `/login` | 檢查 `localStorage`：`playwright-cli localstorage-get token`；確認後端 `JWT_SECRET` 與 `app.bootstrap.*` 是否正確 |
| 5173 無法連線 | 確認 `mise run dev` 已就緒，或 `curl -sf http://localhost:5173` 是否通 |

## 10. 相關文件

- `README.md` § 測試與驗證 — 後端 `mvn test` / 前端 `vitest` 入口
- `AGENTS.md` § 指令 — 含 E2E 前置啟動指令
- `docs/ARCHITECTURE.md` §4 / `docs/DEPLOY.md` — 前端路由與部署說明
- 上游文件：`zenbu-labs/terminal-browser`（https://github.com/zenbu-labs/terminal-browser）、`microsoft/playwright-cli` skill（`~/.config/opencode/skills/playwright-cli/SKILL.md`）
