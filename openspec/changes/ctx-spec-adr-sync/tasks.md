## 1. 規格與 ADR 固化

- [x] 1.1 建立 `openspec/specs/persistence/spec.md`（WAL + pool=5 + 列表不快取 + WAL 備份三檔），`openspec validate --specs --changes` 通過
- [x] 1.2 更新 `openspec/specs/ops-backup/spec.md` 補 WAL 三檔說明，已新建 `docs/adr/ADR-014-concurrency-wal-vs-cache.md` 記錄「為何選 WAL + debounce/abort 而非全表快取」取捨，`openspec validate` 通過

## 2. 文件對齊

- [x] 2.1 於 `docs/ARCHITECTURE.md` 併發小節補 P95 <150ms 與 `ab -n 20 -c 4` 驗收，`docs/DEPLOY.md` 備份章節補 WAL 三檔處理，`docs/adr/README.md` 索引更新
