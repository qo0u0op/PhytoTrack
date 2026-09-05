package com.d0w0b.phytotrack.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Windows 防火牆提示：僅 Windows 生效，不自動提權
 */
@Component
public class FirewallAdvisor {

  private static final Logger log = LoggerFactory.getLogger (FirewallAdvisor.class);

  private final int port;
  private final boolean isWindows;

  public FirewallAdvisor (@Value ("${server.port:8080}") int port) {
    this.port = port;
    this.isWindows = BinaryPaths.isWindows ();
  }

  @EventListener (ApplicationReadyEvent.class)
  public void advise () {
    if (!isWindows) return;
    try {
      Process p = new ProcessBuilder ("netsh", "advfirewall", "firewall", "show", "rule", "name=PhytoTrack")
          .redirectErrorStream (true).start ();
      String out = new String (p.getInputStream ().readAllBytes ());
      p.waitFor ();
      if (out.contains ("PhytoTrack") && out.contains (String.valueOf (port))) {
        return; // 已放行
      }
      System.out.println ("[PhytoTrack] Windows 防火牆未放行 port " + port + "，若無法連線請以管理員執行：");
      System.out.println ("  netsh advfirewall firewall add rule name=\"PhytoTrack\" dir=in action=allow protocol=TCP localport=" + port);
      System.out.println ("  PowerShell: Start-Process netsh -ArgumentList 'advfirewall firewall add rule name=\"PhytoTrack\" dir=in action=allow protocol=TCP localport=" + port + "' -Verb RunAs");
    } catch (Exception e) {
      log.debug ("防火牆偵測略過：{}", e.getMessage ());
    }
  }
}
