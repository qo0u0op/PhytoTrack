package com.d0w0b.phytotrack.config;

import com.moandjiezana.toml.Toml;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * TOML 配置載入與首次自動生成
 *
 * 優先順序：TOML > application.yaml 預設
 * 首次啟動若無 TOML 則生成預設，含亂數 JWT 密鑰
 */
public class PhytotrackTomlEnvironmentPostProcessor implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

  private static final SecureRandom RANDOM = new SecureRandom ();

  @Override
  public void onApplicationEvent (ApplicationEnvironmentPreparedEvent event) {
    ConfigurableEnvironment environment = event.getEnvironment ();
    // 測試環境不走 XDG/可攜，沿用 application-test.yaml
    for (String p : environment.getActiveProfiles ()) {
      if ("test".equals (p)) return;
    }
    // 亦檢查 spring.profiles.active 屬性（可能尚未解析）
    String profiles = environment.getProperty ("spring.profiles.active", "");
    if (profiles.contains ("test")) return;

    Path configPath = BinaryPaths.configPath ();
    Path systemConfig = BinaryPaths.systemConfig ();
    boolean isWindows = BinaryPaths.isWindows ();
    boolean isProd = isProdProfile (environment);

    // Unix 需同時考慮系統級與家目錄（家目錄優先）
    Path primary = configPath;
    Path secondary = isWindows ? null : systemConfig;

    Map<String, Object> props = new HashMap<> ();

    // 若主配置不存在，嘗試生成
    if (!Files.exists (primary)) {
      // Unix 若系統級存在則不生成家目錄，直接載入系統級
      if (!isWindows && Files.exists (secondary)) {
        primary = secondary;
      } else {
        try {
          generateDefaultToml (primary, isProd);
          System.out.println ("[PhytoTrack] 首次啟動已生成配置：" + primary);
          System.out.println ("[PhytoTrack] 首次啟動已生成亂數密鑰，舊 token 失效請重新登入");
        } catch (IOException e) {
          System.err.println ("[PhytoTrack] 生成預設配置失敗：" + e.getMessage ());
        }
      }
    }

    // 載入主配置（若存在）
    if (Files.exists (primary)) {
      try {
        Map<String, Object> tomlProps = loadToml (primary);
        props.putAll (tomlProps);
      } catch (IOException e) {
        System.err.println ("[PhytoTrack] 載入 TOML 失敗：" + e.getMessage ());
      }
    }

    // Unix 系統級作為低優先（若主為家目錄且系統級存在，合併但家目錄優先）
    if (!isWindows && !primary.equals (secondary) && Files.exists (secondary)) {
      try {
        Map<String, Object> sysProps = loadToml (secondary);
        for (Map.Entry<String, Object> e : sysProps.entrySet ()) {
          props.putIfAbsent (e.getKey (), e.getValue ());
        }
      } catch (IOException e) {
        System.err.println ("[PhytoTrack] 載入系統 TOML 失敗：" + e.getMessage ());
      }
    }

    // 若 TOML 未指定 db/log 則以 BinaryPaths 預設補上
    ensurePathProperties (props, primary, isWindows);

    // 將 TOML 屬性以高優先加入環境
    if (!props.isEmpty ()) {
      environment.getPropertySources ().addFirst (new MapPropertySource ("phytotrackToml", props));
    }
  }

  private void generateDefaultToml (Path path, boolean isProd) throws IOException {
    Files.createDirectories (path.getParent ());
    String secret = generateSecret ();
    // 範例即預設，僅 secret 隨機；其餘註釋同 phytotrack.toml.example
    String content = """
        # PhytoTrack 配置手冊 — phytotrack.toml
        # 單檔離線手冊，無需跳轉外部文件；每鍵含 類型/預設/可選值
        # 無效值回落安全預設並印啟動警告
        #
        # ── 路徑契約 ──
        # Windows 可攜: 與 exe 同目錄的 config/phytotrack.toml（解壓即用，可攜優先）
        # Unix XDG: $XDG_CONFIG_HOME/phytotrack/phytotrack.toml（預設 ~/.config/phytotrack/phytotrack.toml）
        # AppImage: 與 AppImage 同目錄的 config/phytotrack.toml（APPIMAGE 存在時優先，不走 XDG）
        # 系統級: /etc/phytotrack/phytotrack.toml（低優先，家目錄覆蓋之）
        # 優先順序: APPIMAGE > isWindows > XDG；家目錄 > 系統級（後者 putIfAbsent）
        #
        # ── 首次生成與冪等 ──
        # 缺檔自動生成含亂數密鑰的預設檔與 data/logs 目錄，冪等不覆蓋既有檔
        # 刪檔重啟即重生成（舊 token 失效）；首次 console 印「已生成亂數密鑰，舊 token 失效請重新登入」
        #
        # ── 保存起點 ──
        #   cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml
        #   # 無需手寫即可得完整起點；改後重啟生效
        #
        # ── 索引 ──
        # → docs/DEPLOY.md#12 Binary 交付 / → docs/ARCHITECTURE.md#6 環境設定
        #

        # ── [server] ── 服務監聽，影響 BrowserOpener 與防火牆提示 → docs/DEPLOY.md#12
        [server]
        # 用途: HTTP 監聽埠 | 類型: integer | 預設: 8080 | 取值: 1-65535，例 9090 | 生效: 重啟後，前端需同埠或代理
        port = 8080

        # ── [app.jwt] ── JWT 簽章與時效 → SecurityConfig / JwtAuthenticationFilter
        [app.jwt]
        # 用途: JWT HS256 簽章密鑰 | 類型: string | 預設: 隨機 48 bytes Base64URL（首次生成） | 取值: ≥32 bytes，例 phytotrack-prod-... | 生效: 重啟後舊 token 失效；警告: 刪檔重生成即失效，僅此檔可配
        secret = "%s"
        # 用途: 未勾記住我的 token 時效 | 類型: integer (ms) | 預設: 3600000 (1 小時) | 取值: 600000-86400000 | 生效: 重啟後新登入生效
        expiration-ms = 3600000
        # 用途: 勾選記住我時的 token 時效 | 類型: integer (ms) | 預設: 604800000 (7 天) | 取值: 3600000-2592000000 | 生效: 重啟後新登入生效，登出保留 lastUsername
        remember-me-expiration-ms = 604800000

        # ── [app.bootstrap] ── 首次建庫帳號說明（不可配，僅註釋提醒） → docs/ARCHITECTURE.md
        [app.bootstrap]
        # 首次建庫時註冊帳號：dev/test 建 admin/staff/viewer 三帳，prod 僅 admin
        # 預設帳密由程式內建（admin/admin123、staff/staff123、viewer/viewer123），不在設定檔配置
        # 首次登入後請立即修改密碼
        # 帳號密碼不可在設定檔配置，僅此處以註釋提醒；若舊檔含 admin-username 等將相容讀取但建議移除

        # ── [ai] ── AI 供應商與推理參數 → AIService / ViewerFilter → docs/DEPLOY.md#3
        [ai]
        # 用途: AI 總開關 | 類型: boolean | 預設: true | 取值: true/false | 生效: 重啟後；false 時 /api/ai/* 回 404 且前端隱藏入口
        enabled = true
        # 用途: 供應商選擇 | 類型: string | 預設: local | 取值: local（本機 llama.cpp）/ external（OpenAI 相容） | 生效: 重啟後；external 需配 base-url/model/api-key
        provider = "local"
        # 用途: OpenAI 相容 base-url（不含 /v1） | 類型: string (url) | 預設: http://localhost:11435 | 取值: 例 https://api.openai.com/v1（external） | 生效: 重啟後；探測 GET /health
        base-url = "http://localhost:11435"
        # 用途: 模型名稱（需對應 llama-server /v1/models） | 類型: string | 預設: qwen_qwen3vl-8b-instruct-q4_k_m | 取值: 例 gpt-4o（external） | 生效: 重啟後
        model = "qwen_qwen3vl-8b-instruct-q4_k_m"
        # 用途: API 金鑰（local 僅占位） | 類型: string | 預設: llama-local-dummy-key | 取值: 例 sk-...（external）| 生效: 重啟後；亦可 env AI_API_KEY 覆蓋此值（env 優先）
        api-key = "llama-local-dummy-key"
        # 用途: 推理溫度 | 類型: float | 預設: 0.1 | 取值: 0.0-2.0，例 0.7 | 生效: 重啟後
        temperature = 0.1
        # 用途: 單次最大生成 tokens | 類型: integer | 預設: 4096 | 取值: 1-128000 | 生效: 重啟後
        max-tokens = 4096
        # 用途: 上下文窗口上限 | 類型: integer | 預設: 128000 | 取值: 4096-200000 | 生效: 重啟後
        max-context-tokens = 128000
        # 用途: 推理努力程度 | 類型: string | 預設: high | 取值: low/medium/high | 生效: 重啟後
        reasoning-effort = "high"
        # 外部範例（provider = "external" 時）：
        # base-url = "https://api.openai.com/v1"
        # model = "gpt-4o"
        # api-key = "sk-..."
        # OpenAI 相容額外標頭（選填，auto 時每次 UUID）：
        # ── [ai.headers] ── OpenAI 相容透傳標頭 → OpenAiHeaderCustomizer
        # [ai.headers]
        # 用途: Opencode 會話標頭 | 類型: string | 預設: auto（未配時自動補 auto） | 取值: auto/UUID，例 auto | 生效: 重啟後
        # x-opencode-session = "auto"
        # 用途: 自訂 User-Agent | 類型: string | 預設: PhytoTrack/1.0 | 取值: 任意字串 | 生效: 重啟後
        # User-Agent = "PhytoTrack/1.0"

        # ── [app.cors] ── CORS 白名單 → CorsConfig → ADR-012
        [app.cors]
        # 用途: 允許跨源白名單 | 類型: string (csv) | 預設: ""（dev→*、prod→拒絕） | 取值: 例 https://app.example.com,https://admin.example.com | 生效: 重啟後
        allowed-origins = ""

        # ── [app.rate-limit] ── 登入/註冊限流 → RateLimitFilter → ADR-012
        [app.rate-limit]
        # 用途: 限流總開關 | 類型: boolean | 預設: true（test→false） | 取值: true/false | 生效: 重啟後
        enabled = true
        # 用途: 每窗口最大請求 | 類型: integer | 預設: 10 | 取值: 1-1000 | 生效: 重啟後；超限回 429 + Retry-After
        requests-per-minute = 10
        # 用途: 窗口秒數 | 類型: integer | 預設: 60 | 取值: 1-3600 | 生效: 重啟後
        window-seconds = 60

        # ── [app.security-headers] ── 安全標頭 → SecurityHeadersFilter → ADR-012
        [app.security-headers]
        # 用途: 安全標頭開關 | 類型: boolean | 預設: false（prod→true） | 取值: true/false | 生效: 重啟後；注入 CSP/HSTS/nosniff/DENY
        enabled = false

        # ── [app.tray] ── 系統托盤常駐 → SystemTrayManager (dorkbox)
        [app.tray]
        # 用途: 系統托盤開關 | 類型: boolean | 預設: true | 取值: true/false | 生效: 重啟後；false 時僅 console，不進托盤
        enabled = true

        # ── [app.ui] ── 自動開瀏覽器 → BrowserOpener
        [app.ui]
        # 用途: 啟動後自動開瀏覽器 | 類型: boolean | 預設: true | 取值: true/false | 生效: 重啟後；SSH/無 DISPLAY 時自動跳過
        auto-open-browser = true
        # 用途: dev 模式自動開啟的 vite 位址 | 類型: string (url) | 預設: http://localhost:5173/ | 取值: 任意 http(s) URL | 生效: 重啟後；僅 dev profile 有效，prod 固定開同 port /
        # dev-frontend-url = "http://localhost:5173/"

        # ── [springdoc] ── OpenAPI / Swagger UI → springdoc
        [springdoc]
        # 用途: 暴露 /v3/api-docs | 類型: boolean | 預設: true | 取值: true/false | 生效: 重啟後
        api-docs-enabled = true
        # 用途: 暴露 /swagger-ui | 類型: boolean | 預設: true | 取值: true/false | 生效: 重啟後
        swagger-ui-enabled = true

        # ── 可選覆蓋：資料庫與日誌位置（預設由 BinaryPaths 依 OS 決定，不配即用預設） ──
        # 用途: SQLite 連線 URL | 類型: string | 預設: Windows: ./data/diagnoses.db / XDG: ~/.local/share/... | 取值: 例 jdbc:sqlite:./data/diagnoses.db | 生效: 重啟後
        # spring.datasource.url = "jdbc:sqlite:./data/diagnoses.db"
        # 用途: 日誌主檔路徑 | 類型: string | 預設: Windows: ./logs/phytotrack.log / XDG: ~/.local/state/... | 取值: 例 logs/phytotrack.log | 生效: 重啟後
        # logging.file.name = "logs/phytotrack.log"
        """.formatted (secret);
    Files.writeString (path, content, StandardCharsets.UTF_8);
    // 同步生成 data/logs 目錄（AppImage 可攜優先）
    Path data = BinaryPaths.isAppImage () ? BinaryPaths.appImageData ()
        : (BinaryPaths.isWindows () ? BinaryPaths.windowsData () : BinaryPaths.xdgData ());
    Path log = BinaryPaths.isAppImage () ? BinaryPaths.appImageLog ()
        : (BinaryPaths.isWindows () ? BinaryPaths.windowsLog () : BinaryPaths.xdgLog ());
    try {
      Files.createDirectories (data.getParent ());
      Files.createDirectories (log.getParent ());
    } catch (IOException ignored) {}
  }

  private String generateSecret () {
    byte[] bytes = new byte[48];
    RANDOM.nextBytes (bytes);
    return Base64.getUrlEncoder ().withoutPadding ().encodeToString (bytes);
  }

  private Map<String, Object> loadToml (Path path) throws IOException {
    Toml toml = new Toml ().read (path.toFile ());
    Map<String, Object> map = new HashMap<> ();
    // server
    Long port = toml.getLong ("server.port");
    if (port != null) map.put ("server.port", port);
    // app.jwt
    String secret = toml.getString ("app.jwt.secret");
    if (secret != null) map.put ("app.jwt.secret", secret);
    Long exp = toml.getLong ("app.jwt.expiration-ms");
    if (exp != null) map.put ("app.jwt.expiration-ms", exp);
    // app.bootstrap
    putIfNotNull (map, "app.bootstrap.admin-username", toml.getString ("app.bootstrap.admin-username"));
    putIfNotNull (map, "app.bootstrap.admin-password", toml.getString ("app.bootstrap.admin-password"));
    putIfNotNull (map, "app.bootstrap.staff-username", toml.getString ("app.bootstrap.staff-username"));
    putIfNotNull (map, "app.bootstrap.staff-password", toml.getString ("app.bootstrap.staff-password"));
    putIfNotNull (map, "app.bootstrap.viewer-username", toml.getString ("app.bootstrap.viewer-username"));
    putIfNotNull (map, "app.bootstrap.viewer-password", toml.getString ("app.bootstrap.viewer-password"));
    // ai
    Boolean aiEnabled = toml.getBoolean ("ai.enabled");
    if (aiEnabled != null) {
      map.put ("ai.enabled", aiEnabled);
      map.put ("app.ai.enabled", aiEnabled);
    }
    String provider = toml.getString ("ai.provider");
    if (provider != null) map.put ("app.ai.provider", provider);
    String baseUrl = toml.getString ("ai.base-url");
    if (baseUrl != null) {
      map.put ("AI_BASE_URL", baseUrl);
      map.put ("app.ai.health-url", baseUrl + "/health");
      map.put ("spring.ai.openai.base-url", baseUrl + "/v1");
    }
    String model = toml.getString ("ai.model");
    if (model != null) map.put ("spring.ai.openai.chat.options.model", model);
    String apiKey = toml.getString ("ai.api-key");
    if (apiKey != null) {
      map.put ("ai.api-key", apiKey);
      map.put ("spring.ai.openai.api-key", apiKey);
    }
    // ai 推理參數（可選）
    Double temp = toml.getDouble ("ai.temperature");
    if (temp != null) {
      map.put ("app.ai.temperature", temp);
      map.put ("spring.ai.openai.chat.options.temperature", temp);
    }
    Long maxTokens = toml.getLong ("ai.max-tokens");
    if (maxTokens != null) {
      map.put ("app.ai.max-tokens", maxTokens);
      map.put ("spring.ai.openai.chat.options.max-tokens", maxTokens);
    }
    Long maxCtx = toml.getLong ("ai.max-context-tokens");
    if (maxCtx != null) {
      map.put ("app.ai.max-context-tokens", maxCtx);
      map.put ("spring.ai.openai.chat.options.max-context-tokens", maxCtx);
    }
    String reasoning = toml.getString ("ai.reasoning-effort");
    if (reasoning != null) {
      map.put ("app.ai.reasoning-effort", reasoning);
      map.put ("spring.ai.openai.chat.options.reasoning-effort", reasoning);
    }
    // ai.headers.* -> ai.headers.* 透傳（OpenAI 相容通用，auto 時每次 UUID）
    try {
      com.moandjiezana.toml.Toml headersTable = toml.getTable ("ai.headers");
      if (headersTable != null) {
        for (Map.Entry<String, Object> e : headersTable.toMap ().entrySet ()) {
          if (e.getValue () != null) map.put ("ai.headers." + e.getKey (), String.valueOf (e.getValue ()));
        }
      }
    } catch (Exception ignored) {}
    // app.cors
    String cors = toml.getString ("app.cors.allowed-origins");
    if (cors != null) map.put ("app.cors.allowed-origins", cors);
    // app.rate-limit
    Boolean rlEnabled = toml.getBoolean ("app.rate-limit.enabled");
    if (rlEnabled != null) map.put ("app.rate-limit.enabled", rlEnabled);
    Long rpm = toml.getLong ("app.rate-limit.requests-per-minute");
    if (rpm != null) map.put ("app.rate-limit.requests-per-minute", rpm);
    Long ws = toml.getLong ("app.rate-limit.window-seconds");
    if (ws != null) map.put ("app.rate-limit.window-seconds", ws);
    // app.security-headers
    Boolean shEnabled = toml.getBoolean ("app.security-headers.enabled");
    if (shEnabled != null) map.put ("app.security-headers.enabled", shEnabled);
    // app.tray
    Boolean trayEnabled = toml.getBoolean ("app.tray.enabled");
    if (trayEnabled != null) map.put ("app.tray.enabled", trayEnabled);
    // app.ui
    Boolean autoOpen = toml.getBoolean ("app.ui.auto-open-browser");
    if (autoOpen != null) map.put ("app.ui.auto-open-browser", autoOpen);
    // springdoc
    Boolean apiDocs = toml.getBoolean ("springdoc.api-docs-enabled");
    if (apiDocs != null) map.put ("springdoc.api-docs.enabled", apiDocs);
    Boolean swagger = toml.getBoolean ("springdoc.swagger-ui-enabled");
    if (swagger != null) map.put ("springdoc.swagger-ui.enabled", swagger);
    // datasource / logging 允許 TOML 直接覆蓋
    String dsUrl = toml.getString ("spring.datasource.url");
    if (dsUrl != null) map.put ("spring.datasource.url", dsUrl);
    String logFile = toml.getString ("logging.file.name");
    if (logFile != null) map.put ("logging.file.name", logFile);
    return map;
  }

  private void putIfNotNull (Map<String, Object> map, String key, String value) {
    if (value != null) map.put (key, value);
  }

  private boolean isProdProfile (ConfigurableEnvironment environment) {
    for (String p : environment.getActiveProfiles ()) {
      if ("prod".equals (p)) return true;
    }
    String profiles = environment.getProperty ("spring.profiles.active", "");
    if (profiles != null && profiles.contains ("prod")) return true;
    String sys = System.getProperty ("spring.profiles.active", "");
    return sys.contains ("prod");
  }

  private void ensurePathProperties (Map<String, Object> props, Path configPath, boolean isWindows) {
    if (!props.containsKey ("spring.datasource.url")) {
      Path data = BinaryPaths.isAppImage () ? BinaryPaths.appImageData ()
          : (isWindows ? BinaryPaths.windowsData () : BinaryPaths.xdgData ());
      String url = "jdbc:sqlite:" + data.toString ().replace ("\\", "/");
      props.put ("spring.datasource.url", url);
      props.put ("phytotrack.datasource.url", url);
      System.setProperty ("phytotrack.datasource.url", url);
      System.setProperty ("spring.datasource.url", url);
    }
    if (!props.containsKey ("logging.file.name")) {
      Path log = BinaryPaths.isAppImage () ? BinaryPaths.appImageLog ()
          : (isWindows ? BinaryPaths.windowsLog () : BinaryPaths.xdgLog ());
      String logStr = log.toString ().replace ("\\", "/");
      props.put ("logging.file.name", logStr);
      props.put ("phytotrack.logging.file", logStr);
      System.setProperty ("phytotrack.logging.file", logStr);
      System.setProperty ("logging.file.name", logStr);
    }
  }
}
