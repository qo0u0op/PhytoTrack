# Case Lifecycle Specification

## Purpose

為案件導入明確狀態生命週期 (待處理／已處理／已結案) 並補全更新契約，使案件可被完整追蹤與修正，避免狀態錯置導致資料不一致。

## Requirements

### Requirement: 案件狀態列舉

系統 SHALL 以列舉值表示案件狀態：`PENDING` (待處理)、`RESOLVED` (已處理)、`CLOSED` (已結案)。

#### Scenario: 建立新案件
- **WHEN** 建立案件
- **THEN** 案件狀態為 `PENDING`

### Requirement: 狀態轉移規則

STAFF/ADMIN SHALL 可將案件由 `PENDING` 標記為 `RESOLVED`；ADMIN SHALL 可將 `RESOLVED` 標記為 `CLOSED`；系統 SHALL 拒絕任何非法轉移。

#### Scenario: 合法轉移
- **WHEN** STAFF 將 `PENDING` 案件標記為 `RESOLVED`
- **THEN** 案件狀態更新為 `RESOLVED`

#### Scenario: 非法轉移
- **WHEN** STAFF 嘗試直接將案件標記為 `CLOSED`
- **THEN** 回應 4xx，且案件狀態維持不變

### Requirement: 案件更新契約補全

更新案件 SHALL 允許修改送件人、作物、病蟲害等多對多關聯，以及既有可編輯欄位。

#### Scenario: 修正送件人
- **WHEN** STAFF 更新案件的送件人姓名
- **THEN** 案件明細顯示更新後的值

### Requirement: 既有狀態資料遷移

系統 SHALL 將既有整數狀態資料對映至新列舉 (`0` → `PENDING`)，遷移後既有案件可正常讀取與查詢。

#### Scenario: 讀取既有案件
- **WHEN** 讀取遷移前建立的案件
- **THEN** 狀態顯示為 `PENDING` 且查詢正常

### Requirement: 案件表單病蟲害排序與診斷結果命名

案件表單中「診斷結果」區塊的病蟲害分類下拉 SHALL 依 `pestCategoryCode` 升冪排序；區塊標題 SHALL 為「診斷結果」並以灰字附註「(可增刪多列，同分類可多筆)」；其中「害物類型」欄位標籤 SHALL 為「害物」，「病蟲害分類」標籤 SHALL 為「害物因素」，新增按鈕文案 SHALL 為「新增因素」且該按鈕 SHALL 獨立一行（前置換行）。

#### Scenario: 病蟲害選單升冪
- **WHEN** 開啟案件新增頁並展開害物因素下拉
- **THEN** 選項依代碼升冪排列

#### Scenario: 診斷結果標籤與灰字
- **WHEN** 檢視診斷結果欄位標籤
- **THEN** 顯示「診斷結果」並於後方以灰字顯示「(可增刪多列，同分類可多筆)」

#### Scenario: 欄位與按鈕更名
- **WHEN** 檢視診斷結果區塊的三欄與按鈕
- **THEN** 分別顯示「害物」「害物因素」「新增因素」

#### Scenario: 新增因素按鈕換行
- **WHEN** 檢視診斷結果區塊
- **THEN** 「＋新增因素」按鈕位於下一行，非與下拉同列

### Requirement: 案件表單作物資訊與建議區塊重組

「作物與診斷資訊」卡片標題 SHALL 更名為「作物資訊」；「土壤、栽培、用藥紀錄」輸入框的 ghost placeholder SHALL 移除（空白）；「建議採取措施」(hintDescription) 與「診斷結果」(pestRows) 兩區塊 SHALL 由「作物資訊」卡片移至「防治建議與簽名」卡片，後者卡片標題 SHALL 更名為「診斷結果與建議」。其中「土壤、栽培、用藥紀錄」資料庫欄位 SHALL 為 `case_description`（`Case.caseDescription`），不再使用 `pestDescription`/`pest_description`。

#### Scenario: 作物資訊卡片更名
- **WHEN** 檢視案件表單上卡標題
- **THEN** 顯示「作物資訊」

#### Scenario: 去除 ghost 文案
- **WHEN** 檢視土壤、栽培、用藥紀錄輸入框
- **THEN** placeholder 為空白，不顯示對應紙本提示

#### Scenario: 區塊搬移與下卡更名
- **WHEN** 檢視診斷結果與建議卡片
- **THEN** 內含診斷結果多列、建議採取措施輸入框、防治建議勾選與簽名，且標題為「診斷結果與建議」

#### Scenario: 欄位更名為 caseDescription
- **WHEN** 檢視資料庫與 API 欄位
- **THEN** 土壤、栽培、用藥紀錄欄位為 `case_description`/`caseDescription`，不再出現 `pestDescription`

