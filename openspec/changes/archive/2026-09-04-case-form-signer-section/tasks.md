## 1. 簽名人區塊獨立

- [x] 1.1 `CaseFormView.vue` 診斷簽名人區塊改 `col-12` 獨立列（防治建議維持其上），驗證 `npm run dev` 開啟表單兩區塊上下分隔、勾選與新增行為不變

## 2. 驗證與回歸

- [x] 2.1 執行 `cd frontend && npm run build`（含 `vue-tsc`），驗證建置通過
- [x] 2.2 執行 `openspec validate --specs --changes --strict`，驗證無錯誤
