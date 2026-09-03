# 提案：簽名人生命週期分流（user as signer 禁直改＋非 user 簽名人可停用）

## Why

`case-signer-auto-fill` 以 `Identifier.user` 關聯 `User.displayName` 實現自動帶入，但兩類簽名人未分流：1) `user as signer`（`user_id != null`）經 `ReferenceDataAdmin` 直改 `identifier` 名稱即與 `User.displayName` 分叉，後續 `ensureForUser` 以 `displayName` 蓋回造成漂移；2) `signer but not user`（`user_id == null`）多為外聘/歷史簽名人，typo 或離職後無法停用，`GET /ref/identifiers` 全量回傳使 `CaseFormView` 名單無限膨脹，typo 修正只能靠 `ADMIN` 硬刪或誤改他人簽名。

## What Changes

- **1. user as signer 鎖定直改**：`Identifier.user_id != null` 視為衍生簽名人，`PUT /api/admin/ref/identifiers/{id}` 對此類 SHALL 拒絕直改名稱（回 `409 USER_LINKED_SIGNER_IMMUTABLE` 並提示「請改個人檔案顯示名稱」），僅容 `PATCH /active` 啟停用；名稱唯一真相源為 `User.displayName`，由 `AccountService.updateProfile` 經 `IdentifierService` 同步更名。
- **2. signer but not user 獨立生命週期**：`Identifier.user_id == null` 為獨立簽名人，`ADMIN` 可 `POST/PUT/PATCH active/DELETE` 全量管理（`PUT` 改名合法）；新增 `active` 欄位（`boolean`，預設 `true`）與 `PATCH /api/admin/ref/identifiers/{id}/active`（`ADMIN`）及 `PATCH /api/ref/identifiers/{id}/active` 自助（`STAFF` 僅己之 `user as signer`）用以軟停用，停用後 `GET /ref/identifiers` 預設僅回 `active=true`（管理頁 `?includeInactive=true` 可見），`CaseService` 仍可引用已停用之歷史 `id` 但表單預選不顯示。
- **3. typo 自助路徑**：`STAFF` 打錯自身衍生簽名人→改 `displayName` 即同步；`STAFF` 建錯獨立簽名人（`user_id == null` 需 `ADMIN` 建，故 typo 主責 `ADMIN`）→ `ADMIN` 透過 `PUT` 更名或 `PATCH active=false` 停用後重建正確；`ADMIN` 誤建或離職簽名人→ `PATCH active=false` 隱藏，不硬刪以保歷史。

**非目標**：不引入簽名人快照冗餘、不改變 `identifier` 主鍵語意、不開放 `STAFF` 直改他人 `user as signer`。

## Capabilities

### New Capabilities

<!-- 無新增能力，僅分流既有 reference-data-admin -->

### Modified Capabilities

- `reference-data-admin`: 分流 `user as signer` 與 `signer but not user` 的編輯與停用規則，新增 `active` 欄位與軟停用行為，調整 `GET /ref/identifiers` 預設過濾

## Impact

- **後端**：`models/Identifier.java` 增 `active`（`NOT NULL DEFAULT true`），`repository/IdentifierRepository` 增 `findByActiveTrue` 等，`service/ReferenceDataService` 與 `IdentifierService` 增 `active` 切換與直改阻擋，`controller/ReferenceDataAdminController` 與 `ReferenceDataController` 增 `PATCH active` 與 `GET` 過濾參數，`service/CaseService` `GET /identifiers/me` 僅回 `active`，`service/DataInitializer` 補 `active=true`。
- **前端**：`CaseFormView.vue` 簽名人清單僅顯示 `active`（後端已過濾），新增頁 `GET /ref/identifiers` 無需改；`ReferenceDataAdminView.vue` 簽名人頁增「顯示已停用」切換與「停用/啟用」按鈕（`PUT` 禁對 `user as signer`，`PATCH active` 放行）。
- **資料**：無破壞性遷移，`active` 預設 `true`（SQLite `ALTER TABLE ADD COLUMN`），既有 `user as signer` 多筆（如 `staff` 2 筆）皆 `active=true` 保留，後續由 admin 停用冗餘。
- **相容性**：`PUT /admin/ref/identifiers/{id}` 對 `user as signer` 新增 `409` 分支，其餘契約不變；`GET /ref/identifiers` 預設過濾屬行為收斂，已停用簽名人仍可被舊案件以 `id` 引用（不影響 `DELETE` 保護）。
