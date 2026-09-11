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
    // 桌面環境 headless 判斷集中於 DesktopEnvironment，避免重複
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
          // Windows GUI 已有桌面，明確非 headless（修復 085ddb9 回歸：之前 static block 強制 false 被移除導致 GUI 仍可能 headless）
          System.setProperty ("java.awt.headless", "false");
        }
      }
    } catch (Exception ignored) {}
    SpringApplication.run (PhytoTrackApplication.class, args);
  }

}
