## Context

`frontend/src/views/DashboardView.vue:14` 當前以 `availableYears`（`stats.availableYears ?? []`）與 `selectedYear: null` 驅動期別篩選；`loadStats:24` 在 `ANNUAL/MONTHLY/HALF_YEAR` 時僅 `if (selectedYear.value) params.year=`，但空庫仍允許可切期別，`watch([period,...])` 立即觸發 `loadStats` 送 `GET /cases/statistics?period=ANNUAL`（缺 `year`）命中 `CaseService.statistics:644` 拋 `VALIDATION_ERROR` 400，攔截器彈錯。樣版 `91` 年份 `select` 僅 `period==='HISTORICAL'` 禁用，`94` 的 `div.form-text` 在 `row` 外破版；`98/104` 的 `HALF_YEAR/MONTHLY` 僅看 `period`。`CaseService` 對 `HISTORICAL` 已正確支援空庫 `total=0`，無需後端變更。

`frontend/src/views/CaseFormView.vue:802`「田區位置」卡常駐顯示，新增模式下使用者在送件人未儲存/未載入前即可選擇田區，與「送件人確定後才填寫診斷」流程不一致；送件人區塊僅有 `儲存送件人`（`!form.senderId`）與 `更新/取消編輯`（`form.senderId && senderDirty`），新增模式無一鍵清空，殘留 `fuzzy` 查詢與 `senderId` 快照易誤提交。`diagnosisVisible:205` 已以 `form.senderId !== null && !senderDirty` 控制診斷區塊，田區位置應同此訊號。

參見 `proposal.md` Why / `specs/case-statistics/spec.md` 空狀態行為與 `specs/case-lifecycle/spec.md` 新增互動。

## Goals / Non-Goals

**Goals:**
- 空庫時四個期別下拉全禁用、視覺灰化不可互動，且年份以 `<select>` 內 ghost `option` 提示；空庫切非歷史期別不發缺參請求、不彈 400，強制停留 `HISTORICAL`；有資料時行為不變。
- 新增案件時田區位置初隱，僅送件人已儲存/已載入（`form.senderId !== null`）後顯示；編輯模式維持可見。
- 新增案件時儲存送件人旁常駐「取消」按鈕，一鍵清空送件人輸入、重置選單與田區狀態，無任何 `Swal`/`alert`。

**Non-Goals:**
- 後端統計契約或 `availableYears` 產生邏輯變更；案件/送件人提交契約變更。
- 分頁/篩選等其他空狀態；引入新依賴或圖表庫。

## Decisions

- **Decision: `hasYears = computed(() => availableYears.length > 0)` 作為單一空庫訊號**
  - Rationale: `availableYears` 為後端真相源；單一計算避免多處 `length===0` 分歧。
  - Alternative: 以 `totalCases===0` 判斷 → 不可靠（期別過濾後 `periodTotal=0` 但仍有歷史年份）。

- **Decision: `disabled` 綁定改為 `!hasYears || period==='…'` 四選單統一**
  - 具體：`period` → `:disabled="!hasYears"`（空庫連期別亦鎖）；`selectedYear` → `:disabled="!hasYears || period==='HISTORICAL'"`；`selectedHalf` → `:disabled="!hasYears || period!=='HALF_YEAR'"`；`selectedMonth` → 同理。`class` 由 Bootstrap `disabled` 自動灰化，無需額外樣式。
  - Alternative: 僅禁用年份 → 期別仍可切，會再觸發錯誤請求。

- **Decision: `watch(period)` + `loadStats` 雙守衛回退**
  - `watch(period, p => { if (!hasYears && p!=='HISTORICAL') period.value='HISTORICAL' })` 阻斷使用者操作；`loadStats` 開頭 `if (!hasYears && period!=='HISTORICAL') { period.value='HISTORICAL'; return }` 阻斷程式化/競態觸發。二者冪等，不額外發請求。
  - Alternative: 僅在樣版禁用期別 → 鍵盤/程式仍可改值，仍會送錯參。

