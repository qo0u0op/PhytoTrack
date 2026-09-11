package com.d0w0b.phytotrack.util;

/**
 * 桌面環境判斷：集中 SSH/ DISPLAY/ headless/ Windows 檢查，避免在多處重複
 */
public final class DesktopEnvironment {

  private DesktopEnvironment () {}

  public static boolean isWindows () {
    return System.getProperty ("os.name", "").toLowerCase ().contains ("win");
  }

  public static boolean isSsh () {
    return System.getenv ("SSH_CONNECTION") != null
        || System.getenv ("SSH_CLIENT") != null
        || System.getenv ("SSH_TTY") != null;
  }

  public static boolean hasDisplay () {
    if (isWindows ()) return true;
    String display = System.getenv ("DISPLAY");
    String wayland = System.getenv ("WAYLAND_DISPLAY");
    return (display != null && !display.isBlank ()) || (wayland != null && !wayland.isBlank ());
  }

  public static boolean isHeadlessOrSsh () {
    if (isSsh ()) return true;
    if (!hasDisplay () && !isWindows ()) return true;
    try {
      return java.awt.GraphicsEnvironment.isHeadless ();
    } catch (Throwable e) {
      return true;
    }
  }
}
