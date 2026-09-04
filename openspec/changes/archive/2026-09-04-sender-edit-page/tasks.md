## 1. 路由與頁面

- [x] 1.1 建立 `frontend/src/views/SenderEditView.vue`，版面與 `CaseFormView.vue` 送件人資料卡片一致（`card-header bg-success`、姓名/顯示名稱/電話/地址選填/縣市/鄉鎮連動/身分別、儲存/更新/取消編輯按鈕），以 `senderApi.detail/update` 載入與提交並提供 inline 驗證，驗證 `npm run build` 通過
- [x] 1.2 於 `frontend/src/router/index.ts` 新增 `/admin/senders/:id/edit` 路由並設定權限（`requiresAuth`、`roles: ['STAFF','ADMIN']`），驗證直接訪問與未授權導向行為

## 2. 列表整合與保持狀態

- [x] 2.1 重構 `frontend/src/views/SendersView.vue` 的 `handleEdit` 為 `router.push` 攜帶當前篩選/分頁/排序狀態（`q/senderTypeId/cityId/districtId/page/size/sort` via query 或 history state）導向編輯頁，移除 `Swal` 編輯 popup，驗證點擊編輯導向正確且參數攜帶完整
- [x] 2.2 於 `SenderEditView.vue` 實作上一筆/下一筆導航（依列表篩選與排序結果的 ID 序列計算 `prevId/nextId`，邊界禁用，導航時保持 query 狀態），並實作返回列表時恢復原篩選/分頁/排序，驗證導航與返回均保持狀態不重置

## 3. 驗證與回歸

- [x] 3.1 執行 `npm run build` 與 `openspec validate --specs --changes --strict`，驗證無錯誤
- [x] 3.2 執行 `mvn test` 相關送件人測試，驗證後端契約未破壞
