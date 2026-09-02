# PhytoTrack 架構文件

> 農作物病蟲害診斷諮詢服務系統。本文說明系統的整體架構、技術選型與各層職責。
> 架構決策的「為什麼」請見 [docs/adr](adr/README.md)。

## 1. 系統概覽

PhytoTrack 是**前後分離**的網頁應用：

- **前端**：Vue 3 + TypeScript + Vite，負責表單輸入與資料呈現 (5 人內 LAN 使用)
- **後端**：Spring Boot 4 REST API，負責業務邏輯、認證授權與資料存取
- **AI 引擎**：本機 llama.cpp (`llama-server`)，由後端透過 Spring AI (OpenAI 相容格式) 代理呼叫

```
瀏覽器 ──▶ Vue 3 前端 (Vite :5173)
              │  /api 代理
              ▼
          Spring Boot :8080  ──▶ SQLite (diagnoses.db)
              │ Spring AI (OpenAI 相容格式)
              ▼
          llama-server :11435 (本機 GGUF 模型)
```

## 2. 技術選型

| 面向 | 選擇 | 說明 |
|------|------|------|
| 後端框架 | Spring Boot 4.0.6 (Java 21) | 現代 Java 生態、自動設定 (Auto-Configuration) |
| 持久層 | Spring Data JPA (Hibernate 7)+ SQLite | 檔案型資料庫，零安裝，適合小規模；預留 PostgreSQL 升級路徑 (見 ADR-007) |
| 認證授權 | Spring Security + JWT (jjwt 0.12)+ BCrypt | 無狀態登入、角色權限 (RBAC) |
| API 規格 | springdoc (OpenAPI 3)+ Swagger UI | Controller 即規格來源，前端型別自動生成 |
| AI 整合 | Spring AI 2.0 (ChatClient) | 以 OpenAI 相容格式串接本機 llama.cpp |
| 前端 | Vue 3 + TypeScript + Pinia + Vue Router + Bootstrap 5 | 組合式 API (Composition API)、型別安全 |
| 錯誤處理 | 全域例外處理 (@RestControllerAdvice) | 統一錯誤結構，避免堆疊外洩 (見 ADR-010) |

## 3. 後端結構 (分層架構)

程式位於 `backend/src/main/java/com/d0w0b/phytotrack/`：

```
config/     設定類 (Security、CORS、OpenAPI、JPA Auditing)
controller/ REST 控制器：接收請求、校驗、呼叫 service
dto/        資料傳輸物件 (record)：API 邊界的請求/回應契約
exception/  業務例外 + 統一錯誤回應
models/     JPA 實體 (約 20 個，對應資料表)
repository/ Spring Data JPA 資料存取層
security/   JWT 產生/驗證、登入過濾器、UserDetails 實作
service/    商業邏輯 (Auth、Case、ReferenceData、AI、資料初始化)
converter/  LocalDate/LocalDateTime 屬性轉換器 (SQLite 相容)
```

### 請求流程

```
HTTP 請求
  → JwtAuthenticationFilter (解析 Bearer Token)
  → SecurityFilterChain (公開端點 / 角色限制)
  → Controller (@Valid 做 Bean Validation)
  → Service (交易邊界 @Transactional)
  → Repository (JPA 查詢)
  → SQLite
```

### 資料模型重點

