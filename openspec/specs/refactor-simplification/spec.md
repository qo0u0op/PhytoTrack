# Refactor Simplification Specification

## Purpose
針對 5 人 LAN 單機情境，對過度設計的桌面整合、配置載入、簽名人併發、路由授權等進行簡化重構，降低重複防禦碼與維護成本，同時保持對外行為不變。此規格承接 8 項過度設計的精簡，目標為單一職責、可測試與可讀性提升。

## Requirements

### Requirement: ViewerFilter 正規化

`ViewerFilter` 空殼 SHALL 移除或實作真遮蔽：若 `AnalyzeRequest` 不含個資則刪除類別並於 `AIService.analyze` 註解說明「當前不含個資」；若需隔離則實作 `name/phone/address/displayName→***` 並附單測。

#### Scenario: 移除空殼
- **WHEN** 檢視 `util/ViewerFilter.java`
- **THEN** 檔案不存在，`AIService` 無 `ViewerFilter.filterForViewer` 呼叫，僅註解說明

#### Scenario: 或實作真遮蔽
- **WHEN** 選擇保留
- **THEN** `filterForViewer` 對含個資的 `AnalyzeRequest` 回傳遮蔽後副本，並有 `ViewerFilterTest` 驗證 `***`

### Requirement: OpenAiHeaderCustomizer 改 ConfigurationProperties

`OpenAiHeaderCustomizer` SHALL 改以 `@ConfigurationProperties(prefix="ai.headers") Map<String,String>` 注入，移除 `Binder` + `env.getProperty` 雙路徑；`opencode.ai` 硬編 SHALL 改為通用透傳（任意 `ai.headers.*` 皆透傳），`x-opencode-session=auto` 的 UUID 生成集中一處。

#### Scenario: 注入簡化
- **WHEN** 檢視 `OpenAiHeaderCustomizer.java`
- **THEN** 構造子注入 `AiHeadersProperties`，無 `Binder.get(env)` 與 `env.getProperty("ai.headers...")`

#### Scenario: 透傳仍生效
- **WHEN** `phytotrack.toml` 設 `ai.headers.x-opencode-session=auto`
- **THEN** 每次 `POST /v1/chat/completions` 帶 `x-opencode-session: UUID`

### Requirement: TOML 載入器拆分

`PhytotrackTomlEnvironmentPostProcessor` SHALL 拆為 `TomlGenerator`（範本生成）與 `TomlLoader`（讀取與 `environment.getPropertySources().addFirst`），範本 SHALL 讀 `phytotrack.toml.example` 而非內嵌 120 行字串；`ensurePathProperties` 與 `BinaryPaths` 重複邏輯收斂。

#### Scenario: 職責分離
- **WHEN** 檢視 `config/`
- **THEN** 存在 `TomlGenerator.java` 與 `TomlLoader.java`，`PostProcessor` 僅協調

#### Scenario: 範本同源
- **WHEN** 比較 `phytotrack.toml.example` 與首次生成檔
- **THEN** 節順序與註釋一致（僅 `secret` 隨機不同）

### Requirement: 桌面環境判斷收斂

SSH/DISPLAY/WAYLAND/headless 判斷 SHALL 收斂至 `util/DesktopEnvironment`（`isHeadlessOrSsh()`、`isWindows()`），`BrowserOpener`/`SystemTrayManager`/`FirewallAdvisor` SHALL 共用；`BinaryPaths` 的 8 方法 SHALL 收斂為 `PlatformPaths` 策略（`WindowsPaths`/`XdgPaths`/`AppImagePaths`）。

#### Scenario: 共用判斷
- **WHEN** `grep -rn SSH_CONNECTION backend/src/main/java`
- **THEN** 僅 `DesktopEnvironment.java` 含該字串

#### Scenario: 路徑策略
- **WHEN** 檢視 `BinaryPaths.java`
- **THEN** 僅暴露 `configPath()/dataPath()/logPath()`，內部委派策略

### Requirement: 系統匣簡化

`SystemTrayManager` SHALL 移除 PowerShell Toast/`notify-send` 雙通知與 Swing fallback 的重複，失敗僅 `log.info`；圖示解析 SHALL 僅嘗試 `classpath:/tray-icon.png` + `exeDir/app/icon.png` 兩路徑。

