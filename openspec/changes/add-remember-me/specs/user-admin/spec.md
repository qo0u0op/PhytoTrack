## ADDED Requirements

### Requirement: 登入記住我選項

`POST /api/auth/login` SHALL 接受選填 `rememberMe: boolean`（預設 `false`，勾選時 token 時效為 7 天），前端 `LoginView` SHALL 提供「記住我」核取方塊，未勾選時維持既有行為；`register` 與 `availability` 端點不變。後端 SHALL 於 `rememberMe` 缺省時視為 `false`，以確保舊客戶端相容。

#### Scenario: 前端勾選並傳遞參數
- **WHEN** 使用者在 `LoginView` 勾選「記住我」並送出帳密
- **THEN** `POST /api/auth/login` 請求主體含 `rememberMe: true`，且成功後 `useAuthStore` 將 `token`/`user` 同步寫入 `localStorage`（關閉瀏覽器後仍有效）

#### Scenario: 前端未勾選
- **WHEN** 使用者未勾選「記住我」並登入
- **THEN** 請求含 `rememberMe: false`（或省略），`useAuthStore` 寫入 `sessionStorage`，關閉分頁後 token 失效需重新登入

#### Scenario: 舊客戶端相容
- **WHEN** 舊版前端僅送 `{"username","password"}` 未帶 `rememberMe`
- **THEN** 後端回 `200` 且簽發 1 小時短效 token，不回 `400`

#### Scenario: 登出清除兩處儲存但保留帳號
- **WHEN** 使用者呼叫 `useAuthStore.logout()`（點擊登出）
- **THEN** `localStorage` 與 `sessionStorage` 的 `token`/`user` 皆被清除，`isAuthenticated` 為 `false`，但 `lastUsername`（上次登入帳號）保留於 `localStorage`

#### Scenario: 登入頁自動帶入上次帳號
- **WHEN** 使用者登出後重新開啟 `LoginView`
- **THEN** 帳號欄位自動帶入 `lastUsername`，密碼為空，使用者僅需輸入密碼即可登入
