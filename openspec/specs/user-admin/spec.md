# User Admin Specification

## Purpose

讓管理者可調整使用者角色、啟停用帳號與重設密碼，形成完整的帳號管理能力，並確保停用與角色異動於後續請求即時生效。

## Requirements

### Requirement: 管理者調整角色

ADMIN SHALL 可變更使用者角色，變更後該使用者的權限於後續請求生效。提權至 `STAFF|ADMIN` SHALL 觸發 `IdentifierService.ensureForUser` 自動建立 `user as signer`（若無 `active` 同名則建，名稱取 `displayName` 優先否則 `username`），並遵守簽名人綁定規則。

#### Scenario: 變更使用者角色
- **WHEN** ADMIN 將使用者調整為 STAFF
- **THEN** 該使用者於後續請求取得 STAFF 權限且 `GET /api/ref/identifiers/me` 可得其簽名人

#### Scenario: 提權撞名提示綁定
- **WHEN** ADMIN 將 `displayName` 已存在 `signer but not user` 同名的 VIEWER 提權為 STAFF
- **THEN** 回 `409 SIGNER_NAME_CONFLICT` 並附既有 `identifier_id`，前端提示是否綁定

### Requirement: 啟停用帳號

ADMIN SHALL 可停用或啟用帳號；停用帳號 SHALL 無法登入，且其既有 token 於後續請求 SHALL 被拒絕。

#### Scenario: 停用後嘗試登入
- **WHEN** 帳號被停用後嘗試登入
- **THEN** 登入失敗並提示帳號已停用

#### Scenario: 停用後使用既有 token
- **WHEN** 帳號被停用後使用既有 token 呼叫受保護 API
- **THEN** 請求遭拒

### Requirement: 管理者重設密碼

ADMIN SHALL 可為使用者重設密碼，重設後該使用者可用新密碼登入。

#### Scenario: 重設使用者密碼
- **WHEN** ADMIN 重設使用者密碼
- **THEN** 該使用者可用新密碼成功登入

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

### Requirement: 提權時簽名人自動註冊與名稱準則

`ROLE_STAFF | ROLE_ADMIN` 使用者 SHALL 於提權當下擁有 `user as signer`，其 `identifier` SHALL 等於 `User.displayName`（`displayName` 空則 `username`），而非角色名。`DATA_INITIALIZER` SHALL 不再建 `張志明/林雅惠/陳建宏` 種子，重建路徑僅由此自動註冊與案件內新建。

#### Scenario: 新 STAFF 自動註冊
- **WHEN** viewer `王小明` 被提權為 STAFF
- **THEN** `identifiers` 新增 `identifier=王小明, user_id=王小明.userId, active=true`，`GET /api/ref/identifiers?includeInactive=false` 可見

#### Scenario: 名稱取帳號而非權限
- **WHEN** 新建 STAFF `displayName=王小明, username=w123`
- **THEN** 簽名人名稱為 `王小明` 而非 `診斷員` 或 `STAFF`

### Requirement: 簽名人綁定端點

`POST /api/admin/ref/identifiers/{id}/bind` SHALL 將 `user IS NULL` 的簽名人綁定至指定 `userId`（限 `ADMIN`），轉為 `user as signer`；若 `user` 已有同名 `active` 簽名人則回 `409`。

#### Scenario: 綁定成功
- **WHEN** ADMIN 呼叫 `POST /api/admin/ref/identifiers/10/bind { userId: 5 }` 且 `10` 為 `user IS NULL`
- **THEN** `10` 的 `user_id` 更新為 `5`，類型轉為 `user as signer`

#### Scenario: 綁定目標已有同名 active
- **WHEN** ADMIN 試圖綁定 `identifier=王小明` 至已有 `王小明` active 的使用者
- **THEN** 回 `409` 提示已存在，無需重複綁定

### Requirement: 註冊信箱查重

`POST /api/auth/register` SHALL 對非空信箱執行全域唯一檢查（去空白、大小寫不敏感）；重複時 SHALL 回 `409` 錯誤碼 `EMAIL_TAKEN`；空值或全空白 SHALL 視為未填寫而不檢查。既有帳號重複回 `409 USERNAME_TAKEN` 的行為維持不變。

#### Scenario: 註冊信箱重複被拒
- **WHEN** 以已被他人使用的信箱（大小寫不同亦視為重複）呼叫 `POST /api/auth/register`
- **THEN** 回 409 錯誤碼 `EMAIL_TAKEN` 且帳號不建立

#### Scenario: 註冊不填信箱可通過
- **WHEN** 以空信箱呼叫 `POST /api/auth/register` 且帳號未重複
- **THEN** 回 201 且帳號建立成功

### Requirement: 帳號與信箱可用性查詢

系統 SHALL 提供未登入可呼叫的公開可用性查詢端點，僅回可用性布林值，不回其他使用者個資：`GET /api/auth/check-username?username=` 回 `{ available }`（去空白後為空視為不可用）；`GET /api/auth/check-email?email=` 回 `{ available }`（空值視為可用，因信箱選填；非空時去空白、大小寫不敏感比對）。

