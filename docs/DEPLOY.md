# PhytoTrack 部署文件

本文說明如何在本機或 5 人區域網路（LAN）內部部署 PhytoTrack。

## 1. 環境需求

| 元件 | 版本 | 用途 |
|------|------|------|
| JDK | 21+ | 執行 Spring Boot 後端 |
| Node.js | 20+ | 建置前端（npm） |
| llama.cpp | 最新版 | 提供 AI 診斷（`llama-server`） |
| GGUF 模型 | 任一繁體中文相容模型 | 如 `llama-3.2-3b-instruct` 或 `qwen2.5-3b-instruct` |

不需要安裝資料庫——後端使用檔案型 SQLite（`diagnoses.db`），首次啟動自動建立資料表與種子資料。

## 2. 後端啟動

```bash
cd backend
./mvnw spring-boot:run
```

啟動成功後：

- API 服務：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui/index.html`

### 常用設定（`backend/src/main/resources/application.yaml`）

| 設定 | 預設 | 說明 |
|------|------|------|
| `app.jwt.secret` | 開發用密鑰 | JWT 簽章密鑰。正式環境務必以 `JWT_SECRET` 環境變數覆蓋 |
| `app.bootstrap.*` | admin/admin123、staff/staff123、viewer/viewer123 | 首次啟動自動建立的管理者、診斷員與檢視者帳號 |
| `app.ai.health-url` | `http://localhost:11435/health` | llama-server 存活檢查端點 |

## 3. llama-server 啟動（AI 診斷）

```bash
# 以 OpenAI 相容模式啟動，port 用 11435 避開 Spring Boot 的 8080
llama-server -m /path/to/model.gguf --port 11435
```

確認連線：

```bash
curl http://localhost:11435/health        # 回傳 ok
curl http://localhost:11435/v1/models     # 列出模型名稱（須與後端 model 設定一致）
```

若未啟動 llama-server，系統其餘功能（案件管理、登入）仍可正常使用，僅 AI 診斷無法執行。

## 4. 前端建置與啟動

開發模式（Vite dev server，含 `/api` 代理至後端 8080）：

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

正式部署（產出靜態檔）：

```bash
cd frontend
npm run build
npm run preview          # 本機預覽 http://localhost:4173
# 或將 dist/ 交由 nginx 等靜態伺服器代管
```

若前後端分開部署，需把 `frontend/vite.config.ts` 的 proxy 目標改為後端實際位址，或在 nginx 設定 `/api` 反向代理至後端。

## 5. 區域網路（LAN）部署（5 人）

1. 後端以 `0.0.0.0` 監聽：`./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.address=0.0.0.0`
2. 前端 build 後以 nginx 服務 `dist/`，並將 `/api` 反向代理至後端 8080
3. 各用戶端瀏覽器開啟前端位址（例如 `http://伺服器IP/`）
4. 防火牆需放行前端對外 port 與後端 8080（若代理已設定，用戶端通常只需連前端 port）

### 部署前檢查清單

- [ ] 已設定強固的 `JWT_SECRET`（勿用預設值）
- [ ] 已變更 bootstrap 預設帳號密碼
- [ ] 後端 `diagnoses.db` 有定期備份
- [ ] llama-server 已隨開機啟動（如需 AI 診斷）

## 6. 升級到 PostgreSQL（選用）

現階段使用 SQLite 起步（理由見 ADR-007）。若未來資料量與並發需求增加，切換方式：

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

搭配 `application-postgres.yaml` 設定連線資訊，並使用既有 schema 資料（`schema.sql` 的 `INSERT` 語句相容 PostgreSQL）。