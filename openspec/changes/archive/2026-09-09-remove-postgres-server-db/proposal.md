## Why

產品已確定以本地單機 SQLite 為最終形態，不上線可最大化避免個人資料外流，PostgreSQL 作為伺服器資料庫的升級路徑已無實質需求，保留反而增加依賴、文件與心智負擔。需直接移除伺服器資料庫相關程式與文件，並於 ADR 與規格中闡明本地優先的取捨。

## What Changes

- **移除 PostgreSQL 支援**：刪除 `backend/src/main/resources/application-postgres.yaml`、相關 `pom.xml` 註解與 `docs/DEPLOY.md:240`、`docs/ARCHITECTURE.md` 的 PostgreSQL 升級章節與提及，僅保留 SQLite 單一資料源
- **依賴與程式收斂**：`pom.xml` 若有 `postgresql` 依賴則移除（現為無，已核實 `backend/pom.xml:92` 僅 `sqlite-jdbc`），`converter/` 與 `SQLiteDialect` 维持，無需額外分支
- **文件與 ADR 同步**：`docs/adr/ADR-007-sqlite-postgresql.md` 補「決策修訂：2026-09-09 起確定不上線，PostgreSQL 路徑予以移除，權衡為本地資料主權與零運維 vs 喪失多寫並發與集中備份」；`docs/ARCHITECTURE.md:2` 技術選型表與 `docs/DEPLOY.md:240` 配合刪除；`README.md` 若提及 PostgreSQL 則移除
- **規格闡明**：`openspec/specs/ops-backup` 或新建 `persistence` 能力以規格形式固定「僅 SQLite 本地持久化」策略

## Capabilities

### New Capabilities
- `persistence`: 本地優先持久化策略（僅 SQLite 檔案型，5 人內 LAN/單機，不支援伺服器 DB）

### Modified Capabilities
- `ops-backup`: 移除 PostgreSQL 相關提及，聚焦 SQLite 本地備份與還原

## Impact

- 後端：`application-postgres.yaml` 刪除、`pom.xml` 無新增、`converter/` 維持
- 文件：`docs/adr/ADR-007*`、`docs/ARCHITECTURE.md`、`docs/DEPLOY.md`、`README.md`、`docs/REQUIREMENTS.md`（如提及）
- 風險：**BREAKING** 對曾以 `postgres` profile 啟動者失效（符合產品方向，本地資料需自行匯出）
