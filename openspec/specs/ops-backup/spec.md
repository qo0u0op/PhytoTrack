# Ops Backup Specification

## Purpose

提供簡單的 SQLite 資料庫備份腳本與操作文件，保障診斷記錄不因誤刪或損壞而遺失，並支援排程自動備份與還原驗證流程。

## Requirements

### Requirement: 備份腳本

系統 SHALL 僅針對 SQLite 本地檔案提供帶時間戳備份腳本，並在 WAL 模式下處理 `-wal/-shm`（拷三檔或先 checkpoint），部署文件 SHALL 說明此差異；不提供伺服器資料庫的備份策略。

#### Scenario: 執行備份
- **WHEN** 執行備份腳本
- **THEN** 產生一個帶時間戳的 SQLite 備份檔（含 WAL 完整性處理）

#### Scenario: 文件記錄
- **WHEN** 檢視部署文件
- **THEN** 文件說明備份腳本的用法、WAL 下的三檔處理與建議頻率