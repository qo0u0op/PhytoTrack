## 1. 依賴與配置

- [x] 1.1 於 `backend/pom.xml` 新增 `spring-boot-starter-actuator` 依賴並驗證 `mvn dependency:tree | grep actuator` 可見
- [x] 1.2 於 `backend/src/main/resources/application.yaml` 新增 `management.endpoints.web.exposure.include: health,info`、`management.endpoint.health.show-details: never`、`management.endpoint.health.probes.enabled: true`，並驗證 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 啟動不報錯

## 2. Actuator 健康檢查

- [x] 2.1 於 `SecurityConfig.java` 放行 `GET /actuator/health` 與 `GET /actuator/info` (匿名 `permitAll ()`)，其餘 Actuator 預設由 `anyRequest ().authenticated ()` 拒絕，驗證 `curl -sf http://localhost:8080/actuator/health | grep UP` 為 200 且 `curl -sf http://localhost:8080/actuator/env` 為 401/404
- [x] 2.2 驗證 `GET /actuator/info` 匿名可訪 (200，空物件或 `{}`) 且不洩漏版本細節 (`showDetails=never` 生效)

## 3. logback 滾動日誌

- [x] 3.1 新增 `backend/src/main/resources/logback-spring.xml`，定義 `ConsoleAppender` 與 `RollingFileAppender` (`file: logs/phytotrack.log`、`SizeAndTimeBasedRollingPolicy`：`logs/phytotrack.%d{yyyy-MM-dd}.%i.log`、`maxFileSize 10MB`、`maxHistory 30`、`totalSizeCap 1GB`)，pattern 含 `[%X{requestId}]`，驗證 `mvn test` 日誌行含 `[requestId]`
- [x] 3.2 於 `RequestIdFilter.java` 以 `MDC.put ("requestId", requestId)` 注入並於 `finally` 中 `MDC.remove ("requestId")`，驗證 `curl -H "X-Request-Id: e2e-test-123" http://localhost:8080/api/cases` 後 `grep e2e-test-123 logs/phytotrack.log` 可關聯
- [x] 3.3 於 `application.yaml` 補 `logging.file.name: logs/phytotrack.log` (可被 env 覆寫) 並驗證 `logs/phytotrack.log` 同時於 console 與檔案輸出

## 4. 忽略與文件

- [x] 4.1 於 `.gitignore` 新增 `logs/` (若尚未忽略 `*.log`) 並驗證 `git status` 不顯示 `logs/` 下的 `*.log`
- [x] 4.2 於 `docs/DEPLOY.md` 新增 §監控 (Actuator `/actuator/health` 探測與 `logs/phytotrack.log` 滾動規則、`grep requestId` 查詢範例) 並驗證文件可讀

## 5. 驗證

- [x] 5.1 後端全量驗證：`cd backend && mvn test` 通過，且 `curl -sf http://localhost:8080/actuator/health` 與 `curl -sf http://localhost:8080/actuator/info` 於 dev 啟動後皆 200
- [x] 5.2 文件與配置驗證：`openspec validate --specs --changes` 通過，`cat logs/phytotrack.log | grep requestId` 可見 `requestId` 欄位
