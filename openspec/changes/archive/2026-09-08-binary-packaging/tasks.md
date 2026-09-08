## 1. 前置建置收斂

- [x] 1.1 將 `mise.toml:package` 精簡為僅 `frontend npm run build → cp dist → backend/src/main/resources/static` 與 `backend mvn package -DskipTests`，不含 `jpackage`，驗證 `mise run package` 產 `backend/target/phytotrack-0.0.1-SNAPSHOT.jar` 且 `static` 含 `index.html`

## 2. Release 矩陣與參數分流（雙 app-image，棄 exe）

- [x] 2.1 精簡 `.github/workflows/release.yml:11` 為雙矩陣 `windows:app-image` 與 `ubuntu:app-image`（皆 `--type app-image` 無平台專參），Windows 後以 `Compress-Archive` 轉 `PhytoTrack-$VERSION-win.zip`，`Build frontend/backend/Run jpackage` 皆 `shell: bash`，驗證 `rm -rf` 綠燈且不再觸發 `exe/deb/macos`
- [x] 2.2 `Run jpackage` 內 `VERSION` 去 `v`、轉點分、截 3 段並前導 `0→1` 修正（`0.0.1 → 1.0.1`），`zip` 與 `app-image` 共用，驗證 `tag v0.0.1-2` 得 `1.0.1` 雙矩陣綠燈

## 3. AppImage 可攜、直接進托盤與 SystemTray（真實圖示）

- [x] 3.1 `ubuntu app-image` 後以 `appimagetool` 將 `dist/phytotrack` 轉 `dist/PhytoTrack-$VERSION-x86_64.AppImage`，`AppRun` 預設 `prod`（可透傳 `spring.profiles.active`）且 `Terminal=false` 直接進托盤，優先複製 `docs/img/icon.svg/.png` 真實圖示（缺失回落 `base64 -d` 1×1），驗證本地可封為 `AppImage` 並雙擊進托盤
- [x] 3.2 整合 `dorkbox/SystemTray`（`com.dorkbox:SystemTray:4.1`）於後端（`BrowserOpener` 旁 `SystemTrayManager`），提供托盤 `開啟 PhytoTrack` / `備份資料庫` / `開啟資料夾` / `開啟日誌資料夾` / `退出`（含備用 Swing 視窗 5 按鈕），驗證 `AppImage` 與 Windows `phytotrack.exe` 於有托盤桌面可見圖示與選單（`tray-icon.png/.svg`）
- [x] 3.3 `Upload installer` 與 `release` 合併多 `dist/*` 發布至 `softprops/action-gh-release@v2`，驗證 `tag v0.0.1-x` 後 Releases 含 `PhytoTrack-*.zip` 與 `PhytoTrack-*.AppImage`

## 4. 驗證

- [x] 4.1 已放棄 `PhytoTrack.bat`（改 `jpackage --java-options -Dspring.profiles.active=prod` 注入 + 直接執行 `phytotrack.exe`），`Big5` 問題隨之移除
- [x] 4.2 執行 `openspec validate --specs --changes` 15 passed，`mvn test` 綠燈（146 passed），`curl http://localhost:8080/` 回前端 — 已驗證，待封存
