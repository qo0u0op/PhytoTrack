## 1. 主題狀態與初始化

- [ ] 1.1 新增 `frontend/src/stores/theme.ts`（Pinia）：`theme: 'light'|'dark'|'auto'`（預設 `auto` 讀 `localStorage['phytotrack-theme']`）、`effectiveTheme` computed、`init()` 同步寫 `document.documentElement.dataset.bsTheme` 並註冊 `matchMedia('(prefers-color-scheme: dark)')` 監聽，驗證 `vitest` `effectiveTheme` 與持久化分支通過
- [ ] 1.2 於 `frontend/src/main.ts` 在 `createPinia()` 後、`mount()` 前呼叫 `useThemeStore().init()`，驗證重新整理後首屏即正確 `data-bs-theme`（無白閃），`npm run build`（`vue-tsc -b`）通過

## 2. 導覽列切換入口

- [ ] 2.1 於 `frontend/src/App.vue` 導覽列右側（帳號區旁）新增主題循環按鈕（`auto→light→dark→auto`，圖示 `bi-circle-half/bi-sun/bi-moon`，`title`/`aria-label` 含當前值），以 `watchEffect` 同步 `data-bs-theme`，驗證登入/未登入頁皆可見且鍵盤 Enter/Space 可切換
- [ ] 2.2 於 `frontend/src/style.css` 補 `[data-bs-theme="dark"]` 覆蓋：`body` 背景 `#212529`、`.hero-section` 漸層降亮、`card` 邊框/陰影微調，驗證 `Dashboard`/`CaseList`/`表單` 於 dark 下無白底殘留且輸入框可讀

## 3. 驗證與文件

- [ ] 3.1 單元與整合驗證：`npm test`（`theme` store 3 場景）綠燈、`npm run build` 綠燈，`playwright` 截圖或 `document.documentElement.getAttribute('data-bs-theme')` 斷言 `light/dark/auto` 切換與 `localStorage` 持久化，驗證 `prefers-color-scheme` 變更時 `auto` 自動跟隨
- [ ] 3.2 執行 `openspec validate --specs --changes` 16+1 passed，首屏重新整理無白閃，`docs/` 無需後端變更
