## Why

前後分離重構已全部完成並驗證，但存在「文件承諾 ≠ 實作現況」的信用落差（根 README 仍描述舊 Thymeleaf/session 架構、ADR-010 承諾的 requestId 串接與錯誤契約未完全兌現、ADR-003 宣稱的測試可測性沒有測試證明），且有真實安全缺口（AI 輸出以 HTML 注入前端、OSIV 開啟、JWT 預設密鑰在 git）。同時，作為 5 人內診斷站工具，目前欠缺案件生命週期、搜尋、使用者管理、參照資料維護等實際營運功能。

## What Changes

- **Phase 0 文件同步與承諾兌現**：重寫根 README；對齊 ADR-010（requestId 以 MDC 進 log、ErrorResponse 補 `details`、語意化例外）；ADR 檔狀態標題同步為「已實作」。
- **安全補洞**：關閉 `spring.jpa.open-in-view`；AI 診斷建議輸出轉義（修 XSS）；JWT 預設密鑰 fail-fast（非 dev profile 啟動即失敗）。
- **測試補強**：新增 `@WebMvcTest`（含 Security/Validation/RBAC）、`@DataJpaTest`、`@SpringBootTest`+MockMvc 整合測試；前端最小 vitest。
- **案件搜尋／篩選**：`GET /api/cases` 增加作物、診斷員、送件人、日期區間、狀態篩選。
- **案件生命週期**：`status` 由 int 改 `CaseStatus` enum（PENDING/RESOLVED/CLOSED）人工標記；更新契約補上送件人與多對多關聯可編輯。**BREAKING**：現有 SQLite `int` 狀態資料需遷移。
- **統計與 Dashboard**：新增統計 API（總數、本月、待處理、作物/病蟲害 topN、狀態比例、趨勢），前端以純 Bootstrap 呈現。
- **案件報告**：單案明細檢視頁 + `@media print` 列印診斷單 + CSV 匯出。
- **使用者管理完整化**：ADMIN 可改角色、啟停用帳號（登入流程補檢查 `active`）、重設密碼。
- **參照資料維護 CRUD**：作物/病蟲害/服務/送達方式/標的 後台管理（含被引用不得刪除檢查）；縣市維持 seed。
- **監控與備份**：Actuator 精簡暴露（health/info）、logback 滾動檔案 appender（含 requestId）、`scripts/backup.sh` 備份 SQLite。

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

- **後端**：`Case` 實體（status 型別）、`User`（active 登入檢查）、CaseController/CaseService（篩選、狀態轉移）、UserAdminController/AuthService（管理操作）、ReferenceDataService（寫入端點）、GlobalExceptionHandler/ErrorResponse（details、MDC）、SecurityConfig（open-in-view、JWT fail-fast）、AIService。
- **資料**：`schema.sql` 的 `cases.status` 欄位型別變更；現有 SQLite 資料庫需重建或手動遷移。
- **前端**：CasesView（篩選）、CaseFormView（XSS 修復、status 選擇）、CaseDetailView（新增）、DashboardView（統計）、UsersView（管理操作）、ReferenceDataView（新增）；`src/types/api.ts` 需以 openapi-typescript 重新生成。
- **依賴**：新增 `spring-boot-starter-actuator`（後端）。
- **文件**：根 README、docs/ARCHITECTURE、docs/DEPLOY、docs/adr（狀態同步＋新增 ADR）。
- **Ops**：新增 `scripts/backup.sh`。