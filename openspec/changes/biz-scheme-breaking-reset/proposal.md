# 提案：業務用 Scheme 破壞性重置 — 部署種子分離與未知項重排

## Why

現行 `schema.sql` 混合業務與開發種子：預填 68 筆作物致業務庫需手工清理、`未知` 分散於末位（`cities 23`/`methods 4`）不利預設選項、 `districts/pest_categories` 排序與「未知優先」語意相悖，且部署仍建 `admin/staff/viewer` 三帳號使 `admin123` 長期存活。下次部署需以乾淨業務 Scheme 上線（僅作物分類、未知前置、單管理員強制改密），並與開發/測試種子徹底分離，否則業務資料與測試案例混淆、預設值不一致。

## What Changes

- **1. 移除所有 `crops` 預設**：**BREAKING** `schema.sql` `INSERT INTO crops` 68 筆全刪，僅保留 `crop_categories` 9 筆；`crops` 空庫由業務人員經 `參照資料管理` 自建，開發/測試另以 `seed-dev.sql` 補回。
- **2. `未知` 重排至首位**：**BREAKING** `cities` 與 `methods` 的 `未知` 改 `id=1`，其餘列 `id` 向後遞移（`cities: 未知 1, 臺北市 2...`、`methods: 未知 1, 有機 2...`）；`districts` 每 `city_id` 內 `未知` 的 `sort_order` 改 `1`，餘 `sort_order` 遞增；`pest_categories` 以 `pest_category_code` 排序使 `X00`（未知）排 `X01` 前（`ORDER BY pest_category_code ASC` 已滿足，僅需種子順序與文件對齊）。
- **3. `districts/pest_categories` 未知前置**：同上，`districts` 以 `(city_id, sort_order)` 前置，`pest_categories` 以 `code` 前置，`schema.sql` 種子順序同步。
- **4. 僅 `admin/admin123` 單帳號**：**BREAKING** `schema.sql` 與 `DataInitializer` 部署種子僅建 `admin/admin123`（`ROLE_ADMIN, active=true`），`staff/viewer` 不再於部署庫建立；`production` profile 啟動後硬編碼僅此帳號。
- **5. 登入強制改密**：`POST /api/auth/login` 若 `username=admin && BCrypt.matches(admin123)` 回 `200` 但附 `mustChangePassword=true`（或 `403 MUST_CHANGE_PASSWORD`），前端攔截導 ` /change-password` 且阻擋其他路由直至 `PUT /api/account/password` 改為非 `admin123`；後端對 `admin` 加 `mustChangePassword` 中間件，未改密前除改密與登出外其餘 `403`。
- **6. 開發/測試獨立種子**：新增 `seed-dev.sql`（`crops 68`、`sender/cases/users` 範例各 N 筆，含 `staff/viewer`）與 `seed-test.sql`（最小可用），`application-dev.yaml`/`application-test.yaml` 以 `spring.sql.init.data-locations=classpath:schema.sql,classpath:seed-dev.sql` 載入，`application-prod.yaml` 僅 `schema.sql`；文件化 `prod` 業務用與 `dev/test` 開發用差異。

**非目標**：不改 `crop_categories` 既有 9 筆、不回填已部署業務庫的 `crops`、不自動遷移舊 `未知` `id` 的外鍵（採 `TRUNCATE` 建議）。

## Capabilities

### New Capabilities
- `ops-backup`: 業務/開發種子分離與部署文件（prod 僅業務 Scheme）

### Modified Capabilities
- `reference-data-admin`: 移除 `crops` 預設、未知重排與排序契約
- `user-admin`: 單帳號部署與強制改密閘門

## Impact

- **後端**：`resources/schema.sql`（刪 crops 68 筆、重排 cities/methods/districts、去 `AUTOINCREMENT` 依賴）、`resources/seed-dev.sql`（新增）、`resources/seed-test.sql`（新增）、`config/DataInitializer.java`（分 profile 建 `admin` 單帳號、`dev/test` 額外建 `staff/viewer`）、`service/AuthService.java`（`mustChangePassword` 判定）、`controller/AuthController.java`（回傳旗標）、`config/SecurityConfig.java`（未改密阻擋）、`application-*.yaml`（`data-locations`）。
- **前端**：`stores/auth.ts` 與 `router/index.ts` 加 `mustChangePassword` 守衛與強制導頁、`views/ChangePasswordView.vue` 提示、`api/index.ts` 型別。
- **資料**：**BREAKING** 既有業務庫若已建 `crops` 需手動 `DELETE FROM crops` 或重建；`未知` `id` 變更需 `TRUNCATE` 或遷移腳本重寫外鍵，文件化建議空庫部署。
- **相容性**：`crops` 空導致舊測試 `cropId=36` 失效，需切 `seed-test.sql`；`admin123` 未改密前舊整合測試需先改密或使用 `dev` 種子帳號。
