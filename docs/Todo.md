# PhytoTrack 作品集開發計畫

> Java Spring Boot 作品，適用於補習班專題發表與面試作品展示
>
> 技術棧：Vue 3 + Bootstrap 5.3 + Axios（前後端分離） | Spring Boot 4 REST API | SQLite（WAL mode）
>
> 預估時程：6 週（每週約 10-15 小時）

---

## Phase 1（第 1-2 週）— MVP：核心 CRUD + 角色認證

**目標**：可登入、可新增/查看案件、三角色權限隔離、附單元測試

### 任務

- [ ] **1-1 Vue 3 前端專案建置（Vite + Bootstrap 5.3 + Axios）**
  - 在專案根目錄建立 `frontend/` 資料夾：
    ```bash
    npm create vite@latest frontend -- --template vue
    cd frontend && npm install
    npm install bootstrap@5.3 axios vue-router@4 pinia
    ```
  - Vite proxy 設定 (`vite.config.js`) 將 `/api` 代理到 Spring Boot：
    ```js
    server: {
      proxy: { '/api': 'http://localhost:8080' }
    }
    ```
  - `main.js` 全域引入 Bootstrap CSS：
    ```js
    import 'bootstrap/dist/css/bootstrap.min.css'
    import 'bootstrap/dist/js/bootstrap.bundle.min.js'
    ```
  - 專案結構：
    ```
    frontend/
    ├── src/
    │   ├── api/              ← axios 實例 + 各模組 API
    │   ├── components/        ← 共用元件（Navbar, Sidebar, Pagination）
    │   ├── views/             ← 頁面元件（Login, CaseList, CaseForm…）
    │   ├── router/            ← Vue Router 設定
    │   ├── stores/            ← Pinia 狀態管理
    │   └── App.vue
    ├── vite.config.js
    └── package.json
    ```
  - 提供 `start-dev.sh`：同時啟動 Spring Boot + Vite dev server
  - 生產建置：`npm run build` → static 資源給 Spring Boot 或 Nginx 服務

- [ ] **1-2 Spring Security + JWT**
  - Spring Security 三種角色（ROLE_VIEWER / ROLE_STAFF / ROLE_ADMIN）
  - BCryptPasswordEncoder 密碼加密
  - JWT 認證（login 回傳 token，前端存 localStorage，axios interceptor 帶 Authorization header）
  - Role-based API endpoint 權限控制（`@PreAuthorize`）
  - [技術驗證點] 說明為什麼選 JWT 而非 Session-based（前後端分離 SPA 天生需要 stateless auth）

- [ ] **1-3 JPA Entity 完整設計（12 張表）**
  - 核心：`Case`、`Sender`、`Crop`、`CropCategory`、`Pest`、`PestCategory`
  - Lookup：`Damage`、`Hint`、`Service`、`Deliver`、`FarmingMethod`
  - 地理：`County`、`Town`
  - 鑑定者：`Identifier`
  - Junction（M:N）：`CaseDamage`、`CaseHint`、`CasePestCategory`
  - 使用者：`User`
  - [技術驗證點] 說明 @ManyToMany vs @OneToMany + join entity 的取捨

- [ ] **1-4 Bean Validation**
  - `@NotBlank` `@Size` `@Pattern` `@PastOrPresent` 等驗證註解
  - 統一的 validation error response 格式（`{ field: "cropId", message: "..." }`），前端 Vue 表單逐欄顯示

- [ ] **1-5 @ControllerAdvice + REST 統一錯誤回應**
  - 自訂 `BusinessException` 繼承體系
  - 統一 JSON 錯誤結構 `{ error: string, code: number, details: ... }`
  - Vue 端 axios interceptor 統一攔截 `4xx/5xx` 處理
  - [技術驗證點] 說明 @ControllerAdvice 與直接 try-catch 的差異

- [ ] **1-6 Vue 頁面：Login + 案件列表/新增/檢視**
  - Login 頁（Vue Router + Pinia auth store + axios interceptor）
  - 案件列表（Bootstrap Table + 分頁元件 + role-based action buttons）
  - 新增案件表單（Bootstrap Form + 縣市→鄉鎮區連動下拉、害物類型→小分類連動）
  - 案件明細頁（read-only 檢視 + 編輯/簽署按鈕依角色顯示）

- [ ] **1-7 測試**
  - `@DataJpaTest` → 各 Repository 基本 CRUD 測試
  - `@WebMvcTest(CaseController.class)` → REST Controller 層測試（MockMvc + JSON assert）
  - `@SpringBootTest` → 完整流程測試（login API → create case → query）
  - Vue 前端可選：Vitest 單元測試（composables / stores / utils）

