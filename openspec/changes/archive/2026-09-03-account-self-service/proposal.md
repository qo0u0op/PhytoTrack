## Why

目前僅 ADMIN 可異動帳號，一般角色無法自助維護個資與密碼，且變更信箱缺乏去重與停用缺乏申請流程，需補齊各角色的自助帳號管理以降低管理負擔並提升安全性。

## What Changes

- 各角色皆可於「帳號管理」頁編輯自身顯示名稱與電子信箱；電子信箱 SHALL 全域唯一且大小寫不敏感去重，更新時需驗證格式
- 各角色皆可修改自身密碼：非 ADMIN 需提供目前密碼驗證，ADMIN 修改他人密碼無需現密碼；密碼需符合既有強度規則
- 各角色可發起「停用帳號請求」，由 ADMIN 審核後執行實際停用；停用後帳號無法登入且既有 token 失效
- 新增個人資料取得與更新、信箱唯一性檢查、密碼變更與停用請求的 API 與前端頁面

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `user-admin`: 擴充自助帳號管理（顯示名稱／信箱編輯、信箱去重、密碼修改需現密碼、停用請求流程）

## Impact

- 後端：`User` 實體新增/驗證 `email` 唯一索引（大小寫不敏感）、`AccountController` 新增 `GET /api/account`、`PUT /api/account/profile`、`PUT /api/account/password`、`POST /api/account/deactivate-request`，ADMIN 端新增 `GET/PUT /api/admin/deactivate-requests` 審核
- 前端：新增 `AccountView.vue`（或 `ProfileView.vue`）與路由、導覽入口、表單驗證與錯誤處理
- 安全：密碼變更需現密碼雜湊比對，ADMIN 例外；需防範信箱枚舉與重放
