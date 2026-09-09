## Context

見 `proposal.md` Why。`remove-postgres-server-db` 已定本地單一 SQLite，`ai-external-provider-viewer-guard` 已定 Viewer 隔離，explore 階段結論為「VIEWER 漫遊篩選不需即時、WAL + pool=5 + debounce/abort 優於全表快取」。

## Goals / Non-Goals

**Goals:**
- 將「為何選 WAL 而非 cache」固化為 spec 與 ADR，避免後人重提 cache
- 明確 4 讀 1 寫漫遊場景的可接受延遲與驗收方式

**Non-Goals:**
- 不改程式（WAL 已在另案 `enable-wal` 實作，本案僅文件化）
- 不重提 PostgreSQL 遷移細節（已移除）

## Decisions

- **新建 `persistence` 而非散寫多 spec**：併發策略屬持久層橫切關注，集中於 `persistence` 比散在 `case-search`/`case-statistics` 易查。替代：各 spec 各加 cache 條目 → 分散 → 捨棄。
- **ADR-014 而非改 ADR-007**：ADR-007 已記錄「為何選 SQLite 不選 PG」，新增 ADR-014 記錄「為何選 WAL + pool=5 不選 cache」，職責分離。替代：全塞 ADR-007 → 過長 → 捨棄。
- **規格用可觀測指標而非實作細節**：`P95 <150ms (4 併發讀)`、`ab -n 20 -c 4` 為驗收，不寫 `HikariConfig` 行號。

## Risks / Trade-offs

- [WAL 備份遺漏] 僅拷 `diagnoses.db` 會丟 `-wal` → Mitigation：規格明確「需含 -wal/-shm 或先 checkpoint」
- [過度文件化] 為上下文建 spec 可能被視為噪音 → Mitigation：保持 1 個 `persistence` 能力，不為每個小決策建 spec

## Migration Plan

1. 建 `openspec/specs/persistence/spec.md` → 補 `docs/adr/ADR-014*` → 同步 `ARCHITECTURE.md` 併發小節
2. `openspec validate` 通過即完成，無程式遷移

## Open Questions

- 無
