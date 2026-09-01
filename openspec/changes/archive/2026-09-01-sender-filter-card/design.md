## Context

見 `proposal.md`。現況 `SendersView.vue:163` 為單一關鍵字搜尋卡片（`searchQ` → `GET /api/senders/search` 或 `list`），無身分別/縣市/鄉鎮市區篩選；表格 `SendersView.vue:188` 欄位為 `鄉鎮`。`SenderResponse` 已含 `senderTypeId/cityName/districtName`，`refApi.cities()` 與 `senderTypes` 已在 `loadRefs` 取得，可直接用於篩選選單。

## Goals / Non-Goals

**Goals:**
- 將搜尋卡片改為四欄篩選卡片（關鍵字、身分別、縣市、鄉鎮市區），縣市→鄉鎮市區聯動，前端本地過濾。
- 表頭 `鄉鎮` → `鄉鎮市區` 更名。

**Non-Goals:**
- 不新增後端篩選 API（`GET /api/senders` 仍全量，前端過濾；量大再獨立 change）。
- 不改表格其他欄位與編輯/刪除流程。
- 不改 `CaseFormView` 的送件人去重候選邏輯。

## Decisions

### D1. 前端本地過濾而非後端 API

`senderApi.list()` 回傳全量（目前 <1k 筆），前端以 `computed filteredSenders` 對 `senderTypeId/cityName/districtName` 與關鍵字作 AND 過濾。替代「後端 `GET /api/senders?senderTypeId&cityId&districtId`」需改 `SenderController/SenderService/CaseSpecifications` 類似 `case-search` 的 `Specification`，成本高且本次量小不需。

### D2. 縣市→鄉鎮市區聯動

`selectedCityId` 為空時 `selectedDistrictId` disabled 且重置；切縣市時重置鄉鎮。選項來自 `refApi.cities()` 的 `CityResponse.districts`，與編輯彈窗的 `city→district` 聯動一致（`SendersView.vue:117`）。替代「獨立 `District` 下拉」會失去縣市約束。

### D3. 欄位更名僅前端表頭

`鄉鎮` → `鄉鎮市區` 僅改 `SendersView.vue:188` `<th>` 文字，不改 API/DB 欄位名（`districtName` 維持）。替代「後端更名」無必要。

## Risks / Trade-offs

- [資料量增大前端過濾效能] → 目前量小可接受，超過 5k 再改後端篩選（與 `case-search` 的 `v_case_search` 類似）。
- [縣市未選時鄉鎮殘留] → 切縣市時重置 `districtId`，`computed` 中以 `!cityId ? []` 防呆。

## Migration Plan

- 前端僅改 `SendersView.vue`，無遷移；`npm run build` 與 `npm test` 驗證，`openspec validate` 通過。

## Open Questions

- 無。
