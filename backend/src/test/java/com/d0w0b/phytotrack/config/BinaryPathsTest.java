package com.d0w0b.phytotrack.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BinaryPathsTest {

  @Test
  void windowsPaths_shouldBeRelativeToExeDir () {
    // 模擬 Windows：os.name 含 win 時應落 exeDir/config
    String originalOs = System.getProperty ("os.name");
    try {
      System.setProperty ("os.name", "Windows 10");
      assertThat (BinaryPaths.isWindows ()).isTrue ();
      Path config = BinaryPaths.windowsConfig ();
      assertThat (config.toString ()).contains ("config");
      assertThat (config.toString ()).endsWith ("phytotrack.toml");
      assertThat (BinaryPaths.windowsData ().toString ()).endsWith ("diagnoses.db");
      assertThat (BinaryPaths.windowsLog ().toString ()).endsWith ("phytotrack.log");
    } finally {
      if (originalOs != null) System.setProperty ("os.name", originalOs);
      else System.clearProperty ("os.name");
    }
  }

  @Test
  void unixPaths_shouldFollowXdg () {
    String originalOs = System.getProperty ("os.name");
    try {
      System.setProperty ("os.name", "Linux");
      assertThat (BinaryPaths.isWindows ()).isFalse ();
      Path config = BinaryPaths.xdgConfig ();
      assertThat (config.toString ()).contains ("phytotrack");
      assertThat (config.toString ()).endsWith ("phytotrack.toml");
      // 即使無 XDG env，回落 ~/.config
      assertThat (config.toString ()).contains (".config");
    } finally {
      if (originalOs != null) System.setProperty ("os.name", originalOs);
      else System.clearProperty ("os.name");
    }
  }

  @Test
  void configPath_shouldDelegateByOs () {
    String originalOs = System.getProperty ("os.name");
    try {
      System.setProperty ("os.name", "Windows 11");
      assertThat (BinaryPaths.configPath ()).isEqualTo (BinaryPaths.windowsConfig ());
      System.setProperty ("os.name", "Mac OS X");
      assertThat (BinaryPaths.configPath ().toString ()).contains ("phytotrack.toml");
    } finally {
      if (originalOs != null) System.setProperty ("os.name", originalOs);
      else System.clearProperty ("os.name");
    }
  }

  @Test
  void appImagePaths_shouldBePortable () {
    // 無 APPIMAGE 時為 false，但方法應存在且路徑以可攜結構結尾
    assertThat (BinaryPaths.isAppImage ()).isFalse ();
    assertThat (BinaryPaths.appImageConfig ().toString ()).endsWith ("config/phytotrack.toml");
    assertThat (BinaryPaths.appImageData ().toString ()).endsWith ("data/diagnoses.db");
    assertThat (BinaryPaths.appImageLog ().toString ()).endsWith ("logs/phytotrack.log");
    // AppImage 優先於 XDG/Windows：若有 APPIMAGE，configPath 應走 appImage
    // 此處僅驗結構，實際 env 由 BinaryPaths.configPath() 於啟動時判斷
  }
}
