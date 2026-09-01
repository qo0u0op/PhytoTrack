# Case Statistics Specification

## Purpose

提供案件統計總覽與 Dashboard 視圖，讓診斷站掌握案件量、作物與病蟲害分布及趨勢。

## Requirements

### Requirement: 統計總覽 API

系統 SHALL 提供統計端點，回傳案件總數、本月新增數、待處理數、作物 topN、病蟲害 topN、狀態比例與近 N 月案件數趨勢。

#### Scenario: 取得統計資料
- **WHEN** 登入使用者呼叫統計端點
- **THEN** 回應包含上述各項統計數據

#### Scenario: 空資料庫
- **WHEN** 資料庫中沒有案件
- **THEN** 各項數值為 0 或空清單，且不回傳錯誤

### Requirement: Dashboard 統計視圖

前端 Dashboard SHALL 以純 Bootstrap (卡片、進度條、表格) 呈現統計資料，不依賴第三方圖表庫。

#### Scenario: 檢視 Dashboard
- **WHEN** 登入使用者進入 Dashboard
- **THEN** 顯示各項統計數據與 AI 模型連線狀態