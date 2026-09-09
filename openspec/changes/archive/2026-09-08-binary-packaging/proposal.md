## Why

Binary 交付已從單一 `phytotrack.toml` 與 `BinaryPaths` 三模式（Win 可攜、AppImage 可攜、XDG）演進至需可重現的打包與發布：本地 `mise run package` 已精簡為僅前置建置（前端→static+Fat Jar），`jpackage` 全由 `.github/workflows/release.yml` 矩陣執行，但期間 `jpackage` 參數對平台不匹配、macOS `app-version` 首段不可為 0 與 AppImage `Icon` 校驗等問題反覆造成黃叉，需將整段歷程沉澱為單一 change 的規格與任務，便於審查與回歸。

## What Changes

- **前置建置收斂至 `mise:package`**：僅 `frontend npm run build → cp dist → backend/src/main/resources/static` 與 `backend mvn package -DskipTests`，不含 `jpackage`，供 `release.yml` 與本地驗證共用
- **Release 工作流矩陣化（精簡）**：僅 `windows-latest:app-image` 與 `ubuntu-latest:app-image` 雙矩陣（皆 `jpackage --type app-image`，Windows 以 `Compress-Archive` 轉 `PhytoTrack-$VERSION-win.zip`），移除 `exe` 安裝版 / `deb` / `macos-latest`，避免 `is not valid on this platform` 與 `Wix` 依賴
- **版本正規化**：`VERSION=${GITHUB_REF#refs/tags/v}` 去 `v`、`${VERSION//-/.}` 轉點分、`cut -d. -f1-3` 截 3 段，`0.0.1 → 1.0.1` 前導 0 修正（`jpackage` 禁首段為 0），`zip` 與 `app-image` 共用
- **Windows Shell 修正**：`Build frontend/backend/Run jpackage` 皆 `shell: bash`，避免 `windows-latest` 預設 `PowerShell` 將 `rm -rf` 解為 `Remove-Item`；Windows 可攜直接執行 `phytotrack.exe`（`--java-options -Dspring.profiles.active=prod` 由 `jpackage` 注入），**已放棄 `PhytoTrack.bat`**，避免 `Big5` 中文 `REM` 亂碼
- **AppImage 可攜 + SystemTray（直接進托盤）**：`ubuntu app-image` 後以 `appimagetool` 將 `dist/phytotrack` 轉 `dist/PhytoTrack-$VERSION-x86_64.AppImage`，`AppRun` 預設 `--spring.profiles.active=prod`（僅 `admin`）且 **不再自動開終端**（直接進托盤），`phytotrack.desktop` 含 `Terminal=false`；後端整合 `dorkbox/SystemTray`（`com.dorkbox:SystemTray:4.1`）於 `SystemTrayManager`/`BrowserOpener` 旁提供托盤（`開啟 PhytoTrack` / `備份資料庫` / `開啟資料夾` / `開啟日誌資料夾` / `退出`）
- **真實圖示策略**：倉庫保留 `docs/img/icon.svg/.png/.ico` 與 `backend/src/main/resources/tray-icon.png/.svg`，`AppImage` 優先複製真實圖示，缺失時回落 `base64 -d` 1×1 透明 `phytotrack.png/.DirIcon` 以通過 `appimagetool` 校驗
- **Prod 僅 admin**：`DataInitializer` 與 `phytotrack.toml` 生成依 `prod` 僅 `admin/admin123`，`AppImage`/`zip` 預設 `prod`，`--spring.profiles.active=dev` 才三帳號

## Capabilities

### New Capabilities
- `ops-binary-packaging`: Binary 打包與發布的完整規格（前置建置、jpackage 矩陣、版本正規化、Shell 與 AppImage 可攜）

### Modified Capabilities
<!-- 純流程沉澱，現有 ops-binary 行為已在 binary-config-xdg-portable 中定義，此 change 僅補打包維度，不改其 REQUIREMENTS -->

## Impact

- 交付：`mise.toml:package`、` .github/workflows/release.yml`（雙 `app-image` → `PhytoTrack-*.zip` + `PhytoTrack-*.AppImage`）、`backend/phytotrack.toml.example`、`docs/DEPLOY.md`
- 運行：`BinaryPaths` 三優先（`APPIMAGE > isWindows > XDG`）、`PhytotrackTomlEnvironmentPostProcessor`、`BrowserOpener`（前端 `/`）、`SystemTrayManager`（托盤：開啟/備份/資料夾/日誌資料夾/退出 + 備用 Swing 視窗）、`SpaConfig`、`AiDisabledController`
