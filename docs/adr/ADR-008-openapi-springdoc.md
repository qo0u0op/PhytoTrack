# ADR-008: OpenAPI (springdoc) 作為 API 規格單一來源

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

前後分離後 (ADR-001)，前端需要知道每個 API 的請求/回應形狀。若用手寫文件或 Postman 集合維護，一定會與程式碼脫節 (過期)。

**選項**:

1. **springdoc-openapi**：從 Controller + Validation annotation 自動產生 OpenAPI 3 規格
2. 手寫 OpenAPI YAML / Swagger 註解
3. 用 Postman 集合當協定來源
4. GraphQL (schema 即契約)

**決策**:

- 加入 `springdoc-openapi` (**3.1.0+**，Boot 4 相容版本，見下方相容性)，產生 `/v3/api-docs`
- **給人看**：Swagger UI / Scalar 圖形介面，可試打 API
- **給程式看**：前端用 `openapi-typescript` 把規格轉成 TS 型別，API 改了前端型別跟著變
- 配合 ADR-005 的 DTO record 與 Validation，OpenAPI 文件自然帶出參數約束

**原因**:

- **單一來源**：規格從程式碼生成，文件永不與實作脫節
- **一次投入、多處受益**：人看的 UI、前端 TS 型別、測試、對外說明都來自同一份規格
- 展示「API-first / contract-first」思維——比只會寫 CRUD 高一階

**相容性 (重要)**:

Spring Boot 4 需要 **springdoc 3.x 的 4.x 分支** (springdoc 3.0 在 Boot 4.0 有已知問題，GitHub #3157 已修)。實作時鎖定 `springdoc-openapi-starter-webmvc-ui` 最新版並實測 `/v3/api-docs` 與 Swagger UI。

**取捨**:

- 註解量增加 (`@Operation`、`@Tag` 等)，需維護
- 規格由程式生成，複雜的「語意合約」 (如狀態機) 仍需手寫說明
- 不用 GraphQL：本專案 API 數量少、資源導向單一，REST + OpenAPI 足以，GraphQL 的彈性是這裡用不到的複雜度