### Requirement: 案件檢視與預覽版面重排

案件檢視頁與預覽彈窗中「服務類別、送件方式」二欄 SHALL 置於「耕種方式」之前；分隔線 SHALL 位於地址區塊下方與鑑定者區塊上方；檢視與預覽中「作物種類」標籤 SHALL 更名為「作物類別」，「作物名稱」標籤 SHALL 更名為「作物」。

#### Scenario: 服務與送件前移
- **WHEN** 檢視案件詳細或預覽
- **THEN** 服務類別與送件方式顯示於耕種方式之前

#### Scenario: 分隔線位置
- **WHEN** 檢視案件詳細
- **THEN** 分隔線位於地址下方與鑑定者上方

#### Scenario: 標籤更名
- **WHEN** 檢視案件詳細的作物欄位
- **THEN** 顯示「作物類別」與「作物」

### Requirement: 案件表單填寫順序與獨立收件卡片

案件表單的「作物資訊」卡片內欄位 SHALL 依序為：服務類別、交付方式、耕種方式、作物類別、作物、被害部位、栽培面積或規模、被害面積或規模、土壤、栽培、用藥紀錄；其中「作物別」標籤 SHALL 更名為「作物類別」，「種植面積」標籤 SHALL 更名為「栽培面積或規模」，「被害面積或植株數」標籤 SHALL 更名為「被害面積或規模」；「收件日期」與「狀態」欄位 SHALL 獨立為「收件資訊」卡片置於「作物資訊」卡片之前；狀態下拉 SHALL 允許已選「已結案」後仍可切換，僅於儲存成功後由後端鎖定（CLOSED 僅 ADMIN 可選）。

#### Scenario: 表單欄位順序
- **WHEN** 檢視作物資訊卡片
- **THEN** 欄位依服務類別、交付方式、耕種方式、作物類別、作物、被害部位、栽培面積或規模、被害面積或規模、土壤之序排列

#### Scenario: 表單標籤更名
- **WHEN** 檢視對應欄位標籤
- **THEN** 顯示作物類別、栽培面積或規模、被害面積或規模

#### Scenario: 收件資訊獨立卡片
- **WHEN** 檢視案件表單
- **THEN** 收件日期與狀態位於獨立的「收件資訊」卡片，且該卡位於作物資訊之前

#### Scenario: 狀態下拉可重選
- **WHEN** 於收件資訊卡片選擇已結案
- **THEN** 仍可切換至其他狀態，僅儲存成功後由後端限制

### Requirement: 建案時診斷簽名人自動帶入

`POST /api/cases` 與 `PUT /api/cases/{id}` 在 `identifierIds` 為空、未傳或空陣列時 SHALL 自動帶入當前登入使用者的關聯 `Identifier`（以 `Identifier.user.userId = 當前使用者` 且 `active=true` 查找；若無則以當前 `displayName` 依 `IdentifierService.ensureForUser` 建立並使用）；若請求已含 `identifierIds` 則 SHALL 原樣採用，不覆蓋或增補。前述自動帶入 SHALL 於交易內完成且回應的 `identifiers` 陣列 SHALL 包含該簽名人。

#### Scenario: 建案未選簽名人自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 建立案件
- **THEN** 案件 `identifiers` 含一筆其 `displayName` 對應且 `active=true` 的簽名人，且 `GET /api/cases/{id}` 可見

#### Scenario: 建案已選簽名人不覆蓋
- **WHEN** STAFF 以 `identifierIds: [2,3]` 建立案件
- **THEN** 案件 `identifiers` 恰為 `[2,3]`，不額外加入當前使用者簽名人

#### Scenario: 無關聯簽名人時即時建立
- **WHEN** 新 STAFF（尚無 `Identifier`）以空清單建案
- **THEN** 系統先建立 `Identifier(identifier=displayName, user=currentUser, active=true)` 再關聯至案件，後續 `GET /api/identifiers` 可見該筆

#### Scenario: 更新時空清單亦自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 更新既有案件
- **THEN** 案件簽名人更新為僅含當前使用者簽名人（整組替換語意同既有多對多）

#### Scenario: 更新時未傳欄位不變
- **WHEN** STAFF 更新案件但未傳 `identifierIds`（null）
- **THEN** 保留原簽名人

#### Scenario: 前端預選當前使用者簽名人
- **WHEN** STAFF 開啟案件新增表單
- **THEN** `GET /api/identifiers/me` 回傳的當前使用者簽名人預設勾選，仍可手動增刪多簽名

