## Purpose

確立本地優先的持久化策略，僅以 SQLite 檔案型資料庫支撐 5 人內 LAN/單機運行，不提供伺服器資料庫升級路徑，以最大化個人資料主權與零運維成本。

## ADDED Requirements

### Requirement: 本地單一資料源

系統 SHALL 僅支援 SQLite 檔案型資料源（`backend/diagnoses.db`，`SQLiteDialect` + `converter/`），不提供 PostgreSQL 或其他伺服器資料庫的 profile、依賴與文件；任何提及 `application-postgres.yaml`、`POSTGRES_*` 或伺服器 DB 遷移的程式與文件 SHALL 視為已移除。

#### Scenario: 嘗試以 postgres profile 啟動
- **WHEN** 執行 `mvn spring-boot:run -Dspring-boot.run.profiles=postgres`
- **THEN** 啟動失敗或明確提示「PostgreSQL 支援已移除，請使用 SQLite 本地運行」

#### Scenario: 文件無伺服器 DB 提及
- **WHEN** 檢視 `docs/ARCHITECTURE.md` 技術選型、`docs/DEPLOY.md` 與 `README.md`
- **THEN** 僅描述 SQLite，不含 PostgreSQL 升級章節或連線設定

### Requirement: 本地資料主權取捨闡明

`docs/adr/ADR-007-sqlite-postgresql.md` SHALL 記錄決策修訂：2026-09-09 起確定不上線，選擇 SQLite 本地優先，權衡為「零運維、資料不出機、5 人並發足夠」 vs 「喪失多寫並發、集中備份與水平擴展」。

#### Scenario: 閱讀 ADR-007
- **WHEN** 檢視 `docs/adr/ADR-007*`
- **THEN** 內含「本地優先、PostgreSQL 路徑已移除」段落與上述權衡說明
