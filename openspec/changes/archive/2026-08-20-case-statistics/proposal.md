# Proposal: case-statistics 案件統計總覽與 Dashboard 視圖

## Why

目前 Dashboard 僅顯示案件總數與 AI 連線狀態，診斷站無法快速掌握案件量、作物與病蟲害分布及趨勢。依 `openspec/specs/case-statistics/spec.md` 契約，提供統計總覽 API 與 Dashboard 統計視圖。

## What Changes

- 新增統計端點 `GET /api/cases/statistics`（登入即可存取），回傳：
  - `totalCases`（案件總數）、`monthNewCases`（本月新增）、`pendingCases`（待處理數）
  - `topCrops` / `topPestCategories`（作物／病蟲害 top 5：名稱＋件數）
  - `statusRatio`（PENDING／RESOLVED／CLOSED 各計數）
  - `monthlyTrend`（近 6 月逐月案件數，`YYYY-MM`）
  - 空資料庫時各項為 `0` 或空清單，不回錯誤
- 統計時間基礎以案件**收件日期（receiveDate）**為準（與 case-search 篩選一致；「本月新增」＝收件日 ≥ 本月初）
- 後端：`StatisticsDtos`（回應 DTO）、`CaseRepository`（`@EntityGraph` 預抓統計關聯 + derived count 查詢）、`CaseService.statistics()`（Java 聚合，本機資料量小故採單一查詢）
- 前端：`api.ts` 重新生成、`api/index.ts` 加 `caseApi.statistics()`、`DashboardView.vue` 以純 Bootstrap（卡片、進度條、表格）呈現統計，不引入第三方圖表庫
- 新增後端測試（含空資料庫情境）

## Capabilities

### New Capabilities

- `case-statistics`: 統計總覽 API 與 Dashboard 統計視圖（規格已存在於主規格 `openspec/specs/case-statistics/spec.md`，本 change 為其實作，故於 `.openspec.yaml` 設 `skip_specs: true`）

## Impact

- 後端：`StatisticsDtos`（新）、`CaseRepository`、`CaseService`、`CaseController`、相關測試
- 前端：`types/api.ts`（重新生成）、`api/index.ts`、`views/DashboardView.vue`
- 文件：`docs/REQUIREMENTS.md`、`docs/ARCHITECTURE.md`、`AGENTS.md`、操作手冊 `docs/manual.typ`、個人筆記
- 資料庫：無 schema 變更