## Why

案件表單多處體驗待優化：作物下拉在未選分類時仍可點擊易誤選；送件人電話候選過早觸發（1-2 碼即搜）造成無效請求與雜訊；候選以 popup 呈現與「取消沿用」位置分離、操作跳躍；且「沿用」用於建立新送件人語意矛盾。

## What Changes

- 作物類別未選時，作物下拉預設禁用（`disabled`），選擇分類後才啟用
- 送件人電話觸發候選門檻改為 4 碼以上才呼叫 `GET /api/senders/search`（既有 `displayName`/`name` 維持原門檻或同為 2 碼以上，電話單獨 4 碼）
- 候選呈現由 `Swal.fire` popup 改為送件人卡內 inline 下拉，位置與「取消沿用」提示同區域（inline 候選列表 + 操作按鈕）
- 候選操作語意：建立新送件人情境的按鈕由「沿用」改為「使用」（或「建立使用」），沿用既有送件人仍為「使用/沿用」二擇一但文案一致為「使用」

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `case-lifecycle`: 案件表單送件人候選觸發門檻與呈現方式、作物下拉啟用邏輯
- `sender-management`: 送件人候選查詢觸發條件（電話 4 碼門檻）與候選呈現（由 popup 改 inline）

## Impact

- 前端：`CaseFormView.vue`（作物 `select` 禁用綁定、`fuzzyFields` watch 門檻拆分、`searchCandidates`/`applyCandidate` 由 `Swal` 改 inline 響應式列表）、`senderApi.search` 呼叫時機
- 後端：無契約變更（搜尋 API 已支援部分比對，僅前端觸發時機改變）
- 文件：`docs/manual.typ` 案件表單操作說明更新
