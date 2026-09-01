## Purpose

讓案件列表可依作物、診斷員、送件人、日期區間與狀態篩選，滿足診斷站的查詢需求。

## ADDED Requirements

### Requirement: 案件列表支援篩選參數

`GET /api/cases` SHALL 接受可選查詢參數：`cropId`、`serviceId`、`senderName` (部分比對)、`receiveDateFrom`、`receiveDateTo`、`status`；多個參數同時存在時 SHALL 以 AND 組合，並維持分頁回傳。

#### Scenario: 依作物與狀態篩選
- **WHEN** 請求帶 `cropId=3` 與 `status=RESOLVED`
- **THEN** 僅回傳同時符合兩條件的案件分頁

#### Scenario: 送件人部分比對
- **WHEN** 請求帶 `senderName=張`
- **THEN** 回傳送件人姓名含「張」的案件

#### Scenario: 未帶篩選條件
- **WHEN** 請求未帶任何篩選參數
- **THEN** 行為與現有分頁列表一致