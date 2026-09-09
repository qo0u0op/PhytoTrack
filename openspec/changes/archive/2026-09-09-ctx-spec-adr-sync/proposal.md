## Why

`openspec/specs` 已涵蓋 12+ 能力，但「為何選 SQLite + WAL + pool=5 而非 cache/PostgreSQL」與「AI 為何限定 Viewer 視角」等上下文仍散在對話與 `docs/notebook`，未被規格與 ADR 固化。隨著 `remove-postgres-server-db` 與 `ai-external-provider-viewer-guard` 落地，需將當前「本地優先、5 人併發不卡、個資不出機」的核心上下文提煉為正式 spec 與 ADR，以免後續誤回退。

## What Changes

- **新建 `persistence` 規格（若已存在則擴充）**：將 `SQLite 單檔 + WAL + pool=5` 明確為併發策略，`v_case_search` 讀讀並行、寫不阻塞讀，`VIEWER` 漫遊篩選命中率低故不快取列表，僅短 TTL 快取統計為可選
- **新建 `concurrency` 規格片段或併入 `persistence`**：定義 `4 讀 1 寫` 場景下的可接受延遲（P95 <150ms）、`journal_mode=WAL` 與 `maximum-pool-size=5` 為基準、備份需含 `-wal/-shm`
- **ADR 同步**：`docs/adr/ADR-007-sqlite-to-postgresql.md` 補「2026-09-09 修訂二：併發不卡改 WAL 而非 cache」段落；新建或更新 `ADR-014-concurrency-cache-decision.md` 記錄「為何選 WAL + debounce/abort 而非全表快取」的取捨
- **文件對齊**：`docs/ARCHITECTURE.md` 技術選型與併發章節、`docs/REQUIREMENTS.md` 能力表（如有）同步上述上下文，不改行為僅固化決策

## Capabilities

### New Capabilities
- `persistence`: 本地 SQLite 併發策略（WAL + pool=5 + VIEWER 隨機篩選不快取）

### Modified Capabilities
- `ops-backup`: 補 WAL 備份需含 `-wal/-shm` 或先 checkpoint 說明（若原規格僅提單檔備份）

## Impact

- 規格：`openspec/specs/persistence/spec.md`（新建或擴充）、`openspec/specs/ops-backup/spec.md`（若需）
- 文件：`docs/adr/ADR-007*`、`docs/adr/ADR-014*`（新建）、`docs/ARCHITECTURE.md` 併發段落
- 程式：無（純規格/文件固化，已實作部分以 `remove-postgres-server-db` 與 `enable-wal` 為準）
