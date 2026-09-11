package com.d0w0b.phytotrack.config;

import com.moandjiezana.toml.Toml;

import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * TOML 讀取器：將 phytotrack.toml 解析為 Spring Environment 屬性
 */
public final class TomlLoader {

  private TomlLoader () {}

  public static Map<String, Object> loadToml (Path path) throws IOException {
    Toml toml = new Toml ().read (path.toFile ());
    Map<String, Object> map = new HashMap<> ();
    Long port = toml.getLong ("server.port");
    if (port != null) map.put ("server.port", port);
    String secret = toml.getString ("app.jwt.secret");
    if (secret != null) map.put ("app.jwt.secret", secret);
    Long exp = toml.getLong ("app.jwt.expiration-ms");
    if (exp != null) map.put ("app.jwt.expiration-ms", exp);
    Long remember = toml.getLong ("app.jwt.remember-me-expiration-ms");
    if (remember != null) map.put ("app.jwt.remember-me-expiration-ms", remember);
    putIfNotNull (map, "app.bootstrap.admin-username", toml.getString ("app.bootstrap.admin-username"));
    putIfNotNull (map, "app.bootstrap.admin-password", toml.getString ("app.bootstrap.admin-password"));
    putIfNotNull (map, "app.bootstrap.staff-username", toml.getString ("app.bootstrap.staff-username"));
    putIfNotNull (map, "app.bootstrap.staff-password", toml.getString ("app.bootstrap.staff-password"));
    putIfNotNull (map, "app.bootstrap.viewer-username", toml.getString ("app.bootstrap.viewer-username"));
    putIfNotNull (map, "app.bootstrap.viewer-password", toml.getString ("app.bootstrap.viewer-password"));
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
    try {
      com.moandjiezana.toml.Toml headersTable = toml.getTable ("ai.headers");
      if (headersTable != null) {
        for (Map.Entry<String, Object> e : headersTable.toMap ().entrySet ()) {
          if (e.getValue () != null) map.put ("ai.headers." + e.getKey (), String.valueOf (e.getValue ()));
        }
      }
    } catch (Exception ignored) {}
    String cors = toml.getString ("app.cors.allowed-origins");
    if (cors != null) map.put ("app.cors.allowed-origins", cors);
    Boolean rlEnabled = toml.getBoolean ("app.rate-limit.enabled");
    if (rlEnabled != null) map.put ("app.rate-limit.enabled", rlEnabled);
    Long rpm = toml.getLong ("app.rate-limit.requests-per-minute");
    if (rpm != null) map.put ("app.rate-limit.requests-per-minute", rpm);
    Long ws = toml.getLong ("app.rate-limit.window-seconds");
    if (ws != null) map.put ("app.rate-limit.window-seconds", ws);
    Boolean shEnabled = toml.getBoolean ("app.security-headers.enabled");
    if (shEnabled != null) map.put ("app.security-headers.enabled", shEnabled);
    Boolean trayEnabled = toml.getBoolean ("app.tray.enabled");
    if (trayEnabled != null) map.put ("app.tray.enabled", trayEnabled);
    Boolean autoOpen = toml.getBoolean ("app.ui.auto-open-browser");
    if (autoOpen != null) map.put ("app.ui.auto-open-browser", autoOpen);
    Boolean apiDocs = toml.getBoolean ("springdoc.api-docs-enabled");
    if (apiDocs != null) map.put ("springdoc.api-docs.enabled", apiDocs);
    Boolean swagger = toml.getBoolean ("springdoc.swagger-ui-enabled");
    if (swagger != null) map.put ("springdoc.swagger-ui.enabled", swagger);
    String dsUrl = toml.getString ("spring.datasource.url");
    if (dsUrl != null) map.put ("spring.datasource.url", dsUrl);
    String logFile = toml.getString ("logging.file.name");
    if (logFile != null) map.put ("logging.file.name", logFile);
    return map;
  }

  public static void ensurePathProperties (Map<String, Object> props, Path configPath, boolean isWindows) {
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

  public static boolean isProdProfile (ConfigurableEnvironment environment) {
    for (String p : environment.getActiveProfiles ()) {
      if ("prod".equals (p)) return true;
    }
    String profiles = environment.getProperty ("spring.profiles.active", "");
    if (profiles != null && profiles.contains ("prod")) return true;
    String sys = System.getProperty ("spring.profiles.active", "");
    return sys.contains ("prod");
  }

  private static void putIfNotNull (Map<String, Object> map, String key, String value) {
    if (value != null) map.put (key, value);
  }
}
