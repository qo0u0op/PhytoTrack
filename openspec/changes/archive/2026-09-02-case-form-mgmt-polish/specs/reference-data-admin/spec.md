## MODIFIED Requirements

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

## ADDED Requirements

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

