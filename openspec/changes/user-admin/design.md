# Design: user-admin 使用者角色、啟停用與重設密碼

## Context

見 `proposal.md`（Why）。現況：`User` 已含 `active`（預設 `true`）與 `Role`（`ROLE_VIEWER/STAFF/ADMIN`）；`UserAdminController` 僅 `GET /api/admin/users`（`@PreAuthorize("hasRole('ADMIN')")`）；`JwtAuthenticationFilter` 以 token 內 `role`/`userId` 直建 `UserPrincipal`（`active=true` 寫死，無 DB 檢查，無狀態設計註記於 `docs/ARCHITECTURE.md` 請求流程）；`AuthService.login` 未檢查 `active`；`UserResponse` 不含 `active`；前端 `UsersView` 僅表格顯示，無操作。

本 change 需在不破壞既有 JWT 無狀態優點過多的前提下，滿足 spec「停用後既有 token 立即失效」與「角色變更後續請求生效」。

## Goals / Non-Goals

**Goals：**
- ADMIN 可變更角色、啟停用帳號、重設他人密碼；操作後行為符合 `user-admin/spec.md` 三組 Scenario
- 停用帳號：登入被拒、既有 token 後續請求被拒（401）
- 前端管理頁可完成上述操作（角色下拉、啟停用開關、重設密碼對話框）

**Non-Goals：**
- 帳號批次匯入/匯出、稽核日誌、密碼策略（強度、過期）、自助變更密碼／忘記密碼流程
- 行為審計、操作者不可停用自身／不可移除最後一位 ADMIN 等進階保護（僅作風險提示，不阻擋 MVP）
- `active` 以外的使用者欄位編輯（顯示名稱／信箱等）

## Decisions

1. **API 設計（皆 `ADMIN`）**
   - `PATCH /api/admin/users/{id}/role` + `RoleUpdateRequest(role)`：`role` 為 `ROLE_VIEWER | ROLE_STAFF | ROLE_ADMIN` 列舉字串，`@NotBlank`，非法值 400
   - `PATCH /api/admin/users/{id}/active` + `ActiveUpdateRequest(active)`：`active` 為 `Boolean`，`@NotNull`
   - `POST /api/admin/users/{id}/reset-password` + `ResetPasswordRequest(newPassword)`：`newPassword` 6–72 字元，BCrypt 雜湊後存儲；回 204 或 200（不回密碼明文）
   - 理由：PATCH 表部分更新語意；重設密碼用 POST 避免冪等誤解。皆以 `@PreAuthorize("hasRole('ADMIN')")` 控管，`@Valid` 驗證。

2. **停用後 token 立即失效：filter 改查 DB**
   - 變更 `JwtAuthenticationFilter.doFilterInternal`：token 驗證通過後，以 `claims.get("userId")` 查 `UserRepository.findById`；若不存在或 `!active`，不寫入 `SecurityContext`（走 `RestAuthenticationEntryPoint` 401）；否則以 DB 實體的 `role`/`active` 建 `UserPrincipal`（覆蓋 token 內 role，實現角色變更即時生效）
   - 替代方案捨棄：(a) 維持無狀態僅縮短 `expirationMs` —— 不滿足「立即失效」；(b) token 黑名單／版本號 —— 需額外存儲與清理，SQLite 場景過度設計；(c) 僅 `CustomUserDetailsService` 檢查 —— filter 不走該路徑，`permitAll` 以外仍會放行
   - 影響：每認證請求多一次 `findById`（主鍵查詢，Hikari pool 1 對 SQLite 可接受）；角色變更與停用即時生效

3. **登入時 active 檢查**
   - `AuthService.login` 在 `authenticationManager.authenticate` 成功後、簽發 token 前，檢查 `user.isActive()`；若 `false` 拋 `ApiException("ACCOUNT_DISABLED", 403/401, "帳號已停用")`，由 `GlobalExceptionHandler` 轉 4xx
   - 理由：避免停用帳號仍拿新 token；與 filter 的 DB 檢查互補（新舊 token 皆擋）

4. **服務層職責**
   - 在 `AuthService` 新增 `updateRole`/`updateActive`/`resetPassword`，或抽 `UserAdminService`；擇一保持 `UserAdminController` 薄。`UserResponse` 擴充 `active`（`Boolean`/`boolean`），`toResponse` 同步
   - DTO 採 `record` + Bean Validation，保持與 `AuthDtos` 一致風格

5. **前端**
   - `frontend/src/views/UsersView.vue`：每列加角色 `<select>`（三選項）、`active` 切換（按鈕或 switch）、重設密碼（彈窗輸入新密碼）；`frontend/src/api/index.ts` 補 `userApi.updateRole/updateActive/resetPassword`
   - 權限：路由與按鈕僅 `ROLE_ADMIN` 可見（沿用 `auth.isAdmin`）；操作失敗由 axios 攔截器統一提示

## Risks / Trade-offs

- 每請求查 DB 取代純無狀態 → 延遲略增但主鍵查詢可控；若日後導入 Redis，可快取 `userId→active/role` 並於變更時失效
- 停用自身帳號 → 管理者可能把自己鎖死；MVP 不阻擋，文件提示「勿停用當前登入帳號」，後續可加「禁止操作自身 `active=false`」檢查
- 最後一位 ADMIN 被降權/停用 → 系統無管理者；MVP 僅提示，後續可加「至少保留一位 active ADMIN」校驗
- 併發角色/狀態變更 → 以 DB 最終值為準，無需鎖；`@Transactional` 保證單次更新原子性
- 既有 token 內 role 與 DB 不一致的短暫窗口 → 已由 filter DB 覆蓋消除

## Migration Plan

- 無 schema 變更（`users.active` 已存在，預設 `true` 兼容既有資料）
- 部署：後端先上線（filter 與 login 檢查兼容舊 token——舊 token 對應的 user 仍 `active=true` 故放行；停用後新邏輯立即拒絕）；前端隨後發布
- 回滾：還原 filter 與 controller 即可；已停用的帳號保持 `active=false`，不影響回滾後登入以外的功能

## Open Questions

- 是否禁止 ADMIN 操作自身帳號的停用/降權？（需產品決策；不影響本 change 任務拆分，預設不禁止，僅文件提示）
- 重設密碼後是否需強制該使用者下次登入變更密碼？（本次不做，僅重設為新密碼可立即登入）
