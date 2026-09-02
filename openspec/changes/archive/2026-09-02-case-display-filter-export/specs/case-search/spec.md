## MODIFIED Requirements

### Requirement: 案件列表支援篩選參數

`GET /api/cases` SHALL 接受可選查詢參數：`receiveDateFrom`、`receiveDateTo`、`status`、`fieldCityId`、`fieldDistrictId`、`senderName` (部分比對，含 `senderQuery` 行為)、`senderTypeId`、`serviceId`、`deliveryId`、`methodId`、`cropCategoryId`、`cropId`、`damageId`、`pestTypeId`、`pestCategoryId`、`hintId`；多個參數同時存在時 SHALL 以 AND 組合，並維持分頁回傳。案件管理頁的篩選卡片 SHALL 為抽屜式，預設收合隱藏，點擊按鈕展開/收合，且篩選欄位版面順序 SHALL 為：`收件日期區間 → 狀態 → 田區縣市 → 田區鄉鎮 → 送件人 → 身分別 → 服務類別 → 送件方式 → 耕種方式 → 作物類別 → 作物 → 被害部位 → 害物 → 害物類別 → 建議類別`。

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
