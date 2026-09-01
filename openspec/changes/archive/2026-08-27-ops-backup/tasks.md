## 1. 腳本

- [x] 1.1 新增 `scripts/backup.sh`：建立 `backups/`、解析來源 `backend/diagnoses.db`、以 `YYYYmmdd-HHMMSS` 複製、成功回 0 並印出路徑、失敗回非零，驗證 `bash scripts/backup.sh` 產生 `backups/phytotrack-*.db`

## 2. 文件

- [x] 2.1 更新 `docs/DEPLOY.md`：新增「備份與還原」章節 (用法、頻率建議、還原步驟、backups 已 gitignore)，驗證 `docs/DEPLOY.md` 含備份說明
- [x] 2.2 確認 `.gitignore` 已含 `backups/` 與 `*.db` (若無則補)，驗證 `git status` 不追蹤備份檔
- [x] 2.3 可選：`README.md` 補一行備份快速參考，驗證文件可見

## 3. 驗證與文件收尾

- [x] 3.1 執行 `bash scripts/backup.sh` 兩次產生不同時間戳備份，`ls backups/` 可見，驗證腳本冪等
- [x] 3.2 `openspec validate --specs --changes` 通過
