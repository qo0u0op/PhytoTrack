## Context

See proposal.md Why. 期間 `jpackage` 單一指令混 `linux/win` 參數在對應平台報 `is not valid on this platform`，`macOS` 的 `app-version` 又因 `0.0.1-2 → 0.0.1.2` 四段與首段 0 而報 `first number cannot be zero`，`AppImage` 則因桌面缺 `Icon` 被 `appimagetool` 退件；`windows-latest` 預設 `PowerShell` 更使 `rm -rf` 失敗。

## Goals / Non-Goals

**Goals:**
- 單一前置建置供本地與 CI 共用
- 每平台僅帶合法參數，版本一次正規化（含 `0.0.1 → 1.0.1` 前導 0 修正）
- AppImage 保持可攜（同目錄 `config/data/logs`）與直接進托盤（`Terminal=false`）
- Windows 放棄 `exe` 安裝版，改可攜 `app-image + zip`（無 `Wix`/無 `bat`），與 Linux 一致直接進托盤
- 圖示使用真實 `docs/img/icon.*` 與 `tray-icon.*`，托盤含備份/資料夾/日誌資料夾

**Non-Goals:**
- 不產 `exe` 安裝版（已放棄，`Wix` 與 `bat` 編碼問題成本高）
- 不產 `deb`（`Arch` 無 `dpkg`，`CI` 的 `ubuntu` 亦不產，僅 `AppImage` 與 `zip`）
- 不產 `macos`（CLI `aarch64` 已移除）

## Decisions

- **前置建置收斂**：`mise:package` 僅 `npm build + cp static + mvn package`，`jpackage` 全在 `release.yml`。替代：`mise` 直包 `jpackage` 則本地需 `dpkg`/`Wix`，跨平台不可攜 → 捨棄。
- **矩陣分流（雙 app-image，棄 exe）**：僅 `windows:app-image` 與 `ubuntu:app-image` 雙矩陣（皆 `--type app-image` 無平台專參），Windows 後以 `Compress-Archive` 轉 `zip` 分發，徹底移除 `exe`/`deb`/`macos` 與 `--win-*`。替代：保留 `exe`（`--win-shortcut --win-menu` + `Wix` + `PhytoTrack.bat`）→ `Big5` 編碼與安裝器維護成本高，`zip` 可攜已滿足需求 → 捨棄。
- **版本正規化**：`VERSION=${GITHUB_REF#refs/tags/v}; VERSION=${VERSION//-/.}; cut -d. -f1-3; 0→1 修正`，`0.0.1-2 → 0.0.1.2 → 1.0.1`，`zip` 與 `app-image` 共用（`jpackage` 禁首段 0）。替代：帶 `-` 直接作 `app-version` → 非法。
- **Shell 統一**：皆 `shell: bash`，`windows-latest` 走 `Git Bash`，`rm -rf` 一致。
- **AppImage 可攜、直接進托盤與 SystemTray**：`AppRun` 預設 `exec .../bin/phytotrack --spring.profiles.active=prod`（可透傳 `spring.profiles.active`），`desktop` `Terminal=false`，不再自動開終端；後端整合 `dorkbox/SystemTray` 於 `SystemTrayManager`/`BrowserOpener` 提供托盤（`開啟 PhytoTrack`/`備份資料庫`/`開啟資料夾`/`開啟日誌資料夾`/`退出`），`AppImage` 雙擊後托盤可見，無 `tty` 問題。替代：保留終端自動開啟（`x-terminal-emulator` 回落鏈）→ 與托盤常駐語意衝突，且多發行版終端路徑不一致 → 捨棄。
- **圖示策略（真實圖示）**：`docs/img/icon.svg/.png/.ico` 與 `backend/src/main/resources/tray-icon.png/.svg` 真實保留，`jpackage --icon` 與 `AppDir` 優先複製真實圖示，`appimagetool` 缺圖時回落 `base64 -d` 1×1 透明 `png`。替代：無圖示 1×1 策略 → 托盤與桌面無辨識度 → 捨棄。

## Risks / Trade-offs

- [Wayland/Hyprland 托盤] 部分合成器無系統匣 → 回落 Swing 備用視窗（`開啟/備份/資料夾/日誌/退出`），仍可操作
- [版本前綴] `0.0.1 → 1.0.1` 與 tag 不完全等價 → 文件明載，並以 `cut` + `sed s/^0+/1/` 保證 `jpackage` 合法，`generate_release_notes` 仍以 tag 為準
- [可攜路徑] `APPIMAGE` 優先於 `XDG`，若使用者設 `XDG_CONFIG_HOME` 仍以同目錄為準 → 符合 AppImage 可攜預期
- [圖示快取] 桌面環境可能快取 `phytotrack.png` → 更新圖示需清快取或改 `dist` 名稱

## Migration Plan

1. 更新 `release.yml` 為雙 `app-image` 矩陣（移除 `bat`/`exe`，`jpackage EXTRA_JAVA` 注入 `prod`），新增 `dorkbox/SystemTray:4.1` 與 `SystemTrayManager`（含圖示、備份、資料夾、日誌資料夾）
2. 打 `tag v0.0.1-x` 觸發雙矩陣，驗 `PhytoTrack-*.zip` 與 `PhytoTrack-*.AppImage` 皆綠，`AppImage` 雙擊直接進托盤，Windows 解壓執行 `phytotrack.exe` 亦進托盤

## Open Questions

- 無