### Phase 1 展示成果

```
1. 登入頁（展示 JWT auth flow + Pinia state）
2. 案件列表（展示 Bootstrap Table + Pageable + role-based buttons）
3. 新增案件表單（展示連動下拉 + Bean Validation error display）
4. 角色權限驗證（Viewer 看不到新增/編輯按鈕）
5. 測試報告（覆蓋率 > 70%）
```

---

## Phase 2（第 3-4 週）— 進階功能：搜尋 / 報表 / API / 審計

**目標**：多條件篩選、統計報表、REST API、審計日誌

### 任務

- [ ] **2-1 JPA Specifications 動態查詢**
  - `CaseSpecification` 實作多條件組合查詢（日期區間、作物、害物、鑑定者、狀態、關鍵字）
  - 取代 `@Query` + `LIKE` 的拼接方式
  - [技術驗證點] 比較 Specifications vs @Query vs Criteria API 的適用場景

- [ ] **2-2 案件編輯 + AuditLog（@Aspect 實作）**
  - `@Aspect` + `@Around` 自動攔截 Service 層修改方法
  - `AuditLog` Entity 記錄：誰、何時、哪個欄位、舊值、新值
  - Commit 批次簽署機制（多筆變更一次確認）
  - [技術驗證點] 說明 AOP 的 join point 選擇（為什麼切 Service 而非 Controller）

- [ ] **2-3 REST API 強化（Vue 前端對接）**
  - `GET /api/pest-categories?pestTypeId={id}` → 連動下拉資料源
  - `GET /api/crops?cropCategoryId={id}` → 作物連動
  - `GET /api/districts?cityId={id}` → 鄉鎮區連動
  - `POST /api/cases` → 新增案件（Vue 表單提交）
  - `PUT /api/cases/{id}` → 編輯案件
  - [技術驗證點] 說明前後端分離後 API 設計原則（RESTful resource naming + versioning）

- [ ] **2-4 報表 + CSV 匯出**
  - 月報表：害物排行、作物別統計（Chart.js）
  - ServeltOutputStream 串流寫入 CSV（不佔記憶體）
  - [技術驗證點] 說明串流寫入 vs 先 collect 再寫入的記憶體差異

- [ ] **2-5 Commit 管理頁面 + Diff 檢視**
  - 編輯草稿列表（已修改但未簽署的案件）
  - Before/After 差異對比（兩欄式或行內標記）

- [ ] **2-6 測試**
  - `@AutoConfigureMockMvc` REST API 測試
  - `@DataJpaTest` + `@TestEntityManager` Specification 測試
  - Service layer 單元測試（Mockito）

### Phase 2 展示成果

```
1. 動態篩選案件列表（展示 JPA Specifications）
2. 案件編輯 + 草稿管理 + Commit（展示 AOP AuditLog）
3. REST API 呼叫（展示 JSON response + autocomplete）
4. 報表圖表 + CSV 匯出（展示 Chart.js + 串流輸出）
5. 測試報告（覆蓋率 > 80%）
```

---

## Phase 3（第 5-6 週）— 打磨：管理後台 + 部署 + 面試準備

**目標**：Admin 後台、可部署的 JAR、面試故事與 README

### 任務

- [ ] **3-1 Admin 管理後台**
  - 使用者管理（列表 / 角色變更 / 啟用停用）
  - 鑑定者管理（CRUD）
  - 參考表管理（Lookup table CRUD，展示擴展性設計）
  - Commit 審計查詢

- [ ] **3-2 診斷結果表單完整實作（下半部）**
  - 害物分類 tab 切換 + checkbox grid
  - 可信度評分（1-5 星，CSS star rating）
  - 病蟲害名稱自動完成（接 2-3 REST API）

- [ ] **3-3 Docker 部署（雙容器）**
  - `frontend/Dockerfile`：Vite build → Nginx 服務靜態檔
  - `backend/Dockerfile`：Spring Boot multi-stage build
  - `docker-compose.yml`（Nginx + Spring Boot + SQLite volume）
  - 一鍵啟動：`docker compose up -d`
  - 前端 API 代理：Nginx config 將 `/api` 轉發到 Spring Boot container
  - [技術驗證點] 說明雙容器部署 vs 單容器 JAR + embedded 靜態檔的取捨

