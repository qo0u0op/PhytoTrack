## Context

見 proposal.md。現況 `CaseFormView.vue` 作物下拉在 `selectedCropCategoryId === null` 時仍啟用顯示全量；`fuzzyFields` watch 對電話無字數門檻；`searchCandidates` 以 `Swal` popup 呈現，`applyCandidate` 以 select popup 選擇，與「取消沿用」alert 區域分離；按鈕文案「沿用」用於新建語意不當。

## Goals / Non-Goals

**Goals:**
- 作物類別未選時禁用，避免誤選；電話 4 碼門檻降低無效搜尋；候選 inline 化與位置統一；文案改「使用」

**Non-Goals:**
- 後端搜尋邏輯變更
- 其他欄位門檻變更

## Decisions

- 作物禁用：`computed` 判斷或直接 `:disabled="!selectedCropCategoryId"` 綁定於作物 `select`，`placeholder` 顯示「請先選擇分類」
- 電話門檻：`watch(fuzzyFields)` 或 `searchCandidates` 內對 `form.senderPhone.trim().length >= 4` 判斷，未達門檻早退；`displayName/name` 維持既有門檻（2 碼）
- Inline 候選：`SendersView` 已有 inline 候選模式可參考；在 `CaseFormView.vue` 送件人卡內新增響應式 `candidateList` + `showCandidate`，`searchCandidates` 賦值後 `v-if` 顯示下拉於 `v-if="form.senderId"` 提示同區域；選擇後呼叫 `applyCandidate` 並收合
- 文案：按鈕由「沿用」改為「使用」，共用於選擇既有與建立新送件人（新建時為「使用新送件人」或「使用」）

## Risks / Trade-offs

- 電話 4 碼門檻可能延遲候選出現：可接受，減少噪音；短號仍可手動按「搜尋候選」觸發
- Inline 下拉遮擋：置於卡片底部，`max-height` + `overflow-auto`

## Migration Plan

- 僅前端，無遷移；舊 popup 程式移除，rollback 為還原