- 核心實體 **Case** (案件)：`@ManyToOne` 關聯 Sender、Method、Crop、Service、Delivery、User (createdBy)；`status` 為 `CaseStatus` 列舉 (`PENDING`/`RESOLVED`/`CLOSED`)，以 `@Enumerated (EnumType.ORDINAL)` 儲存，既有 `INTEGER 0/1/2` 直接對應 (無資料遷移)；`caseDescription` 為土壤、栽培、用藥紀錄 (對應紙本表單，對應舊 `pestDescription`，BREAKING 直接重建 DB)
- 多對多關聯透過 Junction 表：CaseDamage、CaseHint、CasePestCategory (含 `pestNote` 學名：描述，同分類可多筆，無 `UNIQUE (case_id, pest_category_id)`)、CaseIdentifier
- **Sender** (送件人)：`name` 可空、`displayName` (Line/FB 暱稱) 可空，`phone` 與 `displayName` 至少一有值 (Service 層檢查)；顯示規則 `name (displayName)` / `displayName` / `name`；`phone` 非空時以部分唯一索引防重；**不以 DB UNIQUE 強制合併**——建案時以前端候選彈窗人工確認沿用 (帶 `senderId`) 或新建；ADMIN 可硬刪除未被引用的送件人 (見 ADR-011)
- **VIEWER 個資遮蔽**：`CaseService.toDetail/toSummary` 依當前角色判斷，VIEWER 的回應不含送件人姓名/電話/地址，但保留縣市鄉鎮與 `senderId`
- **統計去重鍵**：不重複送件人以 `COALESCE (phone, displayName)` distinct 計數
- 案件列表篩選以視圖 `v_case_search` (`schema.sql` 以 `LEFT OUTER JOIN` 涵蓋可空關聯，多對多以 `GROUP_CONCAT (DISTINCT name, '、')` 頓號聚合，`CaseSearchView` 以 `@Subselect` 唯讀映射，含 `sender_type_id`) 為基礎，經 `CaseSpecifications.buildView ()` 動態組合 17 欄（`receiveDateFrom/To`、`status`、`cityId`/`districtId`、`senderName/senderQuery` 三欄合一、`senderTypeId`、`serviceId`/`deliveryId`/`methodId`、`cropCategoryId`/`cropId`、`damageId`、`pestTypeId`/`pestCategoryId`、`hintId`，AND 組合，鄉鎮必先選縣市；篩選卡 5 列換行：收件日期區間/狀態｜田區縣市鄉鎮/送件人/身分別｜服務/送件/耕種方式｜作物類別作物/被害部位｜害物/害物類別/建議類別，篩選/分頁/排序與卡片開啟狀態以 URL query 雙向同步，檢視/編輯返回保持）；`status` 為列舉字串契約，由 `CaseService` 解析為 `CaseStatus` 後傳入視圖查詢 (非法值 fail-fast 400 `INVALID_STATUS`)，視圖分頁後回補 `Case` 實體以保留 `VIEWER` 遮蔽；`GET /api/cases/export` 沿用相同 `CaseFilter` 穿透篩選全量匯出（`caseId asc`、UTF-8 BOM、全欄位 `"` 引號、狀態中文 `待處理/已處理/已結案`、表頭 `田區位置/身分別`）
- 時間戳與建立者由 **JPA Auditing** 自動填寫 (`@CreatedDate`/`@LastModifiedDate`/`@CreatedBy`)，實作 `AuditorAware` 從 SecurityContext 取值 (見 ADR-006)
- SQLite 日期欄位以 `converter/` 的字串轉換器處理，避免 Hibernate 7 SQLiteDialect 的 epoch 毫秒寫入/嚴格格式讀取不一致問題

### 認證授權

- 登入成功後簽發 JWT (含 userId、role)，前端存於 localStorage，之後以 `Authorization: Bearer <token>` 帶入；停用帳號登入被拒 (`ACCOUNT_DISABLED`)
- `JwtAuthenticationFilter` 每請求以 `userId` 查 DB 驗證 `active`，停用帳號的既有 token 立即 401，且以 DB 的最新 `role` 覆蓋 token 內 role (角色變更有即時生效)
- 角色：`ROLE_VIEWER` (檢視者)/ `ROLE_STAFF` (診斷員)/ `ROLE_ADMIN` (管理者)
- 權限：建立/更新案件與 AI 診斷需 STAFF+；狀態轉移 `RESOLVED → CLOSED` 僅 ADMIN (`PENDING → RESOLVED` 需 STAFF+)；**已結案案件僅 ADMIN 可修改內容** (STAFF 改內容回 403 `CLOSED_CASE_READONLY`，狀態同值 no-op 合法)；刪除案件與使用者管理僅 ADMIN
- 送件人更新：update 依「有提供的 name/phone (未提供沿用現送件人身分)」比照 create 的去重語意關聯或建立送件人，不直接修改可能被多案件共享的既有 Sender row (避免撞 `UNIQUE (name, phone)`)
- 密碼一律 BCrypt 單向雜湊，永不存明文；`/api/auth/register` 僅能建立 VIEWER，防止越權提權
- 安全錯誤語意：**未認證** (無 token／無效／過期) 由 `RestAuthenticationEntryPoint` 回 `401 UNAUTHORIZED` (統一錯誤格式)，前端攔截器據此清除本機 token 並導向登入頁；**已登入但角色不足**由全域例外處理回 `403 ACCESS_DENIED` (見 ADR-010)

