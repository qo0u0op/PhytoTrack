## ADDED Requirements

### Requirement: 簽名人篩選抽屜

簽名人管理頁的篩選列（名稱篩選輸入、顯示已停用勾選、筆數）SHALL 收進可收合的篩選抽屜，預設為隱藏；點擊篩選按鈕 SHALL 展開，再次點擊 SHALL 收合。篩選邏輯維持既有前端本地過濾不變。

#### Scenario: 預設隱藏篩選列
- **WHEN** 使用者進入簽名人管理頁
- **THEN** 僅顯示篩選按鈕，不直接顯示篩選列

#### Scenario: 展開與收合篩選抽屜
- **WHEN** 點擊篩選按鈕
- **THEN** 篩選抽屜展開顯示名稱篩選、顯示已停用勾選與筆數；再次點擊則收合

### Requirement: 簽名人列表欄序

簽名人列表的欄序 SHALL 為帳號在前、身分別在後（ID、名稱、帳號、身分別、狀態、操作）；身分別徽章（使用者/非使用者）樣式維持不變。

#### Scenario: 帳號欄在身分別欄之前
- **WHEN** 檢視簽名人列表表頭
- **THEN** 帳號欄出現在身分別欄左側

### Requirement: 簽名人狀態徽章同款

簽名人列表的狀態欄 SHALL 與使用者管理採用同款徽章樣式（`badge text-bg-success`/`text-bg-secondary`），文字為啟用/停用，對應簽名人的啟用狀態。

#### Scenario: 啟用中顯示成功色徽章
- **WHEN** 檢視啟用中簽名人的狀態欄
- **THEN** 顯示綠色（`text-bg-success`）徽章，文字為啟用

#### Scenario: 已停用顯示灰色徽章
- **WHEN** 檢視已停用簽名人的狀態欄
- **THEN** 顯示灰色（`text-bg-secondary`）徽章，文字為停用