#### Scenario: 查詢已用帳號
- **WHEN** 以已存在的帳號呼叫 `GET /api/auth/check-username`
- **THEN** 回 200 且 `available` 為 `false`

#### Scenario: 查詢可用帳號
- **WHEN** 以未使用的帳號呼叫 `GET /api/auth/check-username`
- **THEN** 回 200 且 `available` 為 `true`

#### Scenario: 查詢已用信箱
- **WHEN** 以已被使用的信箱（大小寫不同亦視為重複）呼叫 `GET /api/auth/check-email`
- **THEN** 回 200 且 `available` 為 `false`

#### Scenario: 查詢空信箱
- **WHEN** 以空值呼叫 `GET /api/auth/check-email`
- **THEN** 回 200 且 `available` 為 `true`

### Requirement: 註冊表單 inline 錯誤

註冊頁全表單 SHALL 於欄位下方以 inline 錯誤訊息呈現，不得使用全域 alert 彈窗：帳號與信箱失焦或輸入停止後 SHALL 以 debounce 呼叫可用性端點並即時顯示；密碼與確認密碼不一致 SHALL 即時顯示於確認密碼欄位下方；送出時後端回 `USERNAME_TAKEN`/`EMAIL_TAKEN`/其他欄位錯誤 SHALL 映射到對應欄位下方；其他未知錯誤維持既有全域處理。註冊成功導登入的成功提示維持不變。

#### Scenario: 輸入重複帳號即時提示
- **WHEN** 使用者在帳號欄輸入已存在的帳號並失焦
- **THEN** 帳號欄位下方顯示重複錯誤訊息，且不彈出全域 alert

#### Scenario: 密碼不一致即時提示
- **WHEN** 使用者在確認密碼欄輸入與密碼不同的值
- **THEN** 確認密碼欄位下方顯示不一致錯誤訊息，且不彈出全域 alert

#### Scenario: 送出時重複映射到欄位
- **WHEN** 使用者送出註冊且後端回 `409 EMAIL_TAKEN`
- **THEN** 信箱欄位下方顯示重複錯誤訊息，且不彈出全域 alert

### Requirement: 註冊與帳號管理信箱格式檢查

註冊與帳號管理的信箱欄位 SHALL 共用同一前端 email regex 即時檢查；格式不符 SHALL 於欄位下方顯示 inline 錯誤並阻擋送出。後端 `@Email` 驗證維持不變（最後防線）。

#### Scenario: 註冊輸入格式錯誤信箱
- **WHEN** 使用者在註冊信箱欄輸入無 `@` 的字串並失焦
- **THEN** 信箱欄位下方顯示格式錯誤訊息，且不彈出全域 alert

### Requirement: 註冊密碼可見切換

註冊頁的密碼與確認密碼欄位 SHALL 各有眼睛 icon 按鈕可切換顯示/隱藏明文；預設為隱藏。

#### Scenario: 切換密碼可見性
- **WHEN** 使用者點擊密碼欄的眼睛 icon
- **THEN** 該欄位在 `password`/`text` 間切換且 icon 狀態同步，不影響已輸入值

### Requirement: 使用者管理搜尋與停用顯示

使用者管理清單 SHALL 提供搜尋框（對帳號、顯示名稱、信箱做前端模糊比對）與「顯示已停用」勾選框；預設不勾選時 SHALL 僅列啟用者，勾選後 SHALL 列出全部（含停用者）；搜尋與勾選 SHALL 可疊加使用。狀態徽章沿用既有啟用/停用樣式。

#### Scenario: 預設隱藏停用者
- **WHEN** 管理者開啟使用者管理頁且未勾選顯示已停用
- **THEN** 清單僅顯示啟用者

#### Scenario: 搜尋疊加勾選
- **WHEN** 管理者輸入關鍵字並勾選顯示已停用
- **THEN** 清單顯示符合關鍵字的全部使用者（含停用者）

### Requirement: 帳號管理異動儲存鈕

帳號管理頁的儲存按鈕 SHALL 僅在以下條件全滿足時顯示：有修改的顯示名稱其「檢查」結果為通過（編輯後失效須重按），有修改且非空的信箱亦同；信箱清空（解除綁定）不需檢查。任一條件未滿足 SHALL 隱藏儲存鈕並於原位置提示先按檢查；兩者皆未修改亦隱藏。顯示名稱與信箱欄 SHALL 各有「取消」鈕可還原載入值。送出時僅做格式複檢（字符層防線），不再以 alert 要求檢查（按鈕可見即代表已通過）。

#### Scenario: 僅改顯示名稱即顯示儲存鈕
- **WHEN** 使用者只修改顯示名稱（信箱未動）並按過檢查通過
- **THEN** 儲存按鈕顯示（送出時仍需先通過顯示名稱檢查）

