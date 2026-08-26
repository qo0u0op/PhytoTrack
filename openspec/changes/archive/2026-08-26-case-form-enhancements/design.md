# Design: case-form-enhancements 欄位正名與害物三段式

## Context

見 `proposal.md`（Why）。現況 `cases.pest_description` 同時用於害物描述與土壤紀錄，語意不清；`case_pest_categories` 的 `UNIQUE(case_id, pest_category_id)` 使同分類多害物無法並存（如兩個細菌性病害皆為 pest_category_id=2 會撞唯一鍵）；作物選擇為扁平下拉，未體現紙本表單的分類勾選。

## Goals / Non-Goals

**Goals：**
- `pestDescription` 正名為 `caseDescription`（土壤、栽培、用藥紀錄），開發初期直接重建 DB
- `case_pest_categories` 支援同案件同分類多筆（移除 unique、新增 `pest_note`），每列對應一害物
- 建案表單作物別級聯、害物三段式列編輯

**Non-Goals：**
- 病蟲害發生地點、電子信箱、被害描述的電子化（僅記 backlog）
- 送件方式 Email/FB/Line 選項新增（backlog）
- 舊 `pest_description` 內容遷移（開發初期直接丟棄，文件註記）

## Decisions

1. **更名策略**
   - `cases.pest_description` → `case_description`（SQL 與 JPA 欄位同步更名，`@Column(name="case_description")`），`Case` 實體欄位 `pestDescription`→`caseDescription`，DTO 同步，`pestDescription` 完全移除
   - 理由：語意對齊紙本；開發初期無遷移成本；BREAKING 明確

2. **害物明細儲存**
   - 移除 `case_pest_categories` 的 `UNIQUE(case_id, pest_category_id)`，新增 `pest_note TEXT`
   - `CaseService` 建案/更新時依前端送來的 `pestCategoryWithNotes: List<{pestCategoryId, pestNote}>` 整組替換 junction（含 note）；`pestDescription` 不再接收害物文字
   - 替代方案捨棄：保留 unique 並僅串字串到 `caseDescription`——無法回答使用者「編輯時如何區分」的問題

3. **作物級聯與參照篩選**
   - 前端 `CaseFormView` 作物區改二下拉：`selectedCropCategoryId`（`null` 為全部）過濾 `crops`；回寫時 `createCrop` 成功後自動選用新作物。`ReferenceDataAdminView` 的作物頁籤已加入名稱篩選與分類下拉，病蟲害頁籤已有類型篩選
   - 後端 `POST /api/admin/ref/crops` 與 `POST /api/admin/ref/identifiers` 需放寬至 `hasAnyRole('STAFF','ADMIN')`（方法層覆蓋類別層 `ADMIN`），其餘 `PUT/DELETE` 維持 `ADMIN`

4. **害物三段式 UI 與儲存**
   - 類型下拉（病害/蟲害…）→ 過濾分類下拉 → 學名：描述輸入框；可「＋新增一列」多筆，同分類多筆合法
   - `CaseService` 建案/更新時依 `List<PestCategoryWithNote>` 執行 `clear+add` 整組替換（不再以 `Set<Long>` 去重，否則同分類多筆會被吃掉）；`pest_note` 前端 `maxLength` 200 提示，後端 `@Size` 限 500，`escapeHtml` 僅用於 AI 顯示前的轉義
   - 簽名人 inline 新增已於前一修復開放 STAFF，無需變更

## Risks / Trade-offs

- 更名為 BREAKING，舊 DB 需刪除重建（開發期可接受，已於 `docs/REQUIREMENTS.md` 註記）；正確路徑為 `./diagnoses.db` 與 `./target/phytotrack-test.db`（`application.yaml`/`application-test.yaml`），`ddl-auto: update` 不會自動刪欄位
- 同分類多筆害物解除 unique 後，`replaceJunctionGroup` 必須改為 `List` 語意，否則測試 `重複 id+不同 note` 會失敗
- CSV `toCsv()` 需新增「害物明細（含 note）」欄位，僅輸出分類名會丟失 `pest_note`；AI prompt 需改送 `caseDescription` + 逐列 `pestNotes` 拼接（原 `pestDescription` 已改語意）
- `case_pest_categories.pest_note` 長度與 XSS 由前後端雙重檢查（前端 `maxLength`＋`escapeHtml`，後端 `@Size`）

## Migration Plan

- 無遷移腳本：開發期執行 `rm backend/diagnoses.db backend/target/phytotrack-test.db` 後重建
- 回滾：還原 `pest_description` 欄位與 unique 約束，需重建 DB

## Open Questions

- `case_pest_categories.pest_note` 是否需長度限制？（本次不設，由前端提示）
