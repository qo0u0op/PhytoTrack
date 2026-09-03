# Ops Backup Specification

## Purpose

提供簡單的 SQLite 資料庫備份腳本與操作文件，保障診斷記錄不因誤刪或損壞而遺失，並支援排程自動備份與還原驗證流程。

## Requirements

### Requirement: 備份腳本

系統 SHALL 提供腳本，將 SQLite 資料庫檔案複製為帶時間戳的備份檔，並於部署文件中記錄使用方法。

#### Scenario: 執行備份
- **WHEN** 執行備份腳本
- **THEN** 產生一個帶時間戳的資料庫備份檔

#### Scenario: 文件記錄
- **WHEN** 檢視部署文件
- **THEN** 文件說明備份腳本的用法與建議頻率