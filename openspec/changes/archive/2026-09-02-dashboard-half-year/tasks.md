## 1. 後端統計

- [x] 1.1 於 `CaseService`/`CaseController` 新增 `HALF_YEAR` 期別（`year`+`half` 1/2 驗證與日期區間過濾），驗證 `GET /api/cases/statistics?period=HALF_YEAR&year=2026&half=1` 僅回該半年數據且缺參回 400
- [x] 1.2 執行 `rm -f backend/target/phytotrack-test.db && mvn test` 通過

## 2. 前端 Dashboard

- [x] 2.1 於 `DashboardView.vue` 期別選單新增 `半年度`，半年度時顯示 `上半年/下半年` 下拉並於呼叫時帶 `half`，驗證切換時統計正確刷新且中文顯示為 `半年度`
- [x] 2.2 執行 `npm run build` 與 `openspec validate --specs --changes` 通過
