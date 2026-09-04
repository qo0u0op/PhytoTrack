## ADDED Requirements

### Requirement: 縣市鄉鎮排序移除

縣市與鄉鎮市區 SHALL 一律依 `id` 升冪排序且不提供 `sortOrder` 欄位（延續害物排序移除模式）。`GET /api/ref/cities` 回應的縣市清單 SHALL 依 `city_id` 升冪，其下鄉鎮 SHALL 依 `district_id` 升冪；`DistrictItem` SHALL 不含 `sortOrder`。

#### Scenario: 縣市依 id 排序且無 sortOrder
- **WHEN** 呼叫 `GET /api/ref/cities`
- **THEN** 縣市依 `id` 升冪回傳，且回應不含 `sortOrder` 欄位

#### Scenario: 鄉鎮依 id 排序
- **WHEN** 檢視任一縣市下鄉鎮清單
- **THEN** 鄉鎮依 `district_id` 升冪排列，與既有呈現順序一致
