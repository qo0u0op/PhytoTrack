## 1. 後端資料與 API

- [x] 1.1 新增 `deactivate_requests` 表與 `users.email` 大小寫不敏感唯一索引（SQLite `COLLATE NOCASE`），處理既有重複資料遷移，並驗證 `mvn test` 通過
- [x] 1.2 實作 `GET /api/account`、`PUT /api/account/profile`（顯示名稱／信箱，信箱去重與格式驗證）與對應 Service/Repository，並驗證 `mvn test` 通過
- [x] 1.3 實作 `PUT /api/account/password`（非 ADMIN 需 currentPassword 雜湊比對，ADMIN 例外）與強度驗證，並驗證 `mvn test` 通過
- [x] 1.4 實作 `POST /api/account/deactivate-request` 與 `GET/PUT /api/admin/deactivate-requests` 審核流程，停用後 token 失效，並驗證 `mvn test` 通過

## 2. 前端帳號管理

- [x] 2.1 新增 `AccountView.vue` 與路由 `/account`、導覽入口，實作個人資料編輯表單與信箱重複錯誤顯示，並驗證 `npm run build` 通過
- [x] 2.2 實作密碼修改表單（現密碼驗證提示，ADMIN 無需現密碼分支）與停用請求發起／狀態顯示，並驗證 `npm run build` 通過

## 3. 驗收

- [x] 3.1 執行 `mvn test`、`npm test`、`npm run build` 與 `openspec validate --specs --changes` 通過