#### Scenario: 通知簡化
- **WHEN** 檢視 `SystemTrayManager.showNotification`
- **THEN** 僅 `log.info("[通知] {}: {}", title, text)`，無 `ProcessBuilder("notify-send")` 與 `powershell`

#### Scenario: 圖示簡化
- **WHEN** 檢視 `resolveIconFile`
- **THEN** 僅兩路徑嘗試，無 4 路徑瀑布

### Requirement: 簽名人併發改約束

`IdentifierService.ensureForUser` SHALL 移除 `synchronized(("signer:"+normalize).intern())`，改以 DB 唯一約束 `UNIQUE(normalized_identifier, user_id)` 或 `@Transactional(isolation=SERIALIZABLE)` + `ON CONFLICT` 重試；全表掃 `findByUserIsNullAndActiveTrue().stream().anyMatch` SHALL 改為 `existsByNormalizedIdentifier` 查詢。

#### Scenario: 無 intern 鎖
- **WHEN** `grep -rn "\.intern()" backend`
- **THEN** 無匹配

#### Scenario: 併發仍安全
- **WHEN** 2 執行緒同時 `ensureForUser` 同名使用者
- **THEN** 僅一筆成功，另一回 `409 DISPLAY_NAME_EXISTS`，無重複簽名人

### Requirement: SPA 授權精簡

`SecurityConfig` SHALL 僅 `permitAll` `"/", "/index.html", "/assets/**", "/api/auth/**", "/api/ai/health", "/v3/api-docs/**", "/swagger-ui/**", "/actuator/health", "/actuator/info"`，前端路由 `/login,/register,/dashboard,/cases/**,/users,/admin/**,/account` SHALL 由 `SpaConfig` 回退 `index.html` 與 `router.beforeEach` 守衛，不在後端枚舉。

#### Scenario: 後端不枚舉 SPA
- **WHEN** 檢視 `SecurityConfig.authorizeHttpRequests`
- **THEN** 無 `"/login", "/register", "/dashboard", "/cases"` 等 SPA 字串

#### Scenario: 前端仍可直連
- **WHEN** 未登入直接訪問 `/cases/123`
- **THEN** 後端回 `index.html`，前端守衛導至 `/login?redirect=/cases/123`

### Requirement: 限流一致性

`RateLimitFilter` 的 `Retry-After` SHALL 取 `RateLimitService.windowSeconds` 而非寫死 `60`；`RateLimitService` 的 `maximumSize` SHALL 依 `persistence` 的 5 人場景調整為 `100`，並以固定視窗 `Bandwidth.simple(requestsPerMinute, Duration.ofSeconds(windowSeconds))` 語意一致。

#### Scenario: Retry-After 一致
- **WHEN** `phytotrack.toml` 設 `requests-per-minute=5, window-seconds=30` 超限
- **THEN** 回 `429` 且 `Retry-After: 30`

#### Scenario: 快取大小適中
- **WHEN** 檢視 `RateLimitService`
- **THEN** `maximumSize(100)` 且 `expireAfterWrite(windowSeconds+5)`

### Requirement: 主題儲存簡化

`frontend/src/stores/theme.ts` SHALL 以單一 `watchEffect` 同步 `data-bs-theme` 與 `localStorage`，移除 `watch(theme)`+`watch(effectiveTheme)` 雙監聽；`addListener` 相容分支 SHALL 保留但以 `media.addEventListener?.('change', handler)` 統一。

#### Scenario: 單一同步
- **WHEN** 檢視 `theme.ts`
- **THEN** 僅一個 `watch`/`watchEffect` 負責 `applyTheme` 與持久化

### Requirement: Schema 單一化

`schema-baseline.sql` SHALL 合併至 `schema.sql` + `data.sql`（或 Flyway `V1__baseline`），兩檔重複的城市/病蟲害種子（~400 行）SHALL 僅存一份，`crops` 空表由註解說明。

#### Scenario: 單一真相
- **WHEN** `grep -c "INSERT OR IGNORE INTO cities" backend/src/main/resources/*.sql`
- **THEN** 僅 `schema.sql` 含該插入，`schema-baseline.sql` 不存在或僅為代理
