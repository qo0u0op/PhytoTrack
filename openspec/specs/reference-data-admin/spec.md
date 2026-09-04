# Reference Data Admin Specification

## Purpose

讓管理者可維護作物、病蟲害、服務方式、送達方式與標的等參照資料，無需修改資料庫種子，確保第一線人員隨時取用最新選項。

## Requirements

### Requirement: 參照資料寫入管理

ADMIN SHALL 可新增、修改與刪除作物、病蟲害、服務方式、送達方式、標的等參照資料；刪除已被案件引用的資料 SHALL 被拒絕。作物管理 SHALL 允許 STAFF 進行新增與修改，但刪除僅 ADMIN 可執行。作物與害物管理頁的篩選卡片 SHALL 為抽屜式，預設收合，作物依分類篩選。病蟲害分類下拉 SHALL 依 `pestCategoryCode` 升冪排序。

#### Scenario: 新增作物
- **WHEN** ADMIN 新增一筆作物
- **THEN** 該作物可於案件表單中選用

#### Scenario: STAFF 新增或修改作物
- **WHEN** STAFF 新增或修改作物
- **THEN** 操作成功且作物可被選用

#### Scenario: STAFF 刪除作物被拒
- **WHEN** STAFF 嘗試刪除作物
- **THEN** 前端不顯示刪除按鈕，且後端回 403

#### Scenario: 刪除被引用資料
- **WHEN** ADMIN 刪除已被案件引用的作物
- **THEN** 回應 4xx，且資料保留

#### Scenario: 作物篩選抽屜
- **WHEN** 進入作物管理頁
- **THEN** 篩選卡片預設收合，僅顯示「篩選」按鈕，展開後可依分類篩選

#### Scenario: 害物篩選抽屜
- **WHEN** 進入害物管理頁
- **THEN** 篩選卡片預設收合，展開後可依類型與關鍵字篩選

#### Scenario: 病蟲害選單升冪
- **WHEN** 檢視害物分類選項
- **THEN** 依代碼升冪排列

### Requirement: 參照資料管理視圖

前端 SHALL 提供 ADMIN 專用的參照資料管理頁面，以列表與表單進行維護。所有管理頁的新增區塊 SHALL 統一樣式（標題列右上角，`篩選` 按鈕在前、`新增` 按鈕在後，`新增` 為 `btn-sm btn-success`、`篩選` 為 `btn-sm btn-outline-primary`）。

#### Scenario: 檢視與編輯參照資料
- **WHEN** ADMIN 進入參照資料管理頁
- **THEN** 可檢視列表並新增／修改／刪除資料

#### Scenario: 統一樣式
- **WHEN** 檢視任意管理頁
- **THEN** 新增區塊按鈕顯示「篩選」在左、「新增」在右且樣式一致

#### Scenario: 害物排序移除
- **WHEN** 檢視害物分類
- **THEN** 以 `pestCategoryCode` 升冪排序且不提供 `sortOrder` 欄位

### Requirement: 參照資料管理分頁

參照資料管理頁（原綜合頁）在任一類別資料筆數大於 20 時 SHALL 顯示與案件管理同款分頁控制（頁碼、每頁筆數 [10,20,50,100]，預設 10），前端本地分頁。

#### Scenario: 未達門檻不顯示
- **WHEN** 某類別資料筆數 ≤20
- **THEN** 不顯示分頁

#### Scenario: 超過門檻可分頁
- **WHEN** 某類別資料筆數 >20
- **THEN** 顯示分頁且可切換頁碼與每頁筆數

### Requirement: 作物與害物獨立管理與導覽

系統 SHALL 將作物與病蟲害分類從參照資料管理中獨立為兩個管理頁，Navbar 標籤分別為 `作物管理` 與 `害物管理`，順序為送件人管理之後、參照資料管理之前（即：儀表板 / 案件管理 / 送件人管理 / 作物管理 / 害物管理 / 參照資料管理 / 使用者管理）。

#### Scenario: 導覽順序
- **WHEN** 使用者檢視導覽列
- **THEN** 依序顯示 作物管理、害物管理於送件人管理與參照資料管理之間

#### Scenario: 作物管理頁可達
- **WHEN** 使用者點擊作物管理
- **THEN** 進入作物列表與維護頁

#### Scenario: 害物管理頁可達
- **WHEN** 使用者點擊害物管理
- **THEN** 進入病蟲害分類列表與維護頁

### Requirement: 管理頁標籤更名

