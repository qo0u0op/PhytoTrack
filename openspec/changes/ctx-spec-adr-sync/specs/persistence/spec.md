## Purpose

固化本地優先的持久化與併發策略，確保 4 讀 1 寫的 VIEWER 漫遊場景不卡，且個人資料不出機的前提取捨可追溯。

## ADDED Requirements

### Requirement: SQLite 併發不卡

系統 SHALL 以 `SQLite WAL` + `Hikari maximum-pool-size=5` 支援 4 併發讀與 1 寫並行，P95（`ab -n 20 -c 4` 對 `GET /api/cases`） SHALL <150ms；`VIEWER` 漫遊篩選的列表查詢 SHALL 不因 `STAFF` 的慢打編輯（未提交前）而排隊。

#### Scenario: 4 讀並行
- **WHEN** 4 個 VIEWER 同時以不同隨機篩選呼叫 `GET /api/cases`
- **THEN** 4 個請求並行執行，總時約等於單次查詢時長（~80ms），非 320ms 接力

#### Scenario: 讀不阻塞寫
- **WHEN** 1 STAFF 執行 `PUT /api/cases/{id}`（400ms 交易）期間 4 VIEWER 持續翻頁
- **THEN** VIEWER 讀取舊快照成功，不報 `database is locked`，寫提交後後續讀可見新資料

### Requirement: 列表不快取，統計可選短 TTL

`v_case_search` 列表查詢 SHALL 不做全表快取（17 欄隨機組合命中率 <15%），`Dashboard` 統計可選 30 秒 TTL 快取（key 含 `period/year/month` + `role`），參照資料（`crop/city/pest` 全表）可永久快取直到寫時 `evict`。

#### Scenario: 隨機篩選不命中
- **WHEN** VIEWER 依序查 `台北+褐斑病` 與 `台中+白粉病`
- **THEN** 兩次皆穿透至 DB，不因命中率低而汙染快取

### Requirement: WAL 備份完整性

備份腳本 SHALL 在 WAL 模式下拷貝 `diagnoses.db`、`diagnoses.db-wal`、`diagnoses.db-shm` 三檔或先執行 `PRAGMA wal_checkpoint(TRUNCATE)` 後再拷單檔，還原後資料完整。

#### Scenario: WAL 備份
- **WHEN** 執行 `scripts/backup.sh` 時 `journal_mode=WAL`
- **THEN** 備份目錄含對應 `-wal/-shm` 或已 checkpoint，單檔還原後不丟尾巴
