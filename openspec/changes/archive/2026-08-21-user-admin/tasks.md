## 1. 後端契約 (DTO 與回應)

- [x] 1.1 擴充 `AuthDtos.UserResponse` 新增 `active` (`boolean`/`Boolean`)，更新 `AuthService.toResponse` 與 `UserAdminController` 回應，並以 `openapi-typescript` 產物可見為驗收
- [x] 1.2 新增 `RoleUpdateRequest` (`role` 必填，限 `ROLE_VIEWER|ROLE_STAFF|ROLE_ADMIN`)、`ActiveUpdateRequest` (`active` 必填)、`ResetPasswordRequest` (`newPassword` 6–72 字元) 並加 `@NotBlank/@NotNull/@Size` 驗證，驗證以 `@Valid` 400 為準

## 2. 後端核心邏輯 (服務與安全)

- [x] 2.1 `AuthService.login` 增加 `active` 檢查：停用帳號拒絕簽發 token，拋 `ACCOUNT_DISABLED` 並由 `GlobalExceptionHandler` 回 4xx，驗證以停用帳號登入 403/401 為準
- [x] 2.2 實作角色/啟停用/重設密碼服務：`AuthService` (或 `UserAdminService`) 新增 `updateRole (id, role)`、`updateActive (id, active)`、`resetPassword (id, newPassword)` (BCrypt 雜湊)，皆 `@Transactional`，驗證以對應單元測試與 repository 查詢為準
- [x] 2.3 修改 `JwtAuthenticationFilter`：驗證 token 後以 `userId` 查 `UserRepository`，若 `active==false` 或不存在則不寫入 `SecurityContext` (後續請求 401)；否則以 DB 的 `role/active` 建 `UserPrincipal` 覆蓋 token 內 role，驗證以停用後舊 token 打受保護 API 401 為準

## 3. 後端控制器

- [x] 3.1 `UserAdminController` 新增 `PATCH /api/admin/users/{id}/role`、`PATCH /api/admin/users/{id}/active`、`POST /api/admin/users/{id}/reset-password` (皆 `@PreAuthorize ("hasRole ('ADMIN')")`、`@Valid`)，回 `UserResponse` 或 204，驗證以 `MockMvc` 401/403/400/200 為準
- [x] 3.2 補齊參數與錯誤語意：`@PathVariable id` 正整數、`RoleUpdateRequest` 非法列舉 400、重設密碼長度不足 400、目標使用者不存在 404，驗證以 controller slice test 為準

## 4. 前端 (管理頁與 API)

- [x] 4.1 `frontend/src/api/index.ts` 補 `userApi.updateRole (id, role)`、`updateActive (id, active)`、`resetPassword (id, newPassword)` (路徑與後端一致)，驗證以 `npm run build` (`vue-tsc`) 通過為準
- [x] 4.2 `frontend/src/views/UsersView.vue` 擴充：每列角色 `<select>`、啟停用切換按鈕、重設密碼對話框 (輸入新密碼 + 確認)，操作後刷新列表，驗證以手動在 dev 以 admin 登入可完成三操作且非 ADMIN 不可見為準
- [x] 4.3 重新生成 `frontend/src/types/api.ts` (`npx openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.ts`)，驗證 `active` 與三端點出現在型別檔

## 5. 測試

- [x] 5.1 後端單元/切片：`AuthService` 登入停用拒絕、`UserAdminControllerTest` (ADMIN 200、非 ADMIN 403、未登入 401、非法 role 400)、`JwtAuthenticationFilter` 停用 token 失效，驗證 `cd backend && ./mvnw test -Dtest=UserAdminControllerTest,AuthServiceTest` 通過
- [x] 5.2 後端整合測試：在 `PhytoTrackIntegrationTest` (或新增 `UserAdminIntegrationTest`) 覆蓋「停用後登入失敗」「停用後舊 token 打 `/api/cases` 401」「重設密碼後新密碼可登入」「變更角色後後續請求權限生效」，驗證 `./mvnw test` 全量通過
- [x] 5.3 前端驗證：`cd frontend && npm run build` (含 `vue-tsc`) 與 `npm test` (如有) 通過

## 6. 文件與收尾

- [x] 6.1 同步 `docs/ARCHITECTURE.md` (新增 3 端點至 API 一覽、說明 filter 的 DB 檢查與 active 語意) 與 `AGENTS.md`/`docs/REQUIREMENTS.md` 標記 (僅於 apply 階段)，驗證 `openspec validate --specs --changes` 通過