### AI 診斷流程

1. 前端表單收集欄位 → `POST /api/ai/analyze`
2. `AIService` 用 Spring AI `ChatClient` 組出 System (角色與回覆規則)+ User (表單內容) 提示詞
3. 非串流 `.call ()` 等待完整回覆 → 回傳建議文字與耗時
4. `GET /api/ai/health` 由後端主動檢查 llama-server 存活，供前端顯示模型狀態

## 4. 前端結構

程式位於 `frontend/src/`：

```
api/      axios 實例 (baseURL /api)+ 型別化 API 函式；攔截器自動附 JWT、統一錯誤彈窗
stores/   Pinia 狀態 (登入 token / user，持久化於 localStorage)
router/   路由表 + 全域守衛 (登入、角色權限)
views/    頁面：Home (hero 首頁)、Login、Register、Dashboard、Cases (列表＋篩選工具列)、CaseDetail (明細＋列印診斷單＋即時 AI 診斷)、CaseForm (診斷表單)、Users (管理員)、ReferenceDataAdmin (ADMIN 參照資料管理，頁籤式 CRUD)
types/    openapi-typescript 由 /v3/api-docs 自動生成的 API 型別 (與後端契約同步)
```

- 開發時 Vite 將 `/api` 代理至後端 `:8080`，避免 CORS
- API 型別產生方式：後端啟動後執行 `npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.ts`
- 診斷表單支援縣市→鄉鎮市區分組下拉、多選 (被害部位/病蟲害/防治建議/簽名人)、AI 診斷 (SweetAlert 呈現結果)

## 5. API 一覽

