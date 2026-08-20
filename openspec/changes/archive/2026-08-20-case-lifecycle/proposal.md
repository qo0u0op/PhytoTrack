# Proposal: case-lifecycle 案件狀態生命週期與更新契約補全

## Why

目前 `Case.status` 為無語意的整數（`0/1/2`），案件建立後狀態無法有效流轉，也無轉移規則約束；更新契約僅能改純量欄位，無法修正送件人或多對多關聯（damage/hint/pestCategory/identifier）。依 `openspec/specs/case-lifecycle/spec.md` 契約，為案件導入明確狀態生命週期並補全更新能力。

## What Changes

- 新增 `CaseStatus` 列舉（`PENDING` / `RESOLVED` / `CLOSED`）；`Case.status` 由 `int` 遷移為列舉，以 `@Enumerated(ORDINAL)` 儲存——**既有 `INTEGER` 值 `0/1/2` 直接對應，DB 欄位與資料不需變更**，符合 spec「0 → PENDING」遷移需求
- **BREAKING**：API 回應的 `status` 由數字改為列舉字串（`CaseSummaryResponse.status` / `CaseResponse.status`）
- 狀態轉移規則：STAFF/ADMIN 可 `PENDING → RESOLVED`；**僅 ADMIN** 可 `RESOLVED → CLOSED`；任何非法轉移（跳階、回退、CLOSED 變更）回應 4xx `INVALID_STATUS_TRANSITION`，狀態維持不變；更新未帶 `status` 或同值時不觸發轉移
- `CaseUpdateRequest.status` 由 `Integer` 改為列舉字串（與 API 契約一致）
- 更新契約補全：`CaseUpdateRequest` 新增送件人欄位（`senderName` / `senderPhone` / `senderAddress` / `senderDistrictId` / `senderTypeId`）與多對多 ID 集合（`damageIds` / `hintIds` / `pestCategoryIds` / `identifierIds`），提供時整組替換
- 篩選對映：`CaseService` 以 `CaseStatus` 解析 filter 與 update 的狀態字串（fail-fast），`CaseSpecifications` 接收列舉、維持純 SQL 條件
- 前端：`caseStatus.ts` 改收字串狀態；`CasesView.vue` 狀態顯示與篩選對應；`CaseFormView.vue` 編輯模式支援狀態選擇（依角色限制選項）與送件人、多對多欄位
- 新增對應測試（後端單元／web slice／repository、前端 vitest）

## Capabilities

### New Capabilities

- `case-lifecycle`: 案件狀態列舉、轉移規則、更新契約補全與既有資料相容（規格已存在於主規格 `openspec/specs/case-lifecycle/spec.md`，本 change 為其實作，故於 `.openspec.yaml` 設 `skip_specs: true`）

### Modified Capabilities

- `case-search`: 其 `status` 對映邏輯（`CaseService.toStatusInt`）將隨遷移移除，篩選契約（列舉字串）不變

## Impact

- 後端：`CaseStatus`（enum）、`Case`、`CaseDtos`（回應 status 型別、更新請求擴充）、`CaseService`（轉移規則、更新契約、對映）、`CaseSpecifications`（接收列舉）、相關測試
- 前端：`utils/caseStatus.ts`、`views/CasesView.vue`、`views/CaseFormView.vue`、`types/api.ts`（重新生成）
- 文件：`docs/REQUIREMENTS.md`、`docs/ARCHITECTURE.md`、`AGENTS.md`、操作手冊 `docs/manual.typ`、個人筆記
- 資料庫：無 schema 變更（`status` 保持 `INTEGER NOT NULL DEFAULT 0`，enum 以 ORDINAL 對映 `0/1/2`）