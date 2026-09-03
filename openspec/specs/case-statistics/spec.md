# Case Statistics Specification

## Purpose

提供案件統計總覽與 Dashboard 視圖，讓診斷站掌握案件量、作物與病蟲害分布及趨勢，作為人力調度與防治宣導的決策依據。

## Requirements

### Requirement: 統計總覽 API

系統 SHALL 提供統計端點，回傳案件總數、本月新增數、待處理數、作物 topN、病蟲害 topN、狀態比例與近 N 月案件數趨勢，以及期別（`HISTORICAL/ANNUAL/MONTHLY/HALF_YEAR`）篩選與 breakdown（作物種類、病蟲害類型、送件方式、耕種方式、防治建議、田區位置縣市）。其中 `HALF_YEAR` 需配合 `year` 與 `half`（1=1-6 月, 2=7-12 月）僅統計該半年案件；`availableYears` SHALL 包含有資料的年份供前端選單使用。

#### Scenario: 取得統計資料
- **WHEN** 登入使用者呼叫統計端點
- **THEN** 回應包含上述各項統計數據

#### Scenario: 空資料庫
- **WHEN** 資料庫中沒有案件
- **THEN** 各項數值為 0 或空清單，且不回傳錯誤

#### Scenario: 田區位置 breakdown 僅回傳縣市
- **WHEN** 呼叫統計端點（含期別篩選）
- **THEN** 回應包含 `fieldCityBreakdown`（依 `fieldDistrict.city` 分組的 Top 10 縣市），且不包含 `fieldDistrictBreakdown`（已移除）

#### Scenario: 半年度篩選
- **WHEN** 呼叫 `GET /api/cases/statistics?period=HALF_YEAR&year=2026&half=1`
- **THEN** 僅統計 2026 年 1-6 月的案件；`half=2` 則為 7-12 月

#### Scenario: 半年度參數驗證
- **WHEN** `period=HALF_YEAR` 但缺 `year` 或 `half`，或 `half` 非 1/2
- **THEN** 回 400 `VALIDATION_ERROR`

### Requirement: Dashboard 統計視圖

前端 Dashboard SHALL 以純 Bootstrap（卡片、進度條、表格）呈現統計資料，不依賴第三方圖表庫；其中「田區位置」卡僅顯示 `fieldCityBreakdown`（Top 10 縣市）。期間顯示 SHALL 為中文（`歷史/年度/月度/半年度`），年份選單 SHALL 預設為可用年份最新值且不含空選項，年份為 `HALF_YEAR` 時同時提供半年度選單（上半年/下半年），AI 卡片標題 SHALL 為「模型狀態」，近半年趨勢卡 SHALL 與耕種方式並列且標題為「近半年案件趨勢」，且頁面 SHALL 不含底部導覽按鈕列與期別小字「期間：xxx」；所有百分比 SHALL 以一位小數顯示。

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
