## ADDED Requirements

### Requirement: 業務用 Scheme 無預設作物且未知前置

`schema.sql` 業務用 Scheme SHALL 僅含 `crop_categories` 種子，`crops` 為空；`cities`、`methods` 的 `未知` SHALL 為 `id=1`，其餘列依次後移，`districts` 每 `city_id` 內 `未知` 的 `sort_order` SHALL 為 `1`，`pest_categories` SHALL 以 `pest_category_code ASC` 使 `X00` 排 `X01` 前。參照資料 `GET /api/ref/*` 仍以現有契約回傳，前端 SHALL 以空 `crops` 呈現「請先建作物」。

#### Scenario: 空庫無作物
- **WHEN** 以 `prod` 業務 Scheme 建庫後呼叫 `GET /api/ref/crop-categories`
- **THEN** 回空 `crops` 陣列，`crop_categories` 仍 9 筆且 `未知` 非必要

#### Scenario: 未知為首
- **WHEN** 呼叫 `GET /api/ref/cities` 或 `GET /api/ref/methods`
- **THEN** 首筆 `id=1` 為 `未知`，其餘依原序後移

#### Scenario: 鄉鎮未知前置
- **WHEN** 呼叫 `GET /api/ref/cities` 取得某 `city` 的 `districts`
- **THEN** 該 `city` 內 `sort_order=1` 為 `未知`，餘 `sort_order` 遞增且 `未知` 可作預設

#### Scenario: 病蟲害未知前置
- **WHEN** 取得 `pest_categories` 依 `pest_category_code ASC`
- **THEN** `X00` 排 `X01` 前，`未知` 為首

### Requirement: 參照資料管理處理空作物

`reference-data-admin` 的作物管理 SHALL 於 `crops` 為空時顯示空狀態並引導至「新增作物」，刪除/修改 `crops` 的 `409` 判斷維持。

#### Scenario: 空作物空狀態
- **WHEN** ADMIN 進入作物管理且庫中無作物
- **THEN** 見空狀態與「新增作物」按鈕，無報錯
