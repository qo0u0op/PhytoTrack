## MODIFIED Requirements

### Requirement: Dashboard 統計視圖

前端 Dashboard SHALL 以純 Bootstrap（卡片、進度條、表格）呈現統計資料，不依賴第三方圖表庫；其中「田區位置」卡僅顯示 `fieldCityBreakdown`（Top 10 縣市）。期間顯示 SHALL 為中文（`歷史/年度/月度/半年度`），年份選單 SHALL 預設為可用年份最新值且不含空選項，年份為 `HALF_YEAR` 時同時提供半年度選單（上半年/下半年），AI 卡片標題 SHALL 為「模型狀態」，近半年趨勢卡 SHALL 與耕種方式並列且標題為「近半年案件趨勢」，且頁面 SHALL 不含底部導覽按鈕列與期別小字「期間：xxx」；所有百分比 SHALL 以一位小數顯示。當 `availableYears` 為空（尚無案件）時，期別、年份、半年度、月份四個下拉 SHALL 全為禁用（disabled、視覺灰化）且不可互動，年份下拉 SHALL 以 `disabled` 空 `option`（ghost）顯示「尚無歷史年份」而非在下拉外以 `div.form-text` 呈現；期別 SHALL 強制停留 `HISTORICAL`，切換至 `ANNUAL/MONTHLY/HALF_YEAR` SHALL 自動回退至 `HISTORICAL` 且不發送缺參請求，避免觸發 `VALIDATION_ERROR`。

#### Scenario: 檢視 Dashboard
- **WHEN** 登入使用者進入 Dashboard
- **THEN** 顯示各項統計數據與模型狀態，且「田區位置」卡顯示縣市 breakdown

#### Scenario: 期間中文化
- **WHEN** 切換期別（`HISTORICAL/ANNUAL/MONTHLY/HALF_YEAR`）
- **THEN** 期間文字顯示為「歷史/年度/月度/半年度」

#### Scenario: 年份必填
- **WHEN** 選擇年度、月度或半年度期別
- **THEN** 年份預設為最新可用年份且無空選項，半年度時半年度選單預設為上半年

#### Scenario: 近半年趨勢位置與標題
- **WHEN** 檢視 Dashboard 的 breakdown 區
- **THEN** 「近半年案件趨勢」卡與耕種方式卡並列，且標題為「近半年案件趨勢」

#### Scenario: 無底部導覽
- **WHEN** 檢視 Dashboard
- **THEN** 頁面不顯示底部按鈕列

#### Scenario: 無期間小字
- **WHEN** 檢視期別選擇器區域
- **THEN** 不顯示期別小字

#### Scenario: 模型狀態標題
- **WHEN** 檢視模型狀態卡
- **THEN** 標題為「模型狀態」

#### Scenario: 百分比一位小數
- **WHEN** 檢視任意百分比
- **THEN** 以一位小數顯示

#### Scenario: 半年度選擇
- **WHEN** 選擇期別為 `半年度` 並切換 `上半年/下半年`
- **THEN** Dashboard 以該半年範圍重新載入統計，且趨勢與 breakdown 僅反映該範圍

#### Scenario: 空庫全禁用與 ghost
- **WHEN** `availableYears` 為空（尚無案件）且檢視 Dashboard 期別選擇器
- **THEN** 期別、年份、半年度、月份四個下拉皆為 `disabled`（灰化不可點），年份下拉內以 `disabled` 空 `option` 顯示「尚無歷史年份」，且不出現下拉外的 `div.form-text`

#### Scenario: 空庫切期別不報錯
- **WHEN** `availableYears` 為空時使用者嘗試將期別切至 `ANNUAL`/`MONTHLY`/`HALF_YEAR`
- **THEN** 期別自動回退至 `HISTORICAL`，不送出缺 `year`/`half`/`month` 的 `GET /cases/statistics` 請求，且不觸發 `VALIDATION_ERROR` 錯誤提示

#### Scenario: 空庫年份下拉版面
- **WHEN** `availableYears` 為空
- **THEN** 年份下拉佔位文字在 `<select>` 內以 ghost 呈現，不破壞 `card-body row g-2` 排版（無額外換行或溢出）
