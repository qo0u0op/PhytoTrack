## ADDED Requirements

### Requirement: 個人資料自助編輯

所有已驗證角色 SHALL 可透過帳號管理取得並更新自身顯示名稱與電子信箱；電子信箱 SHALL 符合信箱格式且全域唯一（大小寫不敏感去重），重複時回 409。

#### Scenario: 更新顯示名稱成功
- **WHEN** 使用者以有效 token 呼叫 `PUT /api/account/profile` 僅變更顯示名稱
- **THEN** 回 200 且後續 `GET /api/account` 反映新名稱

#### Scenario: 更新信箱重複被拒
- **WHEN** 使用者嘗試將信箱改為已被他人使用的信箱（大小寫不同亦視為重複）
- **THEN** 回 409 錯誤碼 `EMAIL_DUPLICATE`

#### Scenario: 信箱格式錯誤
- **WHEN** 送出不符合信箱格式的信箱
- **THEN** 回 400 驗證錯誤

### Requirement: 密碼自助修改

所有已驗證角色 SHALL 可修改自身密碼；非 ADMIN 角色 MUST 提供目前密碼且需通過雜湊比對，ADMIN 修改自身密碼亦需現密碼，ADMIN 為他人重設密碼除外；新密碼 SHALL 符合既有強度規則。

#### Scenario: 非管理員以正確現密碼修改成功
- **WHEN** VIEWER/STAFF 以 `currentPassword` 正確呼叫 `PUT /api/account/password`
- **THEN** 密碼更新，回 204，且可用新密碼登入，舊密碼失效

#### Scenario: 現密碼錯誤被拒
- **WHEN** 提供錯誤的 `currentPassword`
- **THEN** 回 401 錯誤碼 `BAD_CREDENTIALS` 且密碼維持不變

#### Scenario: 管理員為他人重設無需現密碼
- **WHEN** ADMIN 呼叫 `PUT /api/admin/users/{id}/password`
- **THEN** 無需現密碼即可完成重設

### Requirement: 停用帳號請求

所有已驗證角色 SHALL 可發起停用自身帳號的請求；請求 SHALL 由 ADMIN 審核，審核通過後帳號標記為停用，停用後該帳號 SHALL 無法登入且既有 token 於後續請求被拒。

#### Scenario: 發起停用請求
- **WHEN** 使用者呼叫 `POST /api/account/deactivate-request`
- **THEN** 產生待審核請求，回 201，且重複發起時回 409

#### Scenario: 管理員審核通過即停用
- **WHEN** ADMIN 呼叫 `PUT /api/admin/deactivate-requests/{id}` 審核通過
- **THEN** 目標帳號轉為停用，後續登入回 401 且 token 驗證被拒

#### Scenario: 停用後無法登入
- **WHEN** 已停用帳號嘗試登入
- **THEN** 回 401 提示帳號已停用
