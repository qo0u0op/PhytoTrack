# Design: case-search 案件列表篩選

## Context

- 契約：`openspec/specs/case-search/spec.md` (`GET /api/cases` 接受 `cropId`、`serviceId`、`senderName` 部分比對、`receiveDateFrom`、`receiveDateTo`、`status`，AND 組合，維持分頁)。
- 現況：`CaseRepository.findAll (Pageable)` (`@EntityGraph` 帶 `sender`/`crop`/`service`，避免列表 N+1)；`CaseService.list (Pageable)` 直接委派；`CaseController.list` 只有 `@PageableDefault`。
- `status` 現為 `INTEGER` (`schema.sql`：`status INTEGER NOT NULL DEFAULT 0`；seed 全部 0)。case-lifecycle 尚未實作列舉，本 change 的 `status` 篩選需在「接受列舉字串」的契約下對映現有 int (`0`→`PENDING`、`1`→`RESOLVED`、`2`→`CLOSED`)，此為已確認的過渡假設。
- 三層架構 (ADR-003/005)：Controller 不收業務邏輯，篩選參數以 DTO (`CaseFilter`) 跨邊界；`spring.jpa.open-in-view: false`，讀取需在交易內完成 (`list` 已 `@Transactional (readOnly = true)`)。

## Goals / Non-Goals

- **Goals**：動態組合 AND 篩選、維持分頁與現有無參數行為、`status` 以列舉字串收送、前端列表提供篩選工具列、補齊三層與前端測試。
- **Non-Goals**：不實作 case-lifecycle (狀態列舉欄位／轉移規則／遷移)；不做統計聚合；不改資料庫 schema；不處理非法日期格式參數的專屬錯誤碼 (沿用現有統一錯誤處理即可)。

## Decisions

### D1. 以 Spring Data JPA `Specification` 動態組合條件

`CaseRepository` 加入 `JpaSpecificationExecutor<Case>`，service 依 `CaseFilter` 非空欄位組裝 `Specification`，再以 `findAll (Specification, Pageable)` 分頁查詢。

- 優於多個具名 `@Query` 變體 (笛卡爾積方法數爆炸)、`@Query` + `SpEL` 動態條件 (可讀性差、易錯)。
- N+1 對策：規格查詢於 `CaseSpecifications.build` 內以 `root.fetch` 預先抓取 `sender`/`crop`/`service` (to-one 無多重 bag 問題)，並以 `query.getResultType () != Long` 區隔 count 查詢避免重複 fetch；無條件路徑仍走 `CaseRepository.findAll (Pageable)` 的既有 `@EntityGraph`。
  - 替代方案 (試過不採納)：default method 覆寫 `findAll (Specification, Pageable)` 疊加 `@EntityGraph`——此 Spring Data 版本的 `JpaSpecificationExecutor.findAll` 為 abstract，`JpaSpecificationExecutor.super.findAll (...)` 無法編譯 (編譯錯誤)，故改採 fetch join。

### D2. 篩選條件以 `CaseDtos.CaseFilter` 記錄跨邊界

Controller 以 `@RequestParam (required = false)` 接收各篩選參數，組 `CaseFilter` 傳給 `CaseService.list (CaseFilter, Pageable)`；service 內判斷 `null` 決定是否加入條件 (AND 組合)。

- 對映路徑：`cropId`→`crop.cropId`、`serviceId`→`service.serviceId`、`senderName`→`sender.name` (`LIKE %值%`)、`receiveDateFrom/To`→`receiveDate >=/<=`、`status`→`status` 整數。
- `senderName` 的 `%`、`_` 為 SQL LIKE 萬用字元，需跳脫後再組 `%…%`，避免使用者輸入被當萬用字元。

### D3. `status` 字串對映與驗證放在 service 層

`CaseFilter.status` 保留列舉字串；service 以對照表轉 `Integer` (`PENDING`→0、`RESOLVED`→1、`CLOSED`→2)，非法值拋 `ApiException ("INVALID_STATUS", 400)`，沿用統一錯誤形狀 (ADR-010)。

- case-lifecycle 遷移為列舉欄位後，僅需移除對照表，契約 (列舉字串) 不變，前端無感。

### D4. 前端：`caseApi.list` 擴充參數 + 篩選工具列 + 狀態標籤

- `api/index.ts`：`caseApi.list` 型別加上 `cropId?/serviceId?/senderName?/receiveDateFrom?/receiveDateTo?/status?`，參數僅送有值的欄位。
- 重新生成 `types/api.ts` (openapi-typescript)，確保前後端契約同步。
- `CasesView.vue`：卡片式篩選工具列 (作物下拉——由 `refApi.cropCategories` 攤平、服務下拉、送件人文字輸入、收件日期起訖、狀態下拉)；「篩選」與「清除」按鈕；篩選變更回第一頁重新載入。
- 狀態顯示：抽出 `utils/caseStatus.ts` (`statusLabel (int)`、`STATUS_OPTIONS`)，列表以真實狀態顯示標籤 (原固定「待處理」改為對映)，並附 vitest 測試。

## Risks / Trade-offs

- [Specification 查詢下 `root.fetch` 在 SQLite/Spring Data 4 的相容風險] → 以 repository 測試 (DataJpaTest) 驗證；若失敗則退回不 fetch (接受 N+1) 或 JPQL fetch join 具名查詢變體。
- [`status` 過渡對映與未來 case-lifecycle 列舉可能漂移] → 對照集中在 service 單一私有方法，遷移時只改此處。
- [LIKE 效能隨資料量劣化] → 既有 `idx_cases_status`、外鍵索引已存在；案件量級下可接受，統計/報表另議。

## Migration Plan

- 無 schema 變更。API 向後相容 (未帶參數行為不變)；`status` 參數由數字改列舉字串屬已宣告的 **BREAKING** (現無外部呼叫方)。
- 部署：隨後端啟動即可，無資料搬移。

## Open Questions

 (無：`status` 過渡假設與 scope 已於提案階段確認。)