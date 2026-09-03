# 提案：診斷簽名人關聯使用者並於建案時自動帶入

## Why

現行 `identifiers` 雖已有 `user_id` 欄位且 `DataInitializer` 為 admin/staff 預建簽名人，但 `POST /api/cases` 仍需手動勾選 `identifierIds`，使用者常漏選或選錯他人簽名，導致診斷單責任不清。隨著案件量增加，需讓「誰建案就以誰的 `displayName` 簽名」成為預設，減少手動操作並確保簽名人與實際診斷者一致。

## What Changes

- **簽名人—使用者關聯不變且強化**：`Identifier.user` 維持 `ManyToOne`，新增不變條件「每位 `ROLE_STAFF`/`ROLE_ADMIN` SHALL 擁有恰一個以其 `displayName` 命名的 Identifier」；於使用者建立（註冊後由 ADMIN 授權為 STAFF/ADMIN 時、或 `DataInitializer`）與 `displayName` 變更（`AccountService.updateProfile` / `UserAdmin` 調整）時同步建立或更名對應 Identifier，刪除使用者時不自動刪除 Identifier（避免已結案案件外鍵中斷）。
- **建案時自動帶入**：`POST /api/cases` 與 `PUT /api/cases/{id}` 若 `identifierIds` 為空或未傳，後端 SHALL 自動帶入當前登入使用者的關聯 Identifier（以 `user_id` 查找，無則以 `displayName` 即時建立）；若請求已含清單則原樣保留，不覆蓋。前者為主流程，後者保留手動增刪能力。
- **前端預選**：`CaseFormView` 載入時 SHALL 透過 `GET /api/identifiers`（或新增 `GET /api/identifiers/me` 回當前使用者關聯）預選對應簽名人，仍允許使用者增刪多簽名（含多人會診）。
- **查詢與管理**：`GET /api/identifiers` 回傳仍含 `user` 關聯資訊；`ReferenceDataAdmin` 維持 ADMIN 可增刪改，其 `user` 欄位為可選，同步建立的 Identifier 自動關聯建立者。

**非目標**：不引入電子簽章圖檔、不改變 `identifier` 為字串主鍵以外型態、不強制單案件單簽名人（仍支援 `case_identifiers` 多對多）。

## Capabilities

### New Capabilities

<!-- 無新增能力，僅強化既有關聯與預設行為 -->

### Modified Capabilities

- `case-lifecycle`: 擴充案件建立/更新契約，新增「未指定簽名人時自動帶入當前使用者關聯簽名人」規則
- `reference-data-admin`: 強化 Identifier 參照資料規則，新增「STAFF/ADMIN 使用者與 Identifier 一對一（以 displayName 同步）」不變條件與同步行為

## Impact

- **後端**：`models/Identifier.java`（`user` 唯一或索引）、`service/ReferenceDataService.java`（Identifier CRUD 關聯使用者）、`service/UserService`/`AccountService`/`AuthService`（建立/更名時同步 Identifier）、`service/CaseService.java`（建案/更新時自動帶入）、`controller/CaseController.java`（`@AuthenticationPrincipal` 取得當前使用者）、`controller/ReferenceDataController.java`（新增 `GET /api/identifiers/me` 可選）、`repository/IdentifierRepository.java`（`findByUserUserId` / `findByIdentifier`）
- **前端**：`CaseFormView.vue`（載入時預選當前使用者簽名人，`api/reference.ts` 新增 `getMyIdentifier`）、`ReferenceDataAdminView` 顯示關聯使用者
- **資料**：既有 3 筆 `identifiers` 已符合，啟動時對無對應 Identifier 的 STAFF/ADMIN 補建；`users` 的 `displayName` 變更需觸發 Identifier 更名，交易內完成
- **相容性**：既有手動傳 `identifierIds` 的呼叫不受影響；空清單改為自動帶入屬行為變更，已於 spec 明確，CSV 與詳情回應仍含 `identifiers` 陣列
