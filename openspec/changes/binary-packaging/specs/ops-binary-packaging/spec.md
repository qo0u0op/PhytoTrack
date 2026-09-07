## Purpose

沉澱精簡後的 binary 打包與發布流程：僅 Windows exe 與 Linux AppImage（可攜，SystemTray），前置建置與 jpackage 矩陣、版本正規化與無圖示策略。

## ADDED Requirements

### Requirement: 前置建置與 jpackage 分離

系統 SHALL 將 `mise run package` 限為前置建置（`frontend npm run build → cp dist → backend/src/main/resources/static` 與 `backend mvn package -DskipTests`），`jpackage` 全由 `.github/workflows/release.yml` 執行，前置產物為 `backend/target/phytotrack-0.0.1-SNAPSHOT.jar`。

#### Scenario: 本地前置建置
- **WHEN** 執行 `mise run package`
- **THEN** 產生 `backend/target/phytotrack-0.0.1-SNAPSHOT.jar` 且 `static` 含前端 `dist`，不產 `dist/*.deb|exe|AppImage`

#### Scenario: CI 觸發
- **WHEN** 推送 `tag v*`
- **THEN** `release.yml` 先執行前置建置再執行 `jpackage` 矩陣

### Requirement: jpackage 矩陣與平台專參分流

`release.yml` SHALL 僅含 `windows-latest:exe`（`--win-*`）與 `ubuntu-latest:app-image`（無平台專參）雙矩陣，以 `case "${{matrix.type}}"` 分流，避免 `is not valid on this platform`；`deb` 與 `macos` 已移除。

#### Scenario: Windows exe
- **WHEN** `matrix.type=exe` 於 `windows-latest`
- **THEN** `jpackage --type exe --win-shortcut --win-menu` 成功

#### Scenario: Linux AppImage 前置
- **WHEN** `matrix.type=app-image` 於 `ubuntu-latest`
- **THEN** `jpackage --type app-image` 不帶 `--linux-*`/`--win-*`，產 `dist/phytotrack` 供後續 `appimagetool`

#### Scenario: 已移除
- **WHEN** 推送 `tag v*`
- **THEN** 不再觸發 `deb` 與 `macos` 矩陣

### Requirement: 版本正規化

`release.yml` SHALL 將 `GITHUB_REF#refs/tags/v` 去 `v`、`${VERSION//-/.}` 轉點分、`cut -d. -f1-3` 截 3 段，`exe` 與 `app-image` 共用。

#### Scenario: 跨平台版本一致
- **WHEN** 推送 `tag v0.0.1-2`
- **THEN** `exe` 與 `app-image` 以 `0.0.1`（`0.0.1-2 → 0.0.1.2 → 0.0.1`）為 `--app-version`

### Requirement: Windows Shell 一致

`release.yml` 的 `Build frontend/backend/Run jpackage` SHALL 皆 `shell: bash`，避免 `windows-latest` 預設 `PowerShell` 將 `rm -rf` 解為 `Remove-Item`。

#### Scenario: Windows 前端建置
- **WHEN** 於 `windows-latest` 執行 `rm -rf ../backend/src/main/resources/static`
- **THEN** 以 `bash` 執行成功，不報 `A parameter cannot be found that matches parameter name 'rf'`

### Requirement: AppImage 可攜、終端與 SystemTray

`ubuntu app-image` 後 SHALL 以 `appimagetool` 將 `dist/phytotrack` 轉 `dist/PhytoTrack-$VERSION-x86_64.AppImage`，`AppRun` 預設 `--spring.profiles.active=prod`（僅 `admin`），`GUI` 無 `tty` 時自動開終端，`phytotrack.desktop` 含 `Terminal=true` 且以 `base64 -d` 產生 1×1 透明 `phytotrack.png/.DirIcon`；後端整合 `dorkbox/SystemTray`（`com.dorkbox:SystemTray`）於 `BrowserOpener`/`ApplicationReady` 旁提供系統托盤。

#### Scenario: GUI 雙擊自動開終端
- **WHEN** 於桌面環境雙擊 `PhytoTrack-*.AppImage`
- **THEN** `AppRun` 偵測 `! -t 1` 後以系統預設終端重啟，終端內見 `Server started`

#### Scenario: CLI 直跑
- **WHEN** 於終端執行 `./PhytoTrack-*.AppImage`
- **THEN** 直接執行 `bin/phytotrack` 不再開新終端

### Requirement: Windows 啟動器純英文

`Windows` 可攜啟動器 `PhytoTrack.bat` SHALL 為純 ASCII 英文（無中文 `REM`），避免 `Big5` 編碼解析錯誤，並以 `cmd /c` 起 server，預設 `prod`（僅 `admin`）。

#### Scenario: 編碼正確
- **WHEN** 以 `Big5`（`950`）環境執行 `PhytoTrack.bat`
- **THEN** 不報 `?` 亂碼且正常起 `phytotrack.exe`

#### Scenario: cmd 起服
- **WHEN** 雙擊 `PhytoTrack.bat`
- **THEN** 以 `cmd /c` 執行 `phytotrack.exe --spring.profiles.active=prod`，`--spring.profiles.active=dev` 時三帳號

#### Scenario: SystemTray 托盤
- **WHEN** 以 `AppImage` 於有托盤的桌面啟動
- **THEN** 出現 `PhytoTrack` 托盤圖示，含 `Open PhytoTrack`（開 `http://localhost:${port}/`）、`Show Logs`、`Quit` 選單

