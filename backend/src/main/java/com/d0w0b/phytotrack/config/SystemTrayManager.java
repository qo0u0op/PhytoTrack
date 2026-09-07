package com.d0w0b.phytotrack.config;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Image;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 系統匣（dorkbox SystemTray）：Windows / Linux AppIndicator
 * - 支援 Wayland AppIndicator，自動去背
 * - 右鍵選單：開啟 PhytoTrack / 備份資料庫 / 開啟資料夾 / 退出
 * - 系統通知：notify-send / Windows Toast / 備用 Swing
 */
@Component
public class SystemTrayManager {

  private static final Logger log = LoggerFactory.getLogger (SystemTrayManager.class);

  private final int port;
  private final boolean trayEnabled;
  private SystemTray tray;

  public SystemTrayManager (@Value ("${server.port:8080}") int port,
      @Value ("${app.tray.enabled:true}") boolean trayEnabled) {
    this.port = port;
    this.trayEnabled = trayEnabled;
  }

  static {
    System.setProperty ("java.awt.headless", "false");
    // dorkbox 自動偵測，必要時可強制：SystemTray.FORCE_TRAY_TYPE
    SystemTray.AUTO_SIZE = true;
  }

  @EventListener (ApplicationReadyEvent.class)
  public void init () {
    if (!trayEnabled) {
      log.info ("系統匣已關閉（app.tray.enabled=false）");
      return;
    }
    try {
      SystemTray systemTray = SystemTray.get ("PhytoTrack");
      if (systemTray == null) {
        log.warn ("dorkbox SystemTray 不支援，回落備用視窗");
        createFallbackWindow ();
        return;
      }
      this.tray = systemTray;
      systemTray.setTooltip ("PhytoTrack - 農作物病蟲害診斷系統");

      // 圖示：直接使用 PNG（SVG 在 dorkbox 上不穩定，改用點陣）
      try {
        var pngUrl = getClass ().getResource ("/tray-icon.png");
        if (pngUrl != null) {
          systemTray.setImage (pngUrl);
        } else {
          File iconFile = resolveIconFile ();
          if (iconFile != null && iconFile.exists ()) systemTray.setImage (iconFile);
        }
      } catch (Exception ex) {
        log.debug ("Tray icon 設定失敗：{}", ex.getMessage ());
        File iconFile = resolveIconFile ();
        if (iconFile != null && iconFile.exists ()) {
          try { systemTray.setImage (iconFile); } catch (Exception ignored) {}
        }
      }

      systemTray.getMenu ().add (new MenuItem ("開啟 PhytoTrack", e -> openBrowser ()));
      systemTray.getMenu ().add (new MenuItem ("備份資料庫", e -> backupDatabase ()));
      systemTray.getMenu ().add (new MenuItem ("開啟資料夾", e -> openDataFolder ()));
      systemTray.getMenu ().add (new Separator ());
      systemTray.getMenu ().add (new MenuItem ("退出", e -> {
        systemTray.shutdown ();
        System.exit (0);
      }));

      log.info ("dorkbox SystemTray 已建立，port={}", port);
      // 系統通知（預設）
      showNotification ("PhytoTrack", "已啟動並常駐系統匣（右鍵可備份/退出）");
    } catch (Exception e) {
      log.warn ("dorkbox 匣建立失敗：{}，回落備用視窗", e.getMessage ());
      try { createFallbackWindow (); } catch (Exception ex) { log.warn ("備用視窗失敗：{}", ex.getMessage ()); }
    }
  }

  private File resolveIconFile () {
    try {
      Path exeDir = BinaryPaths.exeDir ();
      Path p = exeDir.resolve ("app").resolve ("icon.png");
      if (Files.exists (p)) return p.toFile ();
      p = exeDir.resolve ("icon.png");
      if (Files.exists (p)) return p.toFile ();
      p = exeDir.resolve ("tray-icon.png");
      if (Files.exists (p)) return p.toFile ();
      File dev = new File ("docs/img/icon.png");
      if (dev.exists ()) return dev;
    } catch (Exception ignored) {}
    try (InputStream in = getClass ().getResourceAsStream ("/tray-icon.png")) {
      if (in != null) {
        Path tmp = Files.createTempFile ("phytotrack-icon", ".png");
        Files.copy (in, tmp, StandardCopyOption.REPLACE_EXISTING);
        tmp.toFile ().deleteOnExit ();
        return tmp.toFile ();
      }
    } catch (Exception ignored) {}
    return null;
  }

  private void openBrowser () {
    String url = "http://localhost:" + port + "/";
    try {
      String os = System.getProperty ("os.name", "").toLowerCase ();
      if (os.contains ("win")) {
        new ProcessBuilder ("rundll32", "url.dll,FileProtocolHandler", url).start (); return;
      }
      new ProcessBuilder ("xdg-open", url).start ();
    } catch (Exception e) {
      log.warn ("開啟瀏覽器失敗：{}", e.getMessage ());
    }
  }

