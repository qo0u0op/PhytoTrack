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
    // dev 指向 vite，prod 指向同 port 內嵌前端（/）
    String url = isDev ? devFrontendUrl : "http://localhost:" + port + "/";
    if (!autoOpen) {
      System.out.println ("[PhytoTrack] Server started at " + url + " (auto-open disabled)");
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
    if (!java.awt.GraphicsEnvironment.isHeadless ()) {
      try {
        if (Desktop.isDesktopSupported () && Desktop.getDesktop ().isSupported (Desktop.Action.BROWSE)) {
          Desktop.getDesktop ().browse (new URI (url));
          return;
        }
      } catch (Throwable e) {
        log.debug ("Desktop.browse 失敗：{}", e.getMessage ());
      }
    } else {
      log.debug ("Headless 環境，跳過 Desktop.browse");
    }
    // 回落：僅 Linux 用 xdg-open（headless 時亦嘗試，失敗僅警告）
    try {
      new ProcessBuilder ("xdg-open", url).start ();
    } catch (Throwable e) {
      log.warn ("自動開瀏覽器失敗，請手動開啟 {}", url);
    }
  }
}
