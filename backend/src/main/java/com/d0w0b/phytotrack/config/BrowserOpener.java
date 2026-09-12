package com.d0w0b.phytotrack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

import com.d0w0b.phytotrack.util.DesktopEnvironment;

/**
 * 啟動後自動開瀏覽器，預設開啟，可由 app.ui.auto-open-browser 關閉
 * - dev profile：開啟 vite 即時前端（app.ui.dev-frontend-url，預設 :5173）
 * - prod/binary：開啟同 port 內嵌前端（/）
 * - 有頭→無頭臨時回落（不寫盤）：SSH（含 linux→windows server）或無 DISPLAY 時跳過，TOML 保持有頭
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
      // 診斷：GUI 仍未自動開瀏覽器時由此判斷（isSsh 優先於 isWindows，臨時回落不寫盤）
      log.info ("BrowserOpener：os={}, autoOpen={}, isDev={}, SSH={}, hasDisplay={}, headlessProp={}, DISPLAY={}, WAYLAND_DISPLAY={}",
          System.getProperty ("os.name"), autoOpen, isDev,
          DesktopEnvironment.isSsh (),
          DesktopEnvironment.hasDisplay (),
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
      // 有頭→無頭臨時回落（不寫盤，下次 GUI 仍有頭）；isSsh 優先於 isWindows（含 linux→windows server）
      if (DesktopEnvironment.isSsh ()) {
        log.info ("SSH 會話（含 linux→windows server），跳過自動開瀏覽器，請手動開啟 {}（臨時回落，不改 TOML）", url);
        return;
      }
      boolean isWindows = DesktopEnvironment.isWindows ();
      if (!DesktopEnvironment.hasDisplay ()) {
        log.info ("無 DISPLAY/WAYLAND_DISPLAY，跳過自動開瀏覽器（支援 SSH tty，臨時回落不寫盤），請手動開啟 {}", url);
        return;
      }
      // 亦檢查 headless 屬性，避免在 headless JVM 誤觸 Desktop
      if (DesktopEnvironment.isHeadlessOrSsh ()) {
        log.info ("Headless/SSH 環境，跳過自動開瀏覽器（臨時回落）請手動開啟 {}", url);
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
