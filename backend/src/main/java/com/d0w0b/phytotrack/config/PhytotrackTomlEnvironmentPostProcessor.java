package com.d0w0b.phytotrack.config;

import com.moandjiezana.toml.Toml;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
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
 * 優先順序：env AI_API_KEY > TOML > application.yaml 預設
 * 首次啟動若無 TOML 則生成預設，含亂數 JWT 密鑰
 */
public class PhytotrackTomlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  private static final SecureRandom RANDOM = new SecureRandom ();

  @Override
  public void postProcessEnvironment (ConfigurableEnvironment environment, SpringApplication application) {
    // 測試環境不走 XDG/可攜，沿用 application-test.yaml
    for (String p : environment.getActiveProfiles ()) {
      if ("test".equals (p)) return;
    }
    // 亦檢查 spring.profiles.active 屬性（可能尚未解析）
    String profiles = environment.getProperty ("spring.profiles.active", "");
    if (profiles.contains ("test")) return;

    // .env 已棄用：若仍存在則 WARN（僅 AI_API_KEY 仍由 env 覆蓋）
    Path legacyEnv = Path.of ("backend", ".env");
    if (!Files.exists (legacyEnv)) legacyEnv = Path.of (".env");
    if (Files.exists (legacyEnv)) {
      System.err.println ("[PhytoTrack] WARN backend/.env 已棄用，請遷至 phytotrack.toml（僅 AI_API_KEY 仍支援 env 覆蓋）");
    }

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
        // AI_API_KEY 敏感：env 覆蓋 TOML
        String envKey = System.getenv ("AI_API_KEY");
        if (envKey != null && !envKey.isBlank ()) {
          tomlProps.put ("ai.api-key", envKey);
          tomlProps.put ("spring.ai.openai.api-key", envKey);
        }
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
    // prod 僅 admin，dev/test 含三帳號；prod 的 bootstrap 僅 admin/admin123 且後續不回落
    String bootstrapSection = isProd ? """
        [app.bootstrap]
        admin-username = "admin"
        admin-password = "admin123"
        """ : """
        [app.bootstrap]
        admin-username = "admin"
        admin-password = "admin123"
        staff-username = "staff"
        staff-password = "staff123"
        viewer-username = "viewer"
        viewer-password = "viewer123"
        """;
    String content = """
        # PhytoTrack 配置（phytotrack.toml）
        # Windows 可攜：與 exe 同目錄的 config/phytotrack.toml
        # Unix：$XDG_CONFIG_HOME/phytotrack/phytotrack.toml（預設 ~/.config/phytotrack/phytotrack.toml）
        # 僅 AI_API_KEY 建議以 env AI_API_KEY 覆蓋，其餘皆走此檔
        # prod 僅 admin 生效（staff/viewer 即使配置亦忽略，首次 admin/admin123 後不回落）

        [server]
        port = 8080

        [app.jwt]
        secret = "%s"
        expiration-ms = 3600000

        %s
        [ai]
        enabled = true
        base-url = "http://localhost:11435"
        model = "qwen_qwen2.5-coder-3b-instruct-q8_0"
        api-key = "llama-local-dummy-key"

        [app.cors]
        allowed-origins = ""

        [app.rate-limit]
        enabled = true
        requests-per-minute = 10
        window-seconds = 60

        [app.security-headers]
        enabled = false

        [app.ui]
        auto-open-browser = true

        [springdoc]
        api-docs-enabled = true
        swagger-ui-enabled = true
        """.formatted (secret, bootstrapSection);
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
    String env = System.getenv ("SPRING_PROFILES_ACTIVE");
    if (env != null && env.contains ("prod")) return true;
    String sys = System.getProperty ("spring.profiles.active", "");
    return sys.contains ("prod");
  }

  private void ensurePathProperties (Map<String, Object> props, Path configPath, boolean isWindows) {
    // 若 TOML 未指定 db/log，則以 BinaryPaths 預設補上
    // 直接寫入 props 並以 MapPropertySource 高優先加入，確保 application.yaml 的佔位符前已解析
    if (!props.containsKey ("spring.datasource.url")) {
      Path data = isWindows ? BinaryPaths.windowsData () : BinaryPaths.xdgData ();
      if (BinaryPaths.isAppImage ()) data = BinaryPaths.appImageData ();
      String url = "jdbc:sqlite:" + data.toString ().replace ("\\", "/");
      props.put ("spring.datasource.url", url);
      props.put ("phytotrack.datasource.url", url);
      System.setProperty ("phytotrack.datasource.url", url);
      System.setProperty ("spring.datasource.url", url);
    }
    if (!props.containsKey ("logging.file.name")) {
      Path log = isWindows ? BinaryPaths.windowsLog () : BinaryPaths.xdgLog ();
      if (BinaryPaths.isAppImage ()) log = BinaryPaths.appImageLog ();
      String logStr = log.toString ().replace ("\\", "/");
      props.put ("logging.file.name", logStr);
      props.put ("phytotrack.logging.file", logStr);
      System.setProperty ("phytotrack.logging.file", logStr);
      System.setProperty ("logging.file.name", logStr);
    }
  }
}
