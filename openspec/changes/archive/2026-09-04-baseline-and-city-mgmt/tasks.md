## 1. 初始 schema 與種子整理

- [x] 1.1 `resources/schema.sql` 刪除 68 筆作物種子列與重複未知鄉鎮（假設 `(3,未知,2)`），驗證新庫 `crops` 為空且每縣市僅一未知
- [x] 1.2 新增測試／開發用初始 schema 檔並切換 `application-test.yaml`（與開發指引），驗證新庫含參照種子、無業務資料
- [x] 1.3 依賴種子作物的測試改自建作物（`firstCropId` 等 helper），驗證 `mvn test` 相關測試通過

## 2. 縣市鄉鎮管理

- [x] 2.1 後端新增縣市鄉鎮管理端點（`POST/PUT/DELETE`，`ADMIN`，引用保護 `409`），驗證刪除被引用回 `409`、正常增改 `200/201`
- [x] 2.2 `ReferenceDataAdminView.vue` 新增縣市、鄉鎮兩分頁籤（含縣市聯動）並依新順序重排全部分頁籤，驗證增改刪與排序如 spec
- [x] 2.3 以 `openapi-typescript` 重生 `frontend/src/types/api.ts`，驗證 `npm run build`（含 `vue-tsc`）通過

## 3. 文件與資料庫重建

- [x] 3.1 `docs/DEPLOY.md`（初始 schema 與重建說明）與 `docs/manual.typ`（縣市鄉鎮管理操作）同步，驗證文件一致
- [x] 3.2 刪除重建測試／開發資料庫，驗證建庫成功且參照齊備
- [x] 3.3 執行 `cd backend && mvn test` 全回歸與 `openspec validate --specs --changes --strict`，驗證無錯誤
