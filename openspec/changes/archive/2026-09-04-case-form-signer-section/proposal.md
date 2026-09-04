## Why

案件新增／編輯表單中「防治建議（可複選）」與「診斷簽名人（可複選）」為左右併排的 `col-md-6` 且無視覺區隔，兩組 checkbox 在視覺上貼在一起，易誤認為同一群組。將診斷簽名人獨立為全寬區塊，與防治建議上下隔開。

## What Changes

- `CaseFormView.vue` 診斷簽名人區塊改為全寬獨立列（`col-12`，含上方間距），與防治建議上下排列；其餘欄位順序與行為不變。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `case-lifecycle`: 案件表單診斷簽名人區塊版面（獨立全寬列）

## Impact

- 前端：`CaseFormView.vue` 模板僅此一處；無行為、無 API、無資料變更。
