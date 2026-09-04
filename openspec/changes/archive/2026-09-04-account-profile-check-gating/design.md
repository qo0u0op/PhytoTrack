## Context

見 `proposal.md - Why`。現況：`emailDirty()` 以 `originalEmail !== null` 為前提（null 原始值永遠非 dirty）；信箱欄無取消鈕；`saveProfile` 對未檢查欄位自動代跑（顯示名稱本地檢查必過，等於無閘門）；信箱格式僅送出時檢查。

## Goals / Non-Goals

**Goals:**
- null 原始值正常 dirty；雙欄皆有取消；送出必須經過顯式檢查；格式錯誤即時可見。

**Non-Goals:**
- 後端契約變更（`PUT /api/account/profile` 已支援 null 與 `@Email`）。
- 註冊頁行為變更（沿用共用 regex，不加取消概念）。

## Decisions

### D1 dirty 改 `loaded` 旗標
- **選擇**：`loadProfile` 成功後 `loaded=true`；`displayNameDirty/emailDirty` 改為 `loaded && trim(現值) !== trim(載入值 ?? '')`。null 原始值視為空字串比對。
- **替代考慮**：保留 null 守衛並特判——分支更多，不如單一旗標。

### D2 顯式檢查閘門（按鈕可見性，不再送出時擋）
- **選擇**：`canSave()` 改為：有 dirty 且（顯示名稱未改或已通過檢查）且（信箱未改、或為空、或已通過檢查）。隱藏時原位置顯示動態提示：未修改→「尚未修改任何欄位」；有改未檢→「請先完成修改欄位的檢查」。`saveProfile` 移除檢查閘門 Swal（按鈕可見即代表已通過），僅保留格式複檢作為字符層防線。
- **替代考慮**：送出時擋下 alert——使用者明確要求以按鈕可見性取代 alert，不做。

### D3 信箱取消鈕與即時格式錯
- **選擇**：`cancelEmail()` 比照 `cancelDisplayName()`；`emailFormatErr` computed（非空且 `!EMAIL_PATTERN`）顯示於欄位下方，送出前複檢保留。
- **替代考慮**：格式錯時隱藏儲存鈕——儲存鈕語意為 dirty 顯示，保持單一語意，阻擋放送出端。

## Risks / Trade-offs

- [既有使用者習慣「改了就存」] → 多一次按檢查的成本；提示文案指引，接受。
- [檢查結果與送出間的競態] → 後端仍做最終唯一檢查（`EMAIL_DUPLICATE`），接受。

## Migration Plan

1. **部署**：僅前端，无需後端配合；舊後端相容。
2. **Rollback**：revert 單檔即回退。

## Open Questions

- 無。
