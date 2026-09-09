## Purpose

提供深色模式與主題切換能力，讓使用者於亮/暗/auto 三態間選擇，自動跟隨作業系統偏好並於重整後保持，提升低光環境可讀性與可及性。

## ADDED Requirements

### Requirement: 主題三態與系統跟隨

系統 SHALL 提供 `light / dark / auto` 三態主題，預設為 `auto`；`auto` 時 SHALL 即時跟隨 `prefers-color-scheme`，使用者手動切換後 SHALL 覆蓋系統偏好並持久化。

#### Scenario: 首次進入為 auto 跟隨系統
- **WHEN** 使用者首次開啟前端且 `localStorage['phytotrack-theme']` 不存在，系統為亮或暗
- **THEN** `document.documentElement` 的 `data-bs-theme` 等於系統當前 `prefers-color-scheme`（`dark` 則 `dark`，否則 `light`），且頁面以對應主題渲染

#### Scenario: 切換至深色並持久化
- **WHEN** 使用者點擊主題切換鈕將 `auto → dark`（或 `light → dark`）
- **THEN** `data-bs-theme="dark"` 立即生效，`localStorage['phytotrack-theme']` 寫為 `dark`，重新整理後仍為 `dark`

#### Scenario: 切換回 auto 恢復跟隨
- **WHEN** 使用者由 `dark` 切至 `auto`，且系統為 `light`
- **THEN** `data-bs-theme` 轉為 `light`，`localStorage` 寫為 `auto`，後續系統切至 `dark` 時頁面自動轉 `dark` 無需重整

### Requirement: 主題切換入口可見性

所有頁面（含未登入的 `/login`、`/register`、已登入的 `/dashboard`、`/cases`）SHALL 於導覽列可見主題切換按鈕，顯示當前狀態圖示與提示，鍵盤可達且具 `aria-label`。

#### Scenario: 導覽列按鈕可見
- **WHEN** 使用者瀏覽任意頁面
- **THEN** 導覽列右側（帳號區旁）顯示主題按鈕，`auto` 顯示半圓圖示、`light` 顯示太陽、`dark` 顯示月亮，且 `title`/`aria-label` 為「切換主題：目前為 auto/light/dark」

#### Scenario: 鍵盤操作
- **WHEN** 使用者以 Tab 聚焦主題按鈕並按 Enter/Space
- **THEN** 主題依 `auto → light → dark → auto` 循環切換，焦點不丟失

### Requirement: 視覺一致性與無後端依賴

深色模式 SHALL 僅改變前端呈現，不依賴後端 API 或資料庫；常見元件（導覽列、卡片、表單、表格、`hero-section`、按鈕）於深色下 SHALL 保持可讀對比（文字與背景對比符合可辨識度），不出現白底殘留或文字不可見。

#### Scenario: 卡片與表單深色渲染
- **WHEN** 於 `dark` 主題瀏覽 `Dashboard` 卡片或 `Case` 表單
- **THEN** 卡片背景與文字採用 `data-bs-theme` 對應變數，無白底殘留，輸入框與下拉選單可讀

#### Scenario: 重新整理保持主題
- **WHEN** 使用者於 `dark` 主題重新整理頁面或以新分頁開啟 `/dashboard`
- **THEN** 首屏即為 `dark`（無白底閃爍），`data-bs-theme` 於 `main.ts` 初始化階段已寫入
