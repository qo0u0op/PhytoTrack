## MODIFIED Requirements

### Requirement: 案件明細檢視

系統 SHALL 於前端提供單案明細頁 `/cases/:id`，其欄位呈現順序 SHALL 對齊 `docs/diagnoses.typ` 的紙本邏輯 (保持現有卡片視覺不變，僅重排欄位)：`收件日期 / 案件編號 → 田區位置 (同寄件人/其他) → 送件人 (姓名/顯示名稱) → 電話 → 身分別 → 地址 (縣市鄉鎮 + 住址) → 耕作方式 → 作物種類 → 作物名稱 → 被害部位 → 栽培面積 (cropScale) → 被害面積 (damageScale) → 土壤、栽培、用藥紀錄 (caseDescription) → 服務類別 → 送件方式 → 鑑定者 (identifiers) → 診斷結果 (五類：病害/蟲害/有害動物/生理因子/其他，依 pestType 分組並附 pestNote) → 防治建議 (hints) → 建議採取措施 (hintDescription) → 建立者與時間。電子信箱欄位於資料模型不存在時 SHALL 以 `displayName` 替代（紙本幾乎未用，沿用顯示名稱）；病蟲害發生地點維持現狀；被害描述以 `pestNote` 呈現；送件方式 Email/FB/Line 等同 `網路諮詢`；土壤、栽培、用藥紀錄資料庫欄位為 `case_description`。

#### Scenario: 檢視案件明細
- **WHEN** 使用者開啟某一案件
- **THEN** 顯示完整欄位內容

#### Scenario: 檢視案件明細欄位順序對齊表單
- **WHEN** 使用者開啟 `/cases/:id`
- **THEN** 明細頁依上述順序呈現各區段，且不改變卡片版式與 `@media print` 僅輸出本體的行為

#### Scenario: 明細頁保留遮蔽與列印行為不變
- **WHEN** `ROLE_VIEWER` 開啟明細
- **THEN** 仍遮蔽送件人姓名/電話/地址（顯示為 `***` 或 `***(***)`），僅顯示縣市鄉鎮，且列印仍僅含診斷單本體

#### Scenario: 明細頁顯示作物種類與更名後建議
- **WHEN** 明細包含作物與建議事項
- **THEN** 同時顯示 `作物種類` 與 `作物名稱` (Q5)，且第 6 項建議顯示為「其他回覆」 (Q4)

#### Scenario: 明細頁用語更名
- **WHEN** 使用者檢視明細頁欄位標籤
- **THEN** `病蟲害發生地點` 顯示為 `田區位置`，`送件人身分別` 顯示為 `身分別`，土壤、栽培、用藥紀錄欄位為 `caseDescription`
