# ADR-013：DeepSec 掃描發現處置（2026-09-04）

## 背景

DeepSec 靜態掃描（`deepsec.sarif`，已 gitignore，不進版控）回報 6 項：1 error（hardcoded secret）、4 warning（`permitAll`）、1 error（SQL concat）。經逐項核查，6 項皆為誤報或設計使然，**不需程式變更**，本 ADR 記錄判定依據以供後續掃描比對。

## 判定

### 1. `hardcoded_secret_high_entropy_assignment`（error）— 誤報，接受

- 位置：`config/JwtSecretValidator.java:19`，`DEV_DEFAULT_SECRET` 字面值。
- 查核：該常數僅用於 `DEV_DEFAULT_SECRET.equals(secret)` 比對（fail-fast 守衛，見 ADR-004），從未用於簽章；實際簽章密鑰來自 `app.jwt.secret`（非 dev 強制 `JWT_SECRET` env）。
- 處置：保留；掃描器無法區分「哨兵比對值」與「使用中密鑰」，列為已知誤報。

### 2. `insecure_config_spring_permit_all` ×4（warning）— 設計使然，接受

- 位置：`config/SecurityConfig.java:64,66,68,70`。
- 逐項：
  - `:64` 認證端點（`register/login/abandon-deactivate/check-username/check-email`）：公開為認證流程必要，且登入/註冊已有 rate limit（10/min，見 ADR-012）；可用性查詢僅回布林值。
  - `:66` `GET /api/ai/health`：落地頁模型狀態需匿名可訪，僅回布林值。
  - `:68` OpenAPI/Swagger：開發期文件；正式環境以部署配置限制存取（與程式碼無關）。
  - `:70` `/actuator/health|info`：`api-observability` 規格要求公開，且 `show-details: never`、`exposure=health,info`；其餘 `/actuator/**` 限 ADMIN。
- 處置：保留；皆有規格或 ADR 依據。

### 3. `sast_sql_concat_execute`（error）— 誤報，接受

- 位置：`repository/SenderRepository.java:24`，`search` 的 `@Query`。
- 查核：拼接的僅為靜態 JPQL 片段，使用者輸入經 `:q` 綁定參數（`@Param`）傳入，未拼接進 SQL 字串，無注入面。
- 處置：保留；如掃描器持續誤報，優先以掃描配置排除而非改寫已參數化的查詢。

## 驗證

- `git status` 不再列出 `*.sarif`（`.gitignore` 新增）。
- 後續掃描若新增 findings，比照本格式追加判定；誤報清單不應無故擴大。

## 後果

- 無程式變更；掃描紅燈不代表漏洞，審查以本 ADR 為準。
