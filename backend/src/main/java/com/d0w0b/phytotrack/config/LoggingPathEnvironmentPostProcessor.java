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
 * 日誌路徑早期注入：以 HIGHEST_PRECEDENCE+11 搶在 LoggingApplicationListener (20) 前、
 * 且晚於 ConfigDataEnvironmentPostProcessor (10) 以覆蓋 application.yaml 的 logs/phytotrack.log。
 * 僅設 XDG/可攜預設，TOML 明確值（由 PhytotrackTomlEnvironmentPostProcessor 以 addFirst 注入）
 * 會覆蓋此處預設。
 */
@Order (Ordered.HIGHEST_PRECEDENCE + 11)
public class LoggingPathEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public int getOrder () {
    return Ordered.HIGHEST_PRECEDENCE + 11;
  }

  @Override
  public void postProcessEnvironment (ConfigurableEnvironment environment, SpringApplication application) {
    // 計算 XDG/可攜預設（依 BinaryPaths），以 addFirst 覆蓋 application.yaml 的 logs/phytotrack.log
    // TOML 明確值由 PhytotrackTomlEnvironmentPostProcessor 以 addFirst 後續覆蓋此預設
    Path log = BinaryPaths.isAppImage () ? BinaryPaths.appImageLog ()
        : (BinaryPaths.isWindows () ? BinaryPaths.windowsLog () : BinaryPaths.xdgLog ());
    String logStr = log.toString ().replace ("\\", "/");
    Path logDir = log.getParent ();
    String logDirStr = logDir != null ? logDir.toString ().replace ("\\", "/") : "logs";

    Map<String, Object> map = new HashMap<> ();
    map.put ("logging.file.name", logStr);
    map.put ("phytotrack.logging.file", logStr);
    map.put ("phytotrack.logging.dir", logDirStr);
    environment.getPropertySources ().addFirst (new MapPropertySource ("phytotrackLoggingPath", map));
    // 同步 System property 供 logback 早期讀取
    System.setProperty ("logging.file.name", logStr);
    System.setProperty ("phytotrack.logging.file", logStr);
    System.setProperty ("phytotrack.logging.dir", logDirStr);
  }
}
