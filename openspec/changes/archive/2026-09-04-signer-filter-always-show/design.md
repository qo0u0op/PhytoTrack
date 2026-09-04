## Context

見 `proposal.md - Why`。現況：`SignersView.vue` 篩選列在 Bootstrap `collapse` 抽屜內（`#signerFilter`，預設隱藏），僅有關鍵字與顯示已停用條件。反轉為常顯並加身分別維度。

## Goals / Non-Goals

**Goals:**
- 篩選零點擊即用；身分別可篩；與既有條件疊加。

**Non-Goals:**
- 後端篩選 API（使用者量小，前端過濾足夠）。
- 啟用狀態下拉（本次僅身分別；顯示已停用勾選保留）。

## Decisions

### D1 移除抽屜，常顯篩選卡
- **選擇**：刪除篩選按鈕與 `collapse` 包裝，篩選卡回到常顯（前案抽屜前的版面結構）。
- **替代考慮**：保留按鈕但預設展開——多餘控制項，不如移除。

### D2 身分別下拉（`identityFilter` ref）
- **選擇**：`'all' | 'user' | 'nonuser'`（預設 `all`），以 `userId != null` 判定，併入既有 `filtered` computed（關鍵字→身分別→停用開關順序）。
- **替代考慮**：後端加參數——無 API 變更需求，不做。

## Risks / Trade-offs

- [與已封存抽屜決策衝突] → 本 change 的 MODIFIED 即為正式反轉，封存後主規格以新行為為準。

## Migration Plan

1. **部署**：僅前端；無資料影響。
2. **Rollback**：revert 單檔。

## Open Questions

- 無（下拉選項採預設「全部／使用者／非使用者」）。
