# ADR-014：併發不卡改 WAL 而非全表快取

**日期**: 2026-09-09

**狀態**: 已實作

**背景**:

5 人內 LAN，`Hikari maximum-pool-size=1` + `SQLite DELETE 模式` 導致 `v_case_search`（17 欄隨機組合）與 `Dashboard` 統計在 4 VIEWER 漫遊篩選時排隊。Explore 階段評估「為何選 cache」與 STAFF 慢打時 4 讀的 thread 模型。

**選項**:

1. **全表快取 `v_case_search`**：以 17 欄 + page + role 為 key，命中率 <15%（隨機漫遊），且需為 `VIEWER` 遮蔽單獨 key，失效策略複雜
2. **WAL + pool=5 + debounce/abort**：`PRAGMA journal_mode=WAL` 讓讀不阻塞寫，pool 5 讓 4 讀並行；前端 300ms debounce + abort 舊請求削峰
3. **維持 pool=1，不加 cache**：最簡，但 4 讀接力 320ms，體感頓

**決策**:

- 啟用 `WAL` + `maximum-pool-size=5`，`P95(ab -n 20 -c 4 GET /api/cases) <150ms` 為驗收
- 列表不做全表快取，Dashboard 統計可選 30 秒 TTL（key 含 period/year/month+role），參照資料永久快取直到寫時 evict
- 前端篩選加 debounce/abort 為互補，不在本 ADR 強制

**原因**:

- WAL 讓讀舊快照成功，符合「VIEWER 不需即時」前提；快取省 60ms 查詢，不省 400ms 排隊
- 隨機漫遊命中率低，快取收益小於失效地獄成本
- 5 人場景下 WAL + 5 條連線已讓 4 讀並行，改動僅 2 行設定 + 備份腳本三檔處理

**取捨**:

- WAL 備份需拷 `-wal/-shm` 或先 `wal_checkpoint(TRUNCATE)`，已於 `ops-backup` 規格與 `scripts/backup.sh` 體現
- 寫入仍單寫者，極端同秒多寫仍序列，但 5 人內可接受

**驗證**:

- `PRAGMA journal_mode;` 回 `wal`
- `ab -n 20 -c 4 http://localhost:8080/api/cases` P95 <150ms
- `scripts/backup.sh` 在 WAL 下備份含三檔