### Requirement: 案件表單內內聯簽名人原子建立且默認為非使用者

`POST /api/cases` 與 `PUT /api/cases/{id}` 支援 `inlineIdentifiers: [{ name }]` 與 `identifierIds` 併用，`inlineIdentifiers` 內每筆 SHALL 於同一交易內建立 `Identifier(user IS NULL, active=true)`，名稱去空白重複時復用既有 `active` 同名簽名人（`signer but not user` 優先），建立後與案件以 `case_identifiers` 關聯；若未傳 `identifierIds` 且未傳 `inlineIdentifiers` 則走自動帶入。`STAFF|ADMIN` 於案件內新建者一律 `user IS NULL`，僅提權路徑建 `user as signer`。放棄新增/編輯（前端不提交）時 `inlineIdentifiers` SHALL 不落庫；交易失敗 SHALL 全回滾（含內聯簽名人）。

#### Scenario: 內聯新建默認為非 user
- **WHEN** STAFF 以 `inlineIdentifiers: [{ name: "外聘專家" }]` 建案
- **THEN** 新建 `identifiers` 其 `user_id IS NULL` 且 `active=true`，案件關聯該 `id`

#### Scenario: 併用併去重
- **WHEN** 同時傳 `identifierIds=[1]` 與 `inlineIdentifiers=[{ name: "外聘專家" }]`
- **THEN** 案件最終簽名人為 `1` 加新建之外聘，名稱重複時復用既有 `active` 同名而非新建

#### Scenario: 放棄不落庫
- **WHEN** STAFF 於表單內新增外聘簽名人後取消未提交
- **THEN** 資料庫 `identifiers` 無新增，`GET /api/ref/identifiers` 不可見

#### Scenario: 交易失敗全回滾
- **WHEN** `inlineIdentifiers` 建成但後續 `pestCategoryIds` 校驗失敗致 `400`
- **THEN** 內聯簽名人亦回滾，不殘留

### Requirement: 停用簽名人於新增案件時隱藏

`GET /api/ref/identifiers` 預設僅回 `active=true`，故 `active=false` 的簽名人 SHALL 不出現於 `CaseFormView` 的候選清單；但 `PUT /api/cases/{id}` 仍可引用已停用之歷史 `id`（刪除保護不變）。

#### Scenario: 已停用不出現於新建
- **WHEN** 某簽名人已 `active=false`
- **THEN** `GET /api/ref/identifiers` 不含該筆，`CaseFormView` 不可勾選，但 `GET /api/cases/{歷史id}` 仍顯示其名

### Requirement: 新建案件拒絕停用簽名人

`POST /api/cases` SHALL 拒絕 `identifierIds` 內含 `active=false` 者（回 `409` 或 `400`），`PUT /api/cases/{id}` 更新歷史案件時 SHALL 放行已引用之 `inactive` 以保留顯示。

#### Scenario: 新建引用停用簽名人被拒
- **WHEN** STAFF 以 `identifierIds: [已停用id]` 建立案件
- **THEN** 回 4xx 且案件未建立，簽名人清單仍僅顯示 `active` 候選

#### Scenario: 歷史案件仍顯示停用簽名人
- **WHEN** 檢視已引用停用簽名人的舊案件
- **THEN** 詳情仍以 id 顯示原名，不因停用消失

### Requirement: 案件表單簽名人可辨同名

`CaseFormView` 簽名人候選 SHALL 與管理頁一致顯示 `身分別＋帳號`（如 `使用者 · wang123`），同名時可以帳號區分選擇，提交仍以 `id` 為準。

#### Scenario: 同名以帳號區分
- **WHEN** 存在兩筆 `王小明`（帳號 `wang1`、`wang2`）
- **THEN** 案件表單顯示兩列可辨帳號，勾選後以各自 `id` 提交

### Requirement: 案件表單內內聯參照原子建立

`POST /api/cases` 與 `PUT /api/cases/{id}` 在建立或更新案件時，對於在表單內現場新增的作物與簽名人 SHALL 以內聯欄位於同一交易內原子建立並關聯，交易失敗 SHALL 全回滾且放棄案件時 SHALL 不產生任何參照殘留。具體為：`inlineCrop: {name, cropCategoryId}` 若提供 SHALL 於交易內建立 `Crop`（同分類同名 `409` 去重，成功則復用或新建），`inlineIdentifiers: string[]` 若提供 SHALL 於交易內建立 `Identifier`（`user_id == null`、`active=true`，同名 `active=true` 則復用）；建立後取得之 `id` SHALL 自動加入案件的 `cropId` / `identifierIds` 關聯並與案件一併提交。前端「新增作物／簽名人」在案件提交前 SHALL 僅為本地暫存，不呼叫 `POST /admin/ref/*`，放棄（取消、導回列表、重新整理） SHALL 丟棄暫存且不呼叫後端。

