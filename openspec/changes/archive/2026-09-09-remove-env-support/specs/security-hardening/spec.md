## MODIFIED Requirements

### Requirement: JWT 密鑰 fail-fast

非 dev profile 啟動時若仍使用開發預設密鑰，應用程式 SHALL 於啟動階段失敗並提示需於 `phytotrack.toml` 的 `app.jwt.secret` 提供正式密鑰（不再經由環境變數 `JWT_SECRET`）。

#### Scenario: 以預設密鑰啟動非 dev 環境
- **WHEN** 以 production profile 啟動且 `phytotrack.toml` 的 `app.jwt.secret` 仍為開發預設
- **THEN** 應用程式啟動失敗，並明確提示於 `phytotrack.toml` 設定正式密鑰

### Requirement: 生產環境預設帳號覆寫

生產部署 SHALL 於 `phytotrack.toml` 的 `app.bootstrap` 與 `app.jwt.secret` 覆寫開發預設，不再經由環境變數 `ADMIN_USERNAME/ADMIN_PASSWORD/JWT_SECRET` 覆寫；啟動期若仍為預設密鑰 SHALL fail-fast。

#### Scenario: 生產以環境變數覆寫
- **WHEN** 於 `phytotrack.toml` 設定 `app.jwt.secret` 與 `app.bootstrap.admin-username` 等
- **THEN** 預設 `admin:admin123` 與開發密鑰不再生效，新設定值方可登入與簽章
