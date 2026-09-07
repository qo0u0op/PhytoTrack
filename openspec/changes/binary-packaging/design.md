## Context

See proposal.md Why. 期間 `jpackage` 單一指令混 `linux/win` 參數在對應平台報 `is not valid on this platform`，`macOS` 的 `app-version` 又因 `0.0.1-2 → 0.0.1.2` 四段與首段 0 而報 `first number cannot be zero`，`AppImage` 則因桌面缺 `Icon` 被 `appimagetool` 退件；`windows-latest` 預設 `PowerShell` 更使 `rm -rf` 失敗。

## Goals / Non-Goals

**Goals:**
- 單一前置建置供本地與 CI 共用
- 每平台僅帶合法參數，版本一次正規化
- AppImage 保持可攜（同目錄 `config/data/logs`）與 `GUI` 可見（含 `SystemTray`）
- Windows `PhytoTrack.bat` 純英文，避免 `Big5` 解析錯誤

**Non-Goals:**
- 不產 `deb`（`Arch` 無 `dpkg`，`CI` 的 `ubuntu` 亦不產，僅 `AppImage` 與 `exe`）
- 不產 `macos`（CLI `aarch64` 已移除）

## Decisions

- **前置建置收斂**：`mise:package` 僅 `npm build + cp static + mvn package`，`jpackage` 全在 `release.yml`。替代：`mise` 直包 `jpackage` 則本地需 `dpkg`/`Wix`，跨平台不可攜 → 捨棄。
- **矩陣分流（精簡）**：僅 `windows:exe`（`--win-shortcut --win-menu`）與 `ubuntu:app-image`（無平台專參）雙矩陣，`case` 內各 `jpackage` 僅帶合法參數，徹底移除 `deb`/`macos` 以簡化。替代：保留四矩陣 → 维护與 `jpackage` 參數校驗成本高。
- **版本正規化**：`VERSION=${GITHUB_REF#refs/tags/v}; VERSION=${VERSION//-/.}; cut -d. -f1-3`，`0.0.1-2 → 0.0.1`，`exe` 與 `app-image` 共用。替代：帶 `-` 直接作 `app-version` → 非法。
- **Shell 統一**：皆 `shell: bash`，`windows-latest` 走 `Git Bash`，`rm -rf` 一致。
- **AppImage 可攜、終端與 SystemTray**：`AppRun` 先判 `! -t 1 && -z "$PHYTOTRACK_NO_TERMINAL"` 再 `exec x-terminal-emulator/... -e "$SELF"`，`desktop` 加 `Terminal=true`；後端整合 `dorkbox/SystemTray` 於 `BrowserOpener`/`TrayManager` 提供托盤（`Open`/`Show Logs`/`Quit`），`AppImage` 雙擊後托盤可見。`1×1` 透明 `png` 仍以 `base64 -d` 臨時產生。
- **Windows Bat 純英文與 cmd**：`PhytoTrack.bat` 全 ASCII（`@echo off`、`REM` 皆英文），`cmd /c "%~dp0\phytotrack\bin\phytotrack.exe" --spring.profiles.active=prod %*` 起 server，避免 `Big5`（`950`）下中文 `REM` 解析為 `?` 亂碼。替代：中文 `REM` → `Big5` 環境下 `cmd` 報 `?`。

## Risks / Trade-offs

- [AppImage 終端多樣] `x-terminal-emulator` 非所有發行版有 → 回落鏈 `gnome-terminal → konsole → xfce4-terminal → xterm`，失敗僅不自動開，服務仍起
- [版本前綴] `0.0.1 → 1.0.1` 與 tag 不完全等價 → 文件明載，並以 `cut` 保證 `jpackage` 合法，`generate_release_notes` 仍以 tag 為準
- [可攜路徑] `APPIMAGE` 優先於 `XDG`，若使用者設 `XDG_CONFIG_HOME` 仍以同目錄為準 → 符合 AppImage 可攜預期

## Migration Plan

1. 更新 `release.yml` 精簡為雙矩陣並修正 `PhytoTrack.bat` 為純英文 `cmd /c`，新增 `dorkbox/SystemTray` 依賴與 `TrayManager`
2. 打 `tag v0.0.1-x` 觸發雙矩陣，驗 `exe` 與 `AppImage` 皆綠，`AppImage` 雙擊驗終端與托盤

## Open Questions

- 無
