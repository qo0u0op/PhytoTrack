## Why

現有使用者管理僅提供清單查詢，管理者無法調整角色、啟停用帳號或重設密碼；若人員異動或帳號遭濫用，需直接操作資料庫，缺乏稽核與權限控管。依 `openspec/specs/user-admin/spec.md` 需補齊此帳號管理能力，並滿足「停用後既有 token 立即失效」等安全需求。

## What Changes

- 後端新增帳號管理端點（皆限 `ADMIN`）：
  - 調整角色：`PATCH /api/admin/users/{id}/role`（body 含目標 `role`）
  - 啟停用帳號：`PATCH /api/admin/users/{id}/active`（body 含 `active`）
  - 重設密碼：`POST /api/admin/users/{id}/reset-password`（body 含 `newPassword`）
- `UserResponse` 擴充 `active` 欄位；新增請求 DTO（角色、啟停用、重設密碼）與驗證
- 登入流程檢查 `active`：已停用帳號拒絕登入；JWT 驗證改為每請求查 DB 驗證 `active`，停用帳號的既有 token 於後續請求被拒（401）
- `UserAdminController` 與服務層（`AuthService` 或新增 `UserAdminService`）實作上述邏輯；調整 `JwtAuthenticationFilter`
- 前端 `UsersView` 新增角色下拉、啟停用切換、重設密碼對話框；`api/index.ts` 補對應 `userApi` 方法

## Capabilities

### New Capabilities

<!-- 主規格已含 user-admin，本 change 採 skip_specs: true，不新增 capability 檔案 -->

### Modified Capabilities

<!-- 本 change 不修改 spec 需求，僅實作主規格已定義的 user-admin 需求 -->

## Impact

- 後端：`User`（既有 `active` 欄位）、`UserRepository`、`UserAdminController`、`AuthService`/`UserAdminService`、`JwtAuthenticationFilter`、`JwtTokenProvider`、`UserPrincipal`、`AuthDtos`、`SecurityConfig`、`GlobalExceptionHandler`（如需錯誤碼）
- 前端：`frontend/src/views/UsersView.vue`、`frontend/src/api/index.ts`、`frontend/src/types/api.ts`（重新生成）
- 文件與測試：`docs/REQUIREMENTS.md`（標記待實作→已實作僅於 apply 階段）、`openspec validate`、新增 `UserAdminControllerTest`/`AuthService` 相關測試與整合測試
