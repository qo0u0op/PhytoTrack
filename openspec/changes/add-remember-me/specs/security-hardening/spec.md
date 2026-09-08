## ADDED Requirements

### Requirement: 記住我雙時效 JWT

系統 SHALL 支援 `rememberMe` 分支的 JWT 時效：`POST /api/auth/login` 帶 `rememberMe=true` 時簽發長效 token（`app.jwt.remember-me-expiration-ms`，預設 7 天 = 604800000 ms），未帶或 `false` 時維持 `app.jwt.expiration-ms`（預設 1 小時 = 3600000 ms）。兩時效 SHALL 皆符合 `issuer=phytotrack` 簽章與 `BCrypt(12)` 密碼校驗既有約束，長效時間 MUST 可由 `JWT_REMEMBER_ME_EXPIRATION_MS` / `phytotrack.toml` 的 `app.jwt.remember-me-expiration-ms` 覆蓋。

#### Scenario: 勾選記住我取得長效 token
- **WHEN** 使用者以正確帳密與 `{"rememberMe": true}` 呼叫 `POST /api/auth/login`
- **THEN** 回 `200` 且 `token` 的 `exp - iat` 約為 7 天（容差 ±60s），且 `GET /api/cases` 等受保護 API 於 6 天後仍可驗證通過

#### Scenario: 未勾選維持短效
- **WHEN** 使用者以 `{"rememberMe": false}` 或省略該欄位呼叫 `POST /api/auth/login`
- **THEN** `token` 的 `exp - iat` 約為 1 小時，且 1 小時後驗證失敗（401）

#### Scenario: 長效可由設定覆蓋
- **WHEN** 以 `app.jwt.remember-me-expiration-ms=259200000`（3 天）啟動並以 `rememberMe=true` 登入
- **THEN** `token` 時效為 3 天而非 7 天

#### Scenario: 錯誤帳密仍拒絕
- **WHEN** 以錯誤密碼與 `rememberMe=true` 呼叫登入
- **THEN** 回 `401 BAD_CREDENTIALS`，不簽發 token
