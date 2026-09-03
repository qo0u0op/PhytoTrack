## Context

見 `proposal.md - Why`：`signer-lifecycle` 已分流 `user as signer` 與 `signer but not user` 並引入 `active`，但簽名人仍寄生於 `ReferenceDataAdmin`、 `identifier_id` 仍 `AUTOINCREMENT` 留空洞、`DELETE` 仍開放、提權未自動建且撞名無綁定、案件內新建誤綁 `user`。`DataInitializer` 仍建 `張志明/林雅惠/陳建宏` 種子，`CaseService` 已可自動帶入但 `inlineIdentifiers` 尚未原子化。需一次收斂 ID 策略、生命週期與導覽。

## Goals / Non-Goals

**Goals:**
- 移除 3 筆預設、`id` 去 `AUTOINCREMENT` 並可重設為 1
- 禁刪僅停用，停用後新增案件候選自動移除
- 提權至 `STAFF|ADMIN` 自動建 `displayName`/`username` 為準的 `user as signer`，撞名可綁定
- 案件內新建一律 `user IS NULL` 原子建立
- 簽名人獨立為 `/signers` 並置於 `參照資料管理` 之前，列表顯 `類型` 與 `active`

**Non-Goals:**
- 歷史 `case_identifiers` 快照、不回收已用 `id` 空洞、`STAFF` 改他人 `user as signer`、電子簽章圖檔

## Decisions

### D1 種子移除與 ID 策略
- **選擇**：`schema.sql` 刪 3 筆 `INSERT`，`identifiers.identifier_id` 改 `INTEGER PRIMARY KEY`（SQLite 去 `AUTOINCREMENT` 關鍵字），`DataInitializer` 刪種子邏輯並加遷移：若檢到 `張志明/林雅惠/陳建宏` 且 `user IS NULL` 則 `UPDATE active=1` 保留，否則 `INSERT OR IGNORE` 補？本 proposal 定稿為全刪，遷移以 `UPDATE active=0` 或 `DELETE` 擇一（見 Migration）。`AUTOINCREMENT` 移除後 `sqlite_sequence` 不再自增，`DELETE FROM identifiers` 後 `DELETE FROM sqlite_sequence WHERE name='identifiers'` 即可重設。
- **替代**：保留 `AUTOINCREMENT` 僅 `VACUUM`，但空洞仍不可重用，故選去關鍵字。

### D2 禁刪僅停用
- **選擇**：`ReferenceDataService.deleteIdentifier` 改拋 `405` 或 `409` 並由 `ReferenceDataAdminController.deleteIdentifier` 轉 `405 Method Not Allowed`，保留 `PATCH .../active` 為唯一可見性開關；`GET /ref/identifiers` 已有 `includeInactive` 過濾，停用後新增案件候選自然消失，歷史以 `id` 引用不受影響。
- **替代**：保留 `DELETE` 但僅對 `user IS NULL` 且未被引用者，語意重疊且易誤刪歷史，故統一禁刪。

### D3 自動註冊與名稱準則
- **選擇**：`AuthService.updateRole` 與 `UserAdmin` 提權路徑內 `IdentifierService.ensureForUser` 以 `displayName`（`trim` 非空否則 `username`）為名，若已存在同名 `active` 則復用，否則新建 `user_id = userId`。`DataInitializer` 僅對 `STAFF|ADMIN` 補建，不再以角色名。
- **替代**：以 `username` 為主會與顯示名脫節，故選 `displayName` 優先。

### D4 案件內新建默認非 user
- **選擇**：`CaseService` 內 `inlineIdentifiers` 走 `ReferenceDataService.createIdentifierForCase` 新方法或直接 `Identifier(user=null, active=true)` 同交易建立，名稱去空白重複時 `findByIdentifierAndActiveTrueAndUserIsNull` 復用；`POST /api/admin/ref/identifiers` 經案件入口者亦強制 `user=null`（加參數 `fromCase=true` 或獨立方法），僅提權路徑建 `user` 版。
- **替代**：共用 `createIdentifier` 需區分 `user` 來源，易誤綁，故分流。

### D5 撞名綁定
- **選擇**：提權前 `SELECT * FROM identifiers WHERE identifier = :displayName AND user IS NULL AND active=1`，若存在則 `AuthService` 拋 `409 SIGNER_NAME_CONFLICT` 帶 `existingIdentifierId`，`UsersView.vue` 彈確認後調 `POST /api/admin/ref/identifiers/{id}/bind`（新 `IdentifierService.bindToUser` 設 `user_id`），取消則放行新建（允許同名多筆）。
- **替代**：自動綁定無確認會誤將他人外聘簽名人綁走，故需顯式確認。

### D6 獨立導覽與順序
- **選擇**：新增 `frontend/src/views/SignersView.vue`，`router` 增 `'/signers'` `meta: { requiresAdmin: true }`，`Navbar.vue` 插入於 `參照資料管理` 之前（順序：儀表板 / 案件管理 / 送件人管理 / 作物管理 / 害物管理 / **簽名人管理** / 參照資料管理 / 使用者管理），列表欄含 `ID/名稱/類型(active/user)/操作`。`ReferenceDataAdminView.vue` 保留但隱藏 `identifiers` 籤頁或轉導轉跳。
- **替代**：復用原籤頁僅加欄位會使參照資料管理過重，且簽名人已具獨立生命週期，故獨立頁。

## Risks / Trade-offs

- [種子移除致舊庫 3 筆消失] → `active=false` 軟隱藏或遷移 `DELETE` 二選一，歷史案件以 `id` 仍可查，文件化 **BREAKING**
- [ID 去 AUTOINCREMENT 後重用] → 已停用歷史 `id` 若被重用會混淆，建議僅在空庫重設（`DELETE FROM identifiers` 後才 `DELETE FROM sqlite_sequence`），非空庫不重設
- [禁刪導致無法清理 typo] → 以 `active=false` 替代，管理頁 `includeInactive=true` 可見並可 `active=true` 回復，無硬刪
- [同名多筆] → 允許 `王小明` 同名多筆（`user` 不同或皆 `null`），由 `active` 與綁定收斂，非 `UNIQUE` 約束

## Migration Plan

1. **DB**：`DataInitializer` 於啟動時 `UPDATE identifiers SET active=0 WHERE identifier IN ('張志明','林雅惠','陳建宏')` 或 `DELETE`（依保留策略），`schema.sql` 去 `AUTOINCREMENT`；空庫建議 `DELETE FROM identifiers; DELETE FROM sqlite_sequence WHERE name='identifiers'; VACUUM;` 後重啟由提權重建
2. **後端**：`mvn test` 驗 `PUT` 禁改、`PATCH active`、`409` 綁定、`GET` 過濾、`POST` 案件內新建 `user IS NULL`
3. **前端**：`npm run build` 驗 `SignersView` 導覽順序與類型欄、`CaseFormView` 候選僅 `active`、`UsersView` 409 彈窗
4. **Rollback**：`active` 欄保留，`AUTOINCREMENT` 回加需重建表，種子可手動 `INSERT` 回補

## Open Questions

- 空庫重設 `id` 是否需自動化為 `POST /api/admin/ref/identifiers/reset`？本批僅文件化手動 SQL，不新增端點
