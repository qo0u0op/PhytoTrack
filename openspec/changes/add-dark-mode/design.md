## Context

現行前端以 `bootstrap 5.3.8` 為基礎，`App.vue` 導覽列為 `navbar-dark bg-success`，`style.css` 僅亮色背景（`#f8f9fa`），無任何主題分流。需引入深色模式時避免逐頁重寫，優先採用 Bootstrap 官方 `data-bs-theme`（`light`/`dark`）讓元件以 CSS 變數自動適配。

## Goals / Non-Goals

**Goals:**
- 單一 `data-bs-theme` 驅動全站，首屏即正確主題（無 FOUC 白閃）
- `light/dark/auto` 三態，`auto` 即時跟隨系統且可被手動覆蓋，持久化至 `localStorage`
- 導覽列可見切換入口，鍵盤可達，零後端依賴

**Non-Goals:**
- 不引入 `tailwind` / `Vuetify` 等新框架，僅擴充現有 Bootstrap 變數
- 不為每頁寫獨立深色樣式檔，僅於 `style.css` 補必要覆蓋（`body`、`hero-section`、`card.shadow-sm`）
- 不提供按使用者帳號雲端同步主題（僅本機 `localStorage`）

## Decisions

- **Bootstrap `data-bs-theme` 而非自寫 class**：`document.documentElement.setAttribute('data-bs-theme', effective)` 讓 Bootstrap 原生變數（`--bs-body-bg`、`--bs-card-bg` 等）自動生效，元件無需逐一 `v-if`。替代：`body.dark` 自訂 200 行 CSS → 維護成本高且與 Bootstrap 升級衝突 → 捨棄。
- **Pinia `stores/theme.ts` 集中狀態**：`theme: 'light'|'dark'|'auto'`（`ref` + `localStorage` 讀寫），`effectiveTheme: computed(() => theme==='auto' ? (matchMedia.dark ? 'dark':'light') : theme)`，`watchEffect` 同步 `data-bs-theme`，`onMounted` 註冊 `matchMedia('(prefers-color-scheme: dark)').addEventListener('change')`。替代：純 `useLocalStorage` composable 散落各頁 → 難以統一首屏寫入 → 捨棄。
- **初始化早於 mount**：`main.ts` 於 `createPinia()` 後、`mount()` 前呼叫 `useThemeStore().init()`（同步讀 `localStorage` 並立即 `setAttribute`），避免首屏白底閃爍。替代：`App.vue onMounted` 才寫 → 首屏 FOUC → 捨棄。
- **導覽列循環切換**：按鈕 `auto → light → dark → auto`，圖示 `bi-circle-half / bi-sun / bi-moon`，`aria-label="切換主題"`。替代：三個 radio → 佔位大、移動端不友善 → 捨棄。
- **樣式覆蓋最小化**：`style.css` 僅補 `[data-bs-theme="dark"] body { background: #212529 }`、`[data-bs-theme="dark"] .hero-section` 降低亮度、`[data-bs-theme="dark"] .card` 邊框微調，其餘交由 Bootstrap 變數。替代：全站重寫色票 → 風險高 → 捨棄。

## Risks / Trade-offs

- [首屏閃爍] `init()` 若慢於 CSS 載入仍可能 FOUC → Mitigation：`main.ts` 同步寫 `data-bs-theme` 早於 `import './style.css'` 之後但 `mount` 之前，並在 `index.html` 內聯極短腳本（可選）作雙保險
- [Bootstrap 變數覆蓋遺漏] 自訂 `linear-gradient hero` 於深色下過亮 → Mitigation：`hero-section` 單獨 `[data-bs-theme="dark"]` 覆蓋漸層為深綠/深藍
- [Auto 監聽洩漏] `matchMedia` 未移除 → Mitigation：`store` 生命週期內單次註冊，無需跨頁重建
- [對比度] 深色下 `bg-success navbar` 綠色對比不足 → 保留 `bg-success`（品牌色），文字保持白，深色下 `navbar` 維持原色以保辨識度

## Migration Plan

1. 新增 `stores/theme.ts` 與 `style.css` 深色覆蓋 → `App.vue` 導入切換鈕與 `watch` → `main.ts` `init()` → `npm run build` (`vue-tsc` -b)
2. 無 API/DB 遷移，回滾僅移除 `theme store` 與 `data-bs-theme` 寫入，`localStorage` 鍵可保留無影響
3. 驗證：`npm test`（`theme` 單元測試）、`playwright` 截圖比對亮/暗

## Open Questions

- 無