| 方法 | 路徑 | 權限 | 說明 |
|------|------|------|------|
| POST | /api/auth/register | 公開 | 註冊 (預設 VIEWER) |
| POST | /api/auth/login | 公開 | 登入並取得 JWT |
| POST | /api/auth/me | 登入 | 目前使用者 |
| POST | /api/auth/logout | 登入 | 登出 (JWT 無狀態，前端丟棄 token) |
| GET | /api/cases | 登入 | 分頁案件列表 (經 `v_case_search` 視圖)；篩選參數：`receiveDateFrom/To`、`status`、`cityId`/`districtId` (縣市必先選)、`senderName`/`senderQuery` (`name/displayName/phone` 三欄合一 LIKE)、`senderTypeId`、`serviceId`/`deliveryId`/`methodId`、`cropCategoryId`/`cropId`、`damageId`、`pestTypeId`/`pestCategoryId`、`hintId`，多參數 AND，前端篩選卡 5 列換行 |
| GET | /api/cases/statistics | 登入 | 案件統計總覽：總數／本月新增／待處理／top 作物與病蟲害 (top 5)／狀態比例／近 6 月趨勢；空資料庫回 0 或空清單。月份以收件日期 (`receiveDate`) 為基礎 |
| GET | /api/cases/export | STAFF/ADMIN | CSV 匯出 (`text/csv`，attachment 下載，UTF-8 BOM，全欄位 `"` 引號)：沿用列表相同篩選參數全量匯出（`caseId asc`）；表頭 `收件編號,收件日期,狀態(中文),田區位置,身分別,姓名...栽培面積,被害面積,土壤栽培用藥紀錄,病害...診斷結果,建議事項,防治描述,鑑定者,建立者...` |
| GET | /api/cases/{id} | 登入 | 案件詳細 |
| POST | /api/cases | STAFF+ | 建立案件 |
| PUT | /api/cases/{id} | STAFF+ | 更新案件 (純量欄位、送件人、多對多關聯整組替換、狀態轉移) |
| DELETE | /api/cases/{id} | ADMIN | 刪除案件 |
| GET | /api/ref/* | 登入 | 參照資料 (作物、病蟲害、縣市等下拉選單) |
| POST | /api/admin/ref/damages | ADMIN | 新增被害部位 |
| PUT | /api/admin/ref/damages/{id} | ADMIN | 修改被害部位 |
| DELETE | /api/admin/ref/damages/{id} | ADMIN | 刪除被害部位 (被引用時 409) |
| POST | /api/admin/ref/hints | ADMIN | 新增防治建議 |
| PUT | /api/admin/ref/hints/{id} | ADMIN | 修改防治建議 |
| DELETE | /api/admin/ref/hints/{id} | ADMIN | 刪除防治建議 (被引用時 409) |
| POST | /api/admin/ref/methods | ADMIN | 新增耕種方式 |
| PUT | /api/admin/ref/methods/{id} | ADMIN | 修改耕種方式 |
| DELETE | /api/admin/ref/methods/{id} | ADMIN | 刪除耕種方式 (被引用時 409) |
| POST | /api/admin/ref/deliveries | ADMIN | 新增送件方式 |
| PUT | /api/admin/ref/deliveries/{id} | ADMIN | 修改送件方式 |
| DELETE | /api/admin/ref/deliveries/{id} | ADMIN | 刪除送件方式 (被引用時 409) |
| POST | /api/admin/ref/services | ADMIN | 新增服務類別 |
| PUT | /api/admin/ref/services/{id} | ADMIN | 修改服務類別 |
| DELETE | /api/admin/ref/services/{id} | ADMIN | 刪除服務類別 (被引用時 409) |
| POST | /api/admin/ref/identifiers | ADMIN | 新增簽名人 |
| PUT | /api/admin/ref/identifiers/{id} | ADMIN | 修改簽名人 |
| DELETE | /api/admin/ref/identifiers/{id} | ADMIN | 刪除簽名人 (被引用時 409) |
| POST | /api/admin/ref/sender-types | ADMIN | 新增身分別 |
| PUT | /api/admin/ref/sender-types/{id} | ADMIN | 修改身分別 |
| DELETE | /api/admin/ref/sender-types/{id} | ADMIN | 刪除身分別 (被引用時 409) |
| POST | /api/admin/ref/crops | ADMIN | 新增作物 (需 cropCategoryId) |
| PUT | /api/admin/ref/crops/{id} | ADMIN | 修改作物 |
| DELETE | /api/admin/ref/crops/{id} | ADMIN | 刪除作物 (被引用時 409) |
| POST | /api/admin/ref/crop-categories | ADMIN | 新增作物分類 |
| PUT | /api/admin/ref/crop-categories/{id} | ADMIN | 修改作物分類 |
| DELETE | /api/admin/ref/crop-categories/{id} | ADMIN | 刪除作物分類 (下有作物或被引用時 409) |
| POST | /api/admin/ref/pest-categories | ADMIN | 新增病蟲害小分類 |
| PUT | /api/admin/ref/pest-categories/{id} | ADMIN | 修改病蟲害小分類 |
| DELETE | /api/admin/ref/pest-categories/{id} | ADMIN | 刪除病蟲害小分類 (被引用時 409) |
| GET | /api/senders/search?q= | 登入 | 送件人搜尋 (name/phone/displayName 部分比對，限 10 筆)，供建案去重候選 |
| GET | /api/senders | 登入 | 送件人列表 |
| GET | /api/senders/{id} | 登入 | 送件人詳細 |
| DELETE | /api/senders/{id} | ADMIN | 硬刪除送件人 (被案件引用時 409 `REFERENCE_IN_USE`，無 soft delete) |
| POST | /api/ai/analyze | STAFF+ | AI 診斷 |
| GET | /api/ai/health | 公開 | llama-server 健康檢查 |
| GET | /api/admin/users | ADMIN | 使用者清單 (含 active) |
| PATCH | /api/admin/users/{id}/role | ADMIN | 調整使用者角色 |
| PATCH | /api/admin/users/{id}/active | ADMIN | 啟停用帳號 (停用後既有 token 立即失效) |
| POST | /api/admin/users/{id}/reset-password | ADMIN | 重設使用者密碼 (BCrypt) |

完整規格：Swagger UI (`http://localhost:8080/swagger-ui/index.html`) 或 `/v3/api-docs`。

## 6. 環境設定

後端設定集中在 `backend/src/main/resources/application.yaml`：

- `app.jwt.secret`：JWT 簽章密鑰，正式環境以環境變數 `JWT_SECRET` 覆蓋
- `app.bootstrap.*`：首次啟動自動建立的帳號 (admin / staff / viewer)
- `spring.ai.openai.*`：llama-server 連線設定
- `application-postgres.yaml`：PostgreSQL 升級 profile (見 ADR-007)