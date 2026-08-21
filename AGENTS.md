# PhytoTrack

農作物病蟲害診斷記錄系統。前後分離：Spring Boot 4（Java 21）REST 後端 + Vue 3 / TypeScript 前端 SPA，AI 診斷由本機 llama.cpp 提供。文件與註解慣用繁體中文。

## 指令

- 後端所有驗證：`cd backend && ./mvnw test`；單一測試：`./mvnw test -Dtest=CaseControllerTest`
- 後端啟動（dev）：`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`，Swagger UI 於 <http://localhost:8080/swagger-ui.html>
- 前端建置（含 vue-tsc 型別檢查）：`cd frontend && npm run build`；開發伺服器 `npm run dev`（已將 `/api` 代理至 8080）；前端測試 `npm test`（vitest，happy-dom）
- 操作手冊：`typst compile docs/manual.typ docs/manual.pdf`（PDF 為產物，`*.pdf` 已 gitignore，勿提交）
- 本機 shell 慣例以 `rtk` 開頭執行指令（如 `rtk ./mvnw test`），一般指令亦可直接執行
- 同時啟動前後端：`mise run dev`（`mise.toml` 另有 `dev:backend` / `dev:frontend` / `d2`）
- 前端 `npm install` 免 flag：`frontend/.npmrc` 已設 `legacy-peer-deps=true`（`openapi-typescript@7` 只支援 TS^5，專案用 TS 6），勿移除該設定

## 架構重點

- 三層（Controller → Service → Repository，`backend/src/main/java/com/d0w0b/phytotrack/`），DTO 隔在 API 邊界，實體不進出 Controller（ADR-003 / ADR-005）
- `spring.jpa.open-in-view: false`：交易外不得 Lazy 載入，回傳需 DTO 投影或 FETCH JOIN，否則 `LazyInitializationException` / N+1
- 認證：JWT + BCrypt + RBAC（VIEWER / STAFF / ADMIN），`@PreAuthorize` 集中宣告。非 dev profile 需環境變數 `JWT_SECRET`（`JwtSecretValidator` fail-fast）；dev 預設帳號由 `app.bootstrap.*` 建立（admin/admin123、staff/staff123、viewer/viewer123）
- SQLite 特性：`hibernate-community-dialects` 的 `SQLiteDialect`、Hikari `maximum-pool-size: 1`、`LocalDate`/`LocalDateTime` 需自訂 JPA 轉換器（`converter/`）
- AI：llama-server / LlamaStash proxy 於 **11435**（避開 Spring Boot 的 8080），Spring AI 以 OpenAI 相容格式串接（`api-key` 為 dummy）；模型名稱需對應 `GET /v1/models`。機台特定值（`AI_BASE_URL`/`AI_MODEL`/`AI_API_KEY`）放 `backend/.env`（gitignored，範本 `backend/.env.example`），`application.yaml` 以 `${VAR:預設值}` 承接

## 測試（Spring Boot 4 特有的坑）

- `@MockBean` 已移除 → `@MockitoBean`（`org.springframework.test.context.bean.override.mockito`）
- `@WebMvcTest` / `@DataJpaTest` / `@AutoConfigureTestDatabase` 已搬遷至 `org.springframework.boot.{webmvc,data.jpa,jdbc}.test.autoconfigure`
- `@WithMockUser` 於 web slice 需自行註冊 `TestSecurityContextHolderStrategyAdapter` bean + MockMvc `springSecurity()` configurer（範例見 `CaseControllerTest`）
- 測試走獨立 SQLite（`application-test.yaml` → `./target/phytotrack-test.db`），須加 `@ActiveProfiles("test")`
- JPA Auditing 於 `@DataJpaTest` 仍會覆寫 `createdAt` → 斷言 `isNotNull()`，勿斷言確切時間
- seed 資料的 `senders` 有 `name+phone` UNIQUE，測試送件人資料別撞值

## Git 慣例

- Conventional Commits：英文標題 + 中文內文（`refactor:`、`test:`、`docs:`、`feat:`、`chore:`）
- **預設提交新 commit**。僅當 binary 或「面試相關」等不該進 git 的內容誤入版本控制時，才以原地重寫處理（`git reset --soft <基點>` 依原訊息重新 commit，不新增 commit、不 amend、不 push）
- 分支不主動推送，由使用者決定 PR 時機；Phase 0 已合併 `main`，Phase 1 由 `main` 開新分支進行
- `docs/notebook/`（個人學習筆記）與 `*.pdf` 已 gitignore，不要提交
- `.opencode/` 為本機設定（gitignored），含 OpenSpec 技能（openspec-propose / apply / archive / update / sync）

## OpenSpec 操作

- 主規格 `openspec/specs/`：10 份能力 spec（security-hardening、api-observability、case-search、case-lifecycle、case-statistics、case-report、user-admin、reference-data-admin、sender-management、ops-backup）；security-hardening 已實作交付、api-observability 僅含已交付項（Actuator/logback 歸 Phase 2）
- 工作流：`openspec new change` → proposal → apply（實作＋驗證）→ archive；Phase 1 採**每能力一個獨立 change**（spec 已在主規格，新 change 只需 proposal+tasks，不需 delta spec，於 `.openspec.yaml` 設 `skip_specs: true`）
- 常用指令：`openspec list` / `status --change <name>` / `validate --specs` / `validate --changes`
- umbrella change `hardening-and-operational-features` 與 `case-search` 已 archive 至 `openspec/changes/archive/2026-08-19-.../`（proposal/design/tasks 歷史保留，`.openspec.yaml` 隨目錄移動）；`case-lifecycle` 已 archive 至 `openspec/changes/archive/2026-08-20-case-lifecycle/`；`case-statistics` 已 archive 至 `openspec/changes/archive/2026-08-20-case-statistics/`；`case-report` 已 archive 至 `openspec/changes/archive/2026-08-21-case-report/`；`user-admin` 已 archive 至 `openspec/changes/archive/2026-08-21-user-admin/`；`reference-data-admin` 已 archive 至 `openspec/changes/archive/2026-08-21-reference-data-admin/`；active change：`sender-management`（實作中，未 archive）
- Phase 1 範圍 = 8 能力：case-search（已交付）、case-lifecycle（已交付）、case-statistics（已交付）、case-report（已交付）、user-admin（已交付）、reference-data-admin（已交付）、sender-management（已交付）、ops-backup（security-hardening 已交付、api-observability 剩餘歸 Phase 2）；建議下一能力由 `ops-backup` 開始
- 需求總覽：`docs/REQUIREMENTS.md`（10 能力狀態、Phase 1 範圍、能力間依賴與遷移注意）

## 文件

- `README.md`：快速啟動的權威來源
- `docs/adr/ADR-001~011`：架構決策（前後分離、Boot 4、三層、JWT/RBAC、DTO、JPA Auditing、SQLite→PostgreSQL、OpenAPI、llama 代理、統一錯誤處理、送件人管理），實作前先查對應 ADR
- `docs/ARCHITECTURE.md`、`docs/DEPLOY.md`
- `docs/REQUIREMENTS.md`：10 能力需求總覽與 Phase 1 範圍（見「OpenSpec 操作」）
- `openspec/specs`：Phase 1 能力契約基準（見「OpenSpec 操作」）