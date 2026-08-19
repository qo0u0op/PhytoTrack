# PhytoTrack 架構文件

> 農作物病蟲害診斷諮詢服務系統。本文說明系統的整體架構、技術選型與各層職責。
> 架構決策的「為什麼」請見 [docs/adr](adr/README.md)。

## 1. 系統概覽

PhytoTrack 是**前後分離**的網頁應用：

- **前端**：Vue 3 + TypeScript + Vite，負責表單輸入與資料呈現（5 人內 LAN 使用）
- **後端**：Spring Boot 4 REST API，負責業務邏輯、認證授權與資料存取
- **AI 引擎**：本機 llama.cpp（`llama-server`），由後端透過 Spring AI（OpenAI 相容格式）代理呼叫

```
瀏覽器 ──▶ Vue 3 前端（Vite :5173）
              │  /api 代理
              ▼
          Spring Boot :8080  ──▶ SQLite（diagnoses.db）
              │ Spring AI（OpenAI 相容格式）
              ▼
          llama-server :11435（本機 GGUF 模型）
```

## 2. 技術選型

| 面向 | 選擇 | 說明 |
|------|------|------|
| 後端框架 | Spring Boot 4.0.6（Java 21） | 現代 Java 生態、自動設定（Auto-Configuration） |
| 持久層 | Spring Data JPA（Hibernate 7）+ SQLite | 檔案型資料庫，零安裝，適合小規模；預留 PostgreSQL 升級路徑（見 ADR-007） |
| 認證授權 | Spring Security + JWT（jjwt 0.12）+ BCrypt | 無狀態登入、角色權限（RBAC） |
| API 規格 | springdoc（OpenAPI 3）+ Swagger UI | Controller 即規格來源，前端型別自動生成 |
| AI 整合 | Spring AI 2.0（ChatClient） | 以 OpenAI 相容格式串接本機 llama.cpp |
| 前端 | Vue 3 + TypeScript + Pinia + Vue Router + Bootstrap 5 | 組合式 API（Composition API）、型別安全 |
| 錯誤處理 | 全域例外處理（@RestControllerAdvice） | 統一錯誤結構，避免堆疊外洩（見 ADR-010） |

## 3. 後端結構（分層架構）

程式位於 `backend/src/main/java/com/d0w0b/phytotrack/`：

```
config/     設定類（Security、CORS、OpenAPI、JPA Auditing）
controller/ REST 控制器：接收請求、校驗、呼叫 service
dto/        資料傳輸物件（record）：API 邊界的請求/回應契約
exception/  業務例外 + 統一錯誤回應
models/     JPA 實體（約 20 個，對應資料表）
repository/ Spring Data JPA 資料存取層
security/   JWT 產生/驗證、登入過濾器、UserDetails 實作
service/    商業邏輯（Auth、Case、ReferenceData、AI、資料初始化）
converter/  LocalDate/LocalDateTime 屬性轉換器（SQLite 相容）
```

### 請求流程

```
HTTP 請求
  → JwtAuthenticationFilter（解析 Bearer Token）
  → SecurityFilterChain（公開端點 / 角色限制）
  → Controller（@Valid 做 Bean Validation）
  → Service（交易邊界 @Transactional）
  → Repository（JPA 查詢）
  → SQLite
```

### 資料模型重點

- 核心實體 **Case**（案件）：`@ManyToOne` 關聯 Sender、Method、Crop、Service、Delivery、User（createdBy）
- 多對多關聯透過 Junction 表：CaseDamage、CaseHint、CasePestCategory、CaseIdentifier
- 案件列表篩選以 **Spring Data JPA `Specification`** 動態組合（`CaseSpecifications`，AND 組合）；`status` 現以整數儲存，篩選契約接受列舉字串並由 `CaseService` 對映後傳入查詢（過渡做法，待 case-lifecycle 遷移列舉）
- 時間戳與建立者由 **JPA Auditing** 自動填寫（`@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`），實作 `AuditorAware` 從 SecurityContext 取值（見 ADR-006）
- SQLite 日期欄位以 `converter/` 的字串轉換器處理，避免 Hibernate 7 SQLiteDialect 的 epoch 毫秒寫入/嚴格格式讀取不一致問題

