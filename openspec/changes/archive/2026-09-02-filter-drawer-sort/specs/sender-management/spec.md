## MODIFIED Requirements

### Requirement: 送件人查詢 API

系統 SHALL 提供送件人搜尋端點，依 name / phone / displayName 部分比對，供建案表單與去重候選使用；CaseResponse SHALL 包含 `senderId` 與送件人縣市、鄉鎮市區名稱。送件人管理頁 SHALL 提供篩選卡片，支援依身分別（`senderTypeId`）、縣市（`cityId`）、鄉鎮市區（`districtId`，依縣市聯動）與關鍵字（姓名/電話/顯示名稱）篩選，多條件以 AND 組合。篩選卡片 SHALL 為抽屜式，預設收合隱藏，點擊按鈕展開/收合。

#### Scenario: 依關鍵字搜尋
- **WHEN** 使用者輸入關鍵字搜尋送件人
- **THEN** 回傳姓名或電話或顯示名稱相符的送件人候選

#### Scenario: 案件回應帶送件人識別
- **WHEN** 查詢案件詳細或列表
- **THEN** 回應包含 `senderId` 及送件人縣市、鄉鎮市區名稱

#### Scenario: 依身分別篩選送件人
- **WHEN** 使用者在送件人管理頁選擇身分別後觸發篩選
- **THEN** 僅顯示該身分別的送件人

#### Scenario: 依縣市與鄉鎮市區篩選
- **WHEN** 選擇縣市後（鄉鎮市區選單依縣市聯動），再選擇鄉鎮市區
- **THEN** 僅顯示符合該縣市/鄉鎮市區的送件人，且縣市未選時鄉鎮市區不可選

#### Scenario: 多條件組合篩選
- **WHEN** 同時輸入關鍵字並選擇身分別/縣市/鄉鎮市區
- **THEN** 以 AND 組合篩選，且清除操作重置全部條件

#### Scenario: 欄位更名鄉鎮市區
- **WHEN** 檢視送件人管理表格
- **THEN** 欄位標題顯示為「鄉鎮市區」（原「鄉鎮」），與 `district` 模型一致

#### Scenario: 篩選抽屜預設隱藏
- **WHEN** 進入送件人管理頁
- **THEN** 篩選卡片預設收合，僅顯示「篩選」按鈕

#### Scenario: 展開篩選
- **WHEN** 點擊篩選按鈕
- **THEN** 展開篩選卡片

## ADDED Requirements

### Requirement: 送件人列表排序

送件人列表 SHALL 支援依所有欄位（除操作外）點擊表頭排序，前端本地排序，預設依 ID 降冪，表頭以箭頭指示。

#### Scenario: 點擊表頭排序
- **WHEN** 點擊非操作欄的表頭
- **THEN** 依該欄位切換 asc/desc

#### Scenario: 操作欄不可排序
- **WHEN** 檢視操作欄
- **THEN** 不提供排序
