# Tasks: case-statistics 案件統計總覽與 Dashboard 視圖

## 1. 後端：DTO

- [x] 1.1 新增 `StatisticsDtos.CaseStatisticsResponse` (`totalCases` / `monthNewCases` / `pendingCases` / `topCrops` / `topPestCategories` / `statusRatio` / `monthlyTrend`) 與子 record (`CountName` / `StatusCount` / `MonthCount`)

## 2. 後端：Repository 查詢

- [x] 2.1 `CaseRepository`：覆寫 `findAll ()` 加 `@EntityGraph` (`crop`、`casePestCategories.pestCategory`) 預抓統計關聯；新增 `countByStatus (CaseStatus)` 與 `countByReceiveDateGreaterThanEqual (LocalDate)`

## 3. 後端：Service 統計聚合

- [x] 3.1 `CaseService.statistics ()` (`@Transactional (readOnly = true)`)：回傳總數、本月新增 (`receiveDate ≥ 本月初`)、待處理 (PENDING 計數)、top 作物／病蟲害 (Java 聚合取前 5)、狀態比例 (缺 0 補齊)、近 6 月趨勢 (`YYYY-MM` 逐月計數)

## 4. 後端：Controller 端點

- [x] 4.1 `CaseController` 新增 `GET /statistics` (`isAuthenticated ()`) 委派 `caseService.statistics ()`

## 5. 後端：測試

- [x] 5.1 `CaseServiceTest`：統計聚合 (top 排序、狀態比例補 0、趨勢月份、本月新增)；空資料庫全 0／空清單
- [x] 5.2 `CaseControllerTest`：`GET /api/cases/statistics` 登入可取 200 且結構正確；未登入 401
- [x] 5.3 整合測試：統計端點回 200 且結構完整 (statusRatio 3、trend 6 個月)；空資料庫情境由 CaseServiceTest mock 涵蓋

## 6. 前端：API 與 Dashboard 視圖

- [x] 6.1 由後端 `/v3/api-docs` 重新生成 `types/api.ts`；`api/index.ts` 加 `caseApi.statistics ()`
- [x] 6.2 `DashboardView.vue`：以純 Bootstrap 呈現統計 (總數／本月／待處理卡片、狀態比例進度條、top 作物與病蟲害表格、近 6 月趨勢表格、AI 連線狀態)

## 7. 驗證與文件同步

- [x] 7.1 全量驗證：`cd backend && ./mvnw test`、`cd frontend && npm run build && npm test`、`openspec validate --specs` / `--changes`
- [x] 7.2 同步 `docs/REQUIREMENTS.md` (case-statistics 標實作)、`docs/ARCHITECTURE.md` (統計端點)、`AGENTS.md`、操作手冊 `docs/manual.typ`、`docs/notebook/` 筆記