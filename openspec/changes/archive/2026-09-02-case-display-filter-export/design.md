## Context

現行 `CaseDetailView`/`CasesView` 預覽與 `CaseService.toCsv` 表頭仍使用 `病蟲害發生地點`/`送件人身分別`，與紙本及表單用語脫節；`CasesView` 篩選版面順序為舊版且缺少 `身分別`/`耕種方式`；`CaseFilter`/`CaseSpecifications` 尚未支援 `senderTypeId`/`methodId`，且 CSV 匯出雖接受 `CaseFilter` 但前端未將當前景篩選帶入。見 `proposal.md` 動機與 `specs/case-report,spec.md`/`case-search` 需求。

## Goals / Non-Goals

**Goals:**
- 統一顯示用語為 `田區位置`/`身分別` 並同步 CSV 表頭。
- 補齊 `身分別`/`耕種方式` 篩選（後端查詢與前端選單）並依指定順序重排版面。
- 使 CSV 匯出沿用當前景篩選（前端以 `appliedFilter` 組參數呼叫，後端已支援則僅打通）。

**Non-Goals:**
- 不改篩選語意（AND、分頁、抽屜行為）與權限。
- 不做新欄位遷移或視圖結構大幅重構。

## Decisions

- **用語更名**：前端 `CaseDetailView.vue`/`CasesView.vue` 標籤與 `CaseService.toCsv` 表頭字面量直接替換 `病蟲害發生地點_縣市/鄉鎮/病蟲害發生地 → 田區位置`、`送件人身分別 → 身分別`。替代：抽 i18n，但僅兩處文案，直替最簡。
- **篩選重排**：`CasesView.vue` 篩選表單 DOM 依 `收件日期區間 → 狀態 → 田區縣市 → 田區鄉鎮 → 送件人 → 身分別 → 服務類別 → 送件方式 → 耕種方式 → 作物類別 → 作物 → 被害部位 → 害物 → 害物類別 → 建議類別` 重排，並將 `senderType`/`method` 參照加入 `loadReference`。替代：動態配置排序，但固定順序需求直排即可。
- **身分別/耕種方式後端**：`CaseDtos.CaseFilter` 新增 `senderTypeId/methodId`，`CaseSpecifications.build`/`buildView` 增加 `sender.senderType = :id` 與 `method = :id` 條件；`CaseService` 篩選分支與 `CaseController` 匯出參數同步。替代：重用現有 `pestType` 欄位不可行，需新增。
- **篩選穿透至 CSV**：前端匯出按鈕從 `appliedFilter` 取值組 `URLSearchParams` 呼叫 `/api/cases/export?` 同參；後端 `exportCsv` 已吃 `CaseFilter` 故無需視圖新增欄位。替代：前端先取列表再匯出，不符全量匯出需求。

## Risks / Trade-offs

- [文案 BREAKING] 舊表頭/標籤依賴需更新 → 搜尋 `病蟲害發生地點` 舊字串並同步測試。
- [篩選新增] 無 `CaseSearchView` 欄位時 `senderType/method` 需走 `Case` 實體查詢（`findAllById` 回補）→ 與現行 `exportCsv` 兩階段查詢一致，風險可控。
- [匯出穿透] 前端若 `appliedFilter` 未同步導致範圍不一致 → 統一以 `appliedFilter` 為單一來源並於匯出函式內轉換。

## Migration Plan

1. 前端標籤替換與 `toCsv` 表頭替換。
2. 後端 `CaseFilter`/`Specifications`/`Controller` 擴充 `senderTypeId/methodId`。
3. `CasesView` 篩選版面重排並新增兩選單，匯出函式帶入篩選參數。
4. 測試更新：`CaseServiceTest` 篩選與 CSV、`CaseControllerTest` 匯出穿透。

## Open Questions

- 無。
