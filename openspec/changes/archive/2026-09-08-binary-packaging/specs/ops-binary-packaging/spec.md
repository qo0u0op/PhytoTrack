## Purpose

沉澱精簡後的 binary 打包與發布流程：僅 Windows portable zip 與 Linux AppImage（皆 `jpackage app-image`，直接進 SystemTray，真實圖示），前置建置與 jpackage 矩陣、版本正規化與托盤功能。

## ADDED Requirements

### Requirement: 前置建置與 jpackage 分離

系統 SHALL 將 `mise run package` 限為前置建置（`frontend npm run build → cp dist → backend/src/main/resources/static` 與 `backend mvn package -DskipTests`），`jpackage` 全由 `.github/workflows/release.yml` 執行，前置產物為 `backend/target/phytotrack-0.0.1-SNAPSHOT.jar`。

#### Scenario: 本地前置建置
- **WHEN** 執行 `mise run package`
- **THEN** 產生 `backend/target/phytotrack-0.0.1-SNAPSHOT.jar` 且 `static` 含前端 `dist`，不產 `dist/*.zip|AppImage`

#### Scenario: CI 觸發
- **WHEN** 推送 `tag v*`
- **THEN** `release.yml` 先執行前置建置再執行 `jpackage` 矩陣

### Requirement: jpackage 矩陣與可攜分發

`release.yml` SHALL 僅含 `windows-latest:app-image` 與 `ubuntu-latest:app-image` 雙矩陣（皆 `jpackage --type app-image` 無平台專參），Windows 後以 `Compress-Archive` 轉 `dist/PhytoTrack-$VERSION-win.zip`，Linux 後以 `appimagetool` 轉 `AppImage`；`exe` 安裝版 / `deb` / `macos` 已放棄。

#### Scenario: Windows 可攜
- **WHEN** `matrix.type=app-image` 於 `windows-latest`
- **THEN** `jpackage --type app-image --name phytotrack` 成功且產 `dist/phytotrack`，後打包為 `dist/PhytoTrack-$VERSION-win.zip`（內含 `phytotrack.exe`，`--java-options -Dspring.profiles.active=prod` 已注入）

#### Scenario: Linux AppImage 前置
- **WHEN** `matrix.type=app-image` 於 `ubuntu-latest`
- **THEN** `jpackage --type app-image` 不帶 `--linux-*`/`--win-*`，產 `dist/phytotrack` 供後續 `appimagetool`

#### Scenario: 已移除
- **WHEN** 推送 `tag v*`
- **THEN** 不再觸發 `exe` / `deb` / `macos` 矩陣，不再產生 `PhytoTrack.bat`

### Requirement: 版本正規化

`release.yml` SHALL 將 `GITHUB_REF#refs/tags/v` 去 `v`、`${VERSION//-/.}` 轉點分、`cut -d. -f1-3` 截 3 段並將前導 `0` 修正為 `1`（`jpackage` 禁首段為 0），`zip` 與 `app-image` 共用。

#### Scenario: 跨平台版本一致
- **WHEN** 推送 `tag v0.0.1-2`
- **THEN** `zip` 與 `app-image` 以 `1.0.1`（`0.0.1-2 → 0.0.1.2 → 0.0.1 → 1.0.1`）為 `--app-version`

### Requirement: Windows Shell 一致

`release.yml` 的 `Build frontend/backend/Run jpackage` SHALL 皆 `shell: bash`，避免 `windows-latest` 預設 `PowerShell` 將 `rm -rf` 解為 `Remove-Item`。

#### Scenario: Windows 前端建置
- **WHEN** 於 `windows-latest` 執行 `rm -rf ../backend/src/main/resources/static`
- **THEN** 以 `bash` 執行成功，不報 `A parameter cannot be found that matches parameter name 'rf'`

### Requirement: AppImage 可攜、直接進托盤與 SystemTray

`ubuntu app-image` 後 SHALL 以 `appimagetool` 將 `dist/phytotrack` 轉 `dist/PhytoTrack-$VERSION-x86_64.AppImage`，`AppRun` 預設 `--spring.profiles.active=prod`（僅 `admin`，可透傳 `spring.profiles.active`），`phytotrack.desktop` 含 `Terminal=false` 且 **不再自動開終端**；後端整合 `dorkbox/SystemTray`（`com.dorkbox:SystemTray:4.1`）於 `SystemTrayManager`/`BrowserOpener` 旁提供系統托盤，無托盤時回落 Swing 備用視窗。

#### Scenario: GUI 雙擊直接進托盤
- **WHEN** 於桌面環境雙擊 `PhytoTrack-*.AppImage` 或解壓後執行 `phytotrack.exe`
- **THEN** 不再開新終端，直接啟動 server 並於系統匣顯示 `PhytoTrack` 圖示

#### Scenario: CLI 直跑
- **WHEN** 於終端執行 `./PhytoTrack-*.AppImage --spring.profiles.active=dev` 或 `phytotrack.exe --spring.profiles.active=dev`
- **THEN** 以透傳參數啟動，不強制覆蓋為 `prod`

### Requirement: 真實圖示策略

倉庫 SHALL 保留 `docs/img/icon.svg/.png/.ico` 與 `backend/src/main/resources/tray-icon.png/.svg`，`jpackage --icon` 於 Windows 用 `icon.ico`、Linux 用 `icon.png`，`AppDir` 優先複製 `docs/img/icon.svg/.png`，缺失時回落 `base64 -d` 1×1 透明 `phytotrack.png/.DirIcon` 以通過 `appimagetool` 校驗。

#### Scenario: 圖示內嵌
- **WHEN** 執行 `jpackage` 與 `Build AppImage`
- **THEN** `phytotrack.exe` 內嵌 `icon.ico`，`AppImage` 含 `phytotrack.png/.DirIcon` 與 `tray-icon.png`，`SystemTray` 使用 `tray-icon.png`

### Requirement: SystemTray 托盤完整功能

系統托盤 SHALL 提供 `開啟 PhytoTrack`（開 `http://localhost:${port}/`）、`備份資料庫`（複製 `diagnoses.db` 至 `backups/phytotrack-yyyyMMdd-HHmmss.db`）、`開啟資料夾`（`BinaryPaths.dataPath().parent`）、`開啟日誌資料夾`（`BinaryPaths.logPath().parent`）、`退出` 選單；Windows `explorer`、Linux `xdg-open`，備用 Swing 視窗含對應五按鈕。

#### Scenario: 托盤存在時
- **WHEN** 以 `AppImage` 或 Windows `phytotrack.exe` 於有托盤的桌面啟動
- **THEN** 出現 `PhytoTrack` 托盤圖示，含上述五選單，`Show Logs` 已改為 `開啟日誌資料夾`

#### Scenario: 托盤不存在時
- **WHEN** 桌面無系統匣支援
- **THEN** 顯示 Swing 備用視窗（`320x180`，含 `開啟 PhytoTrack` / `備份資料庫` / `開啟資料夾` / `開啟日誌資料夾` / `退出`）

#### Scenario: 備份與開啟
- **WHEN** 點擊 `備份資料庫` 或 `開啟資料夾` / `開啟日誌資料夾`
- **THEN** 執行 `Files.copy` 備份並 `notify-send`/`PowerShell Toast` 通知，開啟對應資料夾
