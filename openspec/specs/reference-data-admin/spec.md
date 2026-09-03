# Reference Data Admin Specification

## Purpose

讓管理者可維護作物、病蟲害、服務方式、送達方式與標的等參照資料，無需修改資料庫種子。

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

每位 `ROLE_STAFF` 與 `ROLE_ADMIN` 使用者 SHALL 擁有恰一個以其 `displayName` 命名的 `Identifier`（診斷簽名人），`Identifier.user` SHALL 指向該使用者；`VIEWER` 不強制。系統 SHALL 於使用者建立（`AuthService` 註冊後由 ADMIN 授權、`User` 建立、`DataInitializer`）與 `displayName` 變更（`AccountService.updateProfile`、`Admin` 調整）時同步建立或更名對應 Identifier，並於交易內完成；刪除使用者 SHALL 不自動刪除其 Identifier（避免已結案案件 `case_identifiers` 外鍵中斷）。

#### Scenario: Staff 建立時自動建立簽名人
- **WHEN** ADMIN 將新註冊使用者授權為 `ROLE_STAFF`（或系統透過 `DataInitializer` 建立 staff）
- **THEN** `identifiers` 新增一筆 `identifier = displayName` 且 `user_id` 指向該使用者

#### Scenario: 修改顯示名稱同步更名簽名人
- **WHEN** STAFF 透過帳號管理將 `displayName` 由「診斷員A」改為「診斷員B」
- **THEN** 其關聯的 `Identifier.identifier` 同步更新為「診斷員B」，案件詳情中歷史簽名仍顯示原字串（若需追溯以案件快照為準）

#### Scenario: 已有簽名人不重複建立
- **WHEN** 已擁有簽名人的使用者再次變更非顯示名稱欄位（如 email）
- **THEN** 不新增 `Identifier`，僅在 `displayName` 變更時更名

#### Scenario: 刪除使用者保留簽名人
- **WHEN** ADMIN 刪除一名 STAFF 使用者
- **THEN** 其關聯 `Identifier` 保留，後續仍可被案件引用與刪除保護（`existsByCaseIdentifiersIdentifierIdentifierId`）

#### Scenario: 識別簽名人清單可見關聯
- **WHEN** 以 `STAFF` 身分呼叫 `GET /api/identifiers`
- **THEN** 回傳清單中每筆含 `identifier` 名稱且後端可透過 `findByUserUserId` 定位當前使用者之簽名人
