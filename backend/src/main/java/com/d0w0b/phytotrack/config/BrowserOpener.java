package com.d0w0b.phytotrack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * 啟動後自動開瀏覽器，預設開啟，可由 app.ui.auto-open-browser 關閉
 * - dev profile：開啟 vite 即時前端（app.ui.dev-frontend-url，預設 :5173）
 * - prod/binary：開啟同 port 內嵌前端（/）
 */
@Component
public class BrowserOpener {

  private static final Logger log = LoggerFactory.getLogger (BrowserOpener.class);

  private final int port;
  private final boolean autoOpen;
  private final String devFrontendUrl;
  private final boolean isDev;

  public BrowserOpener (@Value ("${server.port:8080}") int port,
      @Value ("${app.ui.auto-open-browser:true}") boolean autoOpen,
      @Value ("${app.ui.dev-frontend-url:http://localhost:5173/}") String devFrontendUrl,
      @Value ("${spring.profiles.active:}") String activeProfiles) {
    this.port = port;
    this.autoOpen = autoOpen;
    this.devFrontendUrl = devFrontendUrl;
    this.isDev = activeProfiles != null && activeProfiles.contains ("dev");
  }

  @EventListener (ApplicationReadyEvent.class)
  public void open () {
    try {
      // dev 指向 vite，prod 指向同 port 內嵌前端（/）
      String url = isDev ? devFrontendUrl : "http://localhost:" + port + "/";
      // 診斷：GUI 仍未自動開瀏覽器時由此判斷
      log.info ("BrowserOpener：os={}, autoOpen={}, isDev={}, SSH={}, headlessProp={}, DISPLAY={}, WAYLAND_DISPLAY={}",
          System.getProperty ("os.name"), autoOpen, isDev,
          System.getenv ("SSH_CONNECTION") != null || System.getenv ("SSH_CLIENT") != null || System.getenv ("SSH_TTY") != null,
          System.getProperty ("java.awt.headless"),
          System.getenv ("DISPLAY"), System.getenv ("WAYLAND_DISPLAY"));
      if (!autoOpen) {
        System.out.println ("[PhytoTrack] Server started at " + url + " (auto-open disabled)");
        log.info ("自動開瀏覽器已關閉（app.ui.auto-open-browser=false），若為正式 binary 請確認 phytotrack.toml 設 auto-open-browser=true");
        return;
      }
      if (isDev) {
        System.out.println ("[PhytoTrack] Frontend (vite) at " + url);
        System.out.println ("[PhytoTrack] API: http://localhost:" + port + "/api, Swagger: http://localhost:" + port + "/swagger-ui/index.html");
      } else {
        // binary 已將前端 dist 打進 static，/ 與 /api 同 port
        System.out.println ("[PhytoTrack] Server started at " + url + " (前端)");
        System.out.println ("[PhytoTrack] API: " + url + "api, Swagger: " + url + "swagger-ui/index.html");
      }
      // SSH 會話（任意方向 Linux↔Windows）皆無桌面，直接跳過避免 XToolkit/WinStation 毒化
      boolean isSsh = System.getenv ("SSH_CONNECTION") != null
          || System.getenv ("SSH_CLIENT") != null
          || System.getenv ("SSH_TTY") != null;
      if (isSsh) {
        log.info ("SSH 會話，跳過自動開瀏覽器，請手動開啟 {}", url);
        return;
      }
      // 有顯示環境才嘗試 Desktop.browse，SSH tty 直接跳過避免 XToolkit 毒化
      // Windows 本地不依賴 DISPLAY/WAYLAND_DISPLAY（直接可用 Desktop.browse / rundll32）
      String osName = System.getProperty ("os.name", "").toLowerCase ();
      boolean isWindows = osName.contains ("win");
      String display = System.getenv ("DISPLAY");
      String wayland = System.getenv ("WAYLAND_DISPLAY");
      boolean hasDisplay = isWindows || (display != null && !display.isBlank ()) || (wayland != null && !wayland.isBlank ());
      if (!hasDisplay) {
        log.info ("無 DISPLAY/WAYLAND_DISPLAY，跳過自動開瀏覽器（支援 SSH tty），請手動開啟 {}", url);
        return;
      }
      try {
        if (!java.awt.GraphicsEnvironment.isHeadless ()) {
          try {
            if (Desktop.isDesktopSupported () && Desktop.getDesktop ().isSupported (Desktop.Action.BROWSE)) {
              Desktop.getDesktop ().browse (new URI (url));
              return;
            }
          } catch (Throwable e) {
            log.debug ("Desktop.browse 失敗：{}", String.valueOf (e.getMessage ()));
          }
        } else {
          log.debug ("Headless 環境，跳過 Desktop.browse");
        }
      } catch (Throwable e) {
        log.debug ("Headless 檢查失敗：{}", String.valueOf (e.getMessage ()));
      }
      // 回落：依 OS 分流，Windows 用 rundll32，Linux 用 xdg-open
      try {
        if (isWindows) {
          new ProcessBuilder ("rundll32", "url.dll,FileProtocolHandler", url).start ();
        } else {
          new ProcessBuilder ("xdg-open", url).start ();
        }
        return;
      } catch (Throwable e) {
        log.debug ("回落開啟瀏覽器失敗：{}", String.valueOf (e.getMessage ()));
      }
      log.debug ("自動開瀏覽器未成功，請手動開啟 {}", url);
    } catch (Throwable outer) {
      log.warn ("自動開瀏覽器異常，跳過（不影響 Server）：{}", String.valueOf (outer.getMessage ()));
    }
  }
}
