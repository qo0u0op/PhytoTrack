## 1. 後端模型與約束

- [x] 1.1 放寬 `Sender` 欄位：`name` 改 `nullable=true`、新增 `displayName` (`nullable=true`)、`phone` 改 `nullable=true`，Service 層檢查 `phone` 與 `displayName` 至少一有值，顯示邏輯支援 `name (displayName)`，驗證以 `SenderRepository` 單元測試與 `CaseService.create` 400 為準
- [x] 1.2 移除 `UNIQUE (name, phone)`，新增 `phone` 部分唯一索引 (SQLite `WHERE phone IS NOT NULL AND phone <> ''`)，並處理遷移 (開發期重建或 `ALTER`)，驗證以 `DataInitializer` 啟動與重複 `phone` 寫入 409 為準

## 2. 後端搜尋與刪除服務

- [x] 2.1 `SenderRepository` 新增 `search (String q)` (`LOWER (name) LIKE %q% OR LOWER (phone) LIKE %q% OR LOWER (displayName) LIKE %q%`，分頁/限 10)，`SenderService` 包裝，驗證以 `SenderServiceTest` 搜尋回候選為準
- [x] 2.2 實作 `SenderService.delete (id)`：404 檢查、`CaseRepository.existsBySenderSenderId` 為 true 時拋 `REFERENCE_IN_USE` 409，否則硬刪除，驗證以 `SenderServiceTest` 刪除 204/409/404 為準
- [x] 2.3 `CaseRepository` 新增 `existsBySenderSenderId (Long senderId)`，`SenderRepository` 保留 `findByNameAndPhone` 僅作相容，驗證以 repository 測試為準

## 3. 後端遮蔽、統計與 DTO 擴充

- [x] 3.1 `CaseDtos.CaseResponse`/`CaseSummaryResponse` 新增 `senderId`、`senderCityName`、`senderDisplayName` (`senderDistrictName` 已有)，`CaseService.toDetail/toSummary` 以 `SecurityContextHolder` 判斷 `ROLE_VIEWER` 時遮蔽 `senderName/phone/address` 為 null，保留縣市鄉鎮，驗證以 `WithMockUser (roles=VIEWER)` slice test 為準
- [x] 3.2 `CaseService.statistics` 調整「不重複送件人」為 `COALESCE (phone, displayName)` distinct 計數，驗證以 `CaseServiceTest` 統計去重為準
- [x] 3.3 `CaseService.create` 改用 `senderId` 沿用或 `search` 候選人工確認，不再以 `name+phone` 強合併，驗證以 `CaseServiceTest` 建案沿用/新建分支為準

## 4. 後端控制器

- [x] 4.1 新增 `SenderController` (`@RequestMapping ("/api/senders")`)：`GET /search?q=` (登入即可)、`GET /` 列表、`GET /{id}` 詳細 (ADMIN 或登入？依 spec 搜尋供建案使用，登入即可)、`DELETE /{id}` (`@PreAuthorize ("hasRole ('ADMIN')")`)，回 200/204/401/403/404/409，驗證以 `MockMvc` 401/403/400/409/200 為準
- [x] 4.2 補齊參數與錯誤語意：`q` 空白 400、`id` 非法 400、不存在 404、被引用刪除 409，驗證以 `SenderControllerTest` slice test 為準

## 5. 前端 (管理頁、去重候選、遮蔽)

- [x] 5.1 `frontend/src/api/index.ts` 新增 `senderApi.search (q)`、`list ()`、`detail (id)`、`remove (id)` (路徑與後端一致)，驗證以 `npm run build` (`vue-tsc`) 通過為準
- [x] 5.2 新增 `frontend/src/views/SendersView.vue` (路由 `/admin/senders`，`meta.adminOnly`)，表格 (`name/displayName/phone/district/city`)+ 搜尋 + ADMIN 刪除，驗證以 `admin` 可刪除且 `STAFF` 不可見為準
- [x] 5.3 `CaseFormView` 送件人區塊整合去重候選：輸入 `name/phone` 時調 `senderApi.search`，以 `Swal` 列候選供「沿用」或「新建」，選沿用時帶 `senderId`，驗證以 `admin` 建案候選彈窗為準
- [x] 5.4 `CasesView`/`CaseDetailView` 依 `auth.isViewer` 遮蔽送件人姓名/電話/地址 (縣市鄉鎮保留)，`CaseFormView` 的 `displayName` 顯示支援 `name (displayName)`，驗證以 `viewer` 登入案件列表/詳細不見個資為準
- [x] 5.5 更新 `frontend/src/router/index.ts` 與導覽列 (`App.vue`)，對 `ADMIN` 顯示「送件人管理」入口，`senderId` 相關路由守衛，驗證以路由 403/重導為準
- [x] 5.6 重新生成 `frontend/src/types/api.ts` (`npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.ts`)，驗證 `senderId` 與 `Senders` 端點出現在型別檔

## 6. 測試與文件

- [x] 6.1 後端切片：`SenderControllerTest` 覆蓋 ADMIN 200、非 ADMIN 403、未登入 401、刪除被引用 409、搜尋 200，驗證 `cd backend && ./mvnw test -Dtest=SenderControllerTest` 通過
- [x] 6.2 後端整合測試：在 `PhytoTrackIntegrationTest` (或新增 `SenderIntegrationTest`) 覆蓋「搜尋候選回沿用」「刪除未被引用 204」「刪除被引用 409」「VIEWER 遮蔽」「統計去重鍵」，驗證 `./mvnw test` 全量通過
- [x] 6.3 前端驗證：`cd frontend && npm run build` (含 `vue-tsc`) 與 `npm test` 通過
- [x] 6.4 同步 `docs/ARCHITECTURE.md` (新增 senders 端點、遮蔽與去重說明) 與 `AGENTS.md`/`docs/REQUIREMENTS.md` 標記 (僅於 apply 階段)，驗證 `openspec validate --specs --changes` 通過
