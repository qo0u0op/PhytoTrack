# Deprecated Cleanup Specification

## Purpose
系統性清理已標棄用或僅為一次性遷移保留的相容層，避免棄用碼長期殘留造成維護負擔與誤用。此規格承接前期 `case-lifecycle` 等遷移後遺留的 6 項棄用設計，確保 Boot 4 基準下無 `spring.factories`、無舊 Token 回落、無雙參數別名等歷史包袱。

## Requirements

### Requirement: 移除 Spring Boot 2 相容檔

系統 SHALL 僅以 `META-INF/spring/org.springframework.context.ApplicationListener.imports` 註冊 `PhytotrackTomlEnvironmentPostProcessor`，`META-INF/spring.factories` SHALL 刪除；`validate` 與啟動日誌 SHALL 不再引用舊檔。

#### Scenario: 僅保留 imports 檔
- **WHEN** 檢視 `backend/src/main/resources/META-INF/spring/`
- **THEN** 僅存在 `org.springframework.context.ApplicationListener.imports` 含 `PhytotrackTomlEnvironmentPostProcessor`，無 `spring.factories`

#### Scenario: 啟動不依賴舊檔
- **WHEN** 以 `mvn spring-boot:run` 啟動
- **THEN** `PhytotrackTomlEnvironmentPostProcessor` 仍由 `imports` 載入，啟動成功且無 `spring.factories` 警告

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
