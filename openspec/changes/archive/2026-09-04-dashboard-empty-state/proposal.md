## Why

空庫（`availableYears` 為空）時 Dashboard 期別篩選器仍允許可切 `ANNUAL/HALF_YEAR/MONTHLY`，`loadStats` 以缺 `year` 呼叫 `GET /cases/statistics?period=ANNUAL` 觸發後端 `VALIDATION_ERROR`（400）致全局錯誤彈窗；同時「尚無歷史年份」以 `div.form-text` 呈現於 `<select>` 下方破壞卡片排版，未達成「全灰禁用、ghost 提示在下拉內」的預期。

新增案件時田區位置與送件人區塊常駐，導致送件人未確定前即可誤選田區；送件人輸入僅有「儲存送件人」而無一鍵清空，易殘留模糊查詢狀態且需手動刪除各欄。需將田區位置改為送件人確定後才顯示，並在儲存旁提供無提示的取消清空。

## What Changes

- **前端空狀態守衛**：`DashboardView.vue` 新增 `hasYears` 判定；當 `availableYears.length === 0` 時：(1) 期別與年/半年度/月份四個 `<select>` 全 `disabled`（灰化），(2) 強制 `period` 停留 `HISTORICAL`，切換至非歷史期別自動回退且不發請求，(3) `loadStats` 在非歷史期別且無年份時早退避免送 `year=null` 請求。
- **歷史年份 ghost**：移除 `div.form-text`「尚無歷史年份」；改為 `<select>` 內 `disabled` 空 `option` 占位（ghost），不破壞 `row g-2` 版面；有年份時無空選項，預設最新年。
- **田區位置初隱**：`CaseFormView.vue` 新增案件（`editId === null`）時「田區位置」卡預設隱藏（`v-if="fieldLocationVisible"`），僅在 `saveSender` 成功或 `applyCandidate` / `searchCandidates` 載入既有送件人（`form.senderId !== null`）後顯示；編輯模式（`editId !== null`）維持可見。`fieldSameAsSender` 邏輯與提交驗證在隱藏時不生效。
- **送件人取消清空**：新增案件時「儲存送件人」按鈕旁新增「取消」按鈕（`v-if="!editId"` 常駐，`!form.senderId` 時亦可見），點擊直接清空送件人輸入（`senderName/senderDisplayName/senderPhone/senderAddress` 置空、`senderDistrictId/senderTypeId` 與 `selectedSenderCityId` 回預設、必要時 `fieldDistrictId/selectedFieldCityId/fieldSameAsSender` 同步重置、`senderId=null`、`lastFuzzyQuery=''`），不彈 `Swal`/`alert`，不觸發 fuzzy 搜尋。
- **無後端契約變更**：`GET /cases/statistics` 維持現有驗證；案件送件人/田區位置欄位與提交契約不變。

## Capabilities

### New Capabilities
<!-- 無新增能力 -->

### Modified Capabilities
- `case-statistics`: Dashboard 統計視圖空狀態行為（期別篩選禁用與 ghost 占位、空庫不觸發驗證錯誤）
- `case-lifecycle`: 案件表單送件人/田區位置互動（新增時田區位置初隱、送件人取消一鍵清空無提示）

## Impact

- 前端：`frontend/src/views/DashboardView.vue`（`availableYears/hasYears`、`loadStats` 守衛、`watch(period)` 回退、`template` 四選單 `disabled` 與 ghost `<option>`）；`frontend/src/views/CaseFormView.vue`（`fieldLocationVisible`、`saveSender`/`applyCandidate`/`searchCandidates` 後顯示田區、新增模式取消按鈕 `resetSenderForm`、同步 `diagnosisVisible`）
- 後端：無變更（`CaseService.statistics` 已支援 `HISTORICAL` 空庫 0/空清單；案件建立/更新契約不變）
- 驗證：空庫與案件表單手動驗證 + `npm run build` + `mvn test` 相關統計與案件測試
