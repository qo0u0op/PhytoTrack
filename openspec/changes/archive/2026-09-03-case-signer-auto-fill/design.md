## Context

見 `proposal.md - Why`：`identifiers.user` 已存在（`Identifier.java:19`），`DataInitializer.java:64` 為 staff/admin 預建 3 筆簽名人，但 `CaseService.java:208` 仍要求前端手動傳 `identifierIds`，常漏選。`open-in-view: false` 下 `Case` 僅透過 `CaseIdentifier` 多對多關聯 `Identifier`，DTO 隔離；`User.displayName` 為簽名人字面來源。需在不破壞既有「多簽名人」能力下，讓「當前使用者」預設簽入。

## Goals / Non-Goals

**Goals:**
- STAFF/ADMIN 至少擁有一筆以其 `displayName` 命名的 `Identifier`，並隨 `displayName` 同步更名
- 建案/更新時 `identifierIds` 空值自動帶入當前使用者簽名人（無則即時建立），有值則原樣保留
- 前端表單預選當前使用者簽名人，仍可增刪
- 既有 `identifiers`（含 staff 擁 2 筆的種子）與已結案案件引用不受影響

**Non-Goals:**
- 單案件單簽名人強制、電子簽章圖檔、Identifier 更名時追溯歷史快照（仍以關聯 Identifier 當前名稱顯示）
- 刪除使用者連帶刪 Identifier（保留以免外鍵中斷）
- VIEWER 簽名人強制建立（VIEWER 不診斷，僅在升為 STAFF/ADMIN 時建立）

## Decisions

### D1 同步點：集中於 `IdentifierService`（或 `ReferenceDataService`）+ 呼叫方

- **選擇**：抽 `IdentifierService.ensureForUser(User user)`（`findByUserUserId` 不存在則 `identifier = user.displayName` 新建；存在多筆時取首筆並在 `displayName` 變更時同步更名首筆），由三處呼叫：`AuthService.register` 後（不立即建，待角色升為 STAFF 再建）、`AccountService.updateProfile` / `UserAdminService.updateRole` / `updateActive` 後（若 `displayName` 變則更名；若角色變為 STAFF/ADMIN 且無簽名人則建立）、`DataInitializer` 補建（啟動時對所有 STAFF/ADMIN 無簽名人者補建）。
- **替代**：以 JPA `EntityListener` 監聽 `User` 更動；但 `displayName` 變更涉及跨實體（User→Identifier）且需事務，顯式服務呼叫更可控、易測試。
- **查詢**：`IdentifierRepository: findByUserUserId(Long userId)`, `findByIdentifier(String name)` 預留，種子多對一（staff 有 2 筆）保持相容，新建時不強求唯一。

### D2 自動帶入：於 `CaseService` 交易內處理，而非 Controller

- **選擇**：`CaseService.createCase(CaseDtos.CreateRequest req, UserPrincipal principal)` 與 `updateCase` 內，`if (req.identifierIds()==null || req.identifierIds().isEmpty()) { Identifier auto = identifierService.ensureForUser(currentUser); req = req.withIdentifierIds(List.of(auto.getIdentifierId())); }`（record 以 copy 方式）。`addIdentifiers` 前以 `getRef` 驗存在，整體 `@Transactional`。
- **為何 Service 而非 Controller**：需 `UserRepository` 查找與可能的即時建立，與現有 `getRef` 驗證同層；`Principal` 僅在 Controller 取得，轉 `userId` 傳入 Service，保持分層。
- **替代**：前端獨佔預選，不補後端；但直接 `curl` 或舊前端仍可能空清單，後端兜底更可靠。

### D3 前端預選：復用清單 + 可選 `GET /api/identifiers/me`

- **選擇**：優先復用 `GET /api/identifiers`（`ReferenceDataController.java:86`），`CaseFormView.vue: onMounted` 以 `useAuthStore().user.displayName` 或 `GET /api/auth/me` 取得當前使用者，於 `identifiers` 清單中 `find(i => i.userId === currentUserId || i.identifier === displayName)` 預選；若無匹配且 STAFF，則呼叫 `POST /api/admin/ref/identifiers` 不可行（僅 ADMIN），改由後端建案兜底。為便利可新增 `GET /api/identifiers/me → IdNameResponse`（需 STAFF+），前端優先調用，失敗回退清單比對。
- **替代**：僅後端兜底不做前端預選；但使用者期待表單打開即見己名，故前端預選提升 UX。

### D4 並行與一致性

- **選擇**：`identifierService.ensureForUser` 以 `synchronized` 或 DB 唯一索引（`user_id + identifier` 部分唯一）防重建；本專案 5 人 LAN，`findThenSave` 競態可接受，失敗重試由 `DataIntegrityViolationException` 轉查回。
- **替代**：DB 加 `UNIQUE(user_id)` 強制一對一，但與現有 staff 2 筆種子及「多人會診同一人多簽名」彈性衝突，故不加。

## Risks / Trade-offs

- [歷史簽名隨更名而變] → 接受：`CaseIdentifier` 存 FK 非快照，更名後歷史案件顯示新名；若需快照應另加 `identifierSnapshot` 欄位，本批不引入（見 Non-Goals）。
- [staff 既有 2 筆種子與「恰一」敘述衝突] → 以「至少一」為實作不變量，保留既有 2 筆，僅確保更名同步首筆；文件與測試以 `findByUserUserId` 取首筆為準。
- [VIEWER 升 STAFF 時才建，期間建案無簽名人] → 建案兜底會即時建立，故即便未預建也能自動帶入。
- [前端預選依賴 `displayName` 字串比對可能誤配同名] → 優先以 `userId` 比對（需 API 回 `userId`），字串僅回退；`ReferenceDataController.identifiers()` 需補 `userId` 於回應或新增 `me` 端點。
- [刪除使用者後 Identifier 孤兒] → 保留，ADMIN 可於參照資料管理手動清理未被引用的（既有刪除保護 `existsByCaseIdentifiersIdentifierIdentifierId`）。

## Migration Plan

1. **DB**：無 schema 變更（`user_id` 已存在 `Identifier.java:20`），僅資料補建；啟動時 `DataInitializer` 對無簽名人之 STAFF/ADMIN 各補一筆（`findByUserUserId` 空則建）。
2. **部署**：`mvn test` 後 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 手測：STAFF 登入建案不選簽名人 → 詳情含己名；改 `displayName` 後建案含新名；`PUT /api/cases/{id} identifierIds=[]` 自動替換。
3. **前端**：`CaseFormView.vue` 載入預選；無需環境變數。
4. **Rollback**：移除 `CaseService` 自動帶入分支即回原「空清單存空簽名」；Identifier 補建資料保留不影響舊版讀取。

## Open Questions

- `PUT` 時 `identifierIds: null`（未傳）是否視為「不更動」或「自動帶入」？本設計依 `CaseService.java:515` 現行 `if (request.identifierIds()!=null)` 判斷，未傳保留原值，僅空陣列觸發自動帶入，與 spec「空或未傳」文字略差，實作以程式碼為準，後續可於 spec 補 `null` 語意澄清。
