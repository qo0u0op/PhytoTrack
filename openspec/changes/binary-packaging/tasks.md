## 1. 前置建置收斂

- [ ] 1.1 將 `mise.toml:package` 精簡為僅 `frontend npm run build → cp dist → backend/src/main/resources/static` 與 `backend mvn package -DskipTests`，不含 `jpackage`，驗證 `mise run package` 產 `backend/target/phytotrack-0.0.1-SNAPSHOT.jar` 且 `static` 含 `index.html`

## 2. Release 矩陣與參數分流

- [ ] 2.1 精簡 `.github/workflows/release.yml:11` 為雙矩陣 `windows:exe`（`--win-*`）與 `ubuntu:app-image`（無平台專參），`Build frontend/backend/Run jpackage` 皆 `shell: bash`，驗證 `windows` 的 `rm -rf` 綠燈且不再觸發 `deb/macos`
- [ ] 2.2 `Run jpackage` 內 `VERSION` 去 `v`、轉點分、截 3 段，`case "${{matrix.type}}"` 分流 `exe`/`app-image` 合法參數，驗證 `tag v0.0.1-2` 的 `0.0.1` 雙矩陣綠燈

## 3. AppImage 可攜、終端與 SystemTray

- [ ] 3.1 `ubuntu app-image` 後以 `appimagetool` 將 `dist/phytotrack` 轉 `dist/PhytoTrack-$VERSION-x86_64.AppImage`，`AppRun` 預設 `prod` 且 `GUI` 無 `tty` 時自動開終端，`phytotrack.desktop` 含 `Terminal=true`，`base64 -d` 產生 1×1 透明 `png`，驗證本地可封為 `AppImage` 並雙擊開終端
- [ ] 3.2 整合 `dorkbox/SystemTray`（`com.dorkbox:SystemTray:4.1`）於後端（`BrowserOpener` 旁 `SystemTrayManager`），提供托盤 `Open PhytoTrack` / `Show Logs` / `Quit`，驗證 `AppImage` 於有托盤桌面可見圖示與選單
- [ ] 3.3 `Upload installer` 與 `release` 合併多 `dist/*` 發布至 `softprops/action-gh-release@v2`，驗證 `tag v0.0.1-x` 後 Releases 含 `*.exe` 與 `*.AppImage`

## 4. Windows 啟動器與驗證

- [ ] 4.1 提供純英文 `PhytoTrack.bat`（ASCII `REM`/`echo`），`cmd /c "%~dp0\phytotrack\bin\phytotrack.exe" --spring.profiles.active=prod %*` 起服，避免 `Big5` 中文 `REM` 亂碼，驗證 `Big5` 環境下雙擊正常起服
- [ ] 4.2 執行 `openspec validate --specs --changes` 13+1 passed，`mvn test` 152 passed，`curl http://localhost:8080/` 回前端

