## 1. 種子與未知重排

- [ ] 1.1 `resources/schema.sql` 刪 `INSERT INTO crops` 68 筆，將 `cities` `未知` 改 `id=1` 並後移餘 `2..23`，`methods` `未知` 改 `id=1` 後移 `2..4`，`districts` 每 `city_id` 內 `未知` `sort_order=1` 餘遞增，確認 `pest_categories` 以 `pest_category_code ASC` 使 `X00` 前置，驗證 `sqlite3 :memory: < schema.sql && SELECT * FROM cities WHERE city_id=1` 為 `未知` 且 `SELECT COUNT(*) FROM crops` 為 `0`
- [ ] 1.2 新增 `resources/seed-dev.sql`（68 `crops` + `senders/cases/users` 含 `staff/viewer` 範例）與 `resources/seed-test.sql`（68 `crops` + 最小 `staff` 與 1 案件），`application-prod.yaml` 僅 `schema.sql`，`application-dev.yaml`/`application-test.yaml` 改 `spring.sql.init.data-locations` 分別載對應 seed，驗證 `prod` 啟動無 `crops`、`dev` 有 68 且可 `staff` 登入

## 2. 單帳號與強制改密

- [ ] 2.1 `service/DataInitializer` 拆 `ProdDataInitializer(@Profile prod)` 僅建 `admin/admin123`，`DevDataInitializer(@Profile dev,test)` 建 `admin/staff/viewer`，`service/AuthService.login` 增 `mustChangePassword = BCrypt.matches(admin123)` 判定，`controller/AuthController` 回傳附旗標，驗證 `prod` 建庫僅 `admin` 且 `POST /api/auth/login` 回 `mustChangePassword=true`
- [ ] 2.2 新增 `config/MustChangePasswordFilter`（`OncePerRequestFilter`，`admin` 且未改密時除 `PUT /api/account/password, POST /api/auth/logout, GET /api/account` 外回 `403 MUST_CHANGE_PASSWORD`），`config/SecurityConfig` 掛載於 `JwtAuthenticationFilter` 後，`service/AccountService.changePassword` 後旗標消失，驗證未改密前 `GET /api/cases` 回 `403` 且改密後可呼叫
- [ ] 2.3 調整既有整合測試硬編碼 `cropId=36` 改動態 `findFirst`，`PhytoTrackIntegrationTest` 於 `test` profile 使用 `seed-test.sql` 的 `crops`，驗證 `mvn test -Dtest=PhytoTrackIntegrationTest` 全綠

## 3. 前端強制改密與空狀態

- [ ] 3.1 `frontend/src/stores/auth.ts` 增 `mustChangePassword`、`frontend/src/api/index.ts` 型別、`frontend/src/api/http.ts` 攔截 `403 MUST_CHANGE_PASSWORD` 導至 `/change-password`，`frontend/src/router/index.ts` 守衛阻擋非改密路由，`frontend/src/views/ChangePasswordView.vue` 強制提示，驗證 `admin/admin123` 登入後被導改密且未改密前路由被攔
- [ ] 3.2 `frontend/src/views/ReferenceDataAdminView.vue` 與 `CaseFormView.vue` 空 `crops` 空狀態（「請先建作物」）且 `pest_categories` 以 `code` 前置 `X00`，`cities/methods` 預設選 `id=1 未知`，驗證 `npm run dev` 以空庫見空狀態與 `未知` 為首

## 4. 驗證與回歸

- [ ] 4.1 撰寫 `BizSchemeTest`：`prod` 無預設 `crops` 且 `未知` 為首、`dev` 有 68、`admin123` 強制改密 `403`、改密後可業務，驗證 `mvn test -Dtest=BizSchemeTest` 全綠
- [ ] 4.2 執行 `cd backend && mvn test` 全回歸與 `cd frontend && npm run build`（含 `vue-tsc`），驗證 `CaseServiceTest` 等不受空 `crops` 影響
- [ ] 4.3 執行 `openspec validate --specs --changes --strict` 與 `openspec status --change biz-scheme-breaking-reset`，驗證無錯誤且四件製品皆 `done`，`logs/` 仍 gitignore
