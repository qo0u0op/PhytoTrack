## ADDED Requirements

### Requirement: 新增案件田區位置初隱

新增案件（非編輯）時「田區位置」卡 SHALL 預設隱藏，僅在送件人已儲存或已載入（`form.senderId !== null`）後顯示；編輯既有案件時該卡 SHALL 一直可見。隱藏期間 SHALL 不阻擋送件人區塊操作，且案件提交時若田區位置仍未顯示則視為未選（與既有必填驗證一致）。

#### Scenario: 新增時初隱
- **WHEN** 使用者進入案件新增頁且尚未儲存/載入送件人
- **THEN** 不顯示「田區位置」卡

#### Scenario: 儲存送件人後顯示
- **WHEN** 於新增頁點擊「儲存送件人」成功（`POST /api/senders` 回 200 且 `form.senderId` 被賦值）
- **THEN** 顯示「田區位置」卡，可選縣市/鄉鎮市區與「和送件人相同」

#### Scenario: 載入候選後顯示
- **WHEN** 於新增頁透過模糊提示或「搜尋候選」選用既有送件人
- **THEN** 顯示「田區位置」卡，且「和送件人相同」勾選時同步送件人縣市鄉鎮

#### Scenario: 編輯時常顯
- **WHEN** 進入案件編輯頁（`editId !== null`）
- **THEN** 無論 `senderId` 狀態皆顯示「田區位置」卡

### Requirement: 送件人取消一鍵清空

新增案件時送件人區塊的「儲存送件人」旁 SHALL 常駐「取消」按鈕；點擊 SHALL 直接清空送件人輸入（`senderName/senderDisplayName/senderPhone/senderAddress` 置空、`senderDistrictId/senderTypeId` 與縣市選取回預設、`senderId` 置 `null`、`fieldDistrictId` 與「和送件人相同」重置），且 SHALL 不彈任何 `alert`/`Swal` 提示，亦不觸發模糊搜尋提示。已選用既有送件人的「取消編輯」（還原快照）按鈕 SHALL 保留，僅在 `form.senderId !== null && senderDirty` 時顯示，二者職責分離。

#### Scenario: 一鍵清空
- **WHEN** 於新增頁在送件人欄位輸入任意值後點擊「取消」
- **THEN** 送件人四個文字欄位被清空，縣市/鄉鎮市區與身分別回到初始預設，`senderId` 為 `null`，且田區位置卡再次隱藏（因 `senderId` 已空）

#### Scenario: 無提示
- **WHEN** 點擊新增模式的「取消」
- **THEN** 不出現任何 `Swal`/`alert`/`confirm` 彈窗

#### Scenario: 不觸發模糊提示
- **WHEN** 點擊「取消」後
- **THEN** 不立即彈出「有相似的資料，是否帶入?」提示

#### Scenario: 編輯模式不顯示此取消
- **WHEN** 進入案件編輯頁
- **THEN** 不顯示新增模式的「取消」按鈕（僅依既有邏輯顯示「取消編輯」）
