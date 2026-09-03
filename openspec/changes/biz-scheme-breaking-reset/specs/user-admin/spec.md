## ADDED Requirements

### Requirement: 部署僅單管理員與強制改密

`prod` 業務 Scheme SHALL 僅有 `admin/admin123`（`ROLE_ADMIN, active=true`），不含 `staff/viewer`。`POST /api/auth/login` 若 `BCrypt.matches(admin123)` SHALL 回 `mustChangePassword=true`（`200` 附旗標或 `403 MUST_CHANGE_PASSWORD`），前端 SHALL 強制導至改密頁且未改密前除 `PUT /api/account/password` 與登出外其餘 API 回 `403`；改為非 `admin123` 後旗標消失。

#### Scenario: 首次登入提示改密
- **WHEN** 以 `admin/admin123` 於 `prod` 登入
- **THEN** 回 `mustChangePassword=true` 且呼叫 `GET /api/cases` 回 `403` 直至改密

#### Scenario: 改密後旗標消失
- **WHEN** `admin` 呼叫 `PUT /api/account/password` 將 `admin123` 改為新密碼後再登入
- **THEN** `mustChangePassword` 為 `false` 且可正常呼叫業務 API

#### Scenario: 非 admin 不受影響
- **WHEN** `staff` 以非 `admin123` 密碼登入
- **THEN** 無 `mustChangePassword` 旗標且可直接使用

### Requirement: 帳號名稱準則與提權綁定沿用

`user as signer` 名稱 SHALL 取 `displayName`（空則 `username`）而非角色名，提權自動建邏輯維持 signer-overhaul，已有 `signer but not user` 同名撞名 SHALL 走 `409 SIGNER_NAME_CONFLICT` 綁定流程。

#### Scenario: 自動建帳號名
- **WHEN** 新 `staff` `displayName=王小明` 提權
- **THEN** 新建 `identifier=王小明` 而非 `診斷員`

