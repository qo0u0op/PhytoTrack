## 1. Phase 0：文件對齊

- [x] 1.1 重寫根 `README.md`：前後分離架構、技術堆疊 (Boot 4/Java 21/Vue 3/SQLite/Spring AI)、功能清單、llama-server 啟動方式、測試指令、目錄結構
- [x] 1.2 新增 `RequestIdFilter` (OncePerRequestFilter，最優先序) 為每個請求產生 UUID 寫入 MDC
- [x] 1.3 `ErrorResponse` 增加 `details` 欄位；`GlobalExceptionHandler` 從 MDC 讀取 requestId，`ApiException` 亦記錄 log，validation 錯誤填入 `details`
- [x] 1.4 10 個 ADR 檔狀態標題「已決定」→「已實作」與索引同步；更新 ADR-010 描述對齊實作 (泛用 ApiException、details)

## 2. Phase 0：安全補洞

- [x] 2.1 `application.yaml` 設 `spring.jpa.open-in-view: false`
- [x] 2.2 前端新增 `escapeHtml` 工具並套用於 `CaseFormView.vue` AI 建議渲染 (不再以未轉義 HTML 注入 SweetAlert)
- [x] 2.3 JWT 密鑰 fail-fast：非 dev profile 使用開發預設密鑰時啟動失敗 (新增啟動期檢查 bean，密鑰值常數化)
- [x] 2.4 以 `./mvnw test` 與 `npm run build` 驗證 Phase 0 無回歸

## 3. Phase 0：測試補強

- [x] 3.1 `@WebMvcTest` AuthController：登入、註冊、Bean Validation、未授權 401/403
- [x] 3.2 `@WebMvcTest` CaseController：RBAC (viewer 建案 403、登入可列)、Security filter chain
- [x] 3.3 `@DataJpaTest` CaseRepository：junctions 儲存/讀取、SQLite 方言、日期 converter
- [x] 3.4 `@SpringBootTest`＋MockMvc 整合測試：登入→建立案件→查詢→權限拒絕流程
- [x] 3.5 前端最小 vitest (auth store 邏輯)，加入 `npm test` script

## 4. Phase 1：案件生命週期與更新契約

- [ ] 4.1 新增 `CaseStatus` enum (PENDING/RESOLVED/CLOSED)，`Case.status` 改 `@Enumerated (STRING)`，DTO 型別同步
- [ ] 4.2 `schema.sql` `cases.status` 欄位改 TEXT＋CHECK，種子資料對映；文件記錄既有 DB 遷移 (UPDATE cases SET status='PENDING' WHERE status=0)
- [ ] 4.3 `CaseUpdateRequest` 補送件人與四組 junction 欄位；`CaseService.update` 實作 Sender 重設 (依 name+phone 複用) 與 junction 清空重建
- [ ] 4.4 新增 `PUT /api/cases/{id}/status` 與轉移規則驗證 (PENDING→RESOLVED STAFF+；RESOLVED→CLOSED ADMIN；非法轉移 4xx)
- [ ] 4.5 CaseService/CaseController 測試更新 (status 轉移、更新契約)

## 5. Phase 1：案件搜尋

- [ ] 5.1 `CaseRepository` 繼承 `JpaSpecificationExecutor`，新增 `CaseSpecifications` (cropId/serviceId/status 等值、senderName like、日期區間)
- [ ] 5.2 `CaseController.list` 與 `CaseService.list` 支援篩選參數，維持分頁
- [ ] 5.3 前端 CasesView 加入篩選列 (作物、診斷員、送件人、日期、狀態)
- [ ] 5.4 搜尋測試 (含 AND 組合、部分比對、空條件)

## 6. Phase 1：統計與 Dashboard

- [ ] 6.1 新增 `StatsService` 與 `GET /api/stats/overview` (總數/本月/待處理/作物 topN/病蟲害 topN/狀態比例/近 6 月趨勢，空庫回 0/空清單)
- [ ] 6.2 DashboardView 以統計 API 重構 (移除取一頁 hack)，純 Bootstrap 卡片＋進度條＋表格
- [ ] 6.3 統計測試 (含空資料庫)

## 7. Phase 1：案件報告

- [ ] 7.1 前端 CaseDetailView (路由 `/cases/:id`) 顯示完整欄位與 AI 診斷結果
- [ ] 7.2 `@media print` 列印版式 (隱藏導覽列，輸出診斷單)
- [ ] 7.3 `GET /api/cases/export.csv` (isAuthenticated)，StreamingResponseBody＋UTF-8 BOM

## 8. Phase 1：使用者管理

- [ ] 8.1 `JwtAuthenticationFilter` 改由 DB 重載主體 (最新角色、`active=false` 或不存在則拒絕)
- [ ] 8.2 `CustomUserDetailsService` 停用帳號登入拋例外，advice 回「帳號已停用」
- [ ] 8.3 新端點：`PUT /api/admin/users/{id}/role`、`/active`、`/password` (ADMIN)
- [ ] 8.4 前端 UsersView 管理操作 (sweetalert 確認)
- [ ] 8.5 使用者管理測試 (角色變更、停用後登入/token 被拒、重設密碼登入)

## 9. Phase 1：參照資料 CRUD

- [ ] 9.1 `ReferenceDataController` 作物/病蟲害/服務/送達方式/標的 的 POST/PUT/DELETE (ADMIN)
- [ ] 9.2 被引用刪除檢查 (junction 表查詢)，被引用拋 4xx
- [ ] 9.3 前端 ReferenceDataView (ADMIN 專用，列表＋表單)
- [ ] 9.4 參照資料測試 (新增、被引用刪除拒絕)

## 10. Phase 1：備份

- [ ] 10.1 `scripts/backup.sh` (帶時間戳複製 diagnoses.db) 並寫入 DEPLOY.md
- [ ] 10.2 執行備份腳本驗證產出備份檔

## 11. Phase 2：可觀測性與監控

- [ ] 11.1 `pom.xml` 加 `spring-boot-starter-actuator`，exposure 限 health,info
- [ ] 11.2 `logback-spring.xml`：console＋滾動檔案 appender，pattern 含 `%X{requestId}`
- [ ] 11.3 驗證 `/actuator/health` 與錯誤日誌含 requestId

## 12. Phase 2：型別與建置驗證

- [ ] 12.1 `frontend/src/types/api.ts` 已由 openapi-typescript 生成；Phase 1 API 變更後需重新生成
- [ ] 12.2 `./mvnw test` 全綠、`npm run build` (vue-tsc) 綠、前端型別檢查無誤

## 13. Phase 2：文件收尾

- [ ] 13.1 新增 ADR：案件生命週期、統計/Dashboard、使用者管理 (含 token 撤銷取捨)、參照資料維護、監控與備份
- [ ] 13.2 更新 docs/ARCHITECTURE.md、docs/DEPLOY.md (Actuator、backup、status 遷移)、ADR 索引
- [ ] 13.3 檢視 git diff，依功能分階段 commit (見提案後之 commit 建議)
