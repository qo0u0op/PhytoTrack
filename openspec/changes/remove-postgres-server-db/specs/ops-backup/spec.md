## MODIFIED Requirements

### Requirement: 備份腳本

系統 SHALL 僅針對 SQLite 本地檔案提供帶時間戳備份腳本，不提供伺服器資料庫的備份策略；部署文件 SHALL 僅說明 SQLite 本地備份與還原，不含 PostgreSQL 相關章節。

#### Scenario: 執行備份
- **WHEN** 執行備份腳本
- **THEN** 產生一個帶時間戳的 SQLite 備份檔

#### Scenario: 文件記錄
- **WHEN** 檢視部署文件
- **THEN** 文件僅說明 SQLite 本地備份用法與建議頻率，無 PostgreSQL 提及