作物管理頁的篩選與表格中「分類」標籤 SHALL 更名為「類別」（含「全部分類」→「全部類別」與表頭）；害物管理頁中「類型」標籤 SHALL 更名為「因素」（含「全部類型」→「全部因素」與表頭「類型」→「因素」）；參照資料管理的分頁籤「作物分類」 SHALL 更名為「作物類別」。

#### Scenario: 作物管理標籤
- **WHEN** 檢視作物管理篩選與表格
- **THEN** 顯示「類別」與「全部類別」

#### Scenario: 害物管理標籤
- **WHEN** 檢視害物管理篩選與表格
- **THEN** 顯示「因素」與「全部因素」

#### Scenario: 參照資料標籤
- **WHEN** 檢視參照資料分頁籤
- **THEN** 顯示「作物類別」

### Requirement: 診斷簽名人關聯使用者

每位 `ROLE_STAFF` 與 `ROLE_ADMIN` 使用者 SHALL 擁有至少一個以其 `displayName`（若空則 `username`）命名的 `Identifier`（診斷簽名人），`Identifier.user` SHALL 指向該使用者；`VIEWER` 不強制。系統 SHALL 於使用者提權至 `STAFF|ADMIN` 與 `displayName` 變更時同步建立或更名首個 `active` 關聯 Identifier，並於交易內完成；刪除使用者 SHALL 不自動刪除其 Identifier。`Identifier` SHALL 具 `active`（`boolean`，預設 `true`）用於軟停用，停用後 SHALL 從 `GET /api/ref/identifiers` 預設結果中過濾（管理頁以 `?includeInactive=true` 可見），但仍可被既有案件以 `id` 引用（刪除保護不變）。**1.** 種子 `張志明/林雅惠/陳建宏` SHALL 不再預填（`schema.sql` 與 `DataInitializer` 移除，`**BREAKING**`）。**2.** `identifier_id` SHALL 為 `INTEGER PRIMARY KEY` 而非 `AUTOINCREMENT`，空庫首筆為 `1`，重建需 `DELETE FROM identifiers` 後由提權/首建自動重建。**3.** `DELETE /api/admin/ref/identifiers/{id}` SHALL 禁用（回 `405`），僅容 `PATCH .../active` 啟停用；停用後 `GET /api/ref/identifiers` 預設不含該筆，故新增案件對應簽名人自動移除。**6.** 案件表單內新建簽名人 SHALL 默認為 `user IS NULL`（`signer but not user`）。**8.** 簽名人 SHALL 從參照資料管理獨立為 `/signers` 導覽且置於 `參照資料管理` 之前（Navbar 順序：… / 簽名人管理 / 參照資料管理 / …），列表 SHALL 顯示 `active` 與 `類型: user as signer / signer but not user`（`user_id != null` 判斷）。`user as signer`（`user_id != null`）的名稱 SHALL 以 `User.displayName` 為唯一真相源，`PUT /api/admin/ref/identifiers/{id}` 對此類 SHALL 拒絕直改（`409 USER_LINKED_SIGNER_IMMUTABLE`），僅容 `PATCH .../active`；`signer but not user`（`user_id == null`）則 `ADMIN` 可 `PUT` 更名、`PATCH active`。

#### Scenario: Staff 建立時自動建立簽名人
- **WHEN** ADMIN 將新註冊使用者授權為 `ROLE_STAFF`
- **THEN** `identifiers` 新增一筆 `identifier = displayName` 且 `user_id` 指向該使用者

#### Scenario: 修改顯示名稱同步更名簽名人
- **WHEN** STAFF 透過帳號管理將 `displayName` 由「診斷員A」改為「診斷員B」
- **THEN** 其關聯的 `active` 首個 `Identifier.identifier` 同步更新為「診斷員B」

#### Scenario: 已有簽名人不重複建立
- **WHEN** 已擁有簽名人的使用者再次變更非顯示名稱欄位（如 email）
- **THEN** 不新增 `Identifier`，僅在 `displayName` 變更時更名

#### Scenario: 刪除使用者保留簽名人
- **WHEN** ADMIN 刪除一名 STAFF 使用者
- **THEN** 其關聯 `Identifier` 保留，後續仍可被案件引用與刪除保護（`existsByCaseIdentifiersIdentifierIdentifierId`）

#### Scenario: 種子不再預填且 ID 重設
- **WHEN** 空庫首次啟動（`DataInitializer` 已移除 3 筆）
- **THEN** `identifiers` 為空，首個提權建立者 `identifier_id = 1`，不再有 `張志明/林雅惠/陳建宏`

