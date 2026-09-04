## 1. 後端移除 sort_order 改 id 排序

- [x] 1.1 `resources/schema.sql` 移除 `cities.sort_order`、`districts.sort_order` 欄位、`idx_cities_sort`、`idx_districts_sort` 索引與種子 `sort_order` 值，驗證新庫建表無該欄
- [x] 1.2 `models/City.java`、`models/District.java` 移除 `sortOrder` 欄位，`repository/CityRepository.java` 改 `findAllByOrderByCityIdAsc`（保留 `@EntityGraph`），驗證編譯通過
- [x] 1.3 `service/ReferenceDataService.java`（`cities()`、`toCityResponse`）與 `dto/ReferenceDtos.java`（`DistrictItem` 去第三參數）改 id 排序，驗證 `GET /api/ref/cities` 縣市依 `city_id`、鄉鎮依 `district_id` 升冪且無 `sortOrder` 欄位

## 2. 文件與型別

- [x] 2.1 `docs/DEPLOY.md` 加註舊庫孤兒欄位說明（無需動作，可選清理），`docs/ARCHITECTURE.md` 若提及排序則同步，驗證文件一致
- [x] 2.2 以 `openapi-typescript` 重生 `frontend/src/types/api.ts`，驗證 `DistrictItem` 無 `sortOrder` 且 `npm run build`（含 `vue-tsc`）通過

## 3. 驗證與回歸

- [x] 3.1 執行 `cd backend && mvn test` 全回歸，驗證參照資料與案件測試通過且縣市鄉鎮呈現順序不變
- [x] 3.2 執行 `openspec validate --specs --changes --strict`，驗證無錯誤
