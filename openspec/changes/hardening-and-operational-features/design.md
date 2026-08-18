## Context

重構已交付（見 proposal.md「Why」）：三層架構、DTO、JWT/RBAC、Spring AI＋llama.cpp、SQLite。本設計處理兩件事：Phase 0 的信用與安全補救（文件對齊、可觀測性、安全、測試），以及 Phase 1 的營運功能（搜尋、生命週期、統計、報告、使用者管理、參照資料、備份）。

關鍵現況事實：
- `Case.status` 為 `int = 0`，`Case` 的 `sender/method/crop/service/delivery` 皆為 `@ManyToOne`，四組 junction（`caseDamages/caseHints/casePestCategories/caseIdentifiers`）以 `@OneToMany + CascadeType.ALL + orphanRemoval` 管理。
- `Sender` 依 `name + phone` 複用（`findOrCreateSender`），同一 Sender 可被多個案件共用——更新時不可就地改共用 Sender。
- `CaseUpdateRequest` 目前缺送件人與 junction 欄位（打錯字無法修）。
- JWT filter 為純無狀態（不查 DB），token 內含角色；`User.active` 已存在但登入與 filter 皆未檢查。
- `application.yaml` 的 `app.jwt.secret` 有開發預設值並可由 `JWT_SECRET` 覆蓋。
- `docs/adr/` 共 10 個 ADR，索引標「已實作」，但各檔狀態標題仍為「已決定」。

## Goals / Non-Goals

**Goals:**
- 讓「文件承諾 = 實作現況」：README 描述新架構、ADR 狀態與實作同步、錯誤契約（`details`、requestId）完整兌現。
- 消除已知安全缺口：AI 輸出注入、OSIV、JWT 預設密鑰落正式環境。
- 建立可證明的測試覆蓋：controller 層（含 Security）、repository 層、端到端流程。
- 交付診斷站實際營運功能（搜尋、狀態流轉、統計、明細/匯出、使用者管理、參照資料、備份）。
- 前端維持零第三方依賴（純 Bootstrap）。

**Non-Goals:**
- 不引入 Flyway、refresh token、i18n、AI 串流/快取、PDF 套件（列印用 CSS）。
- 不新增縣市/鄉鎮的維護介面（維持 seed，見 proposal）。
- 不做登入速率限制與帳號鎖定（列為已知取捨，另行評估）。

## Decisions

### D1 安全補救

- **OSIV**：`application.yaml` 設 `spring.jpa.open-in-view: false`。既有程式在 Service 交易內讀取 Lazy 關聯，關閉後無影響。
- **AI XSS**：`CaseFormView.vue` 的 `Swal.fire({ html: ... })` 改為先對 AI 建議做 HTML 轉義（新增 `escapeHtml` 工具，或改用 `textContent`/純文字 `swal` 渲染）。決策：在 `frontend/src/utils` 新增 `escapeHtml` 並套用於 AI 建議與任何動態內文，最小改動、可複用。
- **JWT fail-fast**：新增 `@Configuration`（或 `ApplicationRunner`）於非 `dev` profile 且 `app.jwt.secret` 等於開發預設值時拋例外中止啟動。將預設值常數化，避免 magic string。

### D2 可觀測性（對齊 ADR-010）

- **requestId**：新增 `RequestIdFilter`（`OncePerRequestFilter`，優先序最前）為每個請求產生 UUID 並放入 `MDC`，`finally` 清除。`GlobalExceptionHandler` 改為從 `MDC` 讀取 requestId（與回應一致），業務例外（`ApiException`）也記錄 log。此舉兌現 ADR-010「requestId 串接日誌」。
- **錯誤契約**：`ErrorResponse` 增加 `details`（`Map<String,Object>`，可空）；`MethodArgumentNotValidException` 將欄位錯誤填入 `details`。
- **Actuator**：`pom.xml` 加入 `spring-boot-starter-actuator`；`management.endpoints.web.exposure.include=health,info`。
- **logback**：新增 `logback-spring.xml`，console 與滾動 file appender（含 `%X{requestId}` pattern）。

### D3 案件生命週期與更新契約

- **enum**：新增 `CaseStatus { PENDING, RESOLVED, CLOSED }`；`Case.status` 改為 `@Enumerated(EnumType.STRING) CaseStatus`，DTO 對應型別同步改。
- **遷移**：`schema.sql` 的 `cases.status` 欄位改為 `TEXT NOT NULL DEFAULT 'PENDING'` 並含 `CHECK`；既有開發 DB 採「重建」（刪除 `diagnoses.db` 由 schema.sql 重建，種子資料 `INSERT OR IGNORE` 冪等）。文件註明既有資料需手動遷移（`UPDATE cases SET status='PENDING' WHERE status=0`）。
- **轉移規則**：`CaseService.updateStatus(id, newStatus, principal)` 內驗證合法轉移（PENDING→RESOLVED：STAFF+；RESOLVED→CLOSED：ADMIN）；非法轉移拋 `ApiException`（4xx）。權限以 `@PreAuthorize` 於新端點 `PUT /api/cases/{id}/status` 分層（STAFF 端點拒 CLOSED）。
- **更新契約補全**：`CaseUpdateRequest` 增加送件人欄位（`senderName/senderPhone/senderAddress/senderDistrictId/senderTypeId`）與四組 junction ID 集合，與 `CaseCreateRequest` 對齊。Sender 更新決策：**不可就地改共用 Sender**——比照 `findOrCreateSender`，依新 `name+phone` 尋找既有 Sender，存在則重設 `case.sender`，否則建立新 Sender；junctions 以「清空重建」方式更新（`clear()` + 重建，靠 `orphanRemoval` 清理舊關聯）。

