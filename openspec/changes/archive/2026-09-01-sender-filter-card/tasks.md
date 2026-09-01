## 1. 篩選卡片與表格更名

- [x] 1.1 將 `frontend/src/views/SendersView.vue:163` 的搜尋卡片改為四欄篩選卡片（關鍵字、身分別、縣市、鄉鎮市區），身分別選項來自 `refApi.senderTypes()`，縣市/鄉鎮市區來自 `refApi.cities()` 且鄉鎮市區依縣市聯動（縣市未選時 disabled），並驗證 `npm run build` 通過
- [x] 1.2 新增 `computed filteredSenders` 前端本地過濾（關鍵字對 `name/displayName/phone` 部分比對 + `senderTypeId/cityName/districtName` 精確比對，AND 組合），列表改為 `v-for="s in filteredSenders"`，清除按鈕重置四欄，並驗證手動篩選身分別/縣市/鄉鎮市區與關鍵字組合正確
- [x] 1.3 將表格表頭 `鄉鎮` 更名為 `鄉鎮市區`（`SendersView.vue:188`），並驗證 `npm run build` 與既有表格渲染正常

## 2. 驗收

- [x] 2.1 執行 `npm run build` 與 `npm test`（`vitest`）驗證無迴歸，並執行 `openspec validate --specs --changes` 通過
