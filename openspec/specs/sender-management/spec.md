# Sender Management Specification

## Purpose

送件人資料的獨立管理與權限控管：支援多來源送件人 (現場、Line、Facebook、Email) 的顯示、去重與合併，VIEWER 角色遮蔽個資但保留縣市鄉鎮，並提供統計用的一致去重鍵。設計決策見 ADR-011。

## Requirements

### Requirement: 送件人欄位規則

送件人 SHALL 支援 `name`、`displayName`、`phone`、`address`、`district` (含所屬 `city`) 與 `senderType`；`name` MAY 為空，`phone` 與 `displayName` 之間 SHALL 至少一項有值；`address` MAY 為空（空字串或全空白視為 null，解除／未綁定地址）；`displayName` 用於標記來源顯示名稱 (Line/Facebook/Email 暱稱)。

#### Scenario: 有姓名且有顯示名稱
- **WHEN** 送件人同時有 `name` 與 `displayName`
- **THEN** 顯示為 `name (displayName)`

#### Scenario: 只有顯示名稱
- **WHEN** 送件人只有 `displayName`、無 `name`
- **THEN** 直接顯示 `displayName`

#### Scenario: 兩者皆空
- **WHEN** 建立送件人時 `phone` 與 `displayName` 皆未提供
- **THEN** 回應 4xx，且不建立資料

#### Scenario: 無地址可建檔
- **WHEN** 以空地址（未傳、null 或全空白）呼叫 `POST /api/cases` 新建送件人或 `POST|PUT /api/senders`
- **THEN** 回 2xx 且送件人建立／更新成功，地址存為 null，查詢與匯出顯示為空

#### Scenario: 地址仍可正常填寫
- **WHEN** 以非空地址建立或更新送件人
- **THEN** 地址去空白後儲存，行為與既有一致

### Requirement: 送件人去重與合併

送件人辨識 SHALL 以多個弱識別符 (name / phone / displayName) 組合進行，建立案件時列出候選送件人並由使用者人工確認是否沿用或建立新送件人；系統 SHALL NOT 以 DB 唯一鍵強制合併。

#### Scenario: 同識別符候選
- **WHEN** 輸入的姓名或電話與既有送件人部分相符
- **THEN** 系統列出候選清單供使用者確認合併

#### Scenario: 無候選
- **WHEN** 輸入資料與既有送件人皆不符
- **THEN** 系統建立新送件人

### Requirement: 送件人刪除權限

送件人刪除 SHALL 僅 ADMIN 可執行且為硬刪除；被案件引用的送件人 SHALL 拒絕刪除並回 4xx。系統 SHALL NOT 提供 soft delete。

#### Scenario: 刪除未被引用送件人
- **WHEN** ADMIN 刪除未被任何案件引用的送件人
- **THEN** 送件人自資料庫移除

#### Scenario: 刪除被引用送件人
- **WHEN** ADMIN 刪除已被案件引用的送件人
- **THEN** 回應 4xx，且資料保留

### Requirement: VIEWER 個資遮蔽

VIEWER 角色 SHALL NOT 取得送件人姓名、電話與地址，但 SHALL 可取得縣市鄉鎮。

#### Scenario: VIEWER 查詢案件
- **WHEN** VIEWER 查詢案件列表或詳細
- **THEN** 回應不含送件人姓名／電話／地址，但含縣市鄉鎮

#### Scenario: STAFF/ADMIN 查詢案件
- **WHEN** STAFF 或 ADMIN 查詢案件
- **THEN** 回應含完整送件人資料

### Requirement: 統計去重鍵

案件統計的「不重複送件人」SHALL 以 `COALESCE (phone, displayName)` 為鍵計算。

#### Scenario: 依電話去重
- **WHEN** 統計不重複送件人
- **THEN** 有電話者依電話歸併，無電話者依顯示名稱歸併

### Requirement: 送件人查詢 API

系統 SHALL 提供送件人搜尋端點，依 name / phone / displayName 部分比對，供建案表單與去重候選使用；CaseResponse SHALL 包含 `senderId` 與送件人縣市、鄉鎮市區名稱。送件人管理頁 SHALL 提供篩選卡片，支援依身分別（`senderTypeId`）、縣市（`cityId`）、鄉鎮市區（`districtId`，依縣市聯動）與關鍵字（姓名/電話/顯示名稱）篩選，多條件以 AND 組合。篩選卡片 SHALL 為抽屜式，預設收合隱藏，點擊按鈕展開/收合。送件人管理頁 SHALL 不提供「新增」按鈕（新增改由案件表單內儲存送件人完成）。

#### Scenario: 依關鍵字搜尋
- **WHEN** 使用者輸入關鍵字搜尋送件人
- **THEN** 回傳姓名或電話或顯示名稱相符的送件人候選

#### Scenario: 案件回應帶送件人識別
- **WHEN** 查詢案件詳細或列表
- **THEN** 回應包含 `senderId` 及送件人縣市、鄉鎮市區名稱

#### Scenario: 依身分別篩選送件人
- **WHEN** 使用者在送件人管理頁選擇身分別後觸發篩選
- **THEN** 僅顯示該身分別的送件人

#### Scenario: 依縣市與鄉鎮市區篩選
- **WHEN** 選擇縣市後（鄉鎮市區選單依縣市聯動），再選擇鄉鎮市區
- **THEN** 僅顯示符合該縣市/鄉鎮市區的送件人，且縣市未選時鄉鎮市區不可選

#### Scenario: 多條件組合篩選
- **WHEN** 同時輸入關鍵字並選擇身分別/縣市/鄉鎮市區
- **THEN** 以 AND 組合篩選，且清除操作重置全部條件

