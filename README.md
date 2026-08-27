# PhytoTrack

作物病蟲害診斷記錄系統（PhytoTrack），前後分離架構：Spring Boot 4 後端提供 REST API，Vue 3 + TypeScript 前端單頁應用，AI 診斷由本機 llama.cpp 提供。

## 功能一覽

- **認證授權**：JWT（無狀態）+ Spring Security + BCrypt，RBAC 三角色（VIEWER / STAFF / ADMIN）
- **案件管理**：建立、編輯、刪除、分頁列表與詳細查詢，含送件人、作物、被害部位、病蟲害分類、防治建議、診斷簽名人等多對多關聯；列表支援依作物、服務類別、送件人（部分比對）、收件日期區間與狀態篩選（AND 組合）
- **AI 診斷**：以 Spring AI（OpenAI 相容格式）代理本機 llama.cpp，依案件欄位生成診斷建議
- **參照資料**：作物（含分類）、病蟲害（含分類）、縣市／鄉鎮、耕種方式、服務類別、送件方式、身分別、標的等選單資料
- **管理者後台**：使用者列表管理
- **API 文件**：springdoc-openapi 產生 OpenAPI 3 規格，提供 Swagger UI 與前端 TS 型別（openapi-typescript）

## 技術堆疊

| 層 | 技術 |
| --- | --- |
| 後端 | Java 21、Spring Boot 4（`spring-boot-starter-webmvc`）、Spring Data JPA、Spring Security、Spring AI |
| 資料庫 | SQLite（`hibernate-community-dialects`），預留 PostgreSQL profile（見 ADR-007） |
| API 規格 | springdoc-openapi 3.x（單一來源，見 ADR-008） |
| 前端 | Vue 3 + TypeScript + Vite + Pinia + Vue Router + Bootstrap 5 + axios + SweetAlert2 |
| AI | llama.cpp（`llama-server` 於 11435）+ Spring AI ChatClient（見 ADR-009） |

## 專案結構

- `backend/`：Spring Boot 後端（Controller / Service / Repository 三層，見 ADR-003）
- `frontend/`：Vue 3 SPA
- `docs/`：ADR 架構決策、架構說明、部署與操作手冊
- `openspec/`：OpenSpec 規格與變更提案

## 快速啟動

### 1. 後端

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev          # 已安裝 mise（mise 提供 maven 3.9.16）
# 或
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev       # 無 mise 時 fallback（零依賴，wrapper 3.9.16）
```

- 啟動後 Swagger UI：<http://localhost:8080/swagger-ui.html>
- `dev` profile 允許使用開發預設 JWT 密鑰；**正式環境請以環境變數 `JWT_SECRET` 提供密鑰**（未提供且非 dev 環境時會啟動失敗，見 ADR-004）
- 首次啟動自動建立預設帳號：`admin / admin123`（ADMIN）、`staff / staff123`（STAFF）、`viewer / viewer123`（VIEWER），可於 `application.yaml` 的 `app.bootstrap.*` 調整
- 機台特定的 AI 設定（base-url / 模型名稱 / api-key）可於 `backend/.env` 覆寫：`cp backend/.env.example backend/.env` 後修改（未設定時使用預設值）

### 2. AI 模型（選用）

AI 診斷功能需要本機 llama.cpp 在線（後端代理於 `app.ai.health-url`，預設 11435；亦可改用 LlamaStash 的 OpenAI proxy）：

```bash
llama-server -m <模型檔>.gguf --port 11435 --alias <模型名稱> --api-key dummy
```

- 模型名稱需對應後端請求的 `model`（可於 `backend/.env` 的 `AI_MODEL` 調整，查 `GET /v1/models`）
- 可在前端 Dashboard 檢查模型連線狀態，或 `GET /api/ai/health`
- 模型未啟動時，AI 診斷會回傳錯誤（不影響案件 CRUD）

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

開啟 <http://localhost:5173>（Vite 開發伺服器已將 `/api` 代理至 8080）。

## 資料庫

- SQLite 檔案位於 `backend/diagnoses.db`（clone 即跑、零安裝）
- 資料表與種子資料由 `backend/src/main/resources/schema.sql` 建立（`IF NOT EXISTS` 冪等），Hibernate 以 `ddl-auto: update` 同步實體
- 擴充至 PostgreSQL 只需切換 `application-postgres.yaml` profile（見 ADR-007）
- 備份：`bash scripts/backup.sh` 產生帶時間戳備份至 `backups/`（見 `docs/DEPLOY.md` §6）

## 測試與驗證

```bash
# 後端測試
cd backend && mvn test          # 已安裝 mise
# 或 cd backend && ./mvnw test  # 無 mise 時 fallback

# 前端單元測試（vitest）
cd frontend && npm test

# 前端型別檢查與建置
cd frontend && npm run build

# E2E（需先 mise run dev 啟動前後端，見 docs/E2E.md）
playwright-cli open http://localhost:5173/login   # microsoft/playwright-cli（mise 提供）
terminal-browser open http://localhost:5173        # zenbu-labs/terminal-browser（互動式預覽）
```

## 架構決策與文件

- `docs/adr/`：10 份 ADR（前後分離、Boot 4、三層架構、JWT/RBAC、DTO、JPA Auditing、SQLite→PostgreSQL、OpenAPI、llama 代理、統一錯誤處理）
- `docs/ARCHITECTURE.md`：整體架構與請求流程
- `docs/REQUIREMENTS.md`：10 能力需求總覽與 Phase 1 範圍
- `docs/DEPLOY.md`：部署與備份指引
- `docs/E2E.md`：E2E 指引（terminal-browser / playwright-cli 使用方式）
- `docs/manual.typ`：操作手冊（`typst compile` 產生 PDF）

## 規劃中（見 openspec）

統計 Dashboard、使用者管理完整化、送件人管理（displayName、去重合併、VIEWER 個資遮蔽）、參照資料維護、監控與備份等，詳見 `openspec/specs/`（10 份能力契約）與 `openspec/changes/`。案件生命週期（狀態流轉：PENDING→RESOLVED→CLOSED）與更新契約補全已交付。

## 同時啟動前後端（選用，需 mise）

以 `mise run dev` 可同時啟動後端（`dev:backend`，走 `mvn`）與前端（`dev:frontend`），並等待兩者就緒；未安裝 mise 請分開執行 `./mvnw` 與 `npm run dev`。
