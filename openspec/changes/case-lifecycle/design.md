# Design: case-lifecycle 案件狀態生命週期與更新契約補全

## 1. 狀態遷移策略（ORDINAL 相容）

`Case.status` 由 `int` 遷移為 `CaseStatus` 列舉，以 `@Enumerated(EnumType.ORDINAL)` 儲存：

- DB 欄位維持 `status INTEGER NOT NULL DEFAULT 0`，**schema 與既有資料不動**
- 列舉宣告順序即序數：`PENDING=0, RESOLVED=1, CLOSED=2`，與既有 `0/1/2` 完全對應
- 既有案件讀取與篩選（`idx_cases_status`）不需任何資料搬移，符合 spec「0 → PENDING」遷移需求
- 風險：ORDINAL 對列舉順序敏感；換取零遷移成本。以列舉順序鎖定（不得重排）並於註解提醒

## 2. API 契約（BREAKING）

| 欄位 | 前 | 後 |
|---|---|---|
| `CaseSummaryResponse.status` / `CaseResponse.status` | `Integer` | `String`（`PENDING`/`RESOLVED`/`CLOSED`） |
| `CaseUpdateRequest.status` | `Integer` | `String`（同左） |
| `CaseFilter.status` | `String`（已含） | 不變 |

`CaseResponse` 另補 `senderDistrictId` / `senderTypeId`（供前端編輯回填送件人欄位）。

## 3. 狀態轉移規則

| 現況 | 目標 | 允許角色 | 非法時 |
|---|---|---|---|
| PENDING | RESOLVED | STAFF/ADMIN | — |
| RESOLVED | CLOSED | 僅 ADMIN | 非 ADMIN 回應 403 / 業務拒 |
| 其餘變動（跳階、回退、CLOSED 變更） | — | — | 400 `INVALID_STATUS_TRANSITION`，狀態不變 |

- 更新未帶 `status` → 不觸發轉移；帶同值 → 視為無操作
- 角色判斷：`update` 端點已 `@PreAuthorize(STAFF/ADMIN)`，於 Service 以 `SecurityContextHolder` 檢查 ADMIN 才放行 `RESOLVED → CLOSED`

## 4. 更新契約補全

`CaseUpdateRequest` 新增：

- 送件人：`senderName` / `senderPhone` / `senderAddress` / `senderDistrictId` / `senderTypeId`（任一提供即更新案件關聯的既有 Sender 對應欄位；district/type 以 `getRef` 解析）
- 多對多：`damageIds` / `hintIds` / `pestCategoryIds` / `identifierIds`（組非 null 時整組替換：先清空該組 junction 再重建）

既有 `create` 邏輯重構：`addJunctions` 拆為 4 個單組方法，create 與 update 共用。

## 5. 前端

- `caseStatus.ts`：`statusLabel` / `statusBadgeClass` 改收 `CaseStatusValue`（字串），移除整數對映
- `CasesView.vue`：`CaseSummary.status` 改 `string`，顯示用字串版 label/badge（篩選已為字串，不變）
- `CaseFormView.vue`：編輯模式新增狀態選擇（依角色限制選項：STAFF 僅可選 PENDING/RESOLVED，ADMIN 全選）、送件人與多對多欄位送出；`loadCase` 回填 `senderDistrictId` / `senderTypeId`