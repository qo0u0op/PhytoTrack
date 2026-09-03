## Context

見 `proposal.md - Why`。現況：`AuthService.register` 只檢查帳號（`USERNAME_TAKEN`），信箱刻意不檢查（註解「僅由前端檢查按鈕提示」）；`GET /api/account/check-email` 需登入，註冊頁（未登入）無法使用；`RegisterView.vue` 現全無 inline 錯誤——密碼不一致用 `Swal`、送出錯誤靠 `http.ts` 攔截器 `Swal`，且信箱僅 `type=email` 無 regex；`UserRepository` 已有 `existsByEmailIgnoreCase`；後端 `RegisterRequest`/`UpdateProfileRequest` 已有 `@Email`。`AccountView.vue` 的 `canSave()` 綁「檢查按鈕結果」而非 dirty；`UsersView.vue` 無搜尋、停用者混列；`SignersView.vue` 篩選列常駐、欄序為身分別→帳號、狀態徽章為 `bg-primary`/`bg-secondary`（使用者/非使用者）。

## Goals / Non-Goals

**Goals:**
- 註冊信箱與個人資料編輯一致（非空、去空白、大小寫不敏感唯一）。
- 註冊頁未登入即可即時查帳號/信箱可用性，且僅洩漏布林值。
- 註冊全表單錯誤（重複、格式、密碼不一致、送出映射）只出現在欄位下方，不再全域 alert。
- 前端 email regex 與後端 `@Email` 語意對齊；使用者清單可搜尋、可切換停用顯示；帳號儲存鈕跟 dirty；簽名人 UI 與使用者管理同款。

**Non-Goals:**
- 登入錯誤訊息維持統一（防帳號列舉，不在此變更）。
- 個人資料頁既有 `check-email` 行為不變（僅 `AccountView` 儲存鈕顯示邏輯改 dirty）。
- 不改後端 email 驗證規則（`@Email` 已足夠，不新增 regex）。

## Decisions

### D1 信箱查重放 `AuthService.register`（Service 層，與帳號檢查同處）
- **選擇**：非空（去空白後非空）才以 `existsByEmailIgnoreCase` 檢查，重複拋 `EMAIL_TAKEN` 409；空值直接建。與 `AccountService.isEmailAvailable` 語意一致（大小寫不敏感、空值可用）。
- **替代考慮**：DB 唯一約束——SQLite 大小寫敏感且既有重複資料需遷移，應用層檢查即可，交易競態窗口可接受（註冊非高併發）。

### D2 可用性端點掛 `AuthController`（`/api/auth/...`，`permitAll`）
- **選擇**：`GET /api/auth/check-username`、`GET /api/auth/check-email`，回 `{ available, username/email }`，僅布林值；_username 去空白後為空 → `available=false`（避免空帳號被視為可用）；email 空 → `true`（選填）。`SecurityConfig` 在既有 `permitAll` 清單追加兩路徑。
- **替代考慮**：複用 `/api/account/check-email` 並放行——會改變既有需登入端點語意且回傳含 `principal` 邏輯，不如新增註冊語境端點。

### D3 帳號查詢方法新增 `existsByUsername`（或複用 `findByUsername`）
- **選擇**：`UserRepository` 新增 `existsByUsername(String)`（衍生查詢），與既有 `existsByEmailIgnoreCase` 對稱；`trim` 在 Service 層做（與 register 一致）。
- **替代考慮**：直接用 `findByUsername().isPresent()`——多載入實體，`exists` 更省。

### D4 前端全表單 inline error（`RegisterView.vue`，不動全域攔截器）
- **選擇**：全欄位各加 `ref` 錯誤字串：帳號/信箱 debounce（約 400–600ms）失焦/輸入檢查；密碼/確認密碼以 `watch` 即時比對；送出 `catch` 中若錯誤 `code` 可映射到欄位（`USERNAME_TAKEN`→帳號、`EMAIL_TAKEN`/格式→信箱等）則寫入對應欄位並 `return`（吞掉，不再向上拋給攔截器），未知錯誤重新拋出維持攔截器行為；註冊成功導登入提示維持 `Swal`。送出按鈕在任一欄位有錯時 `disabled`（可選，至少阻擋重複送出）。
- **替代考慮**：改全域攔截器跳過 409——影響所有頁面，不如在註冊頁局部處理。

