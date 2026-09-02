## 1. 篩選抽屜

- [x] 1.1 將 `CasesView.vue` 與 `SendersView.vue` 的篩選卡片改為抽屜式（預設收合，按鈕展開/收合），並驗證 `npm run build` 通過
- [x] 1.2 調整抽屜按鈕樣式與無障礙（`aria-expanded`），並驗證窄螢幕下篩選不佔位

## 2. 表格排序

- [x] 2.1 為兩表格的所有非操作欄加入可排序表頭（`asc/desc` 切換，箭頭指示），前端本地排序，並驗證 `npm run build` 通過且點擊排序正確
- [x] 2.2 設定預設排序（案件依收件日期降冪、送件人依 ID 降冪），並驗證初始載入順序正確

## 3. 驗收

- [x] 3.1 執行 `npm run build`、`npm test` 與 `openspec validate --specs --changes` 通過
