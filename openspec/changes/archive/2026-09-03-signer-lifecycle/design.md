## Context

見 `proposal.md - Why`：`Identifier.user` 已分 `user as signer`（`user_id != null`，由 `IdentifierService.ensureForUser` 以 `displayName` 衍生，`CaseService` 自動帶入）與 `signer but not user`（`user_id == null`，由 `ADMIN` 經 `ReferenceDataService` 手建）。`ReferenceDataAdminController` 現全量 `ADMIN` 可 `PUT` 任意簽名人，直改 `user as signer` 即與 `User.displayName` 分叉；`Identifier` 無 `active`，離職/typo 的已用簽名人無法隱藏，`GET /ref/identifiers` 全量回傳使 `CaseFormView` 膨脹。`open-in-view: false` 下仍以 DTO 隔離，`DELETE` 已擋被引用者。

## Goals / Non-Goals

**Goals:**
- `user as signer` 名稱鎖定 `User.displayName`，參照管理禁直改，僅 `active` 可切；typo 由改個人檔案自修
- `signer but not user` 保 `ADMIN` 全管（`PUT` 更名合法），並與前者共用 `active` 軟停用，`GET` 預設僅回 `active=true`
- `STAFF` 對自身首個 `user as signer` 可自助停用 typo，`ADMIN` 可停用任意，停用不影響歷史 `case_identifiers` 的 `id` 引用
- 既有多筆（如 `staff` 2 筆）相容，不強遷一對一

**Non-Goals:**
- 簽名人快照冗餘（歷史隨更名而變仍接受）、`STAFF` 直改他人 `user as signer`、電子簽章圖檔、強制一對一 DB 約束

## Decisions

### D1 模型：`Identifier.active boolean DEFAULT true`，無硬一對一

- **選擇**：`models/Identifier.java` 增 `boolean active=true`（`@Column(nullable=false)`），`schema.sql` 以 `ALTER TABLE identifiers ADD COLUMN active BOOLEAN NOT NULL DEFAULT 1`（SQLite 相容，舊資料預設 `true`），`IdentifierRepository` 增 `findByActiveTrue()`、`findByActiveTrueAndUserUserId` 等，不設 `UNIQUE(user_id)` 以相容既有 2 筆種子與「多人會診同一人多簽名」彈性。
- **替代**：`UNIQUE(user_id)` 強制一對一可徹底防多筆，但需遷移合併既有 2 筆且削弱會診彈性，故不採。

### D2 `PUT` 直改阻擋：`user_id != null` 即拒

- **選擇**：`ReferenceDataService.updateIdentifier(id,name)` 內先 `findById`，若 `e.getUser()!=null` 拋 `ApiException(409, USER_LINKED_SIGNER_IMMUTABLE)`，訊息指引「請改個人檔案顯示名稱」；`user_id==null` 者照常 `setIdentifier(name)`。此檢查置於 Service 層，Controller 僅透傳。
- **為何 Service 而非 Controller**：與 `existsByCaseIdentifiers...` 刪除保護同層，交易內一致，易測。
- **替代**：雙向同步（直改 `identifier` 同步 `user.displayName`）可接受但語意混淆，且 `ADMIN` 無意改 `user` 名時亦被連動，故選拒絕。

### D3 軟停用：`PATCH .../active` + `GET` 預設過濾

- **選擇**：`ReferenceDataService` 增 `updateIdentifierActive(id,boolean)`，`ReferenceDataAdminController` `PATCH /admin/ref/identifiers/{id}/active`（`ADMIN`），`ReferenceDataController` `PATCH /ref/identifiers/{id}/active`（`STAFF` 限 `e.getUser().getUserId()==principal.getUserId()`，否則 `403`，`ADMIN` 亦可）。`GET /ref/identifiers` 加 `@RequestParam includeInactive=false`，預設回 `findByActiveTrue()`，`includeInactive=true` 回全量（管理頁用）；`GET /ref/identifiers/me` 僅回自身首個 `active`（若全停用則回首個停用或 `404` 指引重建）。`CaseService` 的 `GET`/`addIdentifiers` 仍以 `id` 查詢，不擋已停用之歷史引用。
- **替代**：硬刪後重建新 `id` 會使舊案件 FK 失效，故選軟停用。

### D4 權限分流：`STAFF` 僅己之 `user as signer`，`ADMIN` 全量

- **選擇**：`STAFF` 自助路徑僅 `PATCH active` 己之首個簽名人（typo 自清），`POST /ref/identifiers` 不開放 `STAFF`（避免 `user_id==null` 的公共簽名人氾濫，仍由 `ADMIN` 統管）；`STAFF` 若需新建正確獨立簽名人，改請 `ADMIN` 建或改 `displayName` 衍生。`signer but not user` 的 `PUT` 更名仍限 `ADMIN`。
- **替代**：開放 `STAFF` `POST` 建 `user_id==currentUser` 的獨立簽名人可讓 typo 重建更自助，但會使「至少一」語意膨脹為多筆，與首筆同步邏輯衝突，故本批不開放，後續依使用回饋再擴。

## Risks / Trade-offs

- [歷史簽名隨更名仍變] → 接受（與 `case-signer-auto-fill` 一致），`active` 停用不改歷史 `id`，僅隱藏選單；若需快照應另 `identifierSnapshot`，本批不引入。
- [既有 2 筆 `staff` 多筆與「首筆」語意] → `ensureForUser` 僅同步首筆，其餘視為 `user_id==staff` 的冗餘，可由 `ADMIN` 逐筆 `active=false` 收斂，不自動合併。
- [GET 預設過濾使舊前端誤以為簽名人消失] → `includeInactive` 參數保留，管理頁預設關，`CaseDetail` 以 `id` 查歷史不受影響，無破壞。
- [STAFF 自助僅 `PATCH active` 而無 `PUT` 直改，typo 仍需改 `displayName`] → 對 `user as signer` 的 typo 本就源於 `displayName`，改個人檔案即正解，減少分叉。

## Migration Plan

1. **DB**：`ALTER TABLE identifiers ADD COLUMN active BOOLEAN NOT NULL DEFAULT 1`（`update ddl-auto` 或 `schema.sql` 增欄），舊資料全 `true`。
2. **部署**：`mvn test` 後 `GET /ref/identifiers` 驗預設不含已停用，`PUT /admin/ref/identifiers/{linkedId}` 驗 `409`，`PATCH .../active` 驗 `STAFF` 僅己、`ADMIN` 全量，`CaseFormView` 驗名單收斂。
3. **前端**：`ReferenceDataAdminView` 簽名人頁增「顯示已停用」切換與「停用/啟用」按鈕，`CaseFormView` 無需改（後端已過濾）。
4. **Rollback**：`active` 欄位保留，`GET` 參數 `includeInactive` 去除即回全量，`PUT` 阻擋移除即回可直改，無需資料回滾。

## Open Questions

- `GET /ref/identifiers/me` 在自身首個簽名人已 `active=false` 時應回首個停用、`404` 或自動重建並回新？本設計回首個（即使停用）並提示「已停用，請聯繫管理員啟用或改 displayName 重建」，避免自動重建造成多筆。
