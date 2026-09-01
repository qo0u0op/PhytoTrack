## Why

送件人管理頁現行僅提供單一關鍵字搜尋（姓名/電話/顯示名稱經 `GET /api/senders/search?q=`），無法依身分別、縣市、鄉鎮市區精準篩選，站務人員需逐頁查找特定身分別或轄區的送件人，效率低。同時表格欄位 `鄉鎮` 用詞與 `district` 模型及全站 `縣市/鄉鎮市區` 用語不一致，需統一為 `鄉鎮市區`。

## What Changes

- 將 `SendersView.vue:163` 的「搜尋卡片」改為「篩選卡片」，提供四欄篩選：關鍵字（姓名/電話/顯示名稱）、身分別（`senderTypeId`）、縣市（`cityId`）、鄉鎮市區（`districtId`，依縣市聯動），多條件以 AND 組合，前端本地篩選（資料量小，沿用既有 `senderApi.list()` 後前端過濾；若未來後端提供篩選 API 再遷移）。
- 表格欄位 `鄉鎮` 更名為 `鄉鎮市區`（`SendersView.vue:188` 表頭），與 `CaseDetailView`/`Senders` 模型用語一致。
- 保留原有關鍵字搜尋輸入框與清除按鈕行為（清除時重置四欄），分頁/載入狀態不變。
- 不新增後端篩選 API（`GET /api/senders` 仍全量回傳，前端過濾即可；量大再以獨立 change 補後端篩選）。

## Capabilities

### New Capabilities
- 無

### Modified Capabilities
- `sender-management`: 送件人管理頁改為篩選卡片（身分別/縣市/鄉鎮市區 + 關鍵字），欄位更名鄉鎮→鄉鎮市區。

## Impact

- 前端：`frontend/src/views/SendersView.vue:160` 篩選卡片版型、`frontend/src/api/index.ts` 無需改動（仍用 `senderApi.list/search`，篩選為前端本地）；`frontend/src/utils/escapeHtml.ts` 不涉及。
- 後端：無（本次不改 `SenderController`/`SenderService`，若需後端篩選另開 change）。
- 文件：`docs/manual.typ` 送件人管理段落視需要補篩選說明。