#### Scenario: user as signer 禁直改名稱
- **WHEN** ADMIN 對 `user_id != null` 的簽名人呼叫 `PUT /api/admin/ref/identifiers/{id}` 改名
- **THEN** 回 `409` `USER_LINKED_SIGNER_IMMUTABLE`，`identifier` 保持原值

#### Scenario: signer but not user 可直改名稱
- **WHEN** ADMIN 對 `user_id == null` 的簽名人呼叫 `PUT /api/admin/ref/identifiers/{id}` 改名
- **THEN** 回 `200` 且名稱更新

#### Scenario: 刪除禁用僅停用
- **WHEN** ADMIN 對任意簽名人呼叫 `DELETE /api/admin/ref/identifiers/{id}`
- **THEN** 回 `405 Method Not Allowed`，需改 `PATCH .../active { active:false }`

#### Scenario: 停用後預設過濾
- **WHEN** ADMIN 將某簽名人 `PATCH .../active` 設 `active=false`
- **THEN** `GET /api/ref/identifiers` 預設不再含該筆，`?includeInactive=true` 才可見

#### Scenario: 獨立導覽與類型欄位
- **WHEN** 使用者開啟簽名人管理頁 `/signers`
- **THEN** 見獨立導覽（置於 `參照資料管理` 之前）且列表含 `類型` 欄顯示 `user as signer` 或 `signer but not user` 與 `active` 狀態

#### Scenario: 識別簽名人清單可見關聯
- **WHEN** 以 `STAFF` 身分呼叫 `GET /api/identifiers`
- **THEN** 回傳清單中每筆含 `identifier` 名稱且後端可透過 `findByUserUserId` 定位當前使用者之簽名人

#### Scenario: Staff 自助停用 typo
- **WHEN** STAFF 對自身首個 `user as signer` 呼叫 `PATCH /api/ref/identifiers/{id}/active` 設 `false`（typo 自清）
- **THEN** 回 `200` 且該筆轉為停用，後續建案預選不再出現

#### Scenario: Staff 不可停用他人簽名人
- **WHEN** STAFF 對非自身的 `user as signer` 呼叫 `PATCH .../active`
- **THEN** 回 `403`，僅 `ADMIN` 可停用他人

#### Scenario: 案件內新建默認為非 user
- **WHEN** STAFF 於 `POST /api/cases` 以 `inlineIdentifiers: [{ name: "新簽名人" }]` 提交
- **THEN** 新建 `identifiers` 之 `user_id` 為 `null` 且 `active=true`，可被停用

### Requirement: 簽名人綁定與同名去重

當 `PATCH /api/admin/users/{id}/role` 提權 `VIEWER → STAFF|ADMIN` 且 `User.displayName` 已存在 `active` 的 `signer but not user`（`user IS NULL` 且 `identifier == displayName`）時，系統 SHALL 回 `409 SIGNER_NAME_CONFLICT { existingIdentifierId }` 並提示是否綁定；呼叫 `POST /api/admin/ref/identifiers/{existingId}/bind { userId }` SHALL 將該簽名人 `user_id` 更新為該使用者（轉為 `user as signer`），否則走自動新建允許同名多筆，後續以 `active` 收斂。

#### Scenario: 提權撞名提示綁定
- **WHEN** ADMIN 將 `displayName=王小明` 的 VIEWER 提權為 STAFF 且庫中已有一筆 `user IS NULL, identifier=王小明, active=true`
- **THEN** 提權回 `409 SIGNER_NAME_CONFLICT` 並附 `existingIdentifierId`

#### Scenario: 綁定既有簽名人
- **WHEN** ADMIN 確認綁定並呼叫 `POST /api/admin/ref/identifiers/{existingId}/bind { userId }`
- **THEN** 該簽名人 `user_id` 更新為 `userId`，`GET /api/ref/identifiers` 類型轉為 `user as signer`

#### Scenario: 不綁定允許新建
- **WHEN** ADMIN 於撞名提示選取消
- **THEN** 走自動新建，庫中產生第二筆同名 `identifier=王小明` 但 `user_id` 指向新使用者，兩筆並存

### Requirement: 簽名人 ID 非自動遞增

`identifiers.identifier_id` SHALL 為 `INTEGER PRIMARY KEY`（移除 `AUTOINCREMENT`），重建前 SHALL 允許 `DELETE FROM identifiers` 後首筆重回 `1`（透過 `DELETE FROM sqlite_sequence`），文件 SHALL 標註 `id` 非穩定外部引用。