#### Scenario: 欄位更名鄉鎮市區
- **WHEN** 檢視送件人管理表格
- **THEN** 欄位標題顯示為「鄉鎮市區」（原「鄉鎮」），與 `district` 模型一致

#### Scenario: 篩選抽屜預設隱藏
- **WHEN** 進入送件人管理頁
- **THEN** 篩選卡片預設收合，僅顯示「篩選」按鈕

#### Scenario: 展開篩選
- **WHEN** 點擊篩選按鈕
- **THEN** 展開篩選卡片

#### Scenario: 不提供新增按鈕
- **WHEN** 檢視送件人管理標題列
- **THEN** 僅顯示「篩選」按鈕，不顯示「新增」

### Requirement: 送件人篩選卡片

送件人管理頁 SHALL 以篩選卡片呈現四欄（關鍵字、身分別、縣市、鄉鎮市區），縣市/鄉鎮市區為兩層聯動選單，篩選為前端本地過濾（基於 `GET /api/senders` 全量結果），不依賴後端新增篩選 API。

#### Scenario: 篩選卡片呈現
- **WHEN** 使用者進入送件人管理頁
- **THEN** 顯示篩選卡片含關鍵字輸入、身分別/縣市/鄉鎮市區下拉，鄉鎮市區未選縣市時為 disabled

#### Scenario: 清除篩選
- **WHEN** 點擊清除
- **THEN** 四欄重置且列表回到全量

### Requirement: 送件人管理分頁

送件人管理頁 SHALL 在資料筆數大於 20 時顯示分頁控制（頁碼按鈕、頁碼輸入、每頁筆數選擇 [10,20,50,100]，預設 10），與案件管理分頁一致；前端本地分頁（基於 `GET /api/senders` 全量結果）。

#### Scenario: 未達門檻不顯示分頁
- **WHEN** 送件人總數 ≤20
- **THEN** 不顯示分頁控制

#### Scenario: 超過門檻顯示分頁
- **WHEN** 送件人總數 >20
- **THEN** 顯示分頁控制，且可切換頁碼與每頁筆數

#### Scenario: 分頁分頁與篩選共存
- **WHEN** 已套用篩選（身分別/縣市/鄉鎮市區/關鍵字）且結果 >20
- **THEN** 分頁基於篩選後結果計算

### Requirement: 送件人列表排序

送件人列表 SHALL 支援依所有欄位（除操作外）點擊表頭排序，前端本地排序，預設依 ID 降冪，表頭以箭頭指示。

#### Scenario: 點擊表頭排序
- **WHEN** 點擊非操作欄的表頭
- **THEN** 依該欄位切換 asc/desc

#### Scenario: 操作欄不可排序
- **WHEN** 檢視操作欄
- **THEN** 不提供排序

### Requirement: 送件人電話候選 4 碼門檻

送件人候選搜尋中，電話欄位 SHALL 僅在輸入 4 碼以上時觸發後端搜尋；未達門檻 SHALL 不發請求。

#### Scenario: 電話未達門檻不查詢
- **WHEN** 電話輸入少於 4 碼
- **THEN** 系統不呼叫送件人搜尋 API

#### Scenario: 電話達門檻查詢
- **WHEN** 電話輸入 4 碼以上
- **THEN** 系統可呼叫搜尋並回傳候選

### Requirement: 送件人編輯獨立頁面

送件人編輯 SHALL 由獨立頁面提供，而非 popup，其版面與 `CaseFormView.vue` 的「送件人資料」卡片一致（`card shadow-sm` + `card-header bg-success`，欄位含姓名、顯示名稱、電話、地址（選填）、縣市/鄉鎮連動、身分別，操作按鈕含儲存/更新/取消編輯）。使用者於送件人管理列表點擊「編輯」SHALL 導向編輯頁面，頁面 SHALL 以 `GET /api/senders/:id` 載入既有資料並以 `PUT /api/senders/:id` 提交更新，並提供 inline 驗證與 API 錯誤映射（`400 驗證錯誤`、`409 衝突`）。

#### Scenario: 導向編輯頁面
- **WHEN** 使用者在送件人管理列表點擊編輯
- **THEN** 導向獨立編輯頁面並載入該送件人資料，頁面版面與案件編輯的送件人資料卡片一致

#### Scenario: 提交更新成功
- **WHEN** 使用者在編輯頁面修改欄位並提交且通過驗證
- **THEN** 呼叫 `PUT /api/senders/:id`，成功後返回列表並顯示更新結果

#### Scenario: 驗證失敗保留頁面
- **WHEN** 提交資料未通過驗證或發生 `409`
- **THEN** 錯誤訊息顯示於對應欄位或表單頂部，頁面不跳轉

### Requirement: 送件人編輯頁保持篩選與導航狀態

送件人編輯頁 SHALL 保持列表的篩選結果（關鍵字/身分別/縣市/鄉鎮）、分頁（page/size）與排序狀態（sortStates），支援在當前篩選結果內進行上一筆/下一筆導航，且返回列表時 SHALL 恢復原篩選/分頁/排序而不重置。

#### Scenario: 上一筆下一筆導航保持篩選
- **WHEN** 使用者在編輯頁點擊上一筆或下一筆
- **THEN** 依當前列表的篩選與排序結果決定的 ID 序列導向相鄰送件人的編輯頁

#### Scenario: 返回列表恢復狀態
- **WHEN** 使用者從編輯頁返回列表（儲存後或取消）
- **THEN** 列表的篩選、分頁與排序恢復為進入編輯前的狀態
