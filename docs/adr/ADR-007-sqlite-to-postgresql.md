# ADR-007: SQLite 起步、預留 PostgreSQL 升級路徑

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

系統是 5 人內團隊的區域網路 (LAN) 診斷記錄服務。需求是「clone 下來就能跑、零維護、免安裝資料庫」，但實務上未來可能擴到 PostgreSQL。

**選項**:

1. **SQLite 現在用，透過 JPA 方言層預留 PostgreSQL**
2. 直接上 PostgreSQL (本機 Docker / 服務)
3. 直接用 MySQL

**決策**:

- 現階段：**SQLite** (`sqlite-jdbc` + `hibernate-community-dialects` 的 `SQLiteDialect`)，HikariCP `maximum-pool-size: 1`
- 保留既有 `schema.sql` (含 761 行種子資料) 作為資料基底
- 提供 `application-postgres.yaml` profile + PostgreSQL 驅動設定，**升級時只動 datasource 與 dialect，業務程式碼不變**

**原因**:

- **SQLite 現在用**：檔案型資料庫 (`./diagnoses.db`)，零安裝、clone 即跑，適合小型內部工具場景；<5 人查詢量 SQLite 綽綽有餘 (YAGNI)
- **預留 PostgreSQL**：PostgreSQL 是關聯式首選 (JSONB、全文搜尋、並發寫入、真正的連接池)；JPA 的方言抽象層正是為了「換 DB 不動業務碼」
- **為什麼 pool=1**：SQLite 是檔案鎖定，同時多連線寫入會 `database is locked`，連線池 >1 沒有意義

**取捨**:

- SQLite 無真正的並發寫入、DDL 能力弱——對 <5 人團隊刻意接受
- **保持可移植性的代價**：不寫 SQLite 專屬 SQL、不依賴 Hibernate 自動 DDL 產生的方言差異；升級前需用 Flyway 或 dump 遷移既有資料
- 暫不引進 Flyway：現況以 `schema.sql` 單一來源即可，待規模與併發需求出現再遷 (YAGNI，見 ADR 索引)
