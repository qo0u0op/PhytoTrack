## Context

見 proposal.md。現況 `SendersView.vue` 以 `Swal.fire` popup 編輯，欄位受限、連動邏輯需手動 DOM 操作，錯誤僅 `showValidationMessage`。`CaseFormView.vue` 已有完整的「送件人資料」卡片（`card-header bg-success`、姓名/顯示名稱/電話/身分別/縣市/鄉鎮/地址、儲存/更新/取消編輯按鈕、縣市連動、地址選填、inline 驗證）。後端 `GET /api/senders/:id` 與 `PUT /api/senders/:id` 已完備，無需新 API。列表具備關鍵字/身分別/縣市/鄉鎮四欄篩選、本地排序（`sortStates`）、分頁（`page/size`）。

## Goals / Non-Goals

**Goals:**
- 送件人編輯改獨立頁面，版面與 `CaseFormView.vue` 送件人卡片一致，縣市/鄉鎮連動與地址選填邏輯複用
- 編輯頁內上一筆/下一筆導航保持列表篩選/分頁/排序狀態，返回列表亦恢復狀態
- 路由與權限與現有 SPA 一致

**Non-Goals:**
- 新增送件人改頁面（仍由案件表單或保留列表入口，依現況）
- 後端契約變更
- 跨頁持久化（如 localStorage）篩選狀態，僅當次會話往返保持

## Decisions

- 版面複用：抽共用組件 `SenderFormCard.vue` 或直接複製 `CaseFormView.vue` 送件人卡片模板與樣式（`card shadow-sm`、`card-header bg-success`、`row g-3`、欄位 `col-md-3/4` 配置、地址選填、縣市/鄉鎮連動 `selectedCityId` → `districtId`），表單狀態 `reactive` + `v-model.trim`，驗證沿用 `CaseFormView.vue` 的 `senderPhone/displayName` 至少一項與 `senderDistrictId/senderTypeId` 必填邏輯
- 資料來源：編輯頁 `onMounted` 以 `route.params.id` 呼叫 `senderApi.detail` 載入，失敗導回列表並提示；提交以 `senderApi.update`
- 狀態傳遞：列表 `handleEdit` 以 `router.push({ name: 'sender-edit', params: { id }, query: { q, senderTypeId, cityId, districtId, page, size, sort } })` 或 `history.state` 攜帶當前 `appliedQ/appliedSenderTypeId/appliedCityId/appliedDistrictId/page/size/sortStates` 序列化（`sort` 如 `field,asc;field2,desc`）；編輯頁解析 query 還原篩選上下文，重建 `filtered/sorted` ID 序列以計算 `prevId/nextId`，導航按鈕 `router.replace` 切換 `params.id` 而 query 保持不變
- 上一筆/下一筆：編輯頁計算 `orderedIds = sortedSendersIds`（基於列表篩選與排序後的 ID 序列，必要時以 `GET /api/senders` 全量在前端重建或由列表傳入 ID 列表），`currentIndex = orderedIds.indexOf(currentId)`，`prev/next` 禁用於邊界；返回列表以 `router.push({ name: 'senders', query: preservedQuery })` 恢復狀態，避免重置
- 移除 popup：`SendersView.vue` 的 `handleEdit` 改為 `router.push` 導航，刪除 `Swal` 編輯相關程式；保留搜尋/分頁/刪除

## Risks / Trade-offs

- 列表到編輯頁往返：以 query 攜帶狀態優於僅 `history.back`（可分享 URL、重新整理不丟失）；query 過長時改 `sessionStorage` 備援
- ID 序列重建成本：前端全量過濾與排序已在 `SendersView.vue` 實現，編輯頁複用相同邏輯或直接接收序列化 ID 列表，避免二次請求
- 權限：未授權直接輸入 URL 由路由守衛與後端 403 雙重保障
- 版面一致性：共用組件優於複製，若採複製需同步後續 `CaseFormView.vue` 送件人卡片變更

## Migration Plan

- 前端路由新增，無遷移；舊 popup 程式刪除即完成，rollback 為還原該段；編輯頁共用卡片樣式，後續案件表單送件人卡片調整時同步更新
