# Design: case-statistics 案件統計總覽與 Dashboard 視圖

## 1. 端點契約

`GET /api/cases/statistics` (`@PreAuthorize ("isAuthenticated ()")`，登入即可；無角色限制)

回應 (`StatisticsDtos.CaseStatisticsResponse`)：

```json
{
  "totalCases": 120,
  "monthNewCases": 18,
  "pendingCases": 7,
  "topCrops": [{ "name": "水稻", "count": 40 }],
  "topPestCategories": [{ "name": "稻熱病", "count": 25 }],
  "statusRatio": [{ "status": "PENDING", "count": 7 }],
  "monthlyTrend": [{ "month": "2026-03", "count": 14 }]
}
```

- `statusRatio` 依 `CaseStatus` 順序 (PENDING/RESOLVED/CLOSED) 輸出，缺的狀態補 `0`
- `monthlyTrend` 固定近 **6 月** (含本月)；`topCrops` / `topPestCategories` 固定 **top 5**
- 空資料庫：各數值 `0`、各清單為空陣列

## 2. 時間語意

統計以案件**收件日期 (`receiveDate`)**為月份基礎 (與 case-search 的 `receiveDateFrom/To` 篩選一致)：
「本月新增」＝ `receiveDate ≥ 本月初`；趨勢逐月依 `receiveDate` 分組。補登過去收件日期時該案會計入其收件月份。

## 3. 實作方式

- **`CaseRepository`**：覆寫 `findAll ()` 加 `@EntityGraph (attributePaths = {"crop", "casePestCategories.pestCategory"})` 預抓統計所需關聯 (避免逐案 Lazy N+1)；另加 derived count：`countByStatus (CaseStatus)`、`countByReceiveDateGreaterThanEqual (LocalDate)` (「待處理」「本月新增」走資料庫精確計數)
- **`CaseService.statistics ()`** (`@Transactional (readOnly = true)`)：`count ()`、`countByStatus`、`countByReceiveDateGreaterThanEqual` 取三數值；`findAll ()` (EntityGraph 預抓) 以 Java stream 聚合 top 作物／病蟲害 (groupBy 名稱後依計數排序取前 5)、狀態比例 (缺 0 補齊)、近 6 月趨勢 (產生月份清單逐月 count)
- 取捨：top／趨勢以 `findAll ()` 一次抓取 + Java 聚合 (本機 SQLite、案件量小，單一查詢最簡潔)；量大時再改 `@Query` group by
- **`CaseController`**：新增 `GET /statistics` 方法委派 `caseService.statistics ()`

## 4. 前端

- `api.ts` 由後端 `/v3/api-docs` 以 openapi-typescript 重新生成；`api/index.ts` 加 `caseApi.statistics ()`
- `DashboardView.vue` 改寫：
  - 三張卡片：案件總數／本月新增／待處理 (含 AI 連線狀態卡片保留)
  - 狀態比例：三條 Bootstrap `progress-bar` (寬度為各狀態佔比)
  - top 作物、top 病蟲害：Bootstrap 表格 (名稱＋件數)
  - 近 6 月趨勢：Bootstrap 表格 (月份＋件數)
  - 移除舊的 `caseApi.list ({page:0,size:1})` 取總數邏輯，改由 statistics 提供

## 5. 測試

- `CaseServiceTest`：統計聚合正確 (top 排序、狀態比例補 0、趨勢月份)；空資料庫全 0／空清單
- `CaseControllerTest`：`GET /api/cases/statistics` 登入可取、回 200 與結構
- 整合測試：空資料庫情境回 200 且各項為 0