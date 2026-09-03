## 1. 後端註冊檢查與可用性端點

- [x] 1.1 `UserRepository` 新增 `existsByUsername` 衍生查詢，驗證編譯通過且既有測試不受影響
- [x] 1.2 `AuthService.register` 新增非空信箱 `existsByEmailIgnoreCase` 檢查（重複拋 `EMAIL_TAKEN` 409），驗證 `mvn test -Dtest=AuthServiceTest` 通過且既有 `USERNAME_TAKEN` 行為不變
- [x] 1.3 `AuthController` 新增公開 `GET /api/auth/check-username`、`GET /api/auth/check-email`（僅回 `{ available }`，空值語意依 spec），`SecurityConfig` 放行兩路徑，驗證未登入呼叫兩端點回 200 且已用/未用回正確布林值

## 2. 前端註冊 inline 錯誤＋密碼可見＋信箱格式

- [x] 2.1 `api/index.ts` 新增 `checkUsername/checkEmail` 與共用 `EMAIL_PATTERN` 常數，`RegisterView.vue` 全欄位加 inline 錯誤顯示（帳號/信箱 debounce 可用性檢查、密碼不一致 watch 即時比對、信箱 regex），驗證各欄位錯誤皆顯示於欄位下方且無全域 alert
- [x] 2.2 `RegisterView.vue` 送出時將後端欄位錯誤（`USERNAME_TAKEN`/`EMAIL_TAKEN`/格式錯誤）映射到對應欄位（吞掉不拋給攔截器），未知錯誤維持攔截器行為、成功提示維持不變，驗證送出重複信箱時錯誤出現在信箱欄位下方且無全域 alert
- [x] 2.3 `RegisterView.vue` 密碼與確認密碼欄加眼睛 icon 切換顯示/隱藏（預設隱藏），驗證切換不影響已輸入值且 icon 同步

## 3. 使用者管理搜尋與帳號 dirty 儲存鈕

- [x] 3.1 `UsersView.vue` 新增搜尋框（帳號/顯示名稱/信箱前端模糊比對）與「顯示已停用」勾選框（預設不勾＝僅列啟用者，疊加於分頁前），驗證預設隱藏停用者、勾選與搜尋疊加正確
- [x] 3.2 `AccountView.vue` 共用 `EMAIL_PATTERN`（取代內聯 regex），`canSave()` 改 dirty 判斷（任一欄位與載入值不同即顯示儲存鈕），驗證僅改顯示名稱即顯示、未修改即隱藏，且送出前驗證不變

## 4. 簽名人 UI（`reference-data-admin`）

- [x] 4.1 實作 `SignersView.vue` 三項（篩選列收進預設隱藏的 collapse 抽屜、欄序改帳號→身分別、狀態徽章改 `text-bg-success`/`text-bg-secondary` 啟用/停用），驗證抽屜開合、欄序、徽章樣式與既有篩選/分頁行為不變

## 5. 驗證與回歸

- [x] 5.1 撰寫 `RegisterAvailabilityTest`（註冊信箱重複 409、空信箱通過、兩可用性端點四場景），驗證 `mvn test -Dtest=RegisterAvailabilityTest` 全綠
- [x] 5.2 執行 `cd backend && mvn test` 全回歸與 `cd frontend && npm run build`（含 `vue-tsc`），驗證既有註冊/登入測試與建置通過
- [x] 5.3 執行 `openspec validate --specs --changes --strict` 與文件同步（`docs/ARCHITECTURE.md` API 一覽若列註冊契約則補 `EMAIL_TAKEN` 與可用性端點；`docs/manual.typ` 若涉使用者清單/簽名人操作則同步），驗證無錯誤
