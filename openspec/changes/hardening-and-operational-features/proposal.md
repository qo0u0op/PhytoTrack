## Why

前後分離重構已完成並驗證（Phase 0 已交付）：README/ADR 文件與實作同步、requestId 與錯誤契約對齊 ADR-010、安全補洞（OSIV、AI 輸出轉義、JWT 密鑰 fail-fast）、測試覆蓋（後端 22 個測試＋前端 vitest）。現階段真正的缺口是診斷站的實際營運功能——案件生命週期、搜尋、統計、使用者管理、參照資料維護、備份均尚未實作。

## What Changes

### 已完成（Phase 0）

- **文件同步與承諾兌現**：重寫根 README；對齊 ADR-010（requestId 以 MDC 進 log、ErrorResponse 補 `details`、語意化例外）；10 個 ADR 狀態標題同步為「已實作」。
- **安全補洞**：關閉 `spring.jpa.open-in-view`；AI 診斷建議輸出轉義（修 XSS，含案件明細彈窗）；JWT 預設密鑰 fail-fast（非 dev profile 啟動即失敗）。
- **測試補強**：`@WebMvcTest`（含 Security/Validation/RBAC）、`@DataJpaTest`、`@SpringBootTest`+MockMvc 整合測試；前端 vitest（auth store，`npm test`）。

### 待辦（Phase 1）

- **案件搜尋／篩選**：`GET /api/cases` 增加作物、診斷員、送件人、日期區間、狀態篩選。
- **案件生命週期**：`status` 由 int 改 `CaseStatus` enum（PENDING/RESOLVED/CLOSED）人工標記；更新契約補上送件人與多對多關聯可編輯。**BREAKING**：現有 SQLite `int` 狀態資料需遷移。
- **統計與 Dashboard**：新增統計 API（總數、本月、待處理、作物/病蟲害 topN、狀態比例、趨勢），前端以純 Bootstrap 呈現。
- **案件報告**：單案明細檢視頁 + `@media print` 列印診斷單 + CSV 匯出。
- **使用者管理完整化**：ADMIN 可改角色、啟停用帳號（登入流程補檢查 `active`）、重設密碼。
- **參照資料維護 CRUD**：作物/病蟲害/服務/送達方式/標的 後台管理（含被引用不得刪除檢查）；縣市維持 seed。
- **監控與備份**：Actuator 精簡暴露（health/info）、logback 滾動檔案 appender（含 `%X{requestId}`）、`scripts/backup.sh` 備份 SQLite。

## Capabilities

### New Capabilities
- `security-hardening`: AI 輸出轉義、OSIV 關閉、JWT 密鑰 fail-fast
- `api-observability`: 錯誤回應 `details`、requestId 進伺服器 log（MDC）、Actuator health/info、logback
- `case-search`: 案件列表篩選查詢
- `case-lifecycle`: 案件狀態生命週期與更新契約補全
- `case-statistics`: 統計 API 與 Dashboard 視圖
- `case-report`: 單案明細、列印與 CSV 匯出
- `user-admin`: 使用者角色／啟停用／密碼重設管理
- `reference-data-admin`: 參照資料維護 CRUD
- `ops-backup`: SQLite 備份腳本

### Modified Capabilities
<!-- 無既有 spec（openspec/specs/ 目前為空），本 change 全部為新能力 -->

## Impact

**已完成（Phase 0）**
- **後端**：`GlobalExceptionHandler`/`ErrorResponse`（details、MDC requestId）、`SecurityConfig`（open-in-view:false）、JWT 密鑰 fail-fast、`RequestIdFilter`。
- **前端**：`CaseFormView`（AI 輸出 XSS 修復）、`CasesView`（明細彈窗 XSS 修復）、`escapeHtml` 工具、vitest（auth store）。
- **文件**：根 README、docs/adr 狀態同步（ADR-010 對齊）。

**待辦（Phase 1/2）**
- **後端**：`Case` 實體（status 型別）、`User`（active 登入檢查）、CaseController/CaseService（篩選、狀態轉移）、UserAdminController/AuthService（管理操作）、ReferenceDataService（寫入端點）、StatsController（統計）。
- **資料**：`schema.sql` 的 `cases.status` 欄位型別變更；現有 SQLite 資料庫需重建或手動遷移。
- **前端**：CasesView（篩選）、CaseFormView（status 選擇）、CaseDetailView（新增）、DashboardView（統計）、UsersView（管理操作）、ReferenceDataView（新增）；`src/types/api.ts` 於 API 變更後重新生成。
- **依賴**：新增 `spring-boot-starter-actuator`（後端）。
- **文件**：docs/ARCHITECTURE、docs/DEPLOY、新增 ADR。
- **Ops**：新增 `scripts/backup.sh`。