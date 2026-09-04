## Context

見 `proposal.md - Why`。現況：`Sender.address` 為 `@Column(nullable = false)`，`CaseCreateRequest.senderAddress` 與 `SenderUpsertRequest.address` 皆 `@NotBlank`；前端地址欄 `required` 必填。案件更新路徑已容忍 null（沿用舊值），查詢／匯出已 `Optional.orElse("")` 容忍，`VIEWER` 遮蔽取地址前已判空（`isViewer ? null`）。

## Goals / Non-Goals

**Goals:**
- 三條寫入路徑（案件內新建、送件人獨立新增、送件人編輯）皆接受空地址並存 null。
- 前端地址欄去必填，空值不明文送空字串。

**Non-Goals:**
- 去重鍵變更（地址非去重鍵，不動）。
- 歷史假地址清理（既有髒資料由管理員手工修正）。

## Decisions

### D1 驗證放寬＋`blankToNull` 正規化
- **選擇**：兩 DTO 移除 `@NotBlank`（欄位改為可空）；`findOrCreateSender` 與 `SenderService.apply` 以既有 `blankToNull` 語意存入（null／空／全空白→null，其餘 trim）。與 `name/phone/displayName` 既有正規化一致。
- **替代考慮**：僅移除註解不做正規化——空字串與 null 並存會污染查詢顯示，一律正規化為 null。

### D2 Entity 與 `schema.sql` 改可空
- **選擇**：`Sender.address` 改 `@Column(nullable = true)`；`schema.sql` 建表語句同步去除 `NOT NULL`（新庫直接生效）。
- **替代考慮**：僅改 Entity 不動 `schema.sql`——新庫仍建出 NOT NULL，前後矛盾，故同步。

### D3 前端去必填、空值送 `undefined`
- **選擇**：案件表單地址 `<input>` 去 `required`；送出時空值轉 `undefined`（JSON 省略，後端收 null）。送件人編輯流程（`SendersView.vue`）若有同款必填一併去除。
- **替代考慮**：送空字串由後端正規化——仍傳無意義負載，且 `required` 擋在前端，不如直接去必填。

## Risks / Trade-offs

- [既有 SQLite 庫 `ddl-auto: update` 無法放寬既有欄位] → Migration Plan 手動遷移（`ALTER TABLE` 重建欄位或接受新庫才生效）；此為已知 SQLite 限制，文件載明。
- [空地址影響紙本對應] → 紙本地址欄留空即可，无需欄位變更。

## Migration Plan

1. **新庫**：`schema.sql` 已可空，直接生效。
2. **既有庫**：管理員執行一次性遷移（例：`ALTER TABLE senders ...` SQLite 需建表搬資料，或刪除重建測試庫 `./target/*.db`）；`docs/DEPLOY.md` 加註。
3. **部署**：後端先上（放寬驗證），前端後上；舊前端仍必填，新行為需新前端。
4. **Rollback**：加回 `@NotBlank` 即回必填；已存 null 不影響舊碼（查詢已容忍）。

## Open Questions

- 無（空字串正規化為 null 已按既有 `blankToNull` 定案）。
