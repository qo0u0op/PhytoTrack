## Why

`explore` 審查（`fix-case-xss-sanitization` 後）指出後端無系統性過度抽象，但 `CaseService.java:86` 上帝物件（1280 行，含 CRUD/統計/CSV/送件人/junction/Inline）、`ReferenceDataService.java:204` 8 實體複製、`CaseDtos.java:80` 相容多載、`SecurityConfig.java:39` 生產為測試妥協的 `@Autowired(required=false)` 為 P1 過度/欠抽象，推高認知負荷與新增欄位成本。需在不破測試的前提下漸進簡化。

## What Changes

- **CaseService 去可選注入**：移除 `@Autowired(required=false)` 與 `this(..., null, null)` 相容建構，改以建構子必填 + 測試以 `@MockitoBean` / `@TestConfiguration` 供空 bean，刪除散落 6 處 `if (service != null)` 分支。
- **DTO 相容多載收斂**：刪除 `CaseDtos.CaseUpdateRequest` 19/20/22 參數多載與 `CaseService` 19 參數多載，同步更新 `CaseControllerTest` 等改用 canonical `record` 建構，單次 `sed` 取代永久債務。
- **ReferenceDataService 模板化**：抽 `AbstractRefService<T>` 或 `saveTrimmed/existsCheck` 共用，`create/update/delete` 24 方法縮為泛型模板，行為不變。
- **InputSanitizer 微簡化（可選）**：保留雙層防禦，僅刪 `fieldLabel` 多載或遷 `util/validation` 套件，屬低優先 Nit。
- **不做**：不拆 `CaseService` 為 4 服務（風險高，留待獨立 change）、不改 `GlobalExceptionHandler` 四映射、不改 `RateLimit`/`CORS` 策略。

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
<!-- 無 — 純重構，不改 spec 行為 -->

## Impact

- 後端：`service/CaseService.java:86`、`service/ReferenceDataService.java:204`、`dto/CaseDtos.java:80`、`config/SecurityConfig.java:39`、`service/InputSanitizer.java:10`
- 測試：`CaseControllerTest.java:134` 等多載呼叫點更新，`WebMvcTest` 改 `@MockitoBean RateLimitFilter`，`mvn test 148` 保持通過
- 風險：零行為變更，僅結構簡化；以小步提交 + 每步 `mvn test` 驗證