#### Scenario: 從未綁信箱者新增信箱可儲存
- **WHEN** 原始信箱為空的使用者填入新信箱
- **THEN** 儲存按鈕顯示（dirty 正常判定）

#### Scenario: 信箱取消還原
- **WHEN** 使用者修改信箱後點擊取消
- **THEN** 信箱還原載入值且檢查結果清除，儲存按鈕隱藏

#### Scenario: 未按檢查隱藏儲存鈕並提示
- **WHEN** 使用者只修改顯示名稱但未按檢查
- **THEN** 儲存按鈕隱藏，原位置顯示請先完成顯示名稱檢查的提示

#### Scenario: 空信箱無需檢查可送出
- **WHEN** 使用者清空信箱（解除綁定）且顯示名稱未動
- **THEN** 送出不要求信箱檢查，直接更新為 null

#### Scenario: 未修改隱藏儲存鈕
- **WHEN** 使用者未修改任何欄位
- **THEN** 儲存按鈕隱藏

### Requirement: 信箱空值或格式即時檢查

帳號管理與註冊的信箱欄 SHALL 僅接受空值或符合前端共用 email regex 的值；非空且格式不符 SHALL 即時於欄位下方顯示 inline 錯誤並阻擋送出。空值送出時 SHALL 以 null 解除綁定。

#### Scenario: 格式錯誤即時提示
- **WHEN** 使用者在信箱欄輸入無 `@` 的字串
- **THEN** 欄位下方即時顯示格式錯誤，且送出被阻擋

#### Scenario: 清空解除綁定
- **WHEN** 使用者清空已綁信箱並送出（顯示名稱已通過檢查或未動）
- **THEN** 後端儲存為 null，後續載入顯示為空

### Requirement: 降權解綁簽名人保留可見

`PATCH /api/admin/users/{id}/role` 將 `STAFF|ADMIN` 降權至 `VIEWER` 時，系統 SHALL 將其名下 `active` 的 `user as signer` 解綁為非使用者（`user_id = null`、`former_user_id` 記為該使用者、`active` 維持 `true`、`id` 不變）；解綁後 SHALL 繼續顯示於新案件簽名人候選（身分別為非使用者），歷史案件仍以 `id` 顯示原名。

#### Scenario: 降權轉非使用者仍可選
- **WHEN** ADMIN 將 STAFF 王小明降權為 VIEWER
- **THEN** 其簽名人 `user_id` 清空、`active` 維持 `true`、`id` 不變，新案件候選仍可見（非使用者）

### Requirement: 停用解綁簽名人

`PATCH /api/admin/users/{id}/active` 設 `active=false` 時，除既有連動停用外，系統 SHALL 一併將其名下簽名人解綁（`user_id = null`、`former_user_id` 記為該使用者）；可見性維持現行（新案件候選隱藏，管理頁 `?includeInactive=true` 可見）。

#### Scenario: 停用解綁且候選隱藏
- **WHEN** ADMIN 停用 STAFF 王小明
- **THEN** 其簽名人 `user_id` 清空且 `active=false`，新案件候選不可見，管理頁可見停用狀態

### Requirement: 升權或啟用恢復原簽名人

升權至 `STAFF|ADMIN` 或重新啟用（`active=true`）時，系統 SHALL 優先恢復原筆簽名人：以 `former_user_id` 為該使用者、同名（正規化比對）的未綁定舊筆取 `id` 最小者，重新連結（`user_id` 回填）並 `active=true`；無原筆才走既有新建／啟用流程，不得在存在可恢復舊筆時新建重複。他人同名非使用者簽名人（`former_user_id` 不同）SHALL 不被恢復，維持既有撞名綁定流程。

#### Scenario: 升權恢復原筆不新建
- **WHEN** 被降權的王小明（其原簽名人已解綁為非使用者 active）重新升權為 STAFF
- **THEN** 原筆 `id` 重新連結該使用者，不新增第二筆同名簽名人

#### Scenario: 啟用恢復停用舊筆
- **WHEN** 被停用的王小明重新啟用
- **THEN** 其原停用簽名人重新連結並 `active=true`，新案件候選可見

### Requirement: 首頁已登入隱藏認證按鈕

首頁（`/`）Hero 區的「立即登入」「建立帳號」按鈕組 SHALL 僅在未登入時顯示；已登入時 SHALL 隱藏。登入後預設進入儀表板，若使用者再導向回首頁亦 SHALL 隱藏。後端契約與路由不變。

#### Scenario: 未登入顯示按鈕
- **WHEN** 未登入使用者訪問 `/`
- **THEN** Hero 區顯示「立即登入」與「建立帳號」按鈕

#### Scenario: 已登入隱藏按鈕
- **WHEN** 已登入使用者訪問 `/`（含登入後點導覽回首頁）
- **THEN** Hero 區不顯示「立即登入」與「建立帳號」按鈕
