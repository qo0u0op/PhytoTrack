## Why

`phytotrack.toml` 與其範例/自動生成模板目前含明文預設密碼（`admin-password = "admin123"` 等 4 處），任何人拿到設定檔即知管理員憑證，屬設定檔洩漏風險。prod 仍需保留 `admin/admin123` 首啟便利（資料庫為空時可用），但明文不得落地於設定檔。

## What Changes

- **TOML 不再帶帳密配置**：`phytotrack.toml.example` 的 `[app.bootstrap]` 段落不再提供可配置項，僅以註釋提醒預設帳密（admin/admin123 等由程式內建）與「首次登入後請立即修改」；`PhytotrackTomlEnvironmentPostProcessor.generateDefaultToml` 亦僅生成註釋段落，不寫入任何 `*-username`/`*-password`
- **prod 預設帳密維持但不落地**：`DataInitializer` prod 分支維持 `admin/admin123` 首次建號邏輯（資料庫為空時），`application.yaml` 的 `app.bootstrap.*` 預設值維持供後端內部使用；`loadToml` 若讀到舊 TOML 帳密仍可讀取（相容），但新生成/範例不再提供可配置項
- **文件同步**：`docs/DEPLOY.md` 配置表、`README.md` 快速啟動、`docs/manual.typ` 預設帳號段落移除 TOML 明文範例，改為註解範例並說明「首啟後請立即修改密碼」

## Capabilities

### New Capabilities
<!-- 無 -->

### Modified Capabilities
- `security-hardening`: 設定檔不得含帳密配置 — `phytotrack.toml` 範例與自動生成僅以註釋提醒預設帳密，不提供可配置項；prod 首建帳密行為不變

## Impact

- 後端：`config/PhytotrackTomlEnvironmentPostProcessor.java:generateDefaultToml`、`phytotrack.toml.example`（僅此兩處）
- 不改：`service/DataInitializer.java` prod 建號、`application.yaml` 預設值（維持內建預設，僅不落地於 TOML）
- 測試：`grep -r admin123 backend/phytotrack.toml.example` 應為空；`mvn test` 行為不變（仍以內建預設登入）
- 部署：非 BREAKING — 既有 `phytotrack.toml` 含密碼者仍可讀取；新部署不再產生明文密碼檔