### D5 共用 email regex（前端常數，後端不動）
- **選擇**：`api/index.ts`（或 `utils/`）新增 `EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/` 常數（沿用 `AccountView` 既有式樣），`RegisterView` 與 `AccountView` 共用；後端 `@Email` 已覆蓋，不新增後端 regex。
- **替代考慮**：後端加嚴 regex——`@Email` 語意已足夠且改動驗證規則風險高，不做。

### D6 密碼眼睛 icon（`RegisterView.vue` 局部）
- **選擇**：密碼/確認密碼各加 `showPassword`/`showConfirm` ref，`type` 在 `password`/`text` 切換，icon 用 Bootstrap Icons（`bi-eye`/`bi-eye-slash`，專案已有 bi）；不抽共用元件（僅兩欄）。
- **替代考慮**：共用 `PasswordInput` 元件——目前只有註冊頁需要，過度設計。

### D7 使用者清單搜尋＋顯示已停用（`UsersView.vue` 前端過濾）
- **選擇**：`searchQ` ref 對帳號/顯示名稱/信箱前端模糊比對，`showInactive` ref（checkbox，預設 `false`）過濾 `active`；兩者疊加於既有分頁 `computed` 之前；狀態徽章沿用既有 `text-bg-success`/`text-bg-secondary`。
- **替代考慮**：後端加搜尋參數——使用者量小，前端過濾足夠且無 API 變更。

### D8 帳號儲存鈕改 dirty（`AccountView.vue`）
- **選擇**：`canSave()` 改為 `profile.displayName/email` 與 `original*` 比對（去空白後），任一不同即顯示；既有檢查按鈕與送出前驗證保留（顯示≠可送出，不合規仍阻擋）。
- **替代考慮**：移除檢查按鈕——檢查按鈕提供即時可用性回饋，保留。

### D9 簽名人 UI 三項（`SignersView.vue`，屬 `reference-data-admin`）
- **選擇**：篩選列（既有 `filterQ`）收進 Bootstrap `collapse` 抽屜（按鈕觸發，預設隱藏）；欄序對調為帳號→身分別（僅 `<th>`/`<td>` 順序）；狀態徽章改 `text-bg-success`/`text-bg-secondary` 同款（啟用語意對應既有 `userId` 有無：有使用者→啟用色）。
- **狀態**：delta spec 見 `specs/reference-data-admin/spec.md`（三需求），可直接實作。

## Risks / Trade-offs

- [可用性端點被用於帳號枚舉] → 僅回布林值是枚舉固有成本；註冊本就需即時回饋，接受（登入錯誤訊息仍統一）。
- [競態：檢查可用→送出前被搶註] → 送出時後端仍做最終檢查並映射 inline，接受。
- [409 碼新增 `EMAIL_TAKEN`] → 與 `EMAIL_DUPLICATE` 並存（註冊 vs 編輯語境）；文件需註明對應關係。
- [使用者清單預設隱藏停用者] → 顯示行為變更；checkbox 明示且資料仍在，接受。
- [簽名人狀態徽章改字] → 由既有「啟用中/已停用」文字改為與使用者管理一致的「啟用/停用」徽章；純顯示變更，無資料語意改變。

## Migration Plan

1. **DB**：無遷移（應用層檢查＋前端過濾）。
2. **部署**：後端先上（新端點＋註冊檢查），前端後上；舊前端無 inline 但註冊檢查仍生效（改走 alert，短暫降級可接受）。
3. **Rollback**：移除 Service 檢查與端點即回退；前端 revert 即回原行為。

## Open Questions

- 無（信箱大小寫不敏感、空值可用皆已按既有 `isEmailAvailable` 語意定案；regex 沿用 `AccountView` 既有式樣）。
