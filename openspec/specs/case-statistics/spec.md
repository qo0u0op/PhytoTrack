# Case Statistics Specification

## Purpose

提供案件統計總覽與 Dashboard 視圖，讓診斷站掌握案件量、作物與病蟲害分布及趨勢。

## Requirements

### Requirement: 統計總覽 API

系統 SHALL 提供統計端點，回傳案件總數、本月新增數、待處理數、作物 topN、病蟲害 topN、狀態比例與近 N 月案件數趨勢，以及期別（HISTORICAL/ANNUAL/MONTHLY）篩選與 breakdown（作物種類、病蟲害類型、送件方式、耕種方式、防治建議、田區位置縣市）。

#### Scenario: 取得統計資料
- **WHEN** 登入使用者呼叫統計端點
- **THEN** 回應包含上述各項統計數據

#### Scenario: 空資料庫
- **WHEN** 資料庫中沒有案件
- **THEN** 各項數值為 0 或空清單，且不回傳錯誤

#### Scenario: 田區位置 breakdown 僅回傳縣市
- **WHEN** 呼叫統計端點（含期別篩選）
- **THEN** 回應包含 `fieldCityBreakdown`（依 `fieldDistrict.city` 分組的 Top 10 縣市），且不包含 `fieldDistrictBreakdown`（已移除）

### Requirement: Dashboard 統計視圖

前端 Dashboard SHALL 以純 Bootstrap（卡片、進度條、表格）呈現統計資料，不依賴第三方圖表庫；其中「田區位置」卡僅顯示 `fieldCityBreakdown`（Top 10 縣市）。期間顯示 SHALL 為中文（歷史/年度/月度），年份選單 SHALL 預設為可用年份最新值且不含空選項，AI 卡片標題 SHALL 為「AI 連線情況」，近半年趨勢卡 SHALL 與耕種方式並列且標題為「近半年案件趨勢」，且頁面 SHALL 不含底部導覽按鈕列。

#### Scenario: 檢視 Dashboard
- **WHEN** 登入使用者進入 Dashboard
- **THEN** 顯示各項統計數據與 AI 連線情況，且「田區位置」卡顯示縣市 breakdown

#### Scenario: 期間中文化
- **WHEN** 切換期別（HISTORICAL/ANNUAL/MONTHLY）
- **THEN** 期間文字顯示為「歷史/年度/月度」而非英文枚舉

#### Scenario: 年份必填
- **WHEN** 選擇年度或月度期別
- **THEN** 年份預設為最新可用年份且無「請選擇年份」空選項，避免空值送後端導致 400

#### Scenario: 近半年趨勢位置與標題
- **WHEN** 檢視 Dashboard 的 breakdown 區
- **THEN** 「近半年案件趨勢」卡與耕種方式卡並列，且標題為「近半年案件趨勢」

#### Scenario: 無底部導覽
- **WHEN** 檢視 Dashboard
- **THEN** 頁面不顯示「建立新診斷案件/案件管理/使用者管理」底部按鈕列
