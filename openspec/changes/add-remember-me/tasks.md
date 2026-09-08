## 1. 後端契約與簽發

- [x] 1.1 擴充 `dto/AuthDtos.java:LoginRequest` 新增 `Boolean rememberMe`（選填，缺省視為 `false`），`controller/AuthController.java` 透傳至 service，`mvn test -Dtest=AuthControllerTest` 綠燈
- [x] 1.2 於 `security/JwtTokenProvider.java` 新增 `generateToken(User, boolean rememberMe)` 與 `rememberMeExpirationMs`（`@Value("${app.jwt.remember-me-expiration-ms:604800000}")`），舊 `generateToken(User)` 委派 `false`，驗證 `exp - iat` 在 `rememberMe=true` 時約 7 天（±60s）
- [x] 1.3 於 `application.yaml:77` 新增 `app.jwt.remember-me-expiration-ms: ${JWT_REMEMBER_ME_EXPIRATION_MS:604800000}`，`phytotrack.toml.example` 補 `[app.jwt] remember-me-expiration-ms` 註解，`mvn test` 綠燈
- [x] 1.4 於 `service/AuthService.java:login` 依 `request.rememberMe()` 分支簽發（`Boolean.TRUE.equals`），停用/密碼錯誤等分支維持原回碼，補 `AuthServiceTest.rememberMe true→7天/false→1小時` 斷言

## 2. 前端持久化與表單

- [x] 2.1 擴充 `frontend/src/api/index.ts` `authApi.login` 型別為 `{ username, password, rememberMe?: boolean }`，`npm run build`（`vue-tsc`）通過
- [x] 2.2 重構 `frontend/src/stores/auth.ts`：`state` 初始化同時讀 `localStorage/sessionStorage`（local 優先），`setAuth(token, user, rememberMe)` 依旗標主寫 `localStorage` 或 `sessionStorage` 並清另一側，`logout()` 雙清，`vitest` 新增 rememberMe 分支測試
- [x] 2.3 於 `frontend/src/views/LoginView.vue` 新增「記住我」核取方塊（`form.rememberMe = ref(false)`），`submit()` 以 `{...form}` 傳遞，`npm test` 與 `playwright` 登入流程綠燈
- [x] 2.4 登出保留 `lastUsername`：`stores/auth.ts` 新增恆存 `localStorage` 的 `lastUsername`（`setAuth` 更新、`logout` 不清除），`LoginView` 以 `auth.lastUsername` 預填帳號欄，`vitest` 15 passed

## 3. 驗證與文件

- [x] 3.1 後端整合驗證：`mvn test` 146+ 綠燈，`curl POST /api/auth/login {"rememberMe":true}` 取 token 解 `exp` 為 7 天，`{"rememberMe":false}` 為 1 小時，舊 payload（無欄位）仍 1 小時
- [x] 3.2 前端整合驗證：勾選後關閉瀏覽器重開仍保持登入（`localStorage`），未勾選關閉分頁後需重新登入（`sessionStorage`），`openspec validate --specs --changes` 15+1 passed
