# Design: ops-backup 備份腳本與文件

## Context

見 `proposal.md`（Why）。現況 `backend/diagnoses.db` 為 SQLite 單檔，無自動備份；`docs/DEPLOY.md` 尚無備份章節；`backups/` 與 `*.db` 已在 `.gitignore` 但無腳本。

## Goals / Non-Goals

**Goals：**
- 提供一鍵帶時間戳備份，零依賴（僅 `cp`/`date`/`mkdir`）
- 文件涵蓋用法、頻率、還原步驟

**Non-Goals：**
- 自動化排程（cron/systemd timer）、遠端備份、加密、增量備份
- 備份驗證（`sqlite3 .dump` 完整性檢查）超出 MVP，可後補

## Decisions

1. **腳本位置與命名**：`scripts/backup.sh`（POSIX `sh`，非 `bash` 獨有語法，兼容 macOS/Linux），與 `mise.toml` 的 `scripts/` 慣例一致
2. **時間戳格式**：`$(date +%Y%m%d-%H%M%S)`（本地時間，檔名排序即時間排序）
3. **來源路徑解析**：腳本以自身目錄為基準解析 `../backend/diagnoses.db` 與專案根 `diagnoses.db` 的存在性，優先 `backend/diagnoses.db`
4. **錯誤處理**：來源不存在 → `echo` 提示並 `exit 1`；`cp` 失敗 → 同步提示；成功時印出備份路徑

## Risks / Trade-offs

- SQLite 熱備份需確保無寫入中：單檔 `cp` 在 `Hikari pool-size:1` 與低併發下可接受；高併發時建議 `sqlite3 .backup`，本次不引入額外依賴
- `backups/` 未納版控，誤刪即失：文件強調保留策略與異地備份建議

## Migration Plan

- 無遷移；腳本與文件皆新增，刪除即回滾

## Open Questions

- 備份保留期限與清理策略（本次不設，由維運手動清理）
