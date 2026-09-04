# 提案：api-observability Phase 2 (Actuator 與 logback 滾動日誌)

## Why

`api-observability` 於 Phase 0 已交付統一錯誤形狀 (`error.code/message/details` + `requestId`) 與 requestId 日誌串接 (`openspec/specs/api-observability/spec.md`)，但監控與維運所需的 **Actuator 健康檢查**與**滾動日誌** 仍列於 `docs/REQUIREMENTS.md:16` 為 Phase 2 排除項。隨著案件、使用者與送件人等 8 能力已全數交付並進入 LAN 部署，亟需可被外部監控探測的健康端點與可長期保留的結構化日誌，否則部署後無法以 `curl /actuator/health` 判斷存活，也無法以滾動檔案追溯 `requestId` 對應的錯誤。

## What Changes

- **Actuator 精簡暴露**：引入 `spring-boot-starter-actuator`，僅暴露 `health` (與既有 `GET /api/ai/health` 並存，前者供監控探測、後者供 AI 連線檢查) 與 `info`，其他端點預設關閉；`health` 需支援 `?showDetails=never` (預設不洩細節) 與 `management.endpoint.health.probes.enabled` 以相容容器探針。
- **logback 滾動日誌**：以 `logback-spring.xml` 取代預設 console-only，依 `logging.file.name` 與 `SizeAndTimeBasedRollingPolicy` 按日與 10MB 滾動，保留 30 天、總量 1GB 上限；日誌行包含 `requestId` (由 `RequestIdFilter` 以 MDC 注入) 與既有 `requestId` 回應欄位一致，便於關聯查詢。
- **文件與忽略**：`docs/DEPLOY.md` 補 Actuator 與日誌路徑說明；`logs/*.log` 加入 `.gitignore` (不進版控)。

**非目標**：不引入 Prometheus/metrics、trace 分散式追蹤或集中式日誌收集；僅完成本機可維運的最小可觀測性。

## Capabilities

### New Capabilities

<!-- 無新增能力，僅擴充既有 api-observability -->

### Modified Capabilities

- `api-observability`: 新增 Requirement「Actuator 健康檢查端點」與「logback 滾動日誌含 requestId」，補齊 Phase 2 剩餘項。

## Impact

- **後端**：`backend/pom.xml` 新增 `spring-boot-starter-actuator`；`backend/src/main/resources/application.yaml` 新增 `management.*` 與 `logging.*` 配置；新增 `logback-spring.xml`；`config/RequestIdFilter.java` 以 MDC 注入 `requestId` 供 logback pattern 使用；`config/SecurityConfig.java` 放行 `/actuator/health`、`/actuator/info` (匿名可訪，其餘需 ADMIN)。
- **文件**：`docs/DEPLOY.md` §監控、`docs/ARCHITECTURE.md` §可觀測性。
- **相容性**：既有錯誤形狀不變，僅日誌輸出由 console 增為 console+file；Actuator 端點為新增，無破壞性。