#### Scenario: 重設後首筆為 1
- **WHEN** 管理員清空 `identifiers` 並 `DELETE FROM sqlite_sequence WHERE name='identifiers'`
- **THEN** 下一筆新建簽名人 `identifier_id = 1`

### Requirement: 簽名人停用連動與首筆確定性

系統 SHALL 在使用者停用時將其名下 `user as signer` 解綁（`user_id = null`、`former_user_id` 記為該使用者）並置為 `active=false`；在使用者降權出 STAFF/ADMIN（至 VIEWER）時 SHALL 解綁但維持 `active=true`（`id` 不變，仍為新案件候選）；同使用者多筆 `active` 並存時，`ensureForUser` 與同步邏輯 SHALL 以 `identifierId ASC` 首筆為準，其餘不自動更名。

#### Scenario: 停用帳號連動停用簽名人
- **WHEN** STAFF 帳號被停用
- **THEN** 其名下 `active=true` 的 `user as signer` 全數解綁並轉為 `active=false`（`former_user_id` 留存），歷史案件仍以 id 顯示原名

#### Scenario: 降權解綁保留可見
- **WHEN** STAFF 帳號被降級為 VIEWER
- **THEN** 其名下 `active=true` 的 `user as signer` 全數解綁但維持 `active=true`（`former_user_id` 留存），新案件候選仍可見（非使用者）

#### Scenario: 多筆時首筆確定
- **WHEN** 同一使用者存在多筆 `active` 簽名人
- **THEN** 自動帶入與更名只作用於 `identifierId` 最小者，其餘保持不變

### Requirement: 自動帶入全域查重

`ensureForUser` 在新建前 SHALL 檢查全域 `active` 同名；若存在屬他人的 `active` 簽名人（含非使用者），SHALL 走綁定流程或回 `DISPLAY_NAME_EXISTS`，不得靜默新建重複。

#### Scenario: 自動帶入撞名不重複新建
- **WHEN** STAFF 的 `displayName` 已存在屬他人的 `active` 簽名人
- **THEN** 系統不新建第二筆，回 `DISPLAY_NAME_EXISTS` 或導向綁定確認

### Requirement: 簽名人併發冪等與正規化比對

同名 `active` 簽名人 SHALL 在併發下仍只保留一筆（DB 部分唯一索引或異常轉 `409 DISPLAY_NAME_EXISTS`）；名稱比對 SHALL 經正規化（trim、連續空白摺疊、全半形統一、NFC、英文大小寫不敏感）後比較。

#### Scenario: 併發同名只存一筆
- **WHEN** 兩請求同時新建同名 `active` 簽名人
- **THEN** 一筆成功、一筆回 `409 DISPLAY_NAME_EXISTS`，不產生重複

#### Scenario: 大小寫全半形視為同名
- **WHEN** 已有 `王小明` 而新建 `王小明` 的大小寫/全半形變體
- **THEN** 回 `409 DISPLAY_NAME_EXISTS`

### Requirement: 綁定名實一致與重建循環

`bindToUser` SHALL 要求來源簽名人名稱與目標使用者 `displayName` 一致（經同上正規化），不一致 SHALL 拒絕並提示先改名；使用者無 `active` 但有同名 `inactive` 時，`ensureForUser` SHALL 優先提示啟用舊筆而非直接新建。

#### Scenario: 綁定名實不符被拒
- **WHEN** 將外部 `陳建宏` 綁給 `displayName=王小明` 的使用者
- **THEN** 回 4xx 並提示名稱不一致，不改名綁定

#### Scenario: 停用後優先啟用舊筆
- **WHEN** 使用者僅有同名 `inactive` 簽名人而請求自動帶入
- **THEN** 系統提示啟用舊筆，確認後 `active=true` 而非新建第二筆

### Requirement: 重名碰撞範圍界定

重名檢查（新建、改名、自動帶入）SHALL 僅針對非使用者 `active` 同名；同名同姓的使用者簽名人 SHALL 允許並存，以 `帳號` 區分，提權撞非使用者同名時走綁定流程。

#### Scenario: 同名使用者簽名人並存
- **WHEN** 兩位不同 `username` 的 STAFF 使用相同 `displayName`
- **THEN** 各自擁有同名 `active` 簽名人，管理頁與案件表單以帳號區分，不報 `DISPLAY_NAME_EXISTS`

