## Why

`register-availability-inline-errors` 把簽名人篩選列收進預設隱藏的抽屜後，實際使用回饋為負面：篩選是高頻操作，藏起來反而多一次點擊；且缺少身分別（使用者／非使用者）維度的篩選，使用者與非使用者混列難找。本 change 反轉抽屜決策：移除篩選按鈕、預設顯示篩選列，並新增身分別下拉篩選單。

## What Changes

- `SignersView.vue` 移除篩選按鈕與 collapse 抽屜，篩選卡預設直接顯示（名稱篩選、顯示已停用、筆數維持）。
- 新增身分別下拉篩選單：全部／使用者／非使用者（以 `userId` 有無判定），與既有名稱篩選、顯示已停用疊加使用（預設「全部」）。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `reference-data-admin`: 簽名人篩選常顯、身分別下拉篩選（反轉前案抽屜決策）

## Impact

- 前端：`SignersView.vue` 僅此一檔；後端無需更動（前端本地過濾）。
- 相容性：純顯示行為變更；既有篩選、分頁、徽章維持。