#### Scenario: 空表單新增作物後放棄不落庫
- **WHEN** STAFF 在新增案件表單內以 `inlineCrop: {name:"新作物A", cropCategoryId:1}` 暫存作物後，未提交而按取消返回列表
- **THEN** 後端 `crops` 不含「新作物A」，重新進入表單下拉亦無該項

#### Scenario: 內聯作物隨案件一併提交才可見
- **WHEN** STAFF 以 `inlineCrop: {name:"新作物B", cropCategoryId:1}` 併入 `POST /api/cases` 提交
- **THEN** 回應 `201` 且 `GET /api/ref/crop-categories` 可見該作物，案件 `cropName` 為「新作物B」，交易內去重生效

#### Scenario: 內聯簽名人原子建立
- **WHEN** STAFF 以 `inlineIdentifiers: ["新簽名人X"]` 併入 `POST /api/cases` 提交
- **THEN** `GET /api/ref/identifiers` 可見該簽名人且案件 `identifiers` 含其 `id`，若同名 `active=true` 已存在則復用既有 `id`

#### Scenario: 內聯與既有 id 併用
- **WHEN** 請求同時含 `cropId: 36` 與 `inlineCrop: {name:"新作物C", ...}` 或同時含 `identifierIds:[1]` 與 `inlineIdentifiers:["新簽名人Y"]`
- **THEN** 系統 SHALL 以顯式 `cropId`/`identifierIds` 為準或合併兩者（`crop` 以 `inline` 覆蓋顯式，`identifiers` 為並集），文件化其一且不產生重複

#### Scenario: 交易失敗全回滾
- **WHEN** `POST /api/cases` 含 `inlineCrop` 但案件必填 `receiveDate` 缺失而回 `400`
- **THEN** `inlineCrop` 對應的作物亦未落庫，`GET /api/ref/crop-categories` 無該項

#### Scenario: 編輯時內聯新增亦原子
- **WHEN** STAFF 編輯既有案件時以 `inlineIdentifiers: ["編輯新增簽名人"]` 併入 `PUT /api/cases/{id}`
- **THEN** 僅在更新成功時該簽名人可見，放棄編輯則不產生

#### Scenario: 放棄編輯不殘留
- **WHEN** STAFF 在編輯頁暫存 `inlineCrop` 後未提交而取消
- **THEN** 該暫存作物不落庫，列表與管理頁均不可見

### Requirement: 案件表單診斷簽名人區塊獨立

案件新增／編輯表單的「診斷簽名人（可複選）」區塊 SHALL 為獨立卡片（`card shadow-sm`），與「防治建議」上下分離而非左右併排。清單 SHALL 以「顯示名稱-徽章-帳號」為一組，採左右雙欄（`col-md-6`）網格排列，每組含勾選框、顯示名稱、身分別徽章與帳號。既有勾選與新增行為維持不變。

#### Scenario: 簽名人與防治建議併排
- **WHEN** 開啟案件新增或編輯表單
- **THEN** 診斷簽名人區塊為獨立卡片，與防治建議區塊上下分離（不再左右併排）

#### Scenario: 三欄表格對齊
- **WHEN** 檢視診斷簽名人清單
- **THEN** 每組顯示名稱、徽章與帳號為一體，採左右雙欄排列（取代原三欄表格）

### Requirement: 新增案件田區位置初隱

新增案件（非編輯）時「田區位置」卡 SHALL 預設隱藏，僅在送件人已儲存或已載入（`form.senderId !== null`）後顯示；編輯既有案件時該卡 SHALL 一直可見。隱藏期間 SHALL 不阻擋送件人區塊操作，且案件提交時若田區位置仍未顯示則視為未選（與既有必填驗證一致）。

#### Scenario: 新增時初隱
- **WHEN** 使用者進入案件新增頁且尚未儲存/載入送件人
- **THEN** 不顯示「田區位置」卡

#### Scenario: 儲存送件人後顯示
- **WHEN** 於新增頁點擊「儲存送件人」成功（`POST /api/senders` 回 200 且 `form.senderId` 被賦值）
- **THEN** 顯示「田區位置」卡，可選縣市/鄉鎮市區與「和送件人相同」

#### Scenario: 載入候選後顯示
- **WHEN** 於新增頁透過模糊提示或「搜尋候選」選用既有送件人
- **THEN** 顯示「田區位置」卡，且「和送件人相同」勾選時同步送件人縣市鄉鎮

