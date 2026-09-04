## 1. 帳號管理檢查閘門與取消

- [x] 1.1 `AccountView.vue` 新增 `loaded` 旗標並改寫 `displayNameDirty/emailDirty`（null 原始值視為空字串），驗證從未綁信箱者填入信箱後儲存鈕顯示
- [x] 1.2 `AccountView.vue` 新增信箱「取消」鈕（`cancelEmail` 還原載入值並清除檢查結果），驗證取消後儲存鈕隱藏
- [x] 1.3 `saveProfile` 移除自動代跑，改為顯式閘門（未通過檢查擋下並提示先按檢查；空信箱免檢），驗證未按檢查送出被擋、按過檢查可送出
- [x] 1.4 `canSave()` 改為檢查通過才顯示（dirty＋各欄檢查通過；空信箱免檢），隱藏時顯示動態提示；`saveProfile` 移除閘門 Swal 僅留格式複檢，驗證未檢查不見儲存鈕、通過後出現且送出無 alert

## 2. 信箱格式即時提示

- [x] 2.1 `AccountView.vue`（必要時 `RegisterView.vue`）新增非空格式不符的欄位下方即時錯誤，驗證輸入無 `@` 字串即時顯示且送出被阻擋

## 3. 驗證與回歸

- [x] 3.1 執行 `cd frontend && npm run build`（含 `vue-tsc`）與 `cd backend && mvn test`，驗證建置與既有測試通過
- [x] 3.2 執行 `openspec validate --specs --changes --strict`，驗證無錯誤
