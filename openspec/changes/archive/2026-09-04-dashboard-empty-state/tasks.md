## 1. 空狀態守衛與版面修復

- [x] 1.1 在 `frontend/src/views/DashboardView.vue` 新增 `hasYears = computed(() => availableYears.value.length > 0)`，並移除 `div.form-text`「尚無歷史年份」，改為年份 `<select>` 內 `<option v-if="!hasYears" disabled value="">尚無歷史年份</option>` ghost 占位，驗證空庫時 ghost 在下拉內且無 `div.form-text`、`row g-2` 不破版
- [x] 1.2 四選單禁用全灰：期別 `:disabled="!hasYears"`、年份 `:disabled="!hasYears || period==='HISTORICAL'"`、半年度 `:disabled="!hasYears || period!=='HALF_YEAR'"`、月份 `:disabled="!hasYears || period!=='MONTHLY'"`，驗證空庫四個 `select:disabled` 且 Bootstrap 灰化、有庫時依期別正常啟用
- [x] 1.3 空庫期別回退與不發錯參：`watch(period, p => { if (!hasYears.value && p!=='HISTORICAL') period.value='HISTORICAL' })` 與 `loadStats` 開頭 `if (!hasYears.value && period.value!=='HISTORICAL') { period.value='HISTORICAL'; return }`（ANNUAL/MONTHLY/HALF_YEAR 僅 `hasYears` 才帶 `year`），驗證空庫切 ANNUAL/MONTHLY/HALF_YEAR 自動回 HISTORICAL、Network 無 `GET /cases/statistics?period=ANNUAL` 400、`stats` 不為 null

## 2. 案件表單田區位置與送件人取消

- [x] 2.1 田區位置初隱：在 `frontend/src/views/CaseFormView.vue` 新增 `fieldLocationVisible = computed(() => editId !== null || form.senderId !== null)`，將「田區位置」卡 `div.card` 加 `v-if="fieldLocationVisible"`，驗證新增頁初進不顯示田區卡、`saveSender` 成功與 `applyCandidate`/`searchCandidates` 選用候選後顯示、編輯頁恆顯示且 `fieldSameAsSender` 同步正常
- [x] 2.2 送件人取消一鍵清空：新增模式送件人卡內「儲存送件人」旁新增 `v-if="!editId"` 的「取消」按鈕（`btn-outline-secondary`），綁 `resetSenderForm`（清空 `senderName/senderDisplayName/senderPhone/senderAddress`、`senderId=null`、`senderDistrictId/senderTypeId` 與 `selectedSenderCityId` 回 `cities[0]` 預設、`fieldDistrictId=null`/`selectedFieldCityId=null`/`fieldSameAsSender=false`/`lastFuzzyQuery=''`/`senderSnapshot` 重置），驗證點擊後輸入清空、田區卡再次隱藏、無 `Swal` 彈窗且不觸發 fuzzy 提示；保留既有 `form.senderId && senderDirty` 的「取消編輯」還原邏輯

## 3. 驗證

- [x] 3.1 手動回歸（空庫、新增、編輯）：Dashboard 空庫全灰、年份 ghost 在下拉內、無 400；案件新增時田區初隱→儲存/載入後顯示→取消後再次隱藏且無 alert；編輯頁田區常顯；診斷區塊 `diagnosisVisible` 與田區顯示一致
- [x] 3.2 建置與驗證：`cd frontend && npm run build` 通過且 `openspec validate --strict` 無新增錯誤，`mvn test` 相關 `PhytoTrackIntegrationTest#statistics*` 與案件/送件人測試綠（後端無變更，僅確認無回歸）
