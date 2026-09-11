## 1. 範例檔手冊化

- [x] 1.1 重寫 `backend/phytotrack.toml.example` 檔頭為手冊（四路徑契約、優先順序、首次生成冪等、保存起點 `cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml`、索引至 `docs/DEPLOY.md#`），驗證 `head -25` 含 `Windows 可攜`、`XDG`、`AppImage` 關鍵字
- [x] 1.2 為每節（`[server]`、`[app.jwt]`、`[app.bootstrap]`、`[ai]`、`[ai.headers]`、`[app.cors]`、`[app.rate-limit]`、`[app.security-headers]`、`[app.tray]`、`[app.ui]`、`[springdoc]`）補節級說明塊（2-4 行），驗證 `grep -c "^\#.*\[.*\]"` 節數 ≥11
- [x] 1.3 為每欄位補五要素註釋（用途/類型/預設/取值/生效），含 `app.jwt.secret` 亂數警告、`ai.api-key` env 覆蓋、`app.bootstrap` 不可配約束，驗證逐欄 `grep -B1 "port =\|secret =\|provider =\|tray.*enabled"` 上一行含 `類型|預設|取值` 關鍵字
- [x] 1.4 補齊缺漏節與固定順序（`app.tray`、`app.ui.auto-open-browser/dev-frontend-url`、`springdoc`、`spring.datasource/logging`），驗證節順序與 `loadToml` 可讀鍵一致，無遺漏 `grep` 檢查

## 2. 生成檔同源

- [x] 2.1 同步 `PhytotrackTomlEnvironmentPostProcessor.generateDefaultToml()` 內嵌模板與範例一致（僅 `secret` 隨機），驗證 `diff -u <(sed '/secret =/d' phytotrack.toml.example) <( sed '/secret =/d' 生成字串)` 差異 <5 行
- [x] 2.2 執行 `rm -f /tmp/phytotrack-test.toml && mvn -Dtest=PhytotrackTomlEnvironmentPostProcessorTest test` 或手動觸發首次生成，驗證生成檔 `grep -c "^#"` 與範例差異 <5 且含 `[app.tray]`

## 3. 文件收斂

- [x] 3.1 更新 `docs/DEPLOY.md` 配置章節為「以檔內手冊為準」指向 `phytotrack.toml.example`，移除重複欄位表，驗證 `grep -n "phytotrack.toml.example"` 存在且無舊欄位表殘留
- [x] 3.2 更新 `docs/ARCHITECTURE.md` 中 TOML 引用為一句「單一真相、落點見檔內手冊」，驗證 `grep -n "phytotrack.toml"` 僅 1-2 處且無表格重複

## 4. 驗證

- [x] 4.1 執行 `openspec validate --specs --changes --strict` 通過，驗證 delta spec `ops-binary` 的 5 scenarios 可追溯
- [x] 4.2 執行 `mvn test -Dtest=CaseControllerTest`（或全量 `mvn test`）綠燈且無 TOML 解析錯誤，驗證註釋未破壞 `TOML` 語法（`#` 起始、無未閉合字串）
