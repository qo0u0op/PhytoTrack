# Proposal: case-search 案件列表篩選

## Why

目前 `GET /api/cases` 僅支援純分頁，診斷站每天案件量大時無法快速找出特定作物、診斷員或送件人的案件。依 `openspec/specs/case-search/spec.md` 契約，為案件列表加上可選篩選參數，滿足診斷站的查詢需求。這是 Phase 1 的第一個 change，作為後續能力的暖身。

## What Changes

- `GET /api/cases` 接受可選查詢參數：`cropId`、`serviceId`、`senderName`（部分比對）、`receiveDateFrom`、`receiveDateTo`、`status`
- 多個參數同時存在時以 **AND** 組合；未帶任何參數時行為與現有分頁列表一致
- 後端以 Spring Data JPA `Specification` 動態組合條件，維持 `Page` 分頁回傳
- `status` 篩選接受列舉字串（`PENDING` / `RESOLVED` / `CLOSED`），於 service 層對映至現有整數欄位（`0` / `1` / `2`）；此為過渡假設，待 case-lifecycle 將欄位遷移為列舉後無縫銜接，**BREAKING**：`status` 查詢參數不接受數字
- 前端案件列表（`CasesView.vue`）加入篩選工具列（作物、服務類別、送件人、日期區間、狀態），篩選變更時回到第一頁並重新查詢
- 新增對應測試（後端 `CaseControllerTest` / repository、前端 vitest）

## Capabilities

### New Capabilities

- `case-search`: 案件列表依作物、診斷員、送件人、日期區間與狀態篩選（規格已存在於主規格 `openspec/specs/case-search/spec.md`，本 change 為其實作，故於 `.openspec.yaml` 設 `skip_specs: true`）

### Modified Capabilities

（無：既有主規格 case-search 已涵蓋本 change 之契約）

## Impact

- 後端：`CaseRepository`（新增 `JpaSpecificationExecutor` 與 `Specification`）、`CaseService.list` 擴充篩選參數、`CaseController.list` 新增 `@RequestParam`
- 前端：`api/index.ts` 的 `caseApi.list` 參數擴充、`CasesView.vue` 篩選工具列、`types/api.ts` 重新生成
- 文件：`docs/REQUIREMENTS.md`、`AGENTS.md`、操作手冊 `docs/manual.typ`、`README.md`、`docs/ARCHITECTURE.md`、個人筆記同步
- 資料庫：無 schema 變更（`status` 仍為 INTEGER）