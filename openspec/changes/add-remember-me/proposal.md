## Why

目前登入有效期限固定為 1 小時，LAN 單機情境下使用者需頻繁重新輸入帳密，體驗中斷且對每日大量建案的 STAFF/ADMIN 效率影響顯著。需引入「記住我」讓使用者自主選擇短期或長期有效期，減少重複登入同時兼顧安全性（預設仍短效）。

## What Changes

- **後端登入契約擴充**：`AuthDtos.LoginRequest` 新增 `rememberMe: boolean`（選填，預設 `false`），`AuthController`/`AuthService.login` 依此選擇 JWT 時效
- **雙時效 JWT**：`JwtTokenProvider` 新增 `generateToken(User, boolean rememberMe)` 多載，`rememberMe=false` 維持 `app.jwt.expiration-ms`（3600000），`true` 使用 `app.jwt.remember-me-expiration-ms`（預設 7 天 = 604800000），舊單參多載保留相容
- **設定新增**：`application.yaml` 新增 `app.jwt.remember-me-expiration-ms`（`${JWT_REMEMBER_ME_EXPIRATION_MS:604800000}`），可由 `phytotrack.toml` 與環境變數覆蓋
- **前端登入表單**：`LoginView.vue` 新增「記住我」核取方塊（勾選時 token 時效 7 天），預設不勾，送 `rememberMe` 至 `authApi.login`；`RegisterView` 不變
- **前端儲存策略**：`useAuthStore` 依 `rememberMe` 決定持久化為 `localStorage`（勾選）或 `sessionStorage`（未勾，關閉分頁失效），`setAuth(token, user, rememberMe)` 與 `logout()` 同步清理兩處，並於 `state` 初始化時同時讀取 `localStorage/sessionStorage`
- **無破壞性**：未帶 `rememberMe` 的舊請求視為 `false`，既有測試與 API 回應格式不變

## Capabilities

### New Capabilities
<!-- 無 — 記住我為既有登入流程的參數分支，不引入新領域 -->

### Modified Capabilities
- `security-hardening`: JWT 雙時效與 `remember-me-expiration-ms` 設定，維持 `issuer=phytotrack` 與 `BCrypt(12)` 不變
- `user-admin`: 登入請求 `rememberMe` 參數與前端持久化策略，對應 `POST /api/auth/login`

## Impact

- 後端：`dto/AuthDtos.java:LoginRequest`、`security/JwtTokenProvider.java:generateToken`、`service/AuthService.java:login`、`controller/AuthController.java`、`application.yaml:77`、`phytotrack.toml.example`
- 前端：`views/LoginView.vue`、`stores/auth.ts`、`api/index.ts`（`authApi.login` 參數）
- 測試：`AuthControllerTest`、`AuthServiceTest`、`LoginView` / `auth store` 單元測試新增 rememberMe 分支
- 部署：無 DB 變更，環境變數 `JWT_REMEMBER_ME_EXPIRATION_MS` 可選覆蓋；既有 1 小時 token 仍有效直至過期
