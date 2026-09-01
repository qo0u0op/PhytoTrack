## Why

現行 SQLite 以單檔 `diagnoses.db` 承載所有診斷記錄，誤刪、磁碟損壞或部署覆蓋即遺失資料。依 `ops-backup/spec.md` 需提供帶時間戳的備份腳本與文件化操作，降低營運風險。

## What Changes

- 新增 `scripts/backup.sh` (POSIX sh)：複製 `backend/diagnoses.db` (或 `diagnoses.db`) 為 `backups/phytotrack-YYYYmmdd-HHMMSS.db`，帶時間戳、建立 `backups/` 目錄、檢查來源存在與複製成功回 `0`、失敗回非零並提示
- 更新 `docs/DEPLOY.md`：新增「備份與還原」章節，說明腳本用法、建議頻率 (每日或部署前)、還原步驟 (停止服務後覆蓋)、`backups/` 已 `gitignore` 的保留策略
- `README.md` 補充備份一行的快速參考 (可選)

## Capabilities

### New Capabilities

<!-- 主規格已含 ops-backup，本 change 採 skip_specs: true，不新增 capability 檔案 -->

### Modified Capabilities

<!-- 本 change 不修改 spec 需求，僅實作主規格已定義的 ops-backup 需求 -->

## Impact

- 後端：無 (僅腳本讀取 `diagnoses.db` 檔案，不經 JPA)
- 前端：無
- 腳本：`scripts/backup.sh` (新增，可執行權限)、`backups/.gitkeep` (可選)
- 文件：`docs/DEPLOY.md`、`README.md` (可選)、`.gitignore` (確認 `backups/` 與 `*.db` 已忽略)
- 測試：無自動化測試，驗證以手動執行 `bash scripts/backup.sh` 產生帶時間戳備份檔為準