- [ ] **3-4 README 與面試故事**
  - `README.md` 含：
    - ER 圖（使用 mermaid 或 D2 轉圖片）
    - Demo 截圖（4-6 張關鍵畫面）
    - 技術棧與選擇理由
    - 本地啟動步驟（`git clone → cd frontend && npm install && npm run build → cd .. && mvn package → java -jar`）
    - Docker 啟動步驟（`docker compose up`）
  - 準備 3 分鐘 demo 流程（從登入到產出月報表）
  - 準備常見面試問答（見下方）

- [ ] **3-5 最終測試**
  - 端到端流程測試（手動操作一遍所有功能）
  - 邊界案例測試（空列表、特殊字元、大量資料分頁）
  - SQL injection 測試（所有輸入欄位）

### Phase 3 展示成果

```
1. Admin 後台完整操作
2. 完整的診斷表單（上半部 + 下半部）
3. Docker 一鍵啟動（Nginx + Spring Boot 雙容器）
4. README + Demo 截圖
5. 3 分鐘 demo 流程
```

---

## 面試準備 — 預期問答

### 架構決策類

| 問題 | 回答重點 |
|------|---------|
| 為什麼選 SQLite 而非 PostgreSQL？ | 單機部署、零設定、WAL mode 解決並發瓶頸；JPA 抽象層讓未來切換成本極低 |
| 為什麼選 Vue 3 + Bootstrap 而非 React？ | 補習班學的就是 Vue，社群資源豐富；Bootstrap 5.3 元件成熟、文件好，適合後台系統快速疊加 |
| JWT auth 為什麼不選 Session-based？ | 前後端分離 SPA 無法用 cookie session；JWT stateless 適合 API 服務、未來可水平擴展 |
| @ManyToMany vs join entity 的取捨？ | join entity 可附帶額外欄位（confidence），且 Future-proof 對擴展 |
| Specification vs @Query 的選擇？ | Specification 可組合多個 optional 條件，避免 query 爆炸；@Query 適合固定查詢 |

### 技術實作類

| 問題 | 回答重點 |
|------|---------|
| 怎麼實作 AuditLog 的？ | @Aspect 切在 Service 層，攔截 update/delete 方法，反射比對欄位變化 |
| 怎麼處理表單驗證？ | Bean Validation annotation + @Valid + BindingResult，錯誤訊息統一格式 |
| 分頁怎麼做的？ | Spring Data Pageable + REST API 回傳 `Page<T>` JSON，前端 Vue Bootstrap Table 分頁 |
| CSV 匯出怎麼避免記憶體爆掉？ | ServletOutputStream 逐筆寫入，不 collect 到 List 再 flush |
| 測試覆蓋率多少？怎麼確保品質？ | Controller(@WebMvcTest + MockMvc JSON assert) + Repository(@DataJpaTest) + Service(Mockito) 三層測試 |

### 業務邏輯類

| 問題 | 回答重點 |
|------|---------|
| 流水號為什麼不在資料庫做？ | SQLite 無內建 sequence；Java layer 產生 + unique constraint 雙重保障 |
| 鑑定者為什麼獨立一張表？ | 離職後歷史案件仍保留簽名，不因 users 表刪除而消失 |
| 可信度的業務意義？ | 低可信度→歸類「其他」；累積高可信度案例可作為未來快速診斷的參考依據 |

---

## 附註：技術棧選擇摘要（面試用）

```
Spring Boot 4           — 當前最新穩定版，展示對新版 framework 的掌握
Spring Security + JWT   — Stateless auth，前後端分離標準做法
Spring Data JPA         — Entity 設計 + Specifications 動態查詢
Vue 3 + Vite            — Composition API + 極速 HMR，現代前端體驗
Bootstrap 5.3           — 成熟 CSS framework，後台系統快速疊加
Axios                   — HTTP client + interceptor（JWT token 注入）
Pinia                   — Vue 3 官方狀態管理（取代 Vuex）
Vue Router              — 前端路由 + 導航守衛（未登入導回 login）
SQLite（WAL mode）       — 零配置單機資料庫，適合小型內部系統
AOP（@Aspect）           — 審計日誌跨切面實作
JUnit 5 + MockMvc       — 分層測試（Controller / Service / Repository）
Vitest                  — 前端單元測試（composables / stores）
Docker                  — Multi-stage build（分離前端 Nginx + 後端 Spring Boot）
Chart.js                — 前端圖表
```

## 時程總表

