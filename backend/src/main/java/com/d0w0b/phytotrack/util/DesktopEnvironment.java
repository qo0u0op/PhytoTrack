package com.d0w0b.phytotrack.util;

/**
 * 桌面環境判斷：集中 SSH/ DISPLAY/ headless/ Windows 檢查，避免在多處重複
 * <p>
 * 判斷順序：isSsh 優先於 isWindows —— linux→windows server 的 SSH 會話即使 isWindows=true
 * 亦視為無頭，hasDisplay 的 Windows 快捷僅非 SSH 時才視為有顯示
 * 臨時回落不寫盤，下次 GUI 仍有頭
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
    // Windows 有顯示僅非 SSH 時成立；linux→windows server 的 SSH 需回落 headless
    if (isSsh ()) return false;
    if (isWindows ()) return true;
    String display = System.getenv ("DISPLAY");
    String wayland = System.getenv ("WAYLAND_DISPLAY");
    return (display != null && !display.isBlank ()) || (wayland != null && !wayland.isBlank ());
  }

  public static boolean isHeadlessOrSsh () {
    // 順序固定：isSsh → (!hasDisplay && !isWindows) → GraphicsEnvironment.isHeadless()
    // isSsh 優先於 isWindows，避免 Windows SSH 被誤判有頭
    if (isSsh ()) return true;
    if (!hasDisplay () && !isWindows ()) return true;
    try {
      return java.awt.GraphicsEnvironment.isHeadless ();
    } catch (Throwable e) {
      return true;
    }
  }
}