### Requirement: 最後 active 保護

停用全域最後一個 `active` 簽名人 SHALL 需二次確認或被阻擋，避免新案無候選。

#### Scenario: 停用最後一個被阻擋或警告
- **WHEN** ADMIN 停用全域最後一個 `active` 簽名人
- **THEN** 回 4xx 或前端二次確認，確認後才執行

### Requirement: 簽名人篩選抽屜

簽名人管理頁的篩選列（名稱篩選輸入、身分別下拉、顯示已停用勾選、筆數）SHALL 預設直接顯示，不收合；不提供篩選按鈕。篩選邏輯維持既有前端本地過濾不變，各條件 SHALL 可疊加使用。

#### Scenario: 預設隱藏篩選列
- **WHEN** 使用者進入簽名人管理頁
- **THEN** 直接顯示篩選列（含名稱篩選、身分別下拉、顯示已停用勾選與筆數），不再隱藏亦無篩選按鈕（本 change 反轉前案抽屜決策）

#### Scenario: 展開與收合篩選抽屜
- **WHEN** 使用者檢視篩選列
- **THEN** 無需展開或收合，篩選列常顯（抽屜已移除）

#### Scenario: 預設顯示篩選列
- **WHEN** 使用者進入簽名人管理頁
- **THEN** 直接顯示篩選列（含名稱篩選、身分別下拉、顯示已停用勾選與筆數），無篩選按鈕

#### Scenario: 身分別篩選疊加
- **WHEN** 使用者選擇身分別「使用者」並輸入名稱關鍵字
- **THEN** 清單僅顯示符合關鍵字且有關聯使用者的簽名人

### Requirement: 簽名人列表欄序

簽名人列表的欄序 SHALL 為帳號在前、身分別在後（ID、名稱、帳號、身分別、狀態、操作）；身分別徽章（使用者/非使用者）樣式維持不變。

#### Scenario: 帳號欄在身分別欄之前
- **WHEN** 檢視簽名人列表表頭
- **THEN** 帳號欄出現在身分別欄左側

### Requirement: 簽名人狀態徽章同款

簽名人列表的狀態欄 SHALL 與使用者管理採用同款徽章樣式（`badge text-bg-success`/`text-bg-secondary`），文字為啟用/停用，對應簽名人的啟用狀態。

#### Scenario: 啟用中顯示成功色徽章
- **WHEN** 檢視啟用中簽名人的狀態欄
- **THEN** 顯示綠色（`text-bg-success`）徽章，文字為啟用

#### Scenario: 已停用顯示灰色徽章
- **WHEN** 檢視已停用簽名人的狀態欄
- **THEN** 顯示灰色（`text-bg-secondary`）徽章，文字為停用

### Requirement: 參照資料建表邏輯共用與去重

`POST /admin/ref/crops`、`POST /admin/ref/identifiers`（管理頁獨立路徑）與案件內聯路徑 `POST /api/cases` / `PUT /api/cases/{id}` 的 `inlineCrop` / `inlineIdentifiers` SHALL 共用同一建表邏輯：名稱 `trim` 後去重，同分類同名作物或同名 `active=true` 的 `Identifier` SHALL 回 `409` 或於內聯路徑復用既有 `id`（不重建），未被案件引用的孤兒仍可由 `ADMIN` 經 `DELETE /admin/ref/*` 或 `PATCH .../active=false` 清理。

#### Scenario: 管理頁與案件內聯去重一致
- **WHEN** 管理頁已存 `crop: {name:"柑橘", cropCategoryId:1}` 活躍，案件以 `inlineCrop: {name:"柑橘", cropCategoryId:1}` 提交
- **THEN** 案件內聯不新建而復用該 `cropId`，`POST /admin/ref/crops` 以同參數亦回 `409` 或復用

#### Scenario: 同名簽名人復用
- **WHEN** 已有 `Identifier(name:"張三", active=true)`，案件以 `inlineIdentifiers:["張三"]` 提交
- **THEN** 內聯復用該 `id`，不新增重複

#### Scenario: 孤兒可清理
- **WHEN** 某內聯暫存未提交而被丟棄
- **THEN** 無孤兒產生；若曾經管理頁獨立建立且未被引用，`ADMIN` 可 `DELETE` 成功或 `PATCH active=false`

### Requirement: 簽名人 former_user_id 歷史欄位

