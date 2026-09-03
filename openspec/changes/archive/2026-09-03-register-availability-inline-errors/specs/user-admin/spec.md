## ADDED Requirements

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

帳號管理頁的儲存按鈕 SHALL 改為 dirty 判斷：email 或顯示名稱任一與載入時的值不同即顯示；兩者皆未修改 SHALL 隱藏。送出仍需通過既有格式與可用性檢查。

#### Scenario: 僅改顯示名稱即顯示儲存鈕
- **WHEN** 使用者只修改顯示名稱（信箱未動）
- **THEN** 儲存按鈕顯示

#### Scenario: 未修改隱藏儲存鈕
- **WHEN** 使用者未修改任何欄位
- **THEN** 儲存按鈕隱藏
