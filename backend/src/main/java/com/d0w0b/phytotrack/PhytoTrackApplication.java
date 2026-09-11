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
    // AppImage 需同時支援：SSH tty（無 DISPLAY）與 XWayland 桌面（有 DISPLAY）
    // 無顯示環境時強制 headless，避免 AWT/XToolkit 初始化失敗毒化類別致後續 NoClassDefFoundError
    try {
      // SSH 會話（Linux→Windows / Windows→Linux / 任意方向）皆無桌面，直接 headless
      boolean isSsh = System.getenv ("SSH_CONNECTION") != null
          || System.getenv ("SSH_CLIENT") != null
          || System.getenv ("SSH_TTY") != null;
      if (isSsh) {
        System.setProperty ("java.awt.headless", "true");
      } else {
        String os = System.getProperty ("os.name", "").toLowerCase ();
        boolean isWindows = os.contains ("win");
        if (!isWindows) {
          String display = System.getenv ("DISPLAY");
          String wayland = System.getenv ("WAYLAND_DISPLAY");
          boolean hasDisplay = (display != null && !display.isBlank ()) || (wayland != null && !wayland.isBlank ());
          if (!hasDisplay) {
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
