package com.d0w0b.phytotrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 應用程式進入點
 *
 * 所有子套件 (controller、service、repository、models 等) 都位於本類別所在套件
 * com.d0w0b.phytotrack 之下，因此預設的元件掃描 (Component Scan) 即可涵蓋全部，
 * 無需額外設定 scanBasePackages。
 */
@SpringBootApplication
public class PhytoTrackApplication {

  public static void main (String[] args) {
    // 有頭→無頭臨時回落（不寫盤）：SSH（含 linux→windows server）或無 DISPLAY 時 headless
    // isSsh 優先於 isWindows，避免 Windows SSH 被誤判有頭；hasDisplay 的 Windows true 僅非 SSH 時成立
    try {
      if (com.d0w0b.phytotrack.util.DesktopEnvironment.isSsh ()) {
        System.setProperty ("java.awt.headless", "true");
      } else {
        boolean isWindows = com.d0w0b.phytotrack.util.DesktopEnvironment.isWindows ();
        if (!isWindows) {
          if (!com.d0w0b.phytotrack.util.DesktopEnvironment.hasDisplay ()) {
            System.setProperty ("java.awt.headless", "true");
          }
        } else {
          // Windows GUI 已有桌面，明確非 headless（修復 085ddb9 回歸）；SSH 已在上一分支回落，此處僅非 SSH Windows
          System.setProperty ("java.awt.headless", "false");
        }
      }
    } catch (Exception ignored) {}
    SpringApplication.run (PhytoTrackApplication.class, args);
  }

}
