## Why

縣市（`cities`）與鄉鎮市區（`districts`）的 `sort_order` 欄位已無需手動維護順序：種子資料的 `id` 本身即為穩定順序（未知→各縣市依序，鄉鎮每縣市內依序），額外欄位徒增寫入、遷移與 DTO 負擔。延續先前「害物排序移除」（主規格 `reference-data-admin` 已載明病蟲害改依 `pestCategoryCode` 且不提供 `sortOrder`）的同一模式，將縣市鄉鎮改為一律依 `id` 排序並移除 `sort_order`。

## What Changes

- `schema.sql`：移除 `cities.sort_order`、`districts.sort_order` 欄位、兩個 `sort_order` 索引（`idx_cities_sort`、`idx_districts_sort`），種子 `INSERT` 去除 `sort_order` 值。
- 後端：`City`、`District` 實體移除 `sortOrder` 欄位；`CityRepository.findAllByOrderBySortOrderAsc` 改為依 `id` 排序；`ReferenceDataService.cities()` 與 `toCityResponse` 內鄉鎮排序改依 `districtId`；`CityResponse.DistrictItem` 移除 `sortOrder` 欄位。
- 前端：無程式碼變更（縣市鄉鎮未使用 `sortOrder`，僅依後端回傳順序呈現）；`types/api.ts` 以 `openapi-typescript` 重生以去除 `sortOrder` 型別。
- 既有庫遷移：SQLite `ddl-auto: update` 不會刪除舊欄位，舊庫殘留孤兒欄位無害（JPA 不再映射）；如需清理見 Migration Plan。

## Capabilities

### New Capabilities

- 無

### Modified Capabilities

- `reference-data-admin`: 縣市鄉鎮排序改依 `id` 升冪，不再提供 `sortOrder` 欄位（延續害物排序移除模式）

## Impact

- 後端：`models/City.java`、`models/District.java`、`repository/CityRepository.java`、`service/ReferenceDataService.java`、`dto/ReferenceDtos.java`、`resources/schema.sql`
- 前端：`types/api.ts` 重生（`DistrictItem.sortOrder` 消失）；縣市鄉鎮下拉呈現順序改為 id 序（與現行種子順序一致，使用者無感）
- 文件：`docs/DEPLOY.md` 加註舊庫孤兒欄位說明；`docs/ARCHITECTURE.md` 若提及排序則同步
- 相容性：`GET /api/ref/cities` 回應不再含 `sortOrder`；排序結果與現行一致（種子 `id` 即原 `sort_order` 順序），屬行為收斂而非破壞
