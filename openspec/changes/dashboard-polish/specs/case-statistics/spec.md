## MODIFIED Requirements

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
