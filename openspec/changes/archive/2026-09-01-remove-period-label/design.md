## Context

見 `proposal.md`。現況 `DashboardView.vue:102` 仍有 `<div class="small text-muted">期間：{{ periodLabel(...) }}</div>`，與期別下拉及 `期別案件數` 重複；`DashboardView.vue:139` 已於 `6cacd3b` 改為 `模型狀態` 但主規格仍為 `AI 連線情況`。

## Goals / Non-Goals

**Goals:**
- 移除期別小字，同步主規格標題為 `模型狀態`。

**Non-Goals:**
- 不改其他 Dashboard 版式與統計邏輯。

## Decisions

### D1. 直接刪除小字 div

刪除 `DashboardView.vue:102` 整行，期別已由下拉與 `total()` 涵蓋，無需替代顯示。

### D2. 規格收斂標題

主規格 `case-statistics` 的 Dashboard 需求改為 `模型狀態`，與程式碼 `6cacd3b` 一致。

### D3. 百分比一位小數

`percent` 改為 `Math.round((count/total)*1000)/10` 或 `((count/total)*100).toFixed(1)`，模板 `{{ percent(c.count) }}%` 顯示一位小數。進度條 `width: percent(...)%` 亦用同值。替代「整數 `Math.round`」精度不足。

## Risks / Trade-offs

- [期間資訊遺失] → 下拉與期別案件數已足，無風險。

## Migration Plan

- 前端刪除一行，`npm run build` 驗證。

## Open Questions

- 無。
