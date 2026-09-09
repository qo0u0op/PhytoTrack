## Context

現行 `JwtTokenProvider` 僅單一 `expirationMs`（`application.yaml:77` 3600000），`AuthDtos.LoginRequest` 僅 `username/password`，`LoginView.vue` 無選項，`useAuthStore` 固定寫 `localStorage`。需在不破壞既有 1 小時語意與測試的前提下分支為雙時效，並讓前端依使用者選擇決定持久化層級。

## Goals / Non-Goals

**Goals:**
- 記住我勾選時簽發 7 天 token，未勾選維持 1 小時；長效可由設定覆蓋
- 前端持久化與時效一致：勾選 `localStorage`，未勾 `sessionStorage`
- 舊客戶端（無 `rememberMe`）相容，零破壞

**Non-Goals:**
- 不做 `Refresh Token` / `滑動過期` / `HttpOnly Cookie`（維持現行 `Authorization: Bearer` 無狀態）
- 不持久化 `rememberMe` 偏好至後端或 DB，僅當次登入有效
- 不改註冊、密碼重設、帳號停用等其他 `user-admin` 行為

## Decisions

- **雙時效由後端簽發決定**：`JwtTokenProvider.generateToken(User, boolean)` 內 `long exp = rememberMe ? rememberMeExpirationMs : expirationMs`，`@Value("${app.jwt.remember-me-expiration-ms:604800000}")` 注入。替代：前端自行延長 `exp` → 簽章可偽造 → 捨棄。
- **DTO 選填 boolean**：`LoginRequest(Boolean rememberMe)`（`Boolean` 非 `boolean` 以容許缺省）+ `isRememberMe()` 歸一 `Boolean.TRUE.equals`。替代：新端點 `/login/remember` → 多餘 → 捨棄。舊單參 `JwtTokenProvider.generateToken(User)` 保留委派 `false` 以兼容 `AuthServiceTest`。
- **前端儲存分流**：`useAuthStore.setAuth(token, user, rememberMe)` 同寫 `localStorage/sessionStorage` 一份為主、一份清；`state` 初始化 `localStorage.getItem('token') ?? sessionStorage.getItem('token')`（local 優先）；`logout()` 雙清。替代：`Cookie` + `HttpOnly` → 需後端 `Set-Cookie` 與 `CORS credentials`，改動大 → 捨棄。
- **Vue 表單**：`LoginView.vue` `form.rememberMe`（`ref false`）+ `<input type="checkbox">`，`authApi.login` 透傳。`frontend/src/api/index.ts` 型別擴充 `LoginPayload { username, password, rememberMe? }`。

## Risks / Trade-offs

- [長效 token 洩漏風險] 7 天有效期拉長攻擊窗口 → Mitigation：預設不勾（1 小時），長效由使用者顯式選擇；未來可補 `remember-me-expiration-ms` 縮短或加入黑名單
- [雙儲存一致性] `localStorage` 與 `sessionStorage` 同步需雙清 → Mitigation：`setAuth`/`logout` 集中處理，`isAuthenticated` 讀取時雙源判斷
- [時間校準] `exp - iat` 容差 → 測試以 `±60s` 斷言，不以絕對值

## Migration Plan

1. 後端 DTO/Provider/Config → Service/Controller → 前端 store/api/view 依序落地，每步 `mvn test` / `npm test`
2. 舊 token（1 小時）自然過期，無需遷移；新 `phytotrack.toml.example` 補 `app.jwt.remember-me-expiration-ms`
3. 回滾：移除 `rememberMe` 欄位與 `remember-me-expiration-ms`，前端回退至單 `localStorage` 即可

## Open Questions

- 無
