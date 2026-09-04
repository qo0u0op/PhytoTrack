## 1. 後端解綁與恢復

- [x] 1.0 `Identifier` 新增 `formerUser`（`former_user_id` 可空 FK）、`IdentifierRepository` 新增 `findByFormerUserId`、`schema.sql` 同步建欄，驗證編譯通過且新庫建表含該欄
- [x] 1.1 `AuthService.updateRole` 降權至 VIEWER 時解綁名下 active 簽名人（`user_id=null`＋`former_user_id` 留存，`active` 不動），驗證降權後簽名人轉非使用者且候選仍可見、`id` 不變
- [x] 1.2 `AuthService.updateActive(false)` 連動解綁（`deactivateUserSigners` 加 `setUser(null)`＋`setFormerUser`），驗證停用後簽名人解綁＋停用、候選隱藏、管理頁可見
- [x] 1.3 升權／重新啟用（限 STAFF|ADMIN）優先恢復原筆（`former_user_id` 同名未綁定，`id` 最小），無原筆才新建；他人同名仍走撞名流程，驗證升權不產生第二筆同名、既有撞名測試仍 409
- [x] 1.4 `IdentifierService.ensureForUser` 歷史重鏈一路（撞名檢查之前）＋`bindToUser` 清空歷史（含併發保護），驗證解綁舊筆存在時不新建

## 2. 測試與回歸

- [x] 2.1 新增 `SignerUnlinkRelinkTest`（降權解綁可見＋歷史留存、停用解綁隱藏、升權恢復原筆不新建、啟用恢復、ensureForUser 重鏈、他人同名仍撞名），驗證全綠
- [x] 2.2 執行 `cd backend && mvn test` 全回歸，驗證既有 signer/user-admin 測試通過
- [x] 2.3 `docs/DEPLOY.md` 加註 `former_user_id` 欄位驗證 SQL，執行 `openspec validate --specs --changes --strict`，驗證無錯誤
