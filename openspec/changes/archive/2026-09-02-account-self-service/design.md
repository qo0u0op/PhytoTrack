## Context

見 proposal.md Why。現有 `user-admin` 僅提供 ADMIN 對角色／啟停用／重設密碼的管理，缺乏各角色自助維護顯示名稱／信箱與密碼變更、信箱去重與停用申請流程。後端 `User` 已有 `email` 欄位但未作唯一去重，前端無個人帳號頁。

## Goals / Non-Goals

**Goals:** 提供各角色自助帳號管理：顯示名稱／信箱編輯（全域唯一、大小寫不敏感）、密碼修改（非 ADMIN 需現密碼）、停用請求與 ADMIN 審核。

**Non-Goals:** 不處理註冊流程變更、信箱驗證信寄送、帳號復啟自助申請（僅停用請求）、第三方 OAuth。

## Decisions

- **Decision: 信箱唯一性以 DB 唯一索引 + 服務層檢查** — 新增 `UNIQUE(email COLLATE NOCASE)`（SQLite `COLLATE NOCASE`）與 `existsByEmailIgnoreCase` 檢查，更新時排除自身。替代：僅應用層檢查，不選，因併發仍可能重複。
- **Decision: 密碼修改區分 self vs admin** — `PUT /api/account/password` 需 `currentPassword`（除非 ADMIN 改他人），`PUT /api/admin/users/{id}/password` 保持現有重設語意。選擇：自助端點統一比對現密碼，ADMIN 例外以符合需求「管理員除外」。
- **Decision: 停用請求以獨立表 `deactivate_requests`** — 欄位 `id/user_id/status( PENDING/APPROVED/REJECTED)/created_at`，審核通過即呼叫既有停用邏輯（token 失效）。替代：直接標記 user 停用，不選，無法審核與稽核。
- **Decision: 前端獨立 `AccountView.vue`** — 路由 `/account` 供所有角色，導覽由 `App.vue` 依登入狀態顯示；表單分頁為「個人資料」「密碼」「停用請求」。替代：併入現有 UsersView，不選，權限與用途不同。

## Risks / Trade-offs

- [信箱枚舉] → 錯誤訊息統一為 `EMAIL_DUPLICATE`，不洩漏是否為自身重複外之帳號細節，並限制頻率
- [舊 token 仍有效至過期] → 停用後於 `JwtAuthFilter` 檢查 `user.enabled`，立即拒絕
- [同時修改信箱競態] → DB 唯一索引為最終防線，捕獲 `DataIntegrityViolationException` 轉 409

## Migration Plan

- 新增 `deactivate_requests` 表與 `users.email` 唯一索引（對既有重複信箱先以 `id` 最小者保留，其餘置空或加後綴，依實作時檢查）
- 後端新增 `AccountController` 與 `DeactivateRequestRepository/Service`，前端新增路由與導覽
- 回滾：移除新端點與前端頁面，保留 DB 索引與表不影響既有功能

## Open Questions

- 無
