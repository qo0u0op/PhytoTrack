package com.d0w0b.phytotrack.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * TOML 配置載入與首次自動生成（僅協調，實作委派 TomlGenerator/TomlLoader）
 * 優先順序：TOML > application.yaml 預設；首次啟動若無 TOML 則生成預設含亂數 JWT 密鑰
 * 以 HIGHEST_PRECEDENCE+12 搶在 LoggingApplicationListener (20) 前注入，確保日誌路徑
 * 在 logging 初始化前已為 XDG/可攜，覆蓋 LoggingPath 的預設；晚於 LoggingPath (+11)
 */
@Order (Ordered.HIGHEST_PRECEDENCE + 12)
public class PhytotrackTomlEnvironmentPostProcessor
    implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

  @Override
  public int getOrder () {
    return Ordered.HIGHEST_PRECEDENCE + 12;
  }

  @Override
  public void onApplicationEvent (ApplicationEnvironmentPreparedEvent event) {
    ConfigurableEnvironment environment = event.getEnvironment ();
    for (String p : environment.getActiveProfiles ()) {
      if ("test".equals (p)) return;
    }
    String profiles = environment.getProperty ("spring.profiles.active", "");
    if (profiles.contains ("test")) return;

    Path configPath = BinaryPaths.configPath ();
    Path systemConfig = BinaryPaths.systemConfig ();
    boolean isWindows = BinaryPaths.isWindows ();
    boolean isProd = TomlLoader.isProdProfile (environment);

    Path primary = configPath;
    Path secondary = isWindows ? null : systemConfig;

    Map<String, Object> props = new HashMap<> ();

    if (!Files.exists (primary)) {
      if (!isWindows && Files.exists (secondary)) {
        primary = secondary;
      } else {
        try {
          TomlGenerator.generateDefaultToml (primary, isProd);
          System.out.println ("[PhytoTrack] 首次啟動已生成配置：" + primary);
          System.out.println ("[PhytoTrack] 首次啟動已生成亂數密鑰，舊 token 失效請重新登入");
        } catch (IOException e) {
          System.err.println ("[PhytoTrack] 生成預設配置失敗：" + e.getMessage ());
        }
      }
    }

    if (Files.exists (primary)) {
      try {
        Map<String, Object> tomlProps = TomlLoader.loadToml (primary);
        props.putAll (tomlProps);
      } catch (IOException e) {
        System.err.println ("[PhytoTrack] 載入 TOML 失敗：" + e.getMessage ());
      }
    }

    if (!isWindows && !primary.equals (secondary) && Files.exists (secondary)) {
      try {
        Map<String, Object> sysProps = TomlLoader.loadToml (secondary);
        for (Map.Entry<String, Object> e : sysProps.entrySet ()) {
          props.putIfAbsent (e.getKey (), e.getValue ());
        }
      } catch (IOException e) {
        System.err.println ("[PhytoTrack] 載入系統 TOML 失敗：" + e.getMessage ());
      }
    }

    TomlLoader.ensurePathProperties (props, primary, isWindows);

    if (!props.isEmpty ()) {
      environment.getPropertySources ().addFirst (new MapPropertySource ("phytotrackToml", props));
    }
  }
}
