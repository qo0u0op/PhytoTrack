## Purpose

為案件導入明確狀態生命週期（待處理／已處理／已結案）並補全更新契約，使案件可被完整追蹤與修正。

## ADDED Requirements

### Requirement: 案件狀態列舉

系統 SHALL 以列舉值表示案件狀態：`PENDING`（待處理）、`RESOLVED`（已處理）、`CLOSED`（已結案）。

#### Scenario: 建立新案件
- **WHEN** 建立案件
- **THEN** 案件狀態為 `PENDING`

### Requirement: 狀態轉移規則

STAFF/ADMIN SHALL 可將案件由 `PENDING` 標記為 `RESOLVED`；ADMIN SHALL 可將 `RESOLVED` 標記為 `CLOSED`；系統 SHALL 拒絕任何非法轉移。

#### Scenario: 合法轉移
- **WHEN** STAFF 將 `PENDING` 案件標記為 `RESOLVED`
- **THEN** 案件狀態更新為 `RESOLVED`

#### Scenario: 非法轉移
- **WHEN** STAFF 嘗試直接將案件標記為 `CLOSED`
- **THEN** 回應 4xx，且案件狀態維持不變

### Requirement: 案件更新契約補全

更新案件 SHALL 允許修改送件人、作物、病蟲害等多對多關聯，以及既有可編輯欄位。

#### Scenario: 修正送件人
- **WHEN** STAFF 更新案件的送件人姓名
- **THEN** 案件明細顯示更新後的值

### Requirement: 既有狀態資料遷移

系統 SHALL 將既有整數狀態資料對映至新列舉（`0` → `PENDING`），遷移後既有案件可正常讀取與查詢。

#### Scenario: 讀取既有案件
- **WHEN** 讀取遷移前建立的案件
- **THEN** 狀態顯示為 `PENDING` 且查詢正常