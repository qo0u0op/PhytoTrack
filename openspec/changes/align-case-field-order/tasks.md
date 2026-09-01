## 1. 前置確認 (已於 2026-09-01 定版)

- [x] 1.1 與使用者確認 `design.md: Open Questions` 1-6，已定版：Q1 維持 `cropScale/damageScale` 即兩面積、被害描述≒`caseDescription+pest_note`；Q2 維持任意多選 identifiers；Q3 維持網路諮詢；Q4 更名 `其他→其他回覆`；Q5 需要顯示作物種類；Q6 維持現狀，並回寫至 `design.md` 與 `specs/case-report/spec.md`，`openspec validate --strict` 已通過
- [x] 1.2 依確認結果定版 CSV 新表頭最終清單 (`作物種類` 入列、`其他回覆` 更名、`被害描述` 含 `pest_note`)，`proposal.md: Impact` BREAKING 與 `docs/DEPLOY.md` 對照表待 3.2 補齊

## 2. 呈現層重排 (保持卡片設計，僅調順序)

- [x] 2.1 重排 `frontend/src/views/CasesView.vue:viewDetail` 彈窗 `Swal.fire html` 為 `收件日期/編號 → 田區位置 → 送件人/電話/身分別/地址 → 耕作方式 → 作物種類/名稱 → 被害部位 → 栽培/被害面積 → 土壤栽培用藥紀錄 → 服務/送件 → 鑑定者 → 五類分組 (依 pestType 分組，`pestNote` 為 名稱 (備註)) → 建議事項`，驗證 `npm run build` 通過且手動開啟 `VIEWER` 預覽顯示 `***`
- [x] 2.2 重排 `frontend/src/views/CaseDetailView.vue` 明細卡片為同順序 (收件日期/編號 → 田區 → 送件人/電話/身分別/地址 → 耕作 → 作物種類/名稱 (Q5) → 被害部位 → 栽培/被害面積 → 土壤紀錄 (Q1：`caseDescription+pest_note`) → 服務/送件 (Q3 維持網路諮詢) → 鑑定者 (Q2 維持任意) → 五類 → 建議事項 (Q4：`其他回覆`))，保留 `@media print` 僅輸出 `.print-area`，驗證 `npm run build` 與列印預覽僅含診斷單本體且順序與彈窗一致
- [x] 2.3 重排 `backend/src/main/java/com/d0w0b/phytotrack/service/CaseService.java:toCsv` 表頭與 `join` 順序對齊 `design.md: D5` (含 `作物種類` Q5、 `其他回覆` Q4、 `被害描述` 含 `pest_note` Q1)，`pestCategories` 依 `pestType` 分五欄、空類顯示「無」，`hints` 種子 `id=6` 由「其他」更名為「其他回覆」並同步 `schema.sql`，驗證 `mvn test -Dtest=CaseServiceTest#exportCsv_shouldBuildRowsWithBomAndEscape` 與 `PhytoTrackIntegrationTest` 匯出含新表頭且含 BOM

## 3. 驗收與文件

- [x] 3.1 更新 `frontend/src/views/CasesView.vue` 與 `CaseDetailView.vue` 的單元/整合測試或手動核對 (預覽、明細、列印、CSV 欄位順序與 `diagnoses.typ` 一對一對照)，驗證 `npm test` (`vitest run`，happy-dom) 9/9 通過
- [x] 3.2 全量驗證 `mvn test` 94/94 與 `npm run build` 通過，`openspec validate --specs --changes` 無錯誤，`docs/DEPLOY.md` 補 CSV 欄位對照表 (舊→新) 與 release note BREAKING 標註
