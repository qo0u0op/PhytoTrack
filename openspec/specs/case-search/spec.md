# Case Search Specification

## Purpose

讓案件列表可依作物、診斷員、送件人、日期區間與狀態篩選，滿足診斷站的查詢需求，並支援多條件組合與分頁瀏覽大量案件。

## Requirements

### Requirement: 案件列表支援篩選參數

`GET /api/cases` SHALL 接受可選查詢參數：`receiveDateFrom`、`receiveDateTo`、`status`、`fieldCityId`、`fieldDistrictId`、`senderName` (部分比對，含 `senderQuery` 行為)、`senderTypeId`、`serviceId`、`deliveryId`、`methodId`、`cropCategoryId`、`cropId`、`damageId`、`pestTypeId`、`pestCategoryId`、`hintId`；多個參數同時存在時 SHALL 以 AND 組合，並維持分頁回傳。案件管理頁的篩選卡片 SHALL 為抽屜式，預設收合隱藏，點擊按鈕展開/收合，且篩選欄位版面順序 SHALL 為：`收件日期區間 → 狀態 → 田區縣市 → 田區鄉鎮 → 送件人 → 身分別 → 服務類別 → 送件方式 → 耕種方式 → 作物類別 → 作物 → 被害部位 → 害物 → 害物類別 → 建議類別`。進入檢視（`/cases/:id`）或編輯（`/cases/:id/edit`）後按「上一頁」返回 `/cases` 時，篩選條件 SHALL 保持不重置。

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

#### Scenario: 身分別篩選
- **WHEN** 請求帶 `senderTypeId=2`
- **THEN** 僅回傳該身分別的案件

#### Scenario: 耕種方式篩選
- **WHEN** 請求帶 `methodId=1`
- **THEN** 僅回傳該耕種方式的案件

#### Scenario: 篩選欄位排序
- **WHEN** 展開篩選卡片
- **THEN** 欄位依 `收件日期區間 → 狀態 → 田區縣市 → 田區鄉鎮 → 送件人 → 身分別 → 服務類別 → 送件方式 → 耕種方式 → 作物類別 → 作物 → 被害部位 → 害物 → 害物類別 → 建議類別` 順序呈現

#### Scenario: 篩選影響 CSV 匯出
- **WHEN** 使用者套用篩選後呼叫 `GET /api/cases/export` 並帶相同篩選參數
- **THEN** 匯出結果僅含符合該篩選的案件

#### Scenario: 進入檢視後返回保持篩選
- **WHEN** 使用者於案件管理頁套用篩選（例 `status=待處理` 且 `senderTypeId=1`）後進入任一案件的檢視頁，再按瀏覽器上一頁或頁面「返回」按鈕回到 `/cases`
- **THEN** 篩選條件保持與進入前一致，列表顯示相同篩選結果且不重置為無篩選

### Requirement: 案件列表排序

案件列表 SHALL 支援依所有欄位（除操作外）點擊表頭進行升冪/降冪排序，前端本地排序，預設依收件日期降冪，表頭以箭頭指示當前排序。進入檢視/編輯後返回時，排序狀態 SHALL 保持。

#### Scenario: 點擊表頭排序
- **WHEN** 點擊任一非操作欄的表頭
- **THEN** 依該欄位切換 asc/desc 並重排列表

#### Scenario: 預設排序
- **WHEN** 未指定排序
- **THEN** 依收件日期降冪顯示

#### Scenario: 操作欄不可排序
- **WHEN** 檢視操作欄表頭
- **THEN** 不提供排序互動

#### Scenario: 返回保持排序
- **WHEN** 使用者變更排序後進入檢視/編輯再返回
- **THEN** 表頭排序指示與列表順序保持與進入前一致

### Requirement: 案件列表新增按鈕

案件管理頁的標題列 SHALL 顯示「新增」按鈕（原「建立案件」），樣式與其他管理頁一致（`btn-sm btn-success`，右上角與篩選按鈕同列），僅 STAFF 可見。

#### Scenario: 顯示新增按鈕
- **WHEN** STAFF 進入案件管理頁
- **THEN** 顯示「新增」按鈕

#### Scenario: VIEWER 不顯示
- **WHEN** VIEWER 進入案件管理頁
- **THEN** 不顯示新增按鈕

### Requirement: 案件列表分頁與每頁筆數保持

案件列表 SHALL 支援分頁（`page`/`size`）且 `size` 可選 10/20/50/100，預設 10。進入檢視/編輯後返回時，`page` 與 `size` SHALL 保持。

#### Scenario: 調整每頁筆數後返回保持
- **WHEN** 使用者將 `size` 改為 50 並切至第 3 頁後進入檢視再返回
- **THEN** 仍顯示第 3 頁且 `size` 為 50，不重置為第 1 頁或 10 筆/頁

#### Scenario: 重新整理保持狀態
- **WHEN** 使用者於帶有篩選、分頁與排序的 `/cases?status=...&page=2&size=50&sort=...` 重新整理頁面
- **THEN** 仍以 URL 參數還原相同篩選、分頁與排序
