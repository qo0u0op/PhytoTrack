package com.d0w0b.phytotrack.config;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

/**
 * 系統托盤：僅 AppImage/Linux 有桌面時生效
 * 選單：Open PhytoTrack / Show Logs / Quit
 */
@Component
public class SystemTrayManager {

  private static final Logger log = LoggerFactory.getLogger (SystemTrayManager.class);

  private final int port;
  private final boolean enabled;

  public SystemTrayManager (@Value ("${server.port:8080}") int port,
      @Value ("${app.ui.tray-enabled:true}") boolean enabled) {
    this.port = port;
    this.enabled = enabled;
  }

  @EventListener (ApplicationReadyEvent.class)
  public void init () {
    if (!enabled) return;
    // 僅在有圖形環境且非無頭時嘗試
    if (java.awt.GraphicsEnvironment.isHeadless ()) {
      log.debug ("Headless 環境，略過 SystemTray");
      return;
    }
    try {
      SystemTray tray = SystemTray.get ();
      if (tray == null) {
        log.debug ("SystemTray 不可用");
        return;
      }
      tray.setTooltip ("PhytoTrack - http://localhost:" + port);
      tray.setStatus ("PhytoTrack");
      // Tray icon：優先 SVG，失敗回落 PNG
      try {
        java.net.URL svgUrl = getClass ().getResource ("/tray-icon.svg");
        if (svgUrl != null) {
          try {
            tray.setImage (svgUrl);
          } catch (Exception ex) {
            java.net.URL pngUrl = getClass ().getResource ("/tray-icon.png");
            if (pngUrl != null) tray.setImage (pngUrl);
          }
        } else {
          java.net.URL pngUrl = getClass ().getResource ("/tray-icon.png");
          if (pngUrl != null) tray.setImage (pngUrl);
        }
      } catch (Exception ex) {
        log.debug ("Tray icon 設定失敗：{}", ex.getMessage ());
      }

      tray.getMenu ().add (new MenuItem ("Open PhytoTrack", e -> openBrowser ()));
      tray.getMenu ().add (new MenuItem ("Show Logs", e -> showLogs ()));
      tray.getMenu ().add (new MenuItem ("Backup", e -> backup ()));
      tray.getMenu ().add (new MenuItem ("Quit", e -> {
        tray.shutdown ();
        System.exit (0);
      }));
      log.info ("SystemTray 已啟用");
    } catch (Exception e) {
      log.debug ("SystemTray 初始化失敗：{}", e.getMessage ());
    }
  }

  private void openBrowser () {
    String url = "http://localhost:" + port + "/";
    try {
      if (Desktop.isDesktopSupported () && Desktop.getDesktop ().isSupported (Desktop.Action.BROWSE)) {
        Desktop.getDesktop ().browse (new URI (url));
        return;
      }
      String os = System.getProperty ("os.name", "").toLowerCase ();
      String cmd = os.contains ("mac") ? "open" : "xdg-open";
      new ProcessBuilder (cmd, url).start ();
    } catch (Exception ex) {
      log.warn ("開啟瀏覽器失敗：{}", ex.getMessage ());
    }
  }

  private void showLogs () {
    try {
      File logFile = BinaryPaths.logPath ().toFile ();
      String os = System.getProperty ("os.name", "").toLowerCase ();
      String url = logFile.getAbsolutePath ();
      if (os.contains ("win")) {
        new ProcessBuilder ("cmd", "/c", "start", "", url).start ();
      } else if (os.contains ("mac")) {
        new ProcessBuilder ("open", url).start ();
      } else {
        new ProcessBuilder ("xdg-open", url).start ();
      }
    } catch (Exception e) {
      log.warn ("開啟日誌失敗：{}", e.getMessage ());
    }
  }

  private void backup () {
    try {
      java.nio.file.Path db = BinaryPaths.dataPath ();
      if (!java.nio.file.Files.exists (db)) {
        log.warn ("備份失敗：資料庫不存在 {}", db);
        return;
      }
      java.nio.file.Path backupDir;
      if (BinaryPaths.isAppImage ()) {
        backupDir = BinaryPaths.appImageDir ().resolve ("backups");
      } else if (BinaryPaths.isWindows ()) {
        backupDir = BinaryPaths.exeDir ().resolve ("backups");
      } else {
        String xdgData = System.getenv ("XDG_DATA_HOME");
        if (xdgData != null && !xdgData.isBlank ()) {
          backupDir = java.nio.file.Paths.get (xdgData).resolve ("phytotrack").resolve ("backups");
        } else {
          backupDir = java.nio.file.Paths.get (System.getProperty ("user.home"), ".local", "share", "phytotrack", "backups");
        }
      }
      java.nio.file.Files.createDirectories (backupDir);
      String ts = java.time.LocalDateTime.now ().format (java.time.format.DateTimeFormatter.ofPattern ("yyyyMMdd-HHmmss"));
      java.nio.file.Path dest = backupDir.resolve ("phytotrack-" + ts + ".db");
      java.nio.file.Files.copy (db, dest);
      log.info ("備份完成：{}", dest);
      // 嘗試開啟備份目錄
      try {
        String os = System.getProperty ("os.name", "").toLowerCase ();
        if (os.contains ("win")) {
          new ProcessBuilder ("cmd", "/c", "start", "", backupDir.toString ()).start ();
        } else if (os.contains ("mac")) {
          new ProcessBuilder ("open", backupDir.toString ()).start ();
        } else {
          new ProcessBuilder ("xdg-open", backupDir.toString ()).start ();
        }
      } catch (Exception ignored) {}
    } catch (Exception e) {
      log.warn ("備份失敗：{}", e.getMessage ());
    }
  }
}
