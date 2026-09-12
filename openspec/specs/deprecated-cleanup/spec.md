# Deprecated Cleanup Specification

## Purpose
系統性清理已標棄用或僅為一次性遷移保留的相容層，避免棄用碼長期殘留造成維護負擔與誤用。此規格承接前期 `case-lifecycle` 等遷移後遺留的 6 項棄用設計，確保 Boot 4 基準下無 `spring.factories`、無舊 Token 回落、無雙參數別名等歷史包袱。

## Requirements

### Requirement: 保留 spring.factories 註冊檔（imports 單獨無效）

系統 SHALL 以 `META-INF/spring.factories`（`org.springframework.context.ApplicationListener=com.d0w0b.phytotrack.config.PhytotrackTomlEnvironmentPostProcessor`）註冊 `PhytotrackTomlEnvironmentPostProcessor`，`META-INF/spring/org.springframework.context.ApplicationListener.imports` 保留共存。實證：Boot 4.0.6 下僅頂層 `.imports` 不觸發 `ApplicationEnvironmentPreparedEvent` 監聽（`clean` 建置的 AppImage 出現 `bin/config` 未生成、`DB=./diagnoses.db` 扁平、`JwtSecretValidator` 因開發預設 `secret` 失敗），僅恢復 `spring.factories` 後 `任意目錄 + CWD/OWD 後備` 的可攜落點才正常。

#### Scenario: 雙檔共存
- **WHEN** 檢視 `backend/src/main/resources/META-INF/`
- **THEN** 同時存在 `spring.factories`（含 `PhytotrackTomlEnvironmentPostProcessor`）與 `spring/org.springframework.context.ApplicationListener.imports`

#### Scenario: clean 建置仍觸發監聽
- **WHEN** 以 `mvn clean package` 建置後於任意目錄（含 `*.AppImage` 的 CWD）執行
- **THEN** 啟動日誌含 `config 探測` 且生成 `./config/phytotrack.toml`（prod patch），不回落扁平 `./diagnoses.db`

### Requirement: 移除 SenderRepository 棄用方法

`SenderRepository.findByNameAndPhone(String,String)` SHALL 刪除（含 `@Deprecated` 標註）；全庫 `grep` SHALL 無該方法引用，編譯與測試通過。

#### Scenario: 方法已移除
- **WHEN** 檢視 `SenderRepository.java`
- **THEN** 無 `findByNameAndPhone` 宣告

#### Scenario: 無呼叫殘留
- **WHEN** 執行 `rg findByNameAndPhone backend`
- **THEN** 無匹配

### Requirement: 移除 JWT 舊 Token 回落

`JwtAuthenticationFilter` SHALL 移除 `userId==null` 時以 `subject` 回落 `findByUsername` 的相容分支與 `TODO: remove after migration` 註解；僅保留 `userId` 路徑，舊 token SHALL 直接視為無效（走 401）。

#### Scenario: 僅 userId 路徑
- **WHEN** 收到不含 `userId` 的舊 token
- **THEN** `parseToken` 後因 `userId==null` 不落 `findByUsername`，直接不寫入 `SecurityContext`，後續回 401

#### Scenario: 新 token 正常
- **WHEN** 以含 `userId` 的現行 token 呼叫 `GET /api/cases`
- **THEN** 依 `userId` 查庫並建 `UserPrincipal`，行為不變

### Requirement: 收斂 SenderAddressMigration

`SenderAddressMigration` SHALL 增加版本守衛或標 `deprecated` 並於一版後移除；預設新庫（`schema.sql` 已為 `address TEXT` 可空）SHALL 不執行重建事務，僅舊庫觸發。

#### Scenario: 新庫跳過遷移
- **WHEN** 啟動全新 `diagnoses.db`（`address` 已可空）
- **THEN** `migrateIfNeeded` 經 `PRAGMA table_info` 判斷 `notnull==0` 直接 return，無 `ALTER TABLE RENAME` 日誌

#### Scenario: 舊庫仍遷移
- **WHEN** 啟動仍為 `NOT NULL` 的歷史庫
- **THEN** 執行一次重建並印 `senders.address 遷移完成`

### Requirement: 棄用 senderName 別名

`GET /api/cases` 與 `GET /api/cases/export` 的 `senderName` SHALL 標為已棄用（OpenAPI `deprecated: true`，註解標 `@Deprecated`），文件僅保留 `senderQuery` 為主參數；後端仍相容一版但前端 SHALL 僅送 `senderQuery`。

#### Scenario: OpenAPI 標棄用
- **WHEN** 檢視 `/v3/api-docs` 的 `/api/cases` 參數
- **THEN** `senderName` 含 `deprecated: true`，`senderQuery` 為建議

#### Scenario: 前端僅送新參數
- **WHEN** 於 `CasesView.vue` 篩選送件人
- **THEN** 請求僅含 `senderQuery`，無 `senderName`

### Requirement: 清理 AIService 空分支

`AIService.isHealthy` 中 `models_discovered` 的空 `if` 塊 SHALL 刪除，健康檢查邏輯 SHALL 僅保留 `local: /health + models_loaded>0` 與 `external: /v1/models contains model` 兩分支。

#### Scenario: 空塊已刪
- **WHEN** 檢視 `AIService.java`
- **THEN** 無 `int idx2 = body.indexOf("models_discovered")` 空塊

#### Scenario: 健康檢查仍正確
- **WHEN** `isHealthy()` 於 `local` 收到 `{"status":"ok","models_loaded":0}`
- **THEN** 回 `false`
