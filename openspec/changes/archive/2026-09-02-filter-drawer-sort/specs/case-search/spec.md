## MODIFIED Requirements

### Requirement: 案件列表支援篩選參數

`GET /api/cases` SHALL 接受可選查詢參數：`cropId`、`serviceId`、`senderName` (部分比對)、`receiveDateFrom`、`receiveDateTo`、`status`；多個參數同時存在時 SHALL 以 AND 組合，並維持分頁回傳。案件管理頁的篩選卡片 SHALL 為抽屜式，預設收合隱藏，點擊按鈕展開/收合。

#### Scenario: 依作物與狀態篩選
- **WHEN** 請求帶 `cropId=3` 與 `status=RESOLVED`
- **THEN** 僅回傳同時符合兩條件的案件分頁

#### Scenario: 送件人部分比對
- **WHEN** 請求帶 `senderName=張`
- **THEN** 回傳送件人姓名含「張」的案件

#### Scenario: 未帶篩選條件
- **WHEN** 請求未帶任何篩選參數
- **THEN** 行為與現有分頁列表一致

#### Scenario: 篩選抽屜預設隱藏
- **WHEN** 使用者進入案件管理頁
- **THEN** 篩選卡片預設收合，僅顯示「篩選」按鈕

#### Scenario: 展開篩選
- **WHEN** 點擊篩選按鈕
- **THEN** 展開顯示所有篩選欄位，再次點擊收合

## ADDED Requirements

### Requirement: 案件列表排序

案件列表 SHALL 支援依所有欄位（除操作外）點擊表頭進行升冪/降冪排序，前端本地排序，預設依收件日期降冪，表頭以箭頭指示當前排序。

#### Scenario: 點擊表頭排序
- **WHEN** 點擊任一非操作欄的表頭
- **THEN** 依該欄位切換 asc/desc 並重排列表

#### Scenario: 預設排序
- **WHEN** 未指定排序
- **THEN** 依收件日期降冪顯示

#### Scenario: 操作欄不可排序
- **WHEN** 檢視操作欄表頭
- **THEN** 不提供排序互動
