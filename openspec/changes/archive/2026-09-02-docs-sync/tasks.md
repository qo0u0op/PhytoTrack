## 1. 文件同步

- [x] 1.1 更新 `docs/REQUIREMENTS.md` 與 `docs/ARCHITECTURE.md`（能力狀態、視圖 17 欄、篩選 5 列、表頭 `田區位置/身分別`、CSV 全欄位引號與狀態中文），驗證 `grep -rn 病蟲害發生地點 docs` 僅餘歷史段落或零殘留
- [x] 1.2 更新 `docs/diagnoses.typ` 與 `docs/manual.typ` 紙本欄位與操作步驟至最新行為，執行 `typst compile docs/manual.typ docs/manual.pdf` 與 `diagnoses.typ` 編譯成功
- [x] 1.3 更新 `docs/adr/*` 必要補充與 `README.md` 引用，執行 `openspec validate --specs --changes` 通過

## 2. 驗證

- [x] 2.1 執行 `grep -rn 送件人身分別 docs` 與 `grep -rn 病蟲害發生地 docs` 確認新用語已替換，舊用語僅餘 ADR 歷史或已註明
