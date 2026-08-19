# Tasks: case-search 案件列表篩選

## 1. 後端：Repository 篩選能力

- [x] 1.1 `CaseRepository` 加入 `JpaSpecificationExecutor<Case>`；規格查詢的 fetch join（sender/crop/service）於 `CaseSpecifications.build` 內處理（此版 JpaSpecificationExecutor 的 `findAll(Specification, Pageable)` 為 abstract，default method + `@EntityGraph` 覆寫不編譯）

## 2. 後端：Service 動態組合篩選

- [x] 2.1 於 `CaseDtos` 新增 `CaseFilter` 記錄（`cropId`、`serviceId`、`senderName`、`receiveDateFrom`、`receiveDateTo`、`status`，皆可空）
- [x] 2.2 `CaseService.list` 改為 `list(CaseFilter, Pageable)`：依非空欄位組裝 `Specification`（AND 組合、`senderName` LIKE 部分比對並跳脫 `%`/`_`），維持 `Page` 回傳；無任何條件時行為與現況一致
- [x] 2.3 `status` 列舉字串對映整數（`PENDING`→0、`RESOLVED`→1、`CLOSED`→2），非法值拋 `ApiException("INVALID_STATUS", 400)`

## 3. 後端：Controller 接收參數

- [x] 3.1 `CaseController.list` 新增 `@RequestParam(required = false)` 的 `cropId`/`serviceId`/`senderName`/`receiveDateFrom`/`receiveDateTo`/`status`，組 `CaseFilter` 委派 service

## 4. 後端：測試

- [x] 4.1 `CaseRepositoryTest` 新增規格篩選測試（DataJpaTest）：作物＋狀態 AND、`senderName` 部分比對、日期區間、無條件時全量
- [x] 4.2 `CaseServiceTest`：更新 `list` 簽章測試；新增 status 對映、非法 status 拋 400、`Specification` 組合（以 captor 驗證條件）
- [x] 4.3 `CaseControllerTest`：更新 `list` stub 簽章；新增帶篩選參數請求驗證委派；非法 `status` 回 400 `INVALID_STATUS`

## 5. 前端：API 與型別

- [x] 5.1 `api/index.ts` `caseApi.list` 擴充篩選參數型別；重新生成 `types/api.ts`（openapi-typescript）

## 6. 前端：篩選 UI 與狀態顯示

- [x] 6.1 新增 `utils/caseStatus.ts`（`STATUS_OPTIONS` 與 `statusLabel(int)`）並附 vitest 測試
- [x] 6.2 `CasesView.vue` 加入篩選工具列（作物／服務／送件人／收件日期起訖／狀態），篩選與清除按鈕，變更回第一頁重新載入；狀態欄改依 `statusLabel` 顯示

## 7. 文件同步與驗證

- [ ] 7.1 同步 `docs/REQUIREMENTS.md`（case-search 標實作）、`README.md`、`docs/ARCHITECTURE.md`、操作手冊 `docs/manual.typ`、`docs/notebook/` 筆記
- [ ] 7.2 全量驗證：`cd backend && ./mvnw test`、`cd frontend && npm run build && npm test`、`openspec validate --specs` / `--changes`，並以 dev 伺服器實測篩選