## Context

`CaseDetailView.vue:260` 以 `formatTime = v=>v.replace('T',' ').slice(0,19)` 顯示，`CasesView.vue:477` 預覽以 `esc(data.createdAt)` 直接輸出原始 ISO，兩處時間格式與標籤（`建立` vs `建立時間`）分歧。預覽的 `Swal` HTML 為字串模板，需共用同一格式化函式；後端 `CaseResponse` 僅有 `createdByName`，無 `updatedByName`。

## Goals / Non-Goals

**Goals:**
- 預覽與檢視時間格式完全一致（`formatTime`）
- 中繼資料由單行三段改為兩行，標籤改 `編輯者：／更新：`
- Dashboard `progress-bar` 三色語意不變、僅色調微調至 amber/emerald/slate，提升對比

**Non-Goals:**
- 不新增後端 `updatedBy` 欄位為強制（以前端回落為預設，保持零遷移）
- 不改案件列表或 CSV 的 `createdAt/updatedAt` 顯示

## Decisions

- **共用 `formatTime`**：預覽頁由 `esc(data.createdAt)` 改為 `formatTime(data.createdAt)`，與檢視頁同一函式（`replace('T',' ').slice(0,19)`）。替代：後端改回傳已格式化字串 → 破壞 `LocalDateTime` 契約 → 捨棄。
- **兩行版面**：檢視頁 `<div class="col-12 text-muted">` 內以 `<br>` 或兩 `div` 分行，前端回落 `updatedByName ?? createdByName ?? '—'`。替代：三段保持單行 → 可讀性差 → 捨棄。
- **標籤統一**：`建立時間` 視為 `建立` 同義，全改為 `建立：`；第二行改 `編輯者：／更新：`。替代：保留 `建立時間` → 與檢視分歧 → 捨棄。
- **Progress 配色**：`barClass` 由回傳 Bootstrap `bg-warning/bg-success/bg-secondary` 改為回傳 inline `backgroundColor`（`PENDING:#f59e0b` amber-500、`RESOLVED:#10b981` emerald-500、`CLOSED:#64748b` slate-500）或對應自訂 class，模板由 `:class="barClass(...)"` 改為 `:style="{backgroundColor: barColor(...)}"`。替代：續用 Bootstrap 原色 → 對比與現代感不足 → 捨棄；大幅改色（如紅/藍）→ 破壞既有黃/綠/灰語意 → 捨棄。

## Risks / Trade-offs

- [updatedBy 缺失] 後端無該欄時第二行顯示 `—` 或回落建立者 → Mitigation：前端回落，不阻塞
- [預覽字串模板] `Swal` HTML 為字串，需手動 `+ formatTime` → Mitigation：抽 `formatTime` 小函式至 `utils` 或就地定義

## Migration Plan

1. `CaseDetailView.vue:260` 改兩行 + 標籤 → `CasesView.vue:477` 改 `formatTime` + 兩行 → `DashboardView.vue:80,211` 改 `barColor` 配色 → `npm run build`/`npm test`
2. 無 DB 遷移；舊資料 `createdAt/updatedAt` 顯示自動統一

## Open Questions

- 無（`updatedBy` 回落策略已定為前端回落，不需後端變更）
