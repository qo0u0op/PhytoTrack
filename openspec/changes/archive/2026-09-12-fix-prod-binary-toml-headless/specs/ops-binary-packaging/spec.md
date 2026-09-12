# ops-binary-packaging Delta

## MODIFIED Requirements

### Requirement: jpackage 矩陣與可攜分發

`release.yml` SHALL 僅含 `windows-latest:app-image` 與 `ubuntu-latest:app-image` 雙矩陣（皆 `jpackage --type app-image` 無平台專參），**兩者皆以 `--java-options -Dspring.profiles.active=prod` 嵌入 prod**（未來 `deb/rpm` 等 distro 打包的 `jpackage` 基座同樣嵌入），Windows 後以 `Compress-Archive` 轉 `dist/PhytoTrack-$VERSION-win.zip`，Linux 後以 `appimagetool` 轉 `AppImage`；`exe` 安裝版 / `deb` 直出 / `macos` 已放棄。**Binary 交付物首次啟動必為 prod，不提供切回 `dev` 的官方路徑（文件與 spec 明確 prod-only）。**

#### Scenario: Windows 可攜

- **WHEN** `matrix.type=app-image` 於 `windows-latest`
- **THEN** `jpackage --type app-image --name phytotrack` 成功且產 `dist/phytotrack`，後打包為 `dist/PhytoTrack-$VERSION-win.zip`（內含 `phytotrack.exe`，`--java-options -Dspring.profiles.active=prod` 已注入）

#### Scenario: Linux AppImage 前置

- **WHEN** `matrix.type=app-image` 於 `ubuntu-latest`
- **THEN** `jpackage --type app-image` **含 `--java-options -Dspring.profiles.active=prod`**，不帶 `--linux-*`/`--win-*`，產 `dist/phytotrack` 供後續 `appimagetool`

#### Scenario: Binary 直跑亦為 prod

- **WHEN** 解壓後直接執行 `dist/phytotrack/bin/phytotrack`（不經 `AppRun`）
- **THEN** 仍以 `prod` 啟動（`jpackage` 已嵌入），`phytotrack.toml` 首次生成為 prod 預設

#### Scenario: 已移除

- **WHEN** 推送 `tag v*`
- **THEN** 不再觸發 `exe` / `deb` / `macos` 矩陣，不再產生 `PhytoTrack.bat`
