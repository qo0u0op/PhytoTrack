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
 */
@Component
public class BrowserOpener {

  private static final Logger log = LoggerFactory.getLogger (BrowserOpener.class);

  private final int port;
  private final boolean autoOpen;

  public BrowserOpener (@Value ("${server.port:8080}") int port,
      @Value ("${app.ui.auto-open-browser:true}") boolean autoOpen) {
    this.port = port;
    this.autoOpen = autoOpen;
  }

  @EventListener (ApplicationReadyEvent.class)
  public void open () {
    if (!autoOpen) {
      System.out.println ("[PhytoTrack] Server started at http://localhost:" + port + "/ (auto-open disabled)");
      return;
    }
    // 預設開啟前端（/），binary 已將前端 dist 打進 static，/ 與 /api 同 port
    String url = "http://localhost:" + port + "/";
    System.out.println ("[PhytoTrack] Server started at " + url + " (前端)");
    System.out.println ("[PhytoTrack] API: " + url + "api, Swagger: " + url + "swagger-ui/index.html");
    try {
      if (Desktop.isDesktopSupported () && Desktop.getDesktop ().isSupported (Desktop.Action.BROWSE)) {
        Desktop.getDesktop ().browse (new URI (url));
        return;
      }
    } catch (Exception e) {
      log.debug ("Desktop.browse 失敗：{}", e.getMessage ());
    }
    // 回落：xdg-open / open
    try {
      String os = System.getProperty ("os.name", "").toLowerCase ();
      String cmd = os.contains ("mac") ? "open" : "xdg-open";
      new ProcessBuilder (cmd, url).start ();
    } catch (Exception e) {
      log.warn ("自動開瀏覽器失敗，請手動開啟 {}", url);
    }
  }
}
