# PhytoTrack

農作物病蟲害診斷記錄系統。前後分離：Spring Boot 4 (Java 21) REST 後端 + Vue 3 / TypeScript 前端 SPA，AI 診斷由本機 llama.cpp 提供。文件與註解慣用繁體中文。

## 指令

- 後端驗證：`cd backend && mvn test`（mise）或 `./mvnw test`（無 mise, Unix/macOS）/ `.\mvnw.cmd test`（Windows）；單測：`mvn test -Dtest=CaseControllerTest`
- 後端啟動 dev：`mvn spring-boot:run -Dspring-boot.run.profiles=dev`（`http://localhost:8080/swagger-ui.html`）
- 前端建置（含 vue-tsc）：`cd frontend && npm run build`；開發：`npm run dev`（`/api` 代理至 8080）；測試：`npm test`（vitest, happy-dom）
- 同時啟動：`mise run dev`（`dev:backend`/`dev:frontend`/`d2`）；無 mise 時分開執行 `./mvnw` 與 `npm run dev`
- E2E：`playwright-cli`（`mise` 提供 `@playwright/cli`，預設 `chromium`，見 `.playwright/cli.config.json`）與 `terminal-browser`（二進位，需 `mise run dev` 就緒），詳見 `docs/E2E.md`
- 操作手冊：`typst compile docs/manual.typ docs/manual.pdf`（PDF 為產物，已 gitignore，勿提交）；`typst compile docs/diagnoses.typ /tmp/diagnoses.pdf` 驗證
- 本機 shell 以 `rtk` 開頭可選（如 `rtk ./mvnw test`）；`frontend/.npmrc` 已設 `legacy-peer-deps=true`，勿移除

## 架構重點

- 三層 `Controller → Service → Repository`（`backend/src/main/java/com/d0w0b/phytotrack/`），DTO 隔 API 邊界，實體不進 Controller（ADR-003/005）
- `spring.jpa.open-in-view: false`：交易外不得 Lazy，需 DTO/FETCH JOIN
- 認證：JWT + BCrypt(12) + RBAC（VIEWER/STAFF/ADMIN），`@PreAuthorize` 集中；非 dev 需 `JWT_SECRET`（`JwtSecretValidator` fail-fast）；dev 預設帳號 `app.bootstrap.*`（admin/admin123 等）
- SQLite：`hibernate-community-dialects` `SQLiteDialect`、Hikari `maximum-pool-size:1`、`LocalDate/LocalDateTime` 自訂 `converter/`；`v_case_search` 視圖 `@Subselect`（含 `sender_type_id`，17 欄，5 列換行篩選卡）
- AI：llama-server 於 **11435**（避開 8080），Spring AI 以 OpenAI 相容格式串接（`api-key` dummy）；`AI_BASE_URL/AI_MODEL/AI_API_KEY` 放 `backend/.env`（gitignored, 見 `.env.example`），`application.yaml` 以 `${VAR:default}` 承接
- 日誌/監控：`logback-spring.xml` 滾動 `logs/phytotrack-%d{yyyy-MM-dd}.%i.log.gz`（10MB/30日/500MB）；Actuator `health,info` 公開、`metrics` 僅 ADMIN

## 測試（Boot 4 坑）

- `@MockBean` → `@MockitoBean`（`org.springframework.test.context.bean.override.mockito`）
- `@WebMvcTest`/`@DataJpaTest` 已搬至 `org.springframework.boot.{webmvc,data.jpa,jdbc}.test.autoconfigure`
- `@WithMockUser` 需自註冊 `TestSecurityContextHolderStrategyAdapter` + `springSecurity()`（見 `CaseControllerTest`）
- 測試走獨立 SQLite（`application-test.yaml` → `./target/phytotrack-test.db`），須 `@ActiveProfiles("test")`
- JPA Auditing 會覆寫 `createdAt` → 斷言 `isNotNull()` 即可；`senders` 有 `name+phone` 部分唯一，測試別撞值
- 時間格式僅至秒：`@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")`，CSV `fmtTs` 截斷至秒

## Git 慣例

- Conventional Commits：英文標題 + 中文內文（`feat:/fix:/docs:/chore:`）；預設提交新 commit，僅 binary 誤入才 `git reset --soft` 重寫
- 分支不主動推送；`docs/notebook/` 與 `*.pdf`、`logs/`、`backups/` 已 gitignore
- `.opencode/` 為本機設定（gitignored）

## OpenSpec 操作

- 主規格 `openspec/specs/` 11 份（`security-hardening/security-review`、`api-observability`、`case-search`、`case-lifecycle`、`case-statistics`、`case-report`、`user-admin`、`reference-data-admin`、`sender-management`、`ops-backup`）
- 工作流：`openspec new change` → proposal → apply → archive；Phase 1 每能力一獨立 change（`skip_specs:true`），常用：`openspec list`/`status --change <name>`/`validate --specs`/`validate --changes`
- 已全數 archive 至 `openspec/changes/archive/2026-09-02-*`（含 `csv-export-format`/`case-display-filter-export`/`case-list-state-persist`/`docs-sync`/`dashboard-half-year`/`api-observability-phase2`/`security-hardening-phase2`/`case-form-rename-and-layout`/`playwright-cli-fix` 等），目前可能有 0-1 個 active change
- 需求總覽：`docs/REQUIREMENTS.md`（能力狀態與依賴）

## 文件同步（spec ↔ markdown/typst）

- 單一真相源為 `openspec/specs/*`；改動 spec 後 **必須**同步：
  - `docs/REQUIREMENTS.md` 能力表與未電子化清單
  - `docs/ARCHITECTURE.md` 資料模型/篩選/API 一覽/監控與日誌
  - `docs/DEPLOY.md` CSV 歷史表與監控章節（如涉 Actuator/logback）
  - `docs/manual.typ` 操作步驟（篩選 5 列、CSV 說明、狀態等）與 `docs/diagnoses.typ` 紙本欄位（`田區位置/身分別`）
  - `README.md` 功能一覽（如涉使用者可見變更）
- `manual.typ`/`diagnoses.typ` 為 typst 0.13+：函式呼叫不得有空格（`#set page(margin: ...)` 而非 `#set page (margin`；`#align(center)` 而非 `#align (center)`；`table.header([` 而非 `table.header ([`），否則 `typst compile` 報 `expected argument list`
- 驗證：`typst compile docs/manual.typ docs/manual.pdf` 與 `typst compile docs/diagnoses.typ /tmp/diagnoses.pdf` 皆 exit 0；`openspec validate --specs --changes` 13-14 passed；`grep -rn 病蟲害發生地點 docs` 僅餘歷史段落

## 文件

- `README.md`：快速啟動權威來源
- `docs/adr/ADR-001~011`：架構決策，實作前先查
- `docs/ARCHITECTURE.md`/`DEPLOY.md`/`E2E.md`、`docs/REQUIREMENTS.md`、`openspec/specs` 為契約基準
