# Tasks: case-lifecycle 案件狀態生命週期與更新契約補全

## 1. 後端：狀態列舉與遷移

- [x] 1.1 新增 `CaseStatus` 列舉（`PENDING` / `RESOLVED` / `CLOSED`）；`Case.status` 遷移為列舉並以 `@Enumerated(EnumType.ORDINAL)` 儲存，建立時預設 `PENDING`（既有 `INTEGER 0/1/2` 直接對應，無資料遷移）

## 2. 後端：DTO 契約

- [x] 2.1 `CaseSummaryResponse.status` / `CaseResponse.status` 由 `Integer` 改為 `String`（列舉字串）；`CaseUpdateRequest.status` 由 `Integer` 改為 `String`
- [x] 2.2 `CaseUpdateRequest` 新增送件人欄位（`senderName` / `senderPhone` / `senderAddress` / `senderDistrictId` / `senderTypeId`）與多對多 ID 集合（`damageIds` / `hintIds` / `pestCategoryIds` / `identifierIds`）

## 3. 後端：Service 轉移規則與更新契約

- [x] 3.1 `CaseService`：建立案件狀態為 `PENDING`；回應對映改為列舉字串；移除 `toStatusInt`，改以 `CaseStatus` 解析 filter 與 update 的狀態字串（fail-fast，非法值 400 `INVALID_STATUS`）
- [x] 3.2 更新狀態轉移規則：`PENDING → RESOLVED`（STAFF/ADMIN）、`RESOLVED → CLOSED`（僅 ADMIN，以 SecurityContext 角色判斷）；非法轉移（跳階、回退、CLOSED 變更）回應 400 `INVALID_STATUS_TRANSITION` 且狀態不變；未提供或同值不轉移
- [x] 3.3 `CaseService.update` 實作送件人欄位更新（比照 create 的 findOrCreateSender 邏輯）與多對多關聯整組替換

## 4. 後端：Repository

- [x] 4.1 `CaseSpecifications` 由接收 `Integer statusInt` 改為接收 `CaseStatus`（service 解析後傳入）

## 5. 後端：測試

- [x] 5.1 更新既有測試（`CaseServiceTest`、`CaseControllerTest`、`CaseRepositoryTest`）以符合 `status` 字串契約
- [x] 5.2 新增轉移規則測試：合法轉移（PENDING→RESOLVED）、非法轉移 400（跳階、回退）、ADMIN-only（RESOLVED→CLOSED 非 ADMIN 遭拒）、未帶 status 不轉移

## 6. 前端：狀態顯示與編輯

- [x] 6.1 `utils/caseStatus.ts` 改為收字串狀態（`statusLabel` / `STATUS_OPTIONS`）並更新 vitest
- [x] 6.2 `CasesView.vue` 狀態欄顯示與篩選對應字串契約
- [x] 6.3 `CaseFormView.vue` 編輯模式：狀態選擇（依角色限制選項）、送件人與多對多欄位

## 7. 驗證與文件同步

- [x] 7.1 全量驗證：`cd backend && ./mvnw test`、`cd frontend && npm run build && npm test`、`openspec validate --specs` / `--changes`
- [x] 7.2 同步 `docs/REQUIREMENTS.md`（case-lifecycle 標實作、移除對映說明）、`docs/ARCHITECTURE.md`（status 遷移、更新契約）、`AGENTS.md`、`README.md`、操作手冊 `docs/manual.typ`、`docs/notebook/` 筆記