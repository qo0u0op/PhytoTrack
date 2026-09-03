## Why

註冊目前只在後端檢查帳號重複（前端以全域 alert 呈現），且信箱完全不查重，導致使用者要送出後才經由彈窗得知帳號或信箱已被使用，體驗差且與個人資料自助編輯的信箱唯一契約不一致。

追加範圍（同次修訂）：註冊頁除帳密重複外，密碼不一致、欄位驗證等仍走全域 alert；註冊信箱僅靠 `type=email`、無 regex 即時檢查（帳號管理頁與後端 `@Email` 已有）；使用者管理缺搜尋、停用者混列難找；簽名人篩選列常駐佔位、欄序（身分別在前）與狀態徽章樣式和使用者管理不一致；帳號管理儲存鈕綁「檢查按鈕」而非「是否有修改」；註冊密碼無顯示/隱藏切換。

## What Changes

- `POST /api/auth/register` 新增信箱查重：非空信箱大小寫不敏感全域唯一，重複時回 `409 EMAIL_TAKEN`（與 `PUT /api/account/profile` 的 `EMAIL_DUPLICATE` 語意對齊，錯誤碼沿用註冊語境命名）。
- 新增公開可用性查詢端點，供註冊頁即時檢查（未登入可呼叫，僅回可用性布林值，不洩漏其他個資）：
  - `GET /api/auth/check-username?username=` → `{ available }`（去空白後比對，空值視為不可用）。
  - `GET /api/auth/check-email?email=` → `{ available }`（空值視為可用，因信箱選填；非空時去空白、大小寫不敏感比對）。
- `RegisterView.vue` 全表單改 inline error：帳號/信箱（debounce 可用性檢查）、密碼不一致、欄位格式、送出時 `USERNAME_TAKEN`/`EMAIL_TAKEN`/其他後端錯誤皆映射到對應欄位下方，不再經由全域 alert 彈窗（註冊成功導登入提示維持）。
- 註冊與帳號管理的信箱欄位共用同一前端 email regex 即時檢查（與後端 `@Email` 語意對齊：`RegisterView` 新增，`AccountView` 沿用既有）。
- `RegisterView.vue` 密碼與確認密碼欄位加眼睛 icon 切換顯示/隱藏。
- `UsersView.vue`（使用者管理）新增搜尋框（帳號/顯示名稱/信箱模糊比對）與「顯示已停用」勾選框（預設不勾＝僅列啟用者）；狀態徽章沿用既有啟用/停用樣式。
- `AccountView.vue` 儲存鈕改為 dirty 判斷：email 或顯示名稱任一與載入值不同即顯示儲存按鈕（仍需通過既有檢查才可送出）。
- `SignersView.vue`（簽名人管理）：篩選列收進預設隱藏的篩選抽屜；欄序對調為帳號在前、身分別在後；狀態徽章改與使用者管理同款（`text-bg-success`/`text-bg-secondary`＋啟用/停用語意對應）。

## Capabilities

### New Capabilities

- 無（皆為既有行為擴充與 UI 一致性調整）

### Modified Capabilities

- `user-admin`: 註冊信箱唯一性、公開帳號/信箱可用性查詢、註冊全表單 inline 錯誤＋密碼可見切換、信箱 regex、使用者清單搜尋與停用顯示、帳號 dirty 儲存鈕
- `reference-data-admin`: 簽名人篩選抽屜、欄序、狀態徽章同款（spec 見 `specs/reference-data-admin/spec.md`）

## Impact

- 後端：`AuthService.register`（信箱查重）、`AuthController`（新增兩個公開 `GET`）、`SecurityConfig`（公開端點放行）、`UserRepository`（已有 `existsByEmailIgnoreCase`，需新增帳號查詢方法）。`@Email` 後端既有，無需更動。
- 前端：`RegisterView.vue`（全表單 inline＋debounce 檢查＋密碼眼睛 icon＋email regex）、`api/index.ts`（新增 `checkUsername/checkEmail`＋共用 email regex 常數）、`UsersView.vue`（搜尋＋顯示已停用勾選）、`AccountView.vue`（dirty 儲存鈕）、`SignersView.vue`（篩選抽屜＋欄序＋徽章）。
- 相容性：註冊既有 `USERNAME_TAKEN` 不變；新增 `EMAIL_TAKEN` 409 分支；可用性端點為新增，無破壞性；使用者清單預設隱藏停用者為顯示行為變更（資料仍在，勾選即回）。
- 待辦：`specs/reference-data-admin/spec.md` 已建立（含篩選抽屜、欄序、徽章三需求），可直接 apply 實作。
