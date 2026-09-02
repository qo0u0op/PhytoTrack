## 1. Actuator 與日誌

- [x] 1.1 引入 `spring-boot-starter-actuator`，於 `application.yaml` 設定 `management.endpoints.web.exposure.include=health,info,metrics` 與 `health` 細節，於 `SecurityConfig` 限制非 dev ` /actuator/**` 僅 ADMIN/內網，驗證 `GET /actuator/health` 回 `UP` 且含 `db`
- [x] 1.2 新增 `logback-spring.xml` 以 `RollingFileAppender` 實現 `logs/phytotrack-%d{yyyy-MM-dd}.%i.log.gz`（`maxFileSize 10MB`、`maxHistory 30`、`totalSizeCap 500MB`），pattern 含 `requestId`，驗證跨日/大小滾動與壓縮

## 2. 文件與驗證

- [x] 2.1 更新 `docs/ARCHITECTURE.md` 與 `docs/DEPLOY.md` 補監控與日誌章節，執行 `openspec validate --specs --changes` 與 `mvn test` 通過
