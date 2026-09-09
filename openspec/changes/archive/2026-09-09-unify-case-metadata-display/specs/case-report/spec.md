## ADDED Requirements

### Requirement: 案件中繼資料顯示一致性

檢視頁與預覽頁的案件中繼資料 SHALL 以檢視頁的 `formatTime`（`yyyy-MM-dd HH:mm:ss`，`T`→空白、`slice(0,19)`）為基準統一顯示，且中繼資料 SHALL 分兩行呈現：第一行 `建立者：／建立：`，第二行 `編輯者：／更新：`；預覽頁無 `updatedAt` 時第二行可隱藏或顯示 `—`。

#### Scenario: 檢視頁兩行顯示
- **WHEN** 使用者開啟 `GET /api/cases/{id}` 的檢視頁
- **THEN** 頁尾中繼資料分兩行：`建立者：{{createdByName}}／建立：{{formatTime(createdAt)}}` 與 `編輯者：{{updatedByName ?? createdByName ?? '—'}}／更新：{{formatTime(updatedAt)}}`，`createdAt/updatedAt` 皆為 `yyyy-MM-dd HH:mm:ss`

#### Scenario: 預覽頁時間格式與檢視頁一致
- **WHEN** 使用者在列表點擊預覽（`CasesView` preview）且 `createdAt` 為 `2026-09-08T10:30:00`
- **THEN** 預覽內 `建立：` 顯示為 `2026-09-08 10:30:00`（非原始 `2026-09-08T10:30:00`），與檢視頁一致

#### Scenario: 預覽頁亦分兩行
- **WHEN** 預覽資料含 `updatedAt`
- **THEN** 預覽同檢視頁分為 `建立者／建立` 與 `編輯者／更新` 兩行；若無 `updatedAt` 則第二行隱藏或顯示 `—`，不破壞版面

#### Scenario: 時間格式截斷至秒
- **WHEN** 後端回傳 `2026-09-08T10:30:00.123456`（含微秒）
- **THEN** 前端顯示截斷至秒 `2026-09-08 10:30:00`，與檢視頁 `formatTime` 一致