| 週次 | Phase | 里程碑 | 可展示成果 |
|------|-------|--------|-----------|
| 第 1-2 週 | Phase 1 | 核心 CRUD + 認證 | 登入、案件 CRUD、角色權限、測試報告 |
| 第 3-4 週 | Phase 2 | 進階功能 | 動態查詢、審計日誌、REST API、報表、測試 |
| 第 5-6 週 | Phase 3 | 打磨部署 | Admin 後台、Docker、README、demo 流程 |

---

## 雙版本架構設計（2026-06-03 討論）

> 基於面試策略考量，**優先完成農會版（MVP）**，Online 版以設計文件呈現架構能力。

### 整體方案：Shared Kernel + Two Deployment Variants

```
phyto-track (parent pom)
├── phyto-common           ← 90% 業務邏輯（共用核心）
├── phyto-internal         ← 農會版（Vue 3 + Spring Boot REST + SQLite）
└── phyto-online           ← 全省版（Vue 3 + REST API + PG + Redis + K8s）
```

| 模組 | 技術 | 說明 |
|------|------|------|
| **phyto-common** | JPA Entities + Service Interfaces + Service Impl + DTOs | 只寫一次的業務邏輯，兩版共用 |
| **phyto-internal** | Vue 3 SPA + Spring Boot REST + JWT + SQLite | 單機 Docker（前端 build 進 Spring Boot 或 Nginx) |
| **phyto-online** | Vue 3 SPA + REST API + PostgreSQL + Redis + MyBatis + K8s | 前後端分離部署（CDN + API 多副本） |

### 取捨決策

| 維度 | 農會版（Internal） | 全省版（Online） | 取捨原因 |
|------|-------------------|-----------------|---------|
| **前端** | Vue 3 SPA（單機部署） | Vue 3 SPA（CDN + 分離部署） | 兩版同一套 Vue 程式碼，差異只在部署方式與後端 Scaling |
| **認證** | JWT（stateless） | JWT + Refresh Token | 兩版都 JWT；線上版加 refresh token rotation + Redis blacklist |
| **資料庫** | SQLite（WAL mode） | PostgreSQL + Read Replica | 內部零配置部署；線上需 MVCC + 連線池 + 水平讀取 |
| **Cache** | 無 | Redis（Cache-Aside + Session Store + Rate Limiting） | 內部查詢量 low；線上 ~5000 concurrent 需要 |
| **ORM** | Spring Data JPA | JPA + MyBatis（複雜報表） | JPA 管 Entity CRUD，MyBatis 管高效查詢瓶頸 |
| **部署** | Docker（單機） | K8s（多副本 + HPA） | 內部單機即可；線上需要高可用 + 水平擴展 |

### Redis 在 Online 版的三個用途

| 用途 | 實作方式 | 原因 |
|------|---------|------|
| **Cache** | Cache-Aside：作物列表、害物分類等 lookup table | 讀多寫少，減少 DB 查詢 |
| **Session Store** | Spring Session + Redis | K8s 多副本共享 session |
| **Rate Limiting** | Redis + Lua script | 防止單一 client 打爆 API |

### Entry-Level 面試策略

> **把一個版本做到 100% 完整，比兩個版本各做 50% 有用 10 倍。**

| 優先 | 項目 | 面試視角 |
|------|------|---------|
| 1 | Spring Security（角色權限） | Spring Boot 最高頻面試題 |
| 2 | Entity 設計（12 張表） | 展示關聯設計能力 |
| 3 | 案件 CRUD（列表+新增+檢視） | 核心功能完整 |
| 4 | 分頁 + 篩選（Specifications） | 展示處理真實需求 |
| 5 | 單元測試（3 層測試） | Junior 與新手最大差距 |
| 6 | Docker 一鍵啟動 | DevOps 基本常識 |
| 7 | README + 截圖 | 面試官第一印象 |

**不做的**：Phase 3 前不碰 K8s（先會 Docker 就夠了）。

### 面試話術（關於為什麼選 Vue 3）

> 「PhytoTrack 使用 Vue 3 + Bootstrap 5.3 做前端，因為是 SPA 表單系統，需要連動下拉、即時驗證、狀態管理。Vue 的 Composition API 搭配 Pinia 管理 auth state，程式碼比 options API 乾淨很多。**我的原則：場景適合什麼就用什麼，剛好補習班教的就是 Vue。**」

### 開發文件產出（Phase 3 前完成）

- `docs/architecture/online-architecture.md` — 線上版的完整架構設計（含上圖的 K8s 部署拓樸）
- `docs/adr/` — 架構決策紀錄（ADR），涵蓋雙版本每個技術維度的取捨
