# Case Lifecycle Specification

## Purpose

為案件導入明確狀態生命週期 (待處理／已處理／已結案) 並補全更新契約，使案件可被完整追蹤與修正。

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

`POST /api/cases` 與 `PUT /api/cases/{id}` 在 `identifierIds` 為空、未傳或空陣列時 SHALL 自動帶入當前登入使用者的關聯 `Identifier`（以 `Identifier.user.userId = 當前使用者` 查找；若無則以當前 `displayName` 即時建立並使用）；若請求已含 `identifierIds` 則 SHALL 原樣採用，不覆蓋或增補。前述自動帶入 SHALL 於交易內完成且回應的 `identifiers` 陣列 SHALL 包含該簽名人。

#### Scenario: 建案未選簽名人自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 建立案件
- **THEN** 案件 `identifiers` 含一筆其 `displayName` 對應的簽名人，且 `GET /api/cases/{id}` 可見

#### Scenario: 建案已選簽名人不覆蓋
- **WHEN** STAFF 以 `identifierIds: [2,3]` 建立案件
- **THEN** 案件 `identifiers` 恰為 `[2,3]`，不額外加入當前使用者簽名人

#### Scenario: 無關聯簽名人時即時建立
- **WHEN** 新 STAFF（尚無 `Identifier`）以空清單建案
- **THEN** 系統先建立 `Identifier(identifier=displayName, user=currentUser)` 再關聯至案件，後續 `GET /api/identifiers` 可見該筆

#### Scenario: 更新時空清單亦自動帶入
- **WHEN** STAFF 以 `identifierIds: []` 更新既有案件
- **THEN** 案件簽名人更新為僅含當前使用者簽名人（整組替換語意同既有多對多）

#### Scenario: 更新時未傳欄位不變
- **WHEN** STAFF 更新案件但未傳 `identifierIds`（null）
- **THEN** 若原語意為「未傳即不更動」，則保留原簽名人；若本 change 定義「未傳視為空」則自動帶入（實作以 `CaseService` 現有 `identifierIds != null` 判斷為準，此情境驗證保留原值）

#### Scenario: 前端預選當前使用者簽名人
- **WHEN** STAFF 開啟案件新增表單
- **THEN** `GET /api/identifiers`（或 `GET /api/identifiers/me`）回傳的當前使用者簽名人預設勾選，仍可手動增刪多簽名