### 認證授權

- 登入成功後簽發 JWT（含 userId、role），前端存於 localStorage，之後以 `Authorization: Bearer <token>` 帶入
- 角色：`ROLE_VIEWER`（檢視者）/ `ROLE_STAFF`（診斷員）/ `ROLE_ADMIN`（管理者）
- 權限：建立/更新案件與 AI 診斷需 STAFF+；刪除案件與使用者管理僅 ADMIN
- 密碼一律 BCrypt 單向雜湊，永不存明文；`/api/auth/register` 僅能建立 VIEWER，防止越權提權

### AI 診斷流程

1. 前端表單收集欄位 → `POST /api/ai/analyze`
2. `AIService` 用 Spring AI `ChatClient` 組出 System（角色與回覆規則）+ User（表單內容）提示詞
3. 非串流 `.call()` 等待完整回覆 → 回傳建議文字與耗時
4. `GET /api/ai/health` 由後端主動檢查 llama-server 存活，供前端顯示模型狀態

## 4. 前端結構

程式位於 `frontend/src/`：

```
api/      axios 實例（baseURL /api）+ 型別化 API 函式；攔截器自動附 JWT、統一錯誤彈窗
stores/   Pinia 狀態（登入 token / user，持久化於 localStorage）
router/   路由表 + 全域守衛（登入、角色權限）
views/    頁面：Home（hero 首頁）、Login、Register、Dashboard、Cases（列表＋篩選工具列）、CaseForm（診斷表單）、Users（管理員）
types/    openapi-typescript 由 /v3/api-docs 自動生成的 API 型別（與後端契約同步）
```

- 開發時 Vite 將 `/api` 代理至後端 `:8080`，避免 CORS
- API 型別產生方式：後端啟動後執行 `npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.ts`
- 診斷表單支援縣市→鄉鎮市區分組下拉、多選（被害部位/病蟲害/防治建議/簽名人）、AI 診斷（SweetAlert 呈現結果）

## 5. API 一覽

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| POST | /api/auth/register | 公開 | 註冊（預設 VIEWER） |
| POST | /api/auth/login | 公開 | 登入並取得 JWT |
| POST | /api/auth/me | 登入 | 目前使用者 |
| POST | /api/auth/logout | 登入 | 登出（JWT 無狀態，前端丟棄 token） |
| GET | /api/cases | 登入 | 分頁案件列表；篩選參數：`cropId`、`serviceId`、`senderName`（LIKE 部分比對）、`receiveDateFrom`、`receiveDateTo`、`status`（`PENDING`/`RESOLVED`/`CLOSED`），多參數為 AND 組合 |
| GET | /api/cases/{id} | 登入 | 案件詳細 |
| POST | /api/cases | STAFF+ | 建立案件 |
| PUT | /api/cases/{id} | STAFF+ | 更新案件 |
| DELETE | /api/cases/{id} | ADMIN | 刪除案件 |
| GET | /api/ref/* | 登入 | 參照資料（作物、病蟲害、縣市等下拉選單） |
| POST | /api/ai/analyze | STAFF+ | AI 診斷 |
| GET | /api/ai/health | 公開 | llama-server 健康檢查 |
| GET | /api/admin/users | ADMIN | 使用者清單 |

完整規格：Swagger UI（`http://localhost:8080/swagger-ui/index.html`）或 `/v3/api-docs`。

## 6. 環境設定

後端設定集中在 `backend/src/main/resources/application.yaml`：

- `app.jwt.secret`：JWT 簽章密鑰，正式環境以環境變數 `JWT_SECRET` 覆蓋
- `app.bootstrap.*`：首次啟動自動建立的帳號（admin / staff）
- `spring.ai.openai.*`：llama-server 連線設定
- `application-postgres.yaml`：PostgreSQL 升級 profile（見 ADR-007）