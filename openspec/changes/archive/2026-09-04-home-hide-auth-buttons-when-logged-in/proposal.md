## Why

首頁（`/`）為公開落地頁，登入後系統預設導向儀表板（`/dashboard`），但使用者仍可點導覽回到首頁。目前首頁 Hero 區固定顯示「立即登入」「建立帳號」兩個按鈕，即使已登入亦然，造成已認證使用者看到無意義的認證入口。需依登入狀態隱藏該組按鈕。

## What Changes

- `HomeView.vue` 依 `useAuthStore().isAuthenticated` 判斷：未登入時維持顯示「立即登入」「建立帳號」；已登入時隱藏該按鈕組（可選改為顯示「前往儀表板」或不顯示任何按鈕，預設隱藏）。
- 不改變路由或後端契約，僅前端呈現邏輯；已登入使用者透過導覽或直接訪問 `/` 均生效。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `user-admin`: 首頁認證入口按鈕依登入狀態顯示（已登入隱藏「立即登入/建立帳號」）

## Impact

- 前端：`frontend/src/views/HomeView.vue`（條件渲染按鈕組，依 `useAuthStore`）
- 後端：無
- 文件：如 `docs/manual.typ` 描述首頁則同步說明
