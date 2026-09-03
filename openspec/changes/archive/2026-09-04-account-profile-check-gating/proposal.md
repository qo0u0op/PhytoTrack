## Why

`register-availability-inline-errors` 上線後回報三個帳號管理個人資料區問題：(1) 信箱修改後沒有「取消」鈕可還原（顯示名稱有，信箱無）；(2) 從未綁信箱者填入新信箱並通過檢查後仍無「儲存」鈕——`emailDirty()` 誤寫成 `originalEmail !== null && …`，原始值為 null 時永遠判非 dirty；(3) 顯示名稱未按「檢查」即可儲存——`saveProfile` 會自動代跑必過的本地檢查，不符合「先檢查後儲存」要求。另需明文化：信箱要麼空值（解除綁定）要麼符合 regex，格式錯誤應即時 inline 提示而非僅送出時擋下。

## What Changes

- `AccountView.vue` 信箱欄加「取消」鈕（行為同顯示名稱：還原載入值並清除檢查結果）。
- dirty 改以 `loaded` 旗標判斷（載入完成後當前值與載入值去空白比對），原始值 null 不再導致永遠非 dirty。
- 儲存閘門改為顯式檢查：顯示名稱有改必須按過「檢查」（`checkResult === true`，編輯後自動失效）；信箱有改且非空也必須按過檢查；未通過者送出時擋下並提示先按檢查，不再自動代跑。信箱清空（解除綁定）不需檢查。
- 信箱非空且格式不符即時於欄位下方顯示 inline 錯誤；送出前格式複檢維持。

## Capabilities

### New Capabilities

- 無（皆為既有 `user-admin` 行為修正）

### Modified Capabilities

- `user-admin`: 帳號管理 dirty 判定、顯式檢查閘門、信箱取消鈕、信箱格式即時提示

## Impact

- 前端：`AccountView.vue` 僅此一檔；後端無需更動（`PUT /api/account/profile` 已支援 null 解綁與 `@Email`）。
- 相容性：儲存按鈕顯示時機不變（dirty 即顯示）；差別在送出必須先通過顯式檢查，既有「改了就存」的捷徑關閉。
