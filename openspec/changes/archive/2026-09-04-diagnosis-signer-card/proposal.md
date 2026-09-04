## Why

案件表單的診斷簽名人目前與防治建議並列且常駐顯示，無論是否已有診斷即預設可勾選，不符合「先建檔、後診斷」流程。另防治建議以直式清單呈現，與被害部位的橫式多選不一致，版面利用與掃視效率較差。

## What Changes

- 診斷簽名人改為獨立卡片，且僅在診斷結果或防治建議有編輯時才顯示；預設不勾選任何簽名人，除非診斷內容有變更才允許/提示勾選
- 新增/編輯案件時，防治建議呈現由直式改為橫式排列，參考被害部位的 `flex-wrap` 多選樣式

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `case-lifecycle`: 案件表單診斷簽名人顯示條件與卡片獨立化、防治建議版面由直式改橫式

## Impact

- 前端：`CaseFormView.vue` 診斷簽名人區塊抽為獨立 `card` 並以 `computed` 控制顯示（依 `pestRows`/`hintIds`/`hintDescription` dirty 判斷），預設 `identifierIds` 清空；防治建議 `v-for` 樣式由 `form-check` 直式改為 `flex-wrap gap-3` 橫式
- 後端：無契約變更（`identifierIds` 可空，空陣列代表未指定簽名人）
- 文件：`docs/manual.typ` 案件表單說明更新
