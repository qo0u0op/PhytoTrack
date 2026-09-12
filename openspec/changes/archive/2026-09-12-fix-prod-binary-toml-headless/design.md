# Design: fix-prod-binary-toml-headless

## Context

見 `proposal.md - Why`。現況：`release.yml` 僅 Windows `jpackage` 嵌入 prod；`TomlGenerator.generateDefaultToml(path, isProd)` 未使用 `isProd`，生成檔與 `phytotrack.toml.example` 同為 dev 傾向（`security-headers=false, springdoc=true`）；`DesktopEnvironment.hasDisplay()` 對 Windows 無條件 true，若呼叫點未先查 `isSsh` 會誤判 linux→windows server 的 SSH 場景為有頭。

## Goals / Non-Goals

**Goals:**

- 所有 binary 交付物（AppImage / win exe / 未來 deb/rpm 的 `jpackage app-image` 基座）本身即 prod，無需依賴 `AppRun` 包裝
- 首次生成的 `phytotrack.toml` 在 `isProd=true` 時自動為 prod 值（patch），`isProd=false` 時保持 example 原值，`spring-boot:run dev/test` 行為不變
- 有頭配置在 SSH（含 Windows SSH）時臨時 headless，不寫盤，下次 GUI 仍有頭

**Non-Goals:**

- 不提供 binary 切回 dev 的官方路徑（文件與 spec 明確 prod-only，僅透傳參數技術上可覆蓋但不支援）
- 不重做 `BinaryPaths` 三模式目錄契約，僅補 `jpackage` 參數

## Decisions

### D1: jpackage 全平台嵌入 prod

- 選擇：`release.yml` 的 `Run jpackage` 中將 `EXTRA_JAVA="--java-options -Dspring.profiles.active=prod"` 從僅 Windows 改為全平台（`windows-latest` 與 `ubuntu-latest` 皆注入），未來 `deb/rpm` 矩陣同樣注入
- 替代：僅靠 `AppRun` 注入 prod — 已驗證 `dist/phytotrack/bin/phytotrack` 直跑會落回 dev，違反「binary 必須 prod」
- 影響：`AppRun` 的 `grep spring.profiles.active` 透傳邏輯保留，作為顯式覆蓋時不強制 prod 的逃生口，但預設已 prod，無需再依賴它

### D2: TomlGenerator isProd patch（覆蓋 example）

- 選擇：`generateDefaultToml` 以 `phytotrack.toml.example` 模板為基底（`TomlGenerator` 內嵌字串與 example 同源），當 `isProd=true` 時 patch：
  - `[app.security-headers] enabled false → true`
  - `[springdoc] api-docs-enabled true → false`, `swagger-ui-enabled true → false`
  - 其餘（`[app.tray] true, [app.ui] true, [ai] enabled true` 等）保持不變；`app.jwt.secret` 仍亂數
- 替代 A：範例本身改 prod — 會使 `spring-boot:run dev` 首次生成也 prod，不符本地開發預期
- 替代 B：TOML 留空靠 `application.yaml` 的 `on-profile: prod` 覆蓋 — 但 TOML 以 `addFirst` 高優先，會蓋掉 yaml 的 prod 覆蓋，需額外特判
- 實作：`TomlGenerator` 內以 `isProd` 三元決定字串片段，或後處理 `String.replace`，保持 `grep -c "^#"` 差異可控

### D3: 有頭→無頭臨時回落（不持久化）

- 選擇：配置保持 GUI 預設；執行期由 `DesktopEnvironment.isHeadlessOrSsh()` 統一判斷，`PhytoTrackApplication.main` 先設 `java.awt.headless`，`BrowserOpener`/`SystemTrayManager` 以 `isSsh()` / `isHeadlessOrSsh()` 先行 return，不修改 TOML
- 關鍵：`DesktopEnvironment.hasDisplay()` 的 Windows 快捷僅在非 SSH 時視為有頭；所有有頭判定必須經 `isHeadlessOrSsh()` 或至少先查 `isSsh()`，禁止單查 `hasDisplay()`
- 診斷：`SystemTrayManager` 與 `BrowserOpener` 的 `log.info` 保留 `os / isSsh / hasDisplay / headlessProp / DISPLAY / WAYLAND_DISPLAY`，補 `isSsh` 優先語意於註解

### D4: Windows SSH 判斷

- `isSsh()` 以 `SSH_CONNECTION || SSH_CLIENT || SSH_TTY` 為準，Windows OpenSSH Server 同樣會注入這些變數，足以識別 `linux → windows server` 的 SSH 會話
- `isHeadlessOrSsh()` 順序：`isSsh() → (!hasDisplay() && !isWindows()) → GraphicsEnvironment.isHeadless()`，確保 Windows SSH 在第一步即回 true，不被 `hasDisplay()=true` 誤導
- 未來若需更強識別，可增 `SESSIONNAME` 或 `WT_SESSION` 等 Windows 終端變數，但本期不引入，僅以 SSH 三變數為準

## Risks / Trade-offs

- [Binary prod 嵌入] → AppImage 的 `AppRun` 透傳 `dev` 仍可覆蓋 prod，文件需明確「不支援切 dev」避免使用者誤用
- [isProd patch] → 範例與生成檔在 prod 下不再「同源一致」（本期改 spec 為「example 為基底，isProd 時 patch」），需同步更新 `ops-binary` 的「同源」Scenario 文案
- [臨時回落] → SSH 環境下 `tray/ui` 配置雖為 true 但行為為 false， Heads-up：日誌需足夠診斷，否則使用者會困惑為何托盤未出現
- [Windows SSH] → 若 Windows SSH Server 未注入 SSH env（少見的第三方 SSHD），回落會失效，屆時 `GraphicsEnvironment.isHeadless()` 為最後防線

## Migration Plan

1. 改 `release.yml`：全平台 `EXTRA_JAVA` 注入 prod
2. 改 `TomlGenerator`：`isProd` patch 邏輯 + 單測 `TomlGeneratorTest` 補 prod/dev 兩分支
3. 改 `DesktopEnvironment`/`PhytoTrackApplication`/`BrowserOpener`/`SystemTrayManager`：統一 `isSsh` 優先判斷，補註解與日誌
4. 同步 `ops-binary` / `ops-binary-packaging` spec 與 `docs/DEPLOY.md` / `ARCHITECTURE.md` / `phytotrack.toml.example` 註釋
5. 驗證：`mvn test`、`rm -rf config/phytotrack.toml && java -Dspring.profiles.active=prod -jar backend/target/*.jar` 首次生成檢查、`SSH_CONNECTION=1 java -jar` 臨時回落檢查、`typst compile` 與 `openspec validate`

## Open Questions

- deb/rpm 未來是否同樣走 `jpackage app-image` 基座再打包，抑或走 `jpackage --type deb/rpm` 直出？本期僅約束「所有 jpackage 交付物皆嵌入 prod」，具體 type 待後續 change 決定。
