## Context

`api-observability` 已於 Phase 0 完成 `RequestIdFilter`（`X-Request-Id` 生成/透傳）與 `GlobalExceptionHandler` 的統一錯誤形狀（`error.code/message/details` + `requestId`），參見 `openspec/specs/api-observability/spec.md` 與 `backend/src/main/java/com/d0w0b/phytotrack/config/RequestIdFilter.java`。現狀僅 console 輸出、無健康探測端點，LAN 部署（`docs/DEPLOY.md:5`）無法以監控系統curl 探活，也無法以滾動檔案長期追溯 `requestId`。本設計在不改變既有 API 契約的前提下補齊 Actuator 與 logback。

## Goals / Non-Goals

**Goals:**
- Actuator 僅暴露 `health`/`info`，匿名可探測，其餘端點預設關閉且不洩細節
- logback 同時輸出 console 與滾動檔案，檔案含 `requestId`（MDC），保留 30 天、總量 1GB、單檔 10MB
- `.gitignore` 忽略 `logs/*.log`，部署文件可指導監控與日誌查詢

**Non-Goals:**
- 不引入 Prometheus/metrics、distributed tracing 或 ELK 集中化
- 不改變既有錯誤形狀或新增業務 API
- 不為前端提供 Actuator 代理

## Decisions

- **Decision: 依賴 `spring-boot-starter-actuator`（3.x）**
  - Rationale: Boot 4 官方健康檢查，無需自建 `HealthController` 即可相容容器 `liveness/readiness` 探針（`management.endpoint.health.probes.enabled=true`）
  - Alternative: 自寫 `/api/health` 已存在為 AI 檢查（`GET /api/ai/health`），但 Actuator 的 `Component` 機制更利於後續擴充 `db`、`diskSpace` 等指示器；兩者並存不衝突
  - Config: `management.endpoints.web.exposure.include=health,info`，`management.endpoint.health.show-details=never`（預設不洩堆疊），`management.info.*` 空物件可回 200

- **Decision: Security 放行 `health`/`info`，其餘 Actuator 仍需認證/ADMIN**
  - Rationale: 監控探測需匿名 200，而 `env/beans` 等敏感端點不應匿名暴露（符合 `security-hardening` 已關閉 OSIV 的最小暴露原則）
  - Alternative: 全放行但以 `management.endpoints.web.exposure` 限制，仍需 SecurityChain 配合；選擇在 `SecurityConfig.java` 以 `requestMatchers("/actuator/health","/actuator/info").permitAll()` 明確放行，其餘 `anyRequest().authenticated()` 自然 401/403
  - Verification: `curl -sf http://localhost:8080/actuator/health` 200，`curl -sf http://localhost:8080/actuator/env` 401→403

- **Decision: `logback-spring.xml` 取代 `application.yaml` 的 `logging.*` 零散配置**
  - Rationale: `SizeAndTimeBasedRollingPolicy` 需複合滾動，僅 yaml 的 `logging.logback.rollingpolicy` 無法精細控制保留天數與總量；xml 可同時定義 `ConsoleAppender` 與 `RollingFileAppender`，pattern 含 `%X{requestId}` 與既有 `RequestIdFilter` 的 MDC 協作
  - Pattern: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{requestId}] %logger{36} - %msg%n`
  - Alternative: 僅用 `logging.file.name` + `logging.logback.rollingpolicy` 簡配，但無法同時控制 `maxHistory=30` 與 `totalSizeCap=1GB` 的組合；故選 xml
  - File: `backend/src/main/resources/logback-spring.xml`，`application.yaml` 僅保留 `logging.file.name=logs/phytotrack.log` 作為預設（可被 env 覆寫）

- **Decision: MDC 注入於既有 `RequestIdFilter`**
  - Rationale: `RequestIdFilter` 已為每請求生成 `requestId` 並寫入 response header；新增 `MDC.put("requestId", requestId)` 於 `finally` 中 `MDC.remove`，即達成日誌關聯而無需新 filter
  - Alternative: 以 `OncePerRequestFilter` + `HandlerInterceptor` 注入，但重複 filter 增加順序風險；直接擴充既有 filter 最小改動

## Risks / Trade-offs

- **風險：日誌檔案於容器內寫入 `logs/` 導致磁碟滿** → Mitigation: `totalSizeCap 1GB` + `maxHistory 30`，並於 `docs/DEPLOY.md` 提醒以 `cron` 或 `logrotate` 輔助，部署時將 `logs/` 掛載至宿主或外部卷
- **風險：Actuator 暴露被誤用為資訊洩漏** → Mitigation: 僅 `health/info` 放行，其他預設關閉；`show-details=never`；部署文件強調勿以 `management.endpoints.web.exposure.include=*`
- **風險：MDC 洩漏跨請求（線程池重用）** → Mitigation: `RequestIdFilter` 在 `finally` 區塊 `MDC.remove("requestId")`，並於 `Async` 場景（本專案無）以 `TaskDecorator` 傳遞（如後續引入）
- **Trade-off：logback xml vs yaml** → xml 可精細但增加維護面，權衡後以 xml 為主、yaml 僅留 `file.name` 供 env 覆寫，保持簡潔

## Migration Plan

1. 新增依賴與配置（`pom.xml`、`application.yaml`、`logback-spring.xml`、`RequestIdFilter`、`SecurityConfig`），本地 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 啟動後 `curl /actuator/health` 與 `tail logs/phytotrack.log | grep requestId` 驗證
2. 更新 `docs/DEPLOY.md` §監控與 `.gitignore` 的 `logs/`，`typst compile docs/manual.typ` 無需更動
3. 部署：重新 `mvn package` 後以 `java -jar` 啟動，監控系統改以 `/actuator/health` 為探針，原 `GET /api/ai/health` 仍保留供 AI 檢查；回滾僅需還原上述 5 檔並重啟

## Open Questions

- 是否需以 `env` 啟用 `management.endpoint.health.probes.enabled` 的 `liveness/readiness` 分組（`health/liveness`、`health/readiness`）供 K8s 使用？本專案為裸機 LAN 部署，暫留為可選，後續以 `application-prod.yaml` 增量即可，不影響本次 spec。
