# Reference Data Admin Specification

## Purpose

讓管理者可維護作物、病蟲害、服務方式、送達方式與標的等參照資料，無需修改資料庫種子。

## Requirements

### Requirement: 參照資料寫入管理

ADMIN SHALL 可新增、修改與刪除作物、病蟲害、服務方式、送達方式、標的等參照資料；刪除已被案件引用的資料 SHALL 被拒絕。

#### Scenario: 新增作物
- **WHEN** ADMIN 新增一筆作物
- **THEN** 該作物可於案件表單中選用

#### Scenario: 刪除被引用資料
- **WHEN** ADMIN 刪除已被案件引用的作物
- **THEN** 回應 4xx，且資料保留

### Requirement: 參照資料管理視圖

前端 SHALL 提供 ADMIN 專用的參照資料管理頁面，以列表與表單進行維護。

#### Scenario: 檢視與編輯參照資料
- **WHEN** ADMIN 進入參照資料管理頁
- **THEN** 可檢視列表並新增／修改／刪除資料