## Context

`api-observability` 已完成統一錯誤與 `requestId` 日誌（Phase 1），`docs/REQUIREMENTS.md` 明記剩餘 Actuator 與 logback 滾動屬 Phase 2。現行無健康/指標端點且日誌為單檔無滾動，維運可觀測性不足。見 `proposal.md` Why。

## Goals / Non-Goals

**Goals:**
- 提供精簡 Actuator 端點供 LAN 內健康與指標查詢，兼顧安全（非 dev 限權）。
- 提供日滾動與大小滾動的 logback 策略，保留可追蹤性。

**Non-Goals:**
- 不引入分散式追蹤（Zipkin/Prometheus 外部依賴）與複雜告警。
- 不改既有錯誤契約與 `requestId` 生成邏輯。

## Decisions

- **Actuator**：引入 `spring-boot-starter-actuator`，暴露 `health,info,metrics`，`management.endpoints.web.exposure.include` 精簡，`health` 開啟 `db` 與自訂 `llama` 指標；`SecurityConfig` 限制非 dev ` /actuator/**` 需 `ROLE_ADMIN` 或內網 IP。替代：全開放暴露風險高；完全關閉則無監控。
- **Logback**：新增 `logback-spring.xml` 以 `RollingFileAppender` + `TimeBasedRollingPolicy` + `SizeAndTimeBasedFNATP` 實現日與大小雙重滾動，`fileNamePattern` 為 `logs/phytotrack-%d{yyyy-MM-dd}.%i.log.gz`，`maxFileSize 10MB`、`maxHistory 30`、`totalSizeCap 500MB`；`pattern` 納入 `%X{requestId}`。替代：僅依日滾動無法控單檔大小。
- **文件**：於 `docs/ARCHITECTURE.md` 與 `docs/DEPLOY.md` 補監控與日誌章節。

## Risks / Trade-offs

- [暴露風險] Actuator 敏感端點若誤曝光 → 僅白名單三端點並限權。
- [磁碟占用] 日誌總量上限 500MB，若量大仍可能滿 → 可於 `application.yaml` 調整並監控。

## Migration Plan

1. 增加依賴與 `logback-spring.xml`、`application.yaml` 管理端設定。
2. `SecurityConfig` 增加 `/actuator/**` 規則。
3. 補整合測試驗證 `health` 與日誌滾動設定，更新文件。

## Open Questions

- 無。
