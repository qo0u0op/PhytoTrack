## Why

`api-observability` 已交付統一錯誤與 `requestId` 日誌，但 Actuator 監控與 logback 滾動日誌仍列為 Phase 2（見 `docs/REQUIREMENTS.md` 排除項），缺乏健康檢查與日誌維運能力，難以於 LAN 內進行維運監控與問題追蹤。

## What Changes

- 啟用 Spring Boot Actuator 精簡端點：`health`（含 DB 與 llama 健康）、`info` 與 `metrics`（JVM/HTTP），非 dev 環境限內網與 ADMIN 角色，敏感端點不暴露。
- 導入 logback 滾動策略：依日與大小滾動（例 `logs/phytotrack-%d{yyyy-MM-dd}.%i.log.gz`，保留 30 日、單檔 10MB、總量 500MB），`requestId` 已於 Phase 1 寫入日誌，本次僅補滾動與格式。
- 補足文件與驗證：`docs/DEPLOY.md` 與 `docs/ARCHITECTURE.md` 補監控與日誌章節。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `api-observability`: 擴充 Actuator 與日誌滾動相關需求

## Impact

- 後端：`pom.xml` 增加 `spring-boot-starter-actuator`、`src/main/resources/logback-spring.xml`、`application.yaml` actuator 暴露與管理端安全設定。
- 文件：`docs/DEPLOY.md`/`ARCHITECTURE.md` 補監控與日誌維運說明。
- 測試：新增 Actuator 端點與日誌滾動的整合測試；不影響既有 API 行為。
