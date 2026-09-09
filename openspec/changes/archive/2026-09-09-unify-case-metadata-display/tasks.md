## 1. 中繼資料顯示統一

- [x] 1.1 於 `views/CaseDetailView.vue:260` 將單行 `建立者：／建立： ／更新：` 改為兩行 `建立者：／建立：` 與 `編輯者：／更新：`（`formatTime` 維持 `replace('T',' ').slice(0,19)`，`編輯者` 回落 `updatedByName ?? createdByName ?? '—'`），驗證 `npm run build` 與檢視頁顯示為兩行且時間為 `yyyy-MM-dd HH:mm:ss`
- [x] 1.2 於 `views/CasesView.vue:477` 預覽 `Swal` HTML 將 `esc(data.createdAt)` 改為 `formatTime(data.createdAt)` 並改單行 `建立者：／建立時間：` 為兩行 `建立者：／建立：` 與 `編輯者：／更新：`（有 `updatedAt` 顯示，無則隱藏/顯示 `—`），驗證預覽時間格式與檢視頁一致且為兩行
- [x] 1.3 （可選）若需 `updatedByName` 則於 `dto/CaseDtos.CaseResponse` 補欄位並於 `CaseService.toResponse` 填值，否則保持前端回落，驗證 `GET /api/cases/{id}` 回應含 `updatedAt` 且前端不破壞
- [x] 1.4 於 `views/DashboardView.vue:80,211` 將 `barClass`（`bg-warning/bg-success/bg-secondary`）改為 `barColor` 回傳 `#f59e0b/#10b981/#64748b`（PENDING/RESOLVED/CLOSED），模板改 `:style="{ backgroundColor: barColor(...) }"`，驗證 `progress-bar` 三色為 amber/emerald/slate 且語意不變

## 2. 驗證

- [x] 2.1 `npm test` 與 `npm run build` 綠燈，`openspec validate --specs --changes` 通過，預覽與檢視頁時間皆為 `yyyy-MM-dd HH:mm:ss` 且中繼資料分兩行
- [x] 2.2 Dashboard `案件狀態比例` 三條 `progress-bar` 顯示新配色（`#f59e0b`/`#10b981`/`#64748b`），`npm run build` 通過
