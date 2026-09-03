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
