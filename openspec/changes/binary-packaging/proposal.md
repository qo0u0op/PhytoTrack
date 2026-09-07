## Why

Binary 交付已從單一 `phytotrack.toml` 與 `BinaryPaths` 三模式（Win 可攜、AppImage 可攜、XDG）演進至需可重現的打包與發布：本地 `mise run package` 已精簡為僅前置建置（前端→static+Fat Jar），`jpackage` 全由 `.github/workflows/release.yml` 矩陣執行，但期間 `jpackage` 參數對平台不匹配、macOS `app-version` 首段不可為 0 與 AppImage `Icon` 校驗等問題反覆造成黃叉，需將整段歷程沉澱為單一 change 的規格與任務，便於審查與回歸。

## What Changes

- **前置建置收斂至 `mise:package`**：僅 `frontend npm run build → cp dist → backend/src/main/resources/static` 與 `backend mvn package -DskipTests`，不含 `jpackage`，供 `release.yml` 與本地驗證共用
- **Release 工作流矩陣化（精簡）**：僅 `windows-latest:exe`（`--win-*`）與 `ubuntu-latest:app-image` 轉 `AppImage` 雙矩陣，移除 `deb` 與 `macos-latest`，`case "${{matrix.type}}"` 分流避免 `Option is not valid on this platform`
- **版本正規化**：`VERSION=${GITHUB_REF#refs/tags/v}` 去 `v`、`${VERSION//-/.}` 轉點分、`cut -d. -f1-3` 截 3 段，`exe` 與 `app-image` 共用（`deb/macos` 已移除）
- **Windows Shell 修正**：`Build frontend/backend/Run jpackage` 皆 `shell: bash`，避免 `windows-latest` 預設 `PowerShell` 將 `rm -rf` 解為 `Remove-Item`；`Windows` 啟動器改為純英文 `PhytoTrack.bat`（ASCII，`REM` 與 `echo` 皆英文），以 `cmd /c` 起 server 避免 `Big5` 編碼解析錯誤，預設 `prod` 僅 `admin`
- **AppImage 可攜 + SystemTray**：`ubuntu app-image` 後以 `appimagetool` 將 `dist/phytotrack` 轉 `dist/PhytoTrack-$VERSION-x86_64.AppImage`，整合 `dorkbox/SystemTray`（`com.dorkbox:SystemTray`）於 `AppRun`/`BrowserOpener` 旁提供系統托盤（`Open PhytoTrack` / `Show Logs` / `Quit`），`GUI` 無 `tty` 時仍自動開終端，`phytotrack.desktop` 含 `Terminal=true`
- **無圖示策略**：倉庫不存 `docs/img/icon.*`，`AppImage` 步以 `base64 -d` 產生 1×1 透明 `phytotrack.png/.DirIcon` 僅滿足 `appimagetool` 校驗，視覺等同無圖示
- **Prod 僅 admin**：`DataInitializer` 與 `phytotrack.toml` 生成依 `prod` 僅 `admin/admin123`，`AppImage`/`exe` 預設 `prod`，`--spring.profiles.active=dev` 才三帳號

## Capabilities

### New Capabilities
- `ops-binary-packaging`: Binary 打包與發布的完整規格（前置建置、jpackage 矩陣、版本正規化、Shell 與 AppImage 可攜）

### Modified Capabilities
<!-- 純流程沉澱，現有 ops-binary 行為已在 binary-config-xdg-portable 中定義，此 change 僅補打包維度，不改其 REQUIREMENTS -->

## Impact

- 交付：`mise.toml:package`、` .github/workflows/release.yml`（僅 `windows:exe` 與 `ubuntu:app-image→AppImage`）、`backend/phytotrack.toml.example`、`docs/DEPLOY.md`
- 運行：`BinaryPaths` 三優先（`APPIMAGE > isWindows > XDG`）、`PhytotrackTomlEnvironmentPostProcessor`、`BrowserOpener`（前端 `/` + `dorkbox/SystemTray` 托盤）、`SpaConfig`、`AiDisabledController`、`Windows PhytoTrack.bat`（純英文 `cmd /c`）
