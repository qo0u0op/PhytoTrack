package com.d0w0b.phytotrack.config;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Binary 路徑解析：Windows 可攜 vs Unix XDG
 *
 * Windows：base = exe/jar 所在目錄，config/data/logs 相對 base
 * Unix：遵循 XDG Base Directory（XDG_CONFIG_HOME / XDG_DATA_HOME / XDG_STATE_HOME）
 */
public final class BinaryPaths {

  private BinaryPaths () {}

  public static boolean isWindows () {
    return System.getProperty ("os.name", "").toLowerCase ().contains ("win");
  }

  public static Path exeDir () {
    try {
      // CodeSource 為 jar/exe 所在；開發時為 target/classes，回落 user.dir
      File codeSource = new File (BinaryPaths.class.getProtectionDomain ().getCodeSource ().getLocation ().toURI ());
      Path dir = codeSource.isFile () ? codeSource.getParentFile ().toPath () : codeSource.toPath ();
      // 開發時 target/classes -> 回到專案 backend 目錄的父目錄？ 保持簡單：若路徑含 target/classes，回落 user.dir
      String dirStr = dir.toString ();
      if (dirStr.contains ("target" + File.separator + "classes")) {
        return Paths.get (System.getProperty ("user.dir"));
      }
      return dir;
    } catch (URISyntaxException e) {
      return Paths.get (System.getProperty ("user.dir"));
    }
  }

  public static Path windowsConfig () {
    return exeDir ().resolve ("config").resolve ("phytotrack.toml");
  }

  public static Path windowsData () {
    return exeDir ().resolve ("data").resolve ("diagnoses.db");
  }

  public static Path windowsLog () {
    return exeDir ().resolve ("logs").resolve ("phytotrack.log");
  }

  public static Path xdgConfig () {
    String xdg = System.getenv ("XDG_CONFIG_HOME");
    if (xdg != null && !xdg.isBlank ()) {
      return Paths.get (xdg).resolve ("phytotrack").resolve ("phytotrack.toml");
    }
    String home = System.getProperty ("user.home");
    if (home == null || home.isBlank ()) home = System.getProperty ("user.dir");
    return Paths.get (home).resolve (".config").resolve ("phytotrack").resolve ("phytotrack.toml");
  }

  public static Path xdgData () {
    String xdg = System.getenv ("XDG_DATA_HOME");
    if (xdg != null && !xdg.isBlank ()) {
      return Paths.get (xdg).resolve ("phytotrack").resolve ("diagnoses.db");
    }
    String home = System.getProperty ("user.home");
    if (home == null || home.isBlank ()) home = System.getProperty ("user.dir");
    return Paths.get (home).resolve (".local").resolve ("share").resolve ("phytotrack").resolve ("diagnoses.db");
  }

  public static Path xdgLog () {
    String xdg = System.getenv ("XDG_STATE_HOME");
    if (xdg != null && !xdg.isBlank ()) {
      return Paths.get (xdg).resolve ("phytotrack").resolve ("phytotrack.log");
    }
    String home = System.getProperty ("user.home");
    if (home == null || home.isBlank ()) home = System.getProperty ("user.dir");
    return Paths.get (home).resolve (".local").resolve ("state").resolve ("phytotrack").resolve ("phytotrack.log");
  }

  public static Path systemConfig () {
    return Paths.get ("/etc").resolve ("phytotrack").resolve ("phytotrack.toml");
  }

  public static boolean isAppImage () {
    String appImage = System.getenv ("APPIMAGE");
    return appImage != null && !appImage.isBlank ();
  }

  public static Path appImageDir () {
    String appImage = System.getenv ("APPIMAGE");
    if (appImage != null && !appImage.isBlank ()) {
      Path p = Paths.get (appImage);
      Path parent = p.getParent ();
      if (parent != null) return parent;
    }
    // 回落：OWD（AppImage 規範）或 exeDir
    String owd = System.getenv ("OWD");
    if (owd != null && !owd.isBlank ()) return Paths.get (owd);
    return exeDir ();
  }

  public static Path appImageConfig () {
    return appImageDir ().resolve ("config").resolve ("phytotrack.toml");
  }

  public static Path appImageData () {
    return appImageDir ().resolve ("data").resolve ("diagnoses.db");
  }

  public static Path appImageLog () {
    return appImageDir ().resolve ("logs").resolve ("phytotrack.log");
  }

  public static Path configPath () {
    if (isAppImage ()) return appImageConfig ();
    return isWindows () ? windowsConfig () : xdgConfig ();
  }

  public static Path dataPath () {
    if (isAppImage ()) return appImageData ();
    return isWindows () ? windowsData () : xdgData ();
  }

  public static Path logPath () {
    if (isAppImage ()) return appImageLog ();
    return isWindows () ? windowsLog () : xdgLog ();
  }
}