#### Scenario: 編輯時常顯
- **WHEN** 進入案件編輯頁（`editId !== null`）
- **THEN** 無論 `senderId` 狀態皆顯示「田區位置」卡

### Requirement: 送件人取消一鍵清空

新增案件時送件人區塊的「儲存送件人」旁 SHALL 常駐「取消」按鈕；點擊 SHALL 直接清空送件人輸入（`senderName/senderDisplayName/senderPhone/senderAddress` 置空、`senderDistrictId/senderTypeId` 與縣市選取回預設、`senderId` 置 `null`、`fieldDistrictId` 與「和送件人相同」重置），且 SHALL 不彈任何 `alert`/`Swal` 提示，亦不觸發模糊搜尋提示。已選用既有送件人的「取消編輯」（還原快照）按鈕 SHALL 保留，僅在 `form.senderId !== null && senderDirty` 時顯示，二者職責分離。

#### Scenario: 一鍵清空
- **WHEN** 於新增頁在送件人欄位輸入任意值後點擊「取消」
- **THEN** 送件人四個文字欄位被清空，縣市/鄉鎮市區與身分別回到初始預設，`senderId` 為 `null`，且田區位置卡再次隱藏（因 `senderId` 已空）

#### Scenario: 無提示
- **WHEN** 點擊新增模式的「取消」
- **THEN** 不出現任何 `Swal`/`alert`/`confirm` 彈窗

#### Scenario: 不觸發模糊提示
- **WHEN** 點擊「取消」後
- **THEN** 不立即彈出「有相似的資料，是否帶入?」提示

#### Scenario: 編輯模式不顯示此取消
- **WHEN** 進入案件編輯頁
- **THEN** 不顯示新增模式的「取消」按鈕（僅依既有邏輯顯示「取消編輯」）

### Requirement: 作物下拉未選分類時禁用

新增案件時，若未選擇作物類別，作物下拉 SHALL 為禁用狀態（`disabled`），且不接受輸入或選擇；選擇分類後 SHALL 啟用並顯示對應作物清單。

#### Scenario: 未選分類禁用
- **WHEN** 使用者開啟新增案件表單且尚未選擇作物類別
- **THEN** 作物下拉為禁用，無法選擇

#### Scenario: 選擇分類後啟用
- **WHEN** 使用者選擇作物類別
- **THEN** 作物下拉啟用並列出該類別作物

### Requirement: 送件人電話候選門檻與 inline 呈現

案件表單的送件人候選 SHALL 僅在電話輸入 4 碼以上時觸發搜尋；候選結果 SHALL 以 inline 下拉呈現於送件人卡內（與「取消沿用」同區域），而非 popup。候選操作：選擇既有送件人為「使用」，建立新送件人情境的按鈕文案 SHALL 為「使用」而非「沿用」，語意一致。

#### Scenario: 電話未達 4 碼不搜尋
- **WHEN** 使用者在電話欄輸入少於 4 碼
- **THEN** 不呼叫候選搜尋

#### Scenario: 電話達 4 碼觸發 inline 候選
- **WHEN** 電話輸入達 4 碼以上
- **THEN** 呼叫搜尋並於送件人卡內顯示 inline 候選下拉

#### Scenario: 候選 inline 位置
- **WHEN** 出現候選結果
- **THEN** 候選下拉顯示於送件人卡內，與取消沿用提示同區域，而非 popup

#### Scenario: 建立新送件人按鈕文案
- **WHEN** 候選結果提供「建立新送件人」選項
- **THEN** 按鈕顯示為「使用」而非「沿用」

### Requirement: 診斷簽名人顯示條件

診斷簽名人區塊 SHALL 僅在診斷結果（病蟲害因素）或防治建議（`hintIds`/`hintDescription`）有編輯時才顯示；預設 SHALL 不勾選任何簽名人，除非診斷內容有變更。

#### Scenario: 未編輯診斷時隱藏
- **WHEN** 使用者開啟新增案件表單且尚未編輯診斷結果或防治建議
- **THEN** 診斷簽名人卡片不顯示，且 `identifierIds` 為空

#### Scenario: 編輯診斷後顯示
- **WHEN** 使用者編輯診斷結果或防治建議
- **THEN** 診斷簽名人卡片顯示，可勾選簽名人

### Requirement: 防治建議橫式排列

防治建議（`hints`）的呈現 SHALL 由直式改為橫式排列，樣式參考被害部位的橫式多選（`flex-wrap`）。

#### Scenario: 橫式顯示
- **WHEN** 檢視防治建議區塊
- **THEN** 選項以橫式多列呈現，而非直式單列