`identifiers` SHALL 具 `former_user_id`（可空，外鍵指向 `users`，預設 null），記錄解綁前最後所屬使用者；綁定至使用者時 SHALL 清空（已有所屬無需歷史）。既有未綁定簽名人的 `former_user_id` 為 null，恢復流程 SHALL 找不到原筆而走既有行為。

#### Scenario: 解綁留存歷史
- **WHEN** 使用者被降權或停用而簽名人解綁
- **THEN** 該簽名人 `former_user_id` 為該使用者 `userId`

#### Scenario: 綁定清空歷史
- **WHEN** 非使用者簽名人被綁定至使用者
- **THEN** 其 `former_user_id` 清空

### Requirement: ensureForUser 重鏈同名未綁定舊筆

`IdentifierService.ensureForUser` 在使用者無連結 `active` 簽名人時，除既有同名 inactive 連結筆外，SHALL 亦搜尋 `former_user_id` 為該使用者、同名（正規化比對）的未綁定舊筆（`id` 最小優先），命中 SHALL 重新連結至該使用者並 `active=true`，不得直接新建重複。他人同名簽名人（`former_user_id` 不同或為 null）SHALL 不被重鏈，維持既有撞名檢查。使用者已有同名 active 連結筆時維持原行為（更名同步）。

#### Scenario: 重鏈解綁舊筆不新建
- **WHEN** 使用者無連結 active 簽名人，但存在 `former_user_id` 為其、同名的未綁定舊筆
- **THEN** 系統將該舊筆 `user_id` 回填並 `active=true`，不新增簽名人

#### Scenario: 無舊筆才新建
- **WHEN** 使用者無連結 active 簽名人，且無同名舊筆（連結或未綁定皆無）
- **THEN** 走既有新建流程（含全域非使用者同名檢查）

### Requirement: 業務初始種子定義

業務初始資料庫 SHALL 包含完整表結構與參照種子（身分別、縣市鄉鎮、病蟲害、作物分類、被害部位、防治建議、耕種方式、送件方式、服務類別），但 SHALL 不含作物種子（68 筆）與任何業務資料（案件、送件人）。另立測試／開發用初始 schema 檔以此為準，測試與開發以此基礎加入案件、送件人、作物等資料。

#### Scenario: 新庫無作物
- **WHEN** 以業務初始 schema 建庫
- **THEN** `crops` 為空，其他參照表含種子，案件與送件人為空

#### Scenario: 測試基於初始 schema
- **WHEN** 測試需要作物資料
- **THEN** 測試自建作物（或以案件內聯建立），不依賴種子作物

### Requirement: 縣市鄉鎮管理

參照資料管理 SHALL 提供縣市與鄉鎮市區兩分頁籤，支援新增／修改／刪除；鄉鎮 SHALL 歸屬縣市（新增／修改時必選）。刪除已被案件或送件人引用的縣市鄉鎮 SHALL 回 `409` 拒絕（比照既有參照刪除保護）。

#### Scenario: 新增鄉鎮需選縣市
- **WHEN** 新增鄉鎮未選縣市
- **THEN** 回 400 且不建立

#### Scenario: 刪除被引用縣市被拒
- **WHEN** 刪除仍被送件人或案件引用的縣市鄉鎮
- **THEN** 回 `409` 且資料保留

### Requirement: 管理頁分頁籤排序

參照資料管理分頁籤 SHALL 依序為：縣市、鄉鎮市區、身分別、服務類別、送件方式、耕種方式、作物類別、被害部位、防治建議；其餘既有分頁籤接續於後維持現有相對順序。

#### Scenario: 分頁籤順序
- **WHEN** 進入參照資料管理頁
- **THEN** 分頁籤首九位為上述順序

### Requirement: 縣市鄉鎮排序移除

縣市與鄉鎮市區 SHALL 一律依 `id` 升冪排序且不提供 `sortOrder` 欄位（延續害物排序移除模式）。`GET /api/ref/cities` 回應的縣市清單 SHALL 依 `city_id` 升冪，其下鄉鎮 SHALL 依 `district_id` 升冪；`DistrictItem` SHALL 不含 `sortOrder`。

#### Scenario: 縣市依 id 排序且無 sortOrder
- **WHEN** 呼叫 `GET /api/ref/cities`
- **THEN** 縣市依 `id` 升冪回傳，且回應不含 `sortOrder` 欄位

#### Scenario: 鄉鎮依 id 排序
- **WHEN** 檢視任一縣市下鄉鎮清單
- **THEN** 鄉鎮依 `district_id` 升冪排列，與既有呈現順序一致