- **Decision: ghost 以 `<option disabled value="">尚無歷史年份</option>` 置於 `<select>` 內，刪除 `div.form-text`**
  - Rationale: 維持 `row g-2` 內聯排版，空庫不增行；`v-if="!hasYears"` 保證有資料時無空選項，符合既有「年份不含空選項」規格。
  - Alternative: `placeholder` + CSS → `<select>` 無 `placeholder` 原生支援。

- **Decision: `fieldLocationVisible = computed(() => editId !== null || form.senderId !== null)` 控制田區卡 `v-if`**
  - Rationale: 復用既有 `form.senderId` 真值（`saveSender` 成功或 `applyCandidate`/`searchCandidates` 載入後由 `senderSnapshot` 同步賦值），新增時初隱、提交驗證與 `diagnosisVisible` 一致；編輯時 `editId` 非空恆可見，不影響既有編輯流程與 `fieldSameAsSender` 同步。隱藏時 `filteredFieldDistricts` 與 `watch(selectedFieldCityId)` 不需額外守衛。
  - Alternative: 以 `senderDirty` 反向控制 → 會在輸入中途閃爍顯示/隱藏。

- **Decision: 新增模式「取消」按鈕以 `resetSenderForm()` 一鍵清空，無 `Swal`**
  - 實作：`function resetSenderForm() { form.senderId=null; form.senderName=''; form.senderDisplayName=''; form.senderPhone=''; form.senderAddress=''; form.senderDistrictId=cities[0].districts[0].id ?? 0; form.senderTypeId=senderTypes[0].id ?? 0; selectedSenderCityId=cities[0].id; form.fieldDistrictId=null; selectedFieldCityId=null; fieldSameAsSender=false; lastFuzzyQuery=''; senderSnapshot=snapshotSender(); }` 按鈕樣式 `btn-outline-secondary` 與 `儲存送件人` 同列 `v-if="!editId"` 常駐，清除後不觸發 `watch(fuzzyFields)`（因 `lastFuzzyQuery` 已重置且值為空 `<2`）。現有 `cancelSenderEdit`（`form.senderId && senderDirty`）保留用於已選用送件人的還原，二者職責分離。
  - Alternative: 復用 `cancelSenderEdit` → 僅還原快照非清空，不符「直接清空輸入狀態」預期；彈 `Swal` 確認 → 違反「不要 alert」要求。

## Risks / Trade-offs

- [Risk] `watch` 回退觸發二次 `loadStats`（period 變更再進 watch）→ Mitigation: `loadStats` 早退 + `period` 賦同值不觸發（Vue 對同值不通知），實測空庫僅一次 HISTORICAL 請求。
- [Risk] 使用者誤解「期別為何不能切」→ Mitigation: 年份 ghost 已提示「尚無歷史年份」；期別 disabled 的 `title` 可選加 `title="尚無資料"`，不影響規格但提升可理解性。
- [Risk] 未來 `availableYears` 改為分頁/懶載 → Mitigation: `hasYears` 仍以首頁 `availableYears` 為準，空庫判定不受影響。
- [Risk] 田區卡 `v-if` 隱藏時 `form.fieldDistrictId` 仍殘留舊值致提交誤帶 → Mitigation: `resetSenderForm` 同步清空 `fieldDistrictId`；`submit` 驗證以 `effectiveForValidate` 為準，隱藏時必填檢查仍在儲存案件時生效，不在送件人階段誤攔。
- [Risk] 一鍵清空誤觸 → Mitigation: 按鈕文案「取消」且位於送件人卡內，僅影響送件人區塊，不觸發離開頁面或案件儲存；編輯模式不顯示此按鈕，避免誤清已載入案件。

## Migration Plan

- 部署：僅前端 `DashboardView.vue` + `CaseFormView.vue` 變更，後端不需遷移；灰度直接覆蓋。
- 回滾：還原二檔即可，後端無狀態依賴。
- 驗證：Dashboard 空庫全灰 + ghost 在下拉內 + Network 無 400；案件新增時田區位置初隱、儲存/載入送件人後顯示、取消一鍵清空無彈窗；編輯模式田區常顯；`npm run build`、`mvn test` 相關統計與案件測試綠。

## Open Questions

- 無。
