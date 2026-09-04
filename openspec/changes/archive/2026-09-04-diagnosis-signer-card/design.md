## Context

見 proposal.md。現況 `CaseFormView.vue` 診斷簽名人與防治建議同在「診斷結果與建議」卡內並排，`diagnosisVisible` 控制整體顯示，簽名人常駐且可預設勾選；防治建議以直式 `form-check` 清單呈現。

## Goals / Non-Goals

**Goals:**
- 診斷簽名人獨立卡片，依診斷編輯狀態條件顯示，預設不選
- 防治建議改橫式，與被害部位一致

**Non-Goals:**
- 後端 `identifierIds` 驗證變更
- 診斷結果本身的編輯邏輯變更

## Decisions

- 顯示條件：`signerCardVisible = computed(() => diagnosisEdited.value)`，其中 `diagnosisEdited = pestRowsDirty || hintIdsDirty || hintDescriptionDirty`（與初始值比對或以 `length>0`/非空判斷）；`diagnosisVisible` 仍控制整體診斷卡，簽名人卡額外以 `v-if="signerCardVisible"` 獨立
- 預設不勾選：新增案件時 `form.identifierIds = []`，移除既有預設帶入（`loadRefs` 中的 `myIdentifier` 自動勾選）或改為僅在 `signerCardVisible` 後才提示；編輯時若原無簽名人亦保持空，僅診斷有變更才允許勾選
- 卡片獨立：將原表格抽為單獨 `<div class="card shadow-sm mb-4" v-if="signerCardVisible">`，標題「診斷簽名人」
- 橫式：將防治建議的 `v-for` 容器由直式改為 `<div class="d-flex flex-wrap gap-3">` 包 `form-check-inline`，與被害部位同樣式

## Risks / Trade-offs

- 既有案件編輯時若未改診斷但需改簽名人：需先觸發診斷編輯才顯示，符合「無診斷不簽名」原則；若需例外可手動加微小診斷變更
- 預設不選可能遺漏簽名：提交時若 `signerCardVisible` 且 `identifierIds` 為空可提示「請選擇簽名人」但不強制阻擋（由後端允許空）

## Migration Plan

- 僅前端，無遷移；舊版面還原即 rollback
