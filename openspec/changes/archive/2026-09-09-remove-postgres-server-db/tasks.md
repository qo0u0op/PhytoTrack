## 1. 移除伺服器資料庫支援

- [x] 1.1 刪除 `backend/src/main/resources/application-postgres.yaml`，確認 `pom.xml` 無 `postgresql` 依賴（已為無），`mvn test` 綠燈
- [x] 1.2 清理文件 `docs/DEPLOY.md:240` 升級到 PostgreSQL 章節、`docs/ARCHITECTURE.md:2` 技術選型表與相關提及、`README.md` 如提及則移除，`grep -r -i postgres docs/ --exclude-dir=notebook | head` 僅餘「已移除」修訂說明

## 2. 文件與 ADR 同步

- [x] 2.1 於 `docs/adr/ADR-007-sqlite-postgresql.md` 文末補「2026-09-09 修訂：確定不上線，PostgreSQL 路徑已移除」段落，闡明權衡（零運維/資料主權 vs 多寫並發/集中備份），`openspec validate --specs --changes` 通過
- [x] 2.2 執行 `openspec validate --specs --changes` 16 passed 與 `grep -r postgres -i backend/src/main/resources` 為空，確認無殘留 profile
