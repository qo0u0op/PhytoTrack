## MODIFIED Requirements

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

前端 Dashboard SHALL 以純 Bootstrap（卡片、進度條、表格）呈現統計資料，不依賴第三方圖表庫；其中「田區位置」卡僅顯示 `fieldCityBreakdown`（Top 10 縣市）。

#### Scenario: 檢視 Dashboard
- **WHEN** 登入使用者進入 Dashboard
- **THEN** 顯示各項統計數據與 AI 模型連線狀態，且「田區位置」卡顯示縣市 breakdown

## REMOVED Requirements

### Requirement: fieldDistrictBreakdown 回應欄位

**Reason**: 該欄位自 `align-case-field-order` 引入後從未被前端消費、無測試覆蓋、亦未在主規格定義，屬 scope creep。僅保留 `fieldCityBreakdown` 已滿足「田區位置 Top 10」需求。

**Migration**: 前端無需改動（已僅依賴 `fieldCityBreakdown`）；若外部呼叫端曾依賴 `fieldDistrictBreakdown`，請改以 `fieldCityBreakdown` 或自行以 `GET /api/cases` 篩選後聚合。
