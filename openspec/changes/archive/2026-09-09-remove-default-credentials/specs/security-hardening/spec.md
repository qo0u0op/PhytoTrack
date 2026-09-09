## MODIFIED Requirements

### Requirement: 帳號初始化

系統 SHALL 依 profile 決定初始帳號：`dev`/`test` 建立 `admin`/`staff`/`viewer` 三帳號，`prod`（含 binary）僅建立 `admin` 單一帳號與其簽名人；`staff`/`viewer` 於 prod 不自動建立。`prod` 首次啟動資料庫為空時 SHALL 仍可使用內建預設 `admin/admin123` 建號並以 `BCrypt(12)` 儲存（行為不變），但該預設帳密 SHALL 不以可配置項出現在任何 `phytotrack.toml` 設定檔（僅以註釋提醒）。

#### Scenario: dev/test 三帳號
- **WHEN** 以 `dev` 或 `test` 啟動且無既有帳號
- **THEN** `admin`、`staff`、`viewer` 皆自動建立

#### Scenario: production 僅 admin
- **WHEN** 以 `prod` 啟動且無既有帳號
- **THEN** 僅 `admin` 自動建立（`staff`/`viewer` 不建立），且僅 `admin` 擁有簽名人

## ADDED Requirements

### Requirement: 設定檔不得含帳密配置

`phytotrack.toml.example` 與首次自動生成的 `phytotrack.toml` SHALL 不包含任何 `app.bootstrap` 可配置項（`admin-username` / `*-password` 等皆不以有效配置行出現）；`[app.bootstrap]` 段落 SHALL 僅以註釋提醒預設帳密（admin/admin123 等由程式內建）與「首次登入後請立即修改」。`application.yaml` 的內建預設值可保留供後端內部使用，但 TOML 層 SHALL 不提供帳密配置行。既有 TOML 含舊帳密者 SHALL 仍可讀取（相容），但新生成檔 SHALL 不含任何有效帳密配置。

#### Scenario: 範例檔僅註釋提醒
- **WHEN** 檢視 `backend/phytotrack.toml.example`
- **THEN** `grep -E "^admin-username|^admin-password|^staff-username|^viewer-username" backend/phytotrack.toml.example` 無結果，且含「帳號密碼不可在設定檔配置」註釋

#### Scenario: 自動生成僅註釋
- **WHEN** 刪除既有 `phytotrack.toml` 後首次啟動
- **THEN** 生成檔含亂數 `app.jwt.secret` 但 `[app.bootstrap]` 段落不含任何有效 `*-username`/`*-password` 行（僅註釋）
