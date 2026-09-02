## MODIFIED Requirements

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
