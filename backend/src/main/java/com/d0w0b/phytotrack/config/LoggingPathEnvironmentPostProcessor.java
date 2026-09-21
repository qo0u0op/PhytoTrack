package com.d0w0b.phytotrack.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 日誌路徑早期注入：以 HIGHEST_PRECEDENCE 搶在 LoggingApplicationListener (20) 前
 * 注入 {@code logging.file.name} 與 {@code phytotrack.logging.dir}。
 * 僅設 XDG/可攜預設，TOML 明確值（由 PhytotrackTomlEnvironmentPostProcessor 以 addFirst 注入）
 * 會覆蓋此處預設；test profile 的 {@code app.rate-limit.enabled} 等不受影響。
 */
@Order (Ordered.HIGHEST_PRECEDENCE)
public class LoggingPathEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public int getOrder () {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void postProcessEnvironment (ConfigurableEnvironment environment, SpringApplication application) {
    // test profile 跳過 XDG 預設，避免干擾測試隔離（測試以 application-test.yaml 為準）
    for (String p : environment.getActiveProfiles ()) {
      if ("test".equals (p)) return;
    }
    String profiles = environment.getProperty ("spring.profiles.active", "");
    if (profiles != null && profiles.contains ("test")) return;
    // 若已由 TOML 或外部配置明確指定，則不覆蓋
    if (environment.containsProperty ("logging.file.name")) return;

    Path log = BinaryPaths.isAppImage () ? BinaryPaths.appImageLog ()
        : (BinaryPaths.isWindows () ? BinaryPaths.windowsLog () : BinaryPaths.xdgLog ());
    String logStr = log.toString ().replace ("\\", "/");
    Path logDir = log.getParent ();
    String logDirStr = logDir != null ? logDir.toString ().replace ("\\", "/") : "logs";

    Map<String, Object> map = new HashMap<> ();
    map.put ("logging.file.name", logStr);
    map.put ("phytotrack.logging.file", logStr);
    map.put ("phytotrack.logging.dir", logDirStr);
    // 以 addLast 作為預設，TOML 的 addFirst 可覆蓋
    environment.getPropertySources ().addLast (new MapPropertySource ("phytotrackLoggingPath", map));
    // 同步 System property 供 logback 早期讀取（springProperty 會先查 Environment，System 為後備）
    if (System.getProperty ("logging.file.name") == null) {
      System.setProperty ("logging.file.name", logStr);
    }
    if (System.getProperty ("phytotrack.logging.dir") == null) {
      System.setProperty ("phytotrack.logging.dir", logDirStr);
    }
  }
}