  private void backupDatabase () {
    try {
      Path dbPath = BinaryPaths.dataPath ();
      if (!Files.exists (dbPath)) {
        dbPath = BinaryPaths.windowsData ();
        if (!Files.exists (dbPath)) dbPath = BinaryPaths.xdgData ();
      }
      if (!Files.exists (dbPath)) {
        showNotification ("備份失敗", "找不到資料庫：" + dbPath);
        return;
      }
      Path backupDir = dbPath.getParent ().resolve ("backups");
      Files.createDirectories (backupDir);
      String ts = LocalDateTime.now ().format (DateTimeFormatter.ofPattern ("yyyyMMdd-HHmmss"));
      Path backupFile = backupDir.resolve ("phytotrack-" + ts + ".db");
      Files.copy (dbPath, backupFile);
      log.info ("已備份至 {}", backupFile);
      showNotification ("備份完成", backupFile.getFileName ().toString ());
      openFolder (backupDir);
    } catch (Exception e) {
      log.error ("備份失敗", e);
      showNotification ("備份失敗", e.getMessage ());
    }
  }

  private void openDataFolder () {
    try {
      openFolder (BinaryPaths.dataPath ().getParent ());
    } catch (Exception e) {
      log.warn ("開啟資料夾失敗：{}", e.getMessage ());
    }
  }

  private void openFolder (Path dir) {
    try {
      String os = System.getProperty ("os.name", "").toLowerCase ();
      if (os.contains ("win")) { new ProcessBuilder ("explorer", dir.toString ()).start (); return; }
      new ProcessBuilder ("xdg-open", dir.toString ()).start ();
    } catch (Exception e) { log.debug ("openFolder 失敗：{}", e.getMessage ()); }
  }

  private void showNotification (String title, String text) {
    // 預設走系統通知：Linux notify-send、Windows PowerShell Toast，回落 Swing
    try {
      String os = System.getProperty ("os.name", "").toLowerCase ();
      if (os.contains ("linux")) {
        new ProcessBuilder ("notify-send", title, text).start ();
        return;
      }
      if (os.contains ("win")) {
        // PowerShell Toast（不依賴 AWT）
        String ps = "Add-Type -AssemblyName System.Windows.Forms;$n=New-Object System.Windows.Forms.NotifyIcon;$n.Icon=[System.Drawing.SystemIcons]::Information;$n.Visible=$true;$n.ShowBalloonTip(3000,\"" + title.replace ("\"", "`\"") + "\",\"" + text.replace ("\"", "`\"") + "\",[System.Windows.Forms.ToolTipIcon]::Info)";
        new ProcessBuilder ("powershell", "-Command", ps).start ();
        return;
      }
    } catch (Exception e) {
      log.debug ("系統通知失敗：{}", e.getMessage ());
    }
    // 回落：log + 若無匣則 Swing 彈窗已在 fallback
    log.info ("[通知] {}: {}", title, text);
  }

  private void createFallbackWindow () {
    if (java.awt.GraphicsEnvironment.isHeadless ()) return;
    javax.swing.SwingUtilities.invokeLater (() -> {
      try {
        javax.swing.JFrame frame = new javax.swing.JFrame ("PhytoTrack");
        frame.setDefaultCloseOperation (javax.swing.JFrame.HIDE_ON_CLOSE);
        frame.setSize (320, 180);
        frame.setLocationRelativeTo (null);
        // 嘗試置 icon
        try {
          File icon = resolveIconFile ();
          if (icon != null) {
            var img = javax.imageio.ImageIO.read (icon);
            if (img != null) frame.setIconImage (img);
          }
        } catch (Exception ignored) {}
        javax.swing.JPanel panel = new javax.swing.JPanel ();
        panel.setLayout (new java.awt.GridLayout (4, 1, 6, 6));
        javax.swing.JButton b1 = new javax.swing.JButton ("開啟 PhytoTrack");
        b1.addActionListener (e -> openBrowser ());
        javax.swing.JButton b2 = new javax.swing.JButton ("備份資料庫");
        b2.addActionListener (e -> backupDatabase ());
        javax.swing.JButton b3 = new javax.swing.JButton ("開啟資料夾");
        b3.addActionListener (e -> openDataFolder ());
        javax.swing.JButton b4 = new javax.swing.JButton ("退出");
        b4.addActionListener (e -> System.exit (0));
        panel.add (b1); panel.add (b2); panel.add (b3); panel.add (b4);
        panel.setBorder (javax.swing.BorderFactory.createEmptyBorder (10, 10, 10, 10));
        frame.add (panel);
        frame.setVisible (true);
        log.info ("已顯示備用視窗（dorkbox 不支援時）port={}", port);
      } catch (Exception e) {
        log.warn ("備用視窗失敗：{}", e.getMessage ());
      }
    });
  }
}
