## Context

見 `proposal.md - Why`。現況：`updateRole` 降權連動停用但保留連結；`updateActive(false)` 連動停用但保留連結；`ensureForUser` 只認 `findByUserUserId` 連結筆（active 更名／inactive 同名啟用），解綁舊筆（`user IS NULL`）會被略過而新建重複。關鍵難題：解綁後「自己的舊筆」與「他人的同名非使用者簽名人」資料狀態完全一致，純同名比對無法區分（會誤搶並破壞既有 `SIGNER_NAME_CONFLICT` 回歸測試），故以 `former_user_id` 歷史欄位區分。

## Goals / Non-Goals

**Goals:**
- 降權／停用解綁留 `id` 並記錄 `former_user_id`；升權／啟用憑歷史恢復原筆；`ensureForUser` 不再對可恢復舊筆新建重複；既有撞名測試行為不變。

**Non-Goals:**
- 候選清單可見性規則變更（inactive 仍隱藏）。

## Decisions

### D1 降權解綁（`updateRole` → VIEWER）
- **選擇**：角色由 STAFF/ADMIN 變為 VIEWER 時，對 `findByUserUserIdAndActiveTrue` 逐筆 `setUser(null)`＋`setFormerUser(該使用者)`（`active` 不動，`id` 不動）。僅降權觸發；同級或升權不動。本 change 同步 MODIFIED 主規格「簽名人停用連動與首筆確定性」（降權由停用改解綁保留可見）。
- **替代考慮**：比照停用一併 `active=false`——使用者仍在職可用其名建案的情境應保留可見，故維持 active。

### D2 停用解綁（`updateActive(false)`）
- **選擇**：既有 `deactivateUserSigners` 內加 `setUser(null)`＋`setFormerUser`（先解綁再停用，順序無關）。可見性不變。
- **替代考慮**：保留連結——重啟後恢復需連結，但解綁＋歷史重鏈已可恢復，且解綁使管理頁身分別正確，選解綁。

### D3 恢復原筆（升權／啟用，憑 `former_user_id`）
- **選擇**：升權至 STAFF/ADMIN 或 `updateActive(true)`（限角色 STAFF|ADMIN）時，先找候選：`findByFormerUserId(userId)` 中同名（正規化比對現 `displayName`）且 `user IS NULL` 者取 `id` 最小，命中則 `setUser(user)+setActive(true)`；無命中才走既有流程。實作上直接呼叫擴充後的 `ensureForUser`（其已含歷史搜尋一路）；升權路徑在撞名檢查（`SIGNER_NAME_CONFLICT`）前先恢復，自身舊筆恢復後不再誤報，他人同名仍正常 409（既有回歸測試保留）。
- **替代考慮**：純同名比對認定——解綁後自舊與他人同名狀態一致，無法區分，會誤搶並破壞既有撞名測試，故以歷史欄位區分。

### D4 `ensureForUser` 擴充（歷史一路）與 D5 欄位
- **選擇**：`ensureForUser` 內 `synchronized` 區塊重排為：歷史重鏈（`findByFormerUserId` 同名未綁定 `id` 最小者，命中即重鏈啟用返回）→ 全域非使用者 active 撞名檢查（未命中歷史才檢查，他人同名仍拋錯）→ 既有連結 inactive 同名啟用 → 新建。`bindToUser` 綁定時清空 `former_user_id`（已有所屬無需歷史）。`Identifier` 新增 `formerUser`（`@ManyToOne`，`former_user_id` 可空 FK）；`IdentifierRepository` 新增 `findByFormerUserId`；`schema.sql` 同步建欄。
- **替代考慮**：僅在 `updateRole/updateActive` 做恢復、`ensureForUser` 不動——建案自動帶入路徑仍可能新建重複，故 `ensureForUser` 也須擴充。

## Risks / Trade-offs

- [改名後無法認定原筆] → 視為新人新筆，舊筆留作非使用者；管理頁可手工綁定，接受。
- [升權撞名檢查誤報自身舊筆] → 恢復先於檢查（D3 順序），自身舊筆恢復後不再誤報；他人同名仍 409。
- [併發重鏈與新建競態] → 沿用既有 `synchronized intern + DIVE` 模式。
- [既有未綁定簽名人無歷史] → `former_user_id` 為 null，恢復找不到而走既有行為；接受（僅未來解綁走新語意）。

## Migration Plan

1. **DB**：`identifiers` 新增可空 `former_user_id` FK。Hibernate `ddl-auto: update` 可 `ADD COLUMN`（SQLite 支援），既有庫自動補欄；`schema.sql` 同步（新庫直接生效）；`docs/DEPLOY.md` 加註驗證 SQL。
2. **部署**：後端單獨可上；既有連結資料不受影響（僅未來降權／停用走新語意）。
3. **Rollback**：revert 即回舊語意；已解綁者可手工重綁；多餘欄位留置無害。

## Open Questions

- 無（停用可見性採預設維持隱藏）。