### D4 案件搜尋

- `GET /api/cases` 增加 `cropId/serviceId/senderName/receiveDateFrom/receiveDateTo/status` 查詢參數，全數可選。
- `CaseService.list` 改用 `JpaSpecificationExecutor`（`CaseRepository` 繼承）搭配 `CaseSpecifications` 組 `Specification<Case>`：`senderName` 以 `case_.sender.name` join 加 `like`；`status` 以 enum 過濾；`cropId/serviceId` 直接等值；日期用 `between`。join junction 不需（作物為 `@ManyToOne` 單值）。

### D5 統計與報告

- **統計**：新增 `StatsController`（`GET /api/stats/overview`，`isAuthenticated()`）＋ `StatsService`，使用 `CaseRepository` 的派生查詢/JPQL：
  - 總數、本月數、待處理數（`countByStatus`）
  - 作物 topN、病蟲害 topN（junction `casePestCategories` group by）
  - 狀態比例（`countByStatus`）、近 N 月趨勢（`receiveDate` group by month，取最後 6 個月）
  - 空資料庫一律回 0/空清單（spec `case-statistics`）。
- **報告**：新增前端 `CaseDetailView`（路由 `/cases/:id`）；列印以 `@media print` 隱藏導覽列並輸出診斷單版式；CSV 匯出新增 `GET /api/cases/export.csv`（`isAuthenticated()`），以 `StreamingResponseBody` 寫 CSV（UTF-8 BOM 供 Excel 開檔），不引入套件。

### D6 使用者管理（含 token 撤銷）

- JWT filter 改為**由 DB 重載主體**：以 token 的 subject（username）查 `UserRepository`，載入最新角色與 `active`；使用者不存在或 `active=false` 則不建立認證（請求遭拒）。代價：每個受保護請求一次 DB 查詢（SQLite 本機、<5 人可接受）；好處：角色變更、停用立即生效——一次解決 `user-admin` 的兩條需求。
- `AuthService` 登入路徑（`CustomUserDetailsService`）檢查 `active=false` 時拋 `DisabledException`，`GlobalExceptionHandler` 對應回「帳號已停用」。
- 新端點（`UserAdminController`，皆 ADMIN）：`PUT /api/admin/users/{id}/role`、`PUT /api/admin/users/{id}/active`、`PUT /api/admin/users/{id}/password`。

### D7 參照資料 CRUD 與備份

- `ReferenceDataController` 增加作物/病蟲害/服務/送達方式/標的 的 POST/PUT/DELETE（ADMIN）；刪除前以 repository 查詢是否被案件引用，被引用則拋 4xx。junctions 屬多對多，被引用檢查針對 `CaseDamage/CasePestCategory/CaseHint` 等 junction 表。
- `scripts/backup.sh`：`cp backend/diagnoses.db backup/diagnoses-$(date +%Y%m%d-%H%M%S).db`，寫入 DEPLOY.md。

## Risks / Trade-offs

- **[JWT filter 改查 DB]** → 失去純無狀態、每請求一次查詢。Mitigation：本機 SQLite 單寫入、<5 人；必要時以 Caffeine 短 TTL 快取主體。
- **[status 欄位型別遷移]** → `ddl-auto:update` 不保證改欄位型別，SQLite 也可能 `database is locked`。Mitigation：採「刪 DB 重建」＋文件化手動 UPDATE 步驟；`maximum-pool-size: 1` 已避免寫入並發。
- **[junction 清空重建]** → 可能造成無關 delete/insert。Mitigation：案件量小，接受；更新於單一交易內，失敗回滾。
- **[共用 Sender 重設]** → 若兩案件同 Sender，改一個會把 Sender 換成新的，另一案件不受影響（符合預期），但可能產生資料重複。Mitigation：name+phone 複用邏輯一致，重複僅發生於地址/身分不同時——可接受。
- **[統計 join junction]** → group by 量大時慢。Mitigation：本機資料量小；topN 以 `LIMIT` 限制。

## Migration Plan

1. 先合併 Phase 0（README/ADR/安全/測試）——獨立可回滾，不碰資料。
2. Phase 1 部署前：備份 `diagnoses.db`（`scripts/backup.sh`），執行 `schema.sql` 重建（含新 `status` 欄位定義）；既有資料若需保留，先手動 `UPDATE cases SET status='PENDING' WHERE status=0` 再啟動。
3. 回滾：還原備份檔即可（SQLite 單檔）。

## Open Questions

- 統計「近 N 月」的 N 預設值與是否允許查詢參數調整（不影響 spec，實作時可定 6）。
- 案件明細頁是否合併進既有 CaseFormView 的唯讀模式，或獨立檢視（不影響 spec 行為，實作時取決於前端結構）。
