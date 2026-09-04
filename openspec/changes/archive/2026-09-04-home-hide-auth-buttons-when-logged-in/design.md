## Context

見 proposal.md。現況 `HomeView.vue` 固定渲染兩個 `router-link` 按鈕，未依 `useAuthStore().isAuthenticated` 判斷。登入後預設路由為 `/dashboard`，但 `/` 仍可訪問。

## Goals / Non-Goals

**Goals:**
- 已登入時首頁不顯示認證入口按鈕

**Non-Goals:**
- 改變登入後預設導向或新增路由守衛
- 後端契約變更

## Decisions

- 判斷來源：`import { useAuthStore } from '../stores/auth'` 並以 `computed` 或直接 `auth.isAuthenticated` 於模板 `v-if="!auth.isAuthenticated"` 控制按鈕組顯示
- 替代：路由守衛重導已登入者從 `/` 到 `/dashboard` — 會剝奪已登入者查看落地頁資訊的權利，不採用；僅隱藏按鈕為最小侵入

## Risks / Trade-offs

- 無顯著風險；狀態由 Pinia 持久化 `localStorage` 驅動，與導覽列一致

## Migration Plan

- 僅前端條件渲染，無遷移；回滾即移除 `v-if`

## Open Questions

- 無（隱藏後是否顯示「前往儀表板」按鈕為可選增強，本次預設隱藏；如需可後續補）
