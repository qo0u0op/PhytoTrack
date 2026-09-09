## Context

`application-postgres.yaml` 與相關文件構成 PostgreSQL 升級路徑，但產品已定為本地單機，個人資料不上線更安全，保留伺服器 DB 路徑反而增加依賴與認知負擔。

## Goals / Non-Goals

**Goals:**
- 刪除 `application-postgres.yaml` 與所有 PostgreSQL 提及，僅 SQLite
- ADR-007 補修訂段落闡明本地優先取捨

**Non-Goals:**
- 不改備份腳本本身邏輯（仍為 SQLite 單檔備份）
- 不引入加密或遠端備份新功能（本 change 僅移除）

## Decisions

- **直接刪除而非棄用**：`application-postgres.yaml` 無外部依賴，直接刪除最乾淨；`pom.xml` 已無 `postgresql` 依賴，無需再刪。替代：保留並標 deprecated → 增加噪音 → 捨棄。
- **ADR 修訂而非重寫**：於 ADR-007 原文末補「2026-09-09 修訂：確定不上線…」段落，保留歷史決策脈絡。替代：新建 ADR-014 → 分散 → 捨棄。

## Risks / Trade-offs

- [既有 postgres 使用者] 以 `postgres` profile 啟動將失效 → Mitigation：**BREAKING**，文件明確，且本地資料可用 `pg_dump` 事先匯出
- [文件殘留] 全庫 `grep -r postgres -i` 仍可能命中 notebook → Mitigation：僅清理主文件與 ADR，notebook 歷史保留

## Migration Plan

1. 刪除 `application-postgres.yaml` → 清理 `docs/DEPLOY.md:240`、`docs/ARCHITECTURE.md:2`、`README.md` 相關段落 → `docs/adr/ADR-007*` 補修訂
2. `openspec validate --specs --changes` 與 `mvn test` 綠燈

## Open Questions

- 無
