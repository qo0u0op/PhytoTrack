## Why

現行前端僅提供亮色主題，於夜間或低光環境長時間操作（田間回報、夜間診斷建案）易造成視覺疲勞，且作業系統已普遍支援深色偏好（`prefers-color-scheme`），缺乏跟隨系統或手動切換能力影響可及性與使用者體驗。需以最小侵入方式引入深色模式，維持既有版面與元件可用性。

## What Changes

- **主題能力新增**：引入 `ui-theme`（深淺主題切換），支援 `light / dark / auto` 三態，預設 `auto`（跟隨 `prefers-color-scheme`），可於導覽列手動切換並持久化
- **Bootstrap 5.3 Color Mode**：以 `document.documentElement` 的 `data-bs-theme` 驅動（`light`/`dark`），沿用既有 `bootstrap 5.3.8` 與 `bootstrap-icons`，不引入額外 CSS 框架；`style.css` 補深色變數覆蓋（`body` 背景、卡片陰影等）
- **狀態與持久化**：新增 `stores/theme.ts`（Pinia），`state.theme: 'light'|'dark'|'auto'`，`effectiveTheme` 計算 `auto` 下的實際主題，寫入 `localStorage['phytotrack-theme']`，監聽 `matchMedia('(prefers-color-scheme: dark)')` 變更
- **UI 入口**：`App.vue` 導覽列右側（帳號選單旁）新增主題切換按鈕（圖示 `sun/moon-circle/half`，`title` 提示），登入/未登入皆可見；可為 `auto` 時顯示系統當前值
- **無後端變更**：純前端行為，不改 API、DB、認證與權限；既有頁面（Dashboard/CaseList/表單/管理頁）以 Bootstrap 原生變數自動適配，無逐頁重寫

## Capabilities

### New Capabilities
- `ui-theme`: 深色模式與主題切換（`data-bs-theme`、`prefers-color-scheme`、`localStorage` 持久化、三態切換）

### Modified Capabilities
<!-- 無 — 深色模式為純前端呈現分支，不改既有領域需求行為 -->

## Impact

- 前端：`stores/theme.ts`（新增）、`App.vue`（導覽列切換鈕 + `watchEffect` 寫 `data-bs-theme`）、`main.ts`（初始化 theme store 早於 mount）、`style.css`（`[data-bs-theme="dark"]` 覆蓋）、`views/*` 無需逐頁改動（依賴 Bootstrap CSS 變數）
- 相依：沿用 `bootstrap 5.3.8` 的 Color Mode，`bootstrap-icons` 提供圖示，無新 npm 依賴
- 測試：`vitest` 補 `theme store` 單元測試（`auto/light/dark`、持久化、`effectiveTheme`）、`App.vue` 快照或 `data-bs-theme` 斷言
- 風險：零 API 破壞，僅視覺層；已驗 `npm run build`（`vue-tsc`）與 `npm test` 綠燈
