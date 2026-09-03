## Context

見 `proposal.md - Why`：`signer-overhaul` 後簽名人已分流，但 `schema.sql` 仍為混合種子（68 crops、`未知` 末位、3 帳號），致部署業務庫需手工清理且 `未知` 預設不一致。`open-in-view: false`、`ddl-auto: update` 與 `DataInitializer(dev/test)` 現狀決定遷移與 Profile 策略。

## Goals / Non-Goals

**Goals:**
- 業務 Scheme（`prod`）僅 `crop_categories` 與重排後 `未知` 為 1，`crops` 空
- 開發/測試 Scheme 含 68 crops 與範例 sender/cases/users，可即登
- `admin/admin123` 單帳號部署且強制改密閘門，未改密前阻擋業務

**Non-Goals:**
- 已部署業務庫自動回填 `crops`、舊 `未知` id 外鍵自動重寫、自動 `admin123` 輪替

## Decisions

### D1 種子分離：`schema.sql` 業務 + `seed-dev.sql`/`seed-test.sql`
- **選擇**：`schema.sql` 刪 `INSERT INTO crops` 68 筆，保留 `crop_categories` 9 筆；新建 `seed-dev.sql`（68 crops + `INSERT INTO senders/cases/users` 含 `staff/viewer` 與範例案件）與 `seed-test.sql`（68 crops + 最小 `staff` 與 1 案件）；`application-prod.yaml` `spring.sql.init.data-locations: classpath:schema.sql`，`application-dev.yaml` 與 `application-test.yaml` 為 `classpath:schema.sql,classpath:seed-dev.sql` 與 `seed-test.sql`。`DataInitializer` 依 profile 分流：`prod` 僅 `admin`，`dev/test` 額外建 `staff/viewer`（見 D3）。
- **替代**：以 `DataInitializer` 動態建 crops，難審計且與 `schema.sql` 去重，故選靜態 seed 分離。

### D2 未知重排：`id=1` 與 `sort_order=1`
- **選擇**：`cities` `未知` 改 `(1,'未知',1)`，原 `1..22` 後移為 `2..23`；`methods` `未知` 改 `(1,'未知')`，餘 `2..4`；`districts` 每 `city_id` 內 `未知` 的 `sort_order` 改 `1`，餘 `2..N`（`INSERT` 順序同步）；`pest_categories` 已 `ORDER BY pest_category_code ASC` 使 `X00` 前於 `X01`，僅需種子 `INSERT` 順序與 `sortOrder` 文字段對齊，查詢維持 `Comparator.comparing(PestCategory::getPestCategoryCode)`。
- **替代**：以 `CASE WHEN city='未知' THEN 0 ELSE 1 END` 排序可不改 `id`，但前端預設取 `id=1` 語意直觀，故選改 `id` **BREAKING**。

### D3 單帳號與強制改密
- **選擇**：`DataInitializer` `@Profile({"dev","test"})` 仍建 `staff/viewer`，新增 `@Profile("prod") ProdDataInitializer` 僅建 `admin`；`AuthService.login` 後 `if (username.equals("admin") && passwordEncoder.matches("admin123", user.getPassword()))` 設 `mustChangePassword=true`，`AuthController` 回 `{ token, user, mustChangePassword }`（或 `403 MUST_CHANGE_PASSWORD`）；`SecurityConfig` 加 `MustChangePasswordFilter`（`OncePerRequestFilter`）對 `admin` 且 `mustChangePassword` 時除 `PUT /api/account/password, POST /api/auth/logout, GET /api/account` 外回 `403`，前端 `http.ts` 攔截 `403 MUST_CHANGE_PASSWORD` 導 `ChangePasswordView`，`router` 守衛同阻擋。
- **替代**：以前端單攔截可被繞過，故後端閘門必填；以 `403` 而非 `200+flag` 可使未改密前舊 token 立即失效，選 `200+flag` 兼 `403` 雙保險（前端導頁，後端阻擋）。

### D4 併發與遷移
- **選擇**：`crops` 空致舊測試 `cropId=36` 失效，切 `seed-test.sql` 後 `cropId` 從 `1` 起（`柑橘` 仍 `1`？重排後 `cities` 改動不影響 `crops` `id` 連續），測試以 `findFirst` 而非硬編碼 `36`。
- **替代**：保留 `crops` 硬 `id` 映射需對照表，改動大，故選動態查詢。

## Risks / Trade-offs

- [業務庫已有 crops] → `schema.sql` 去種子後舊庫仍有資料，建議 `TRUNCATE crops` 或重建，文件化 **BREAKING**
- [未知 id 變更致外鍵錯位] → `cases.cropId/cityId` 等舊 `id` 若以 `2` 指 `臺北市` 將改指 `未知`，必須空庫部署，遷移需腳本重寫外鍵，風險以文件與 `TRUNCATE` 建議緩解
- [admin123 強制改密繞過] → 前端阻擋可被 `curl` 繞過，後端 `MustChangePasswordFilter` 為強制，`admin` 未改密前業務全 `403`
- [dev/test 種子與 prod 分離致測試覆蓋差異] → `seed-test.sql` 需與 `seed-dev.sql` 子集一致，`migrate` 僅測業務空庫與開發滿庫兩路徑

## Migration Plan

1. **DB**：新部署直接 `prod` 空庫（`crops` 空）；既有業務庫執行 `DELETE FROM crops; DELETE FROM sqlite_sequence WHERE name='crops';` 與 `UPDATE cities SET city='未知' WHERE city_id=1` 等重排（提供 `scripts/migrate-biz-scheme.sql`），或重建庫
2. **後端**：`mvn test` 驗 `prod` 無 crops、`dev` 有 crops、`admin123` 403 閘門
3. **前端**：`npm run build` 驗 `mustChangePassword` 導頁與空作物空狀態
4. **Rollback**：`schema.sql` 回加 `crops` 68 筆、重排回末位、`DataInitializer` 回 3 帳號，移除 `MustChangePasswordFilter`

## Open Questions

- `未知` 重排後舊 `cases` 的 `cityId/cropId` 是否需提供一次性 `UPDATE` 腳本自動重映射，或僅建議空庫？本批僅文件化 `TRUNCATE` 建議，不自動重映射
