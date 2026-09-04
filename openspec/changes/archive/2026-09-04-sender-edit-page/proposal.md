## Why

送件人管理目前以 SweetAlert popup 進行編輯，表單欄位擁擠、無法支援縣市/鄉鎮連動、地址等複雜驗證與錯誤提示，且與案件表單的送件人 inline 編輯體驗不一致。改為獨立編輯頁面可提供完整表單版面、一致的驗證與 API 錯誤處理，並利於後續擴充。編輯頁面採用與案件編輯的「送件人資料」卡片相同版面與互動，並在編輯頁內支援上一筆/下一筆導航且保持列表的篩選、分頁與排序狀態，減少往返成本。

## What Changes

- 送件人管理列表的「編輯」操作由 popup 改為導向獨立編輯頁面（`GET /senders/:id` 取得資料，`PUT /senders/:id` 提交），路由如 `/admin/senders/:id/edit` 或 `/senders/:id/edit`。
- 編輯頁面版面與 `CaseFormView.vue` 的「送件人資料」卡片一致：`card` + `card-header bg-success`、欄位包含姓名、顯示名稱、電話、地址（選填）、縣市/鄉鎮連動、身分別，支援 inline 驗證與 `400/409` 錯誤映射，操作按鈕（儲存/更新/取消編輯）樣式與文案一致。
- 編輯頁面支援上一筆/下一筆導航：以列表當下的篩選結果（關鍵字/身分別/縣市/鄉鎮）、分頁（page/size）與排序狀態（sortStates）決定可導航的 ID 序列，導航時保持狀態不重置；返回列表時亦恢復原篩選/分頁/排序。
- 移除既有 `Swal.fire` 編輯 popup 邏輯，列表保留搜尋/分頁/刪除功能不變；列表點擊編輯時將當前篩選/分頁/排序以 query 或 history state 傳遞給編輯頁。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `sender-management`: 送件人編輯互動由 popup 改為獨立頁面，表單版面與案件編輯的送件人資料卡片一致，並支援保持篩選/分頁/排序的上一筆/下一筆導航；表單驗證與 API 契約維持不變，縣市/鄉鎮連動與地址選填邏輯與案件表單一致

## Impact

- 前端：`SendersView.vue` 移除 `handleEdit` popup，改為 `router.push` 並攜帶當前篩選/分頁/排序狀態；新增 `SenderEditView.vue`（複用 `CaseFormView.vue` 送件人卡片版面與 `senderApi.detail/update`）與 `router/index.ts` 新路由，編輯頁內實作上一筆/下一筆導航與返回列表狀態恢復；`api/index.ts` 既有 API 複用
- 後端：無契約變更（`GET /senders/:id`、`PUT /senders/:id` 已有，僅前端交互改動）
- 文件：`docs/manual.typ` 送件人管理操作說明更新
