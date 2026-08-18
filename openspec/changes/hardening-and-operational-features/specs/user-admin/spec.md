## Purpose

讓管理者可調整使用者角色、啟停用帳號與重設密碼，形成完整的帳號管理能力。

## ADDED Requirements

### Requirement: 管理者調整角色

ADMIN SHALL 可變更使用者角色，變更後該使用者的權限於後續請求生效。

#### Scenario: 變更使用者角色
- **WHEN** ADMIN 將使用者調整為 STAFF
- **THEN** 該使用者於後續請求取得 STAFF 權限

### Requirement: 啟停用帳號

ADMIN SHALL 可停用或啟用帳號；停用帳號 SHALL 無法登入，且其既有 token 於後續請求 SHALL 被拒絕。

#### Scenario: 停用後嘗試登入
- **WHEN** 帳號被停用後嘗試登入
- **THEN** 登入失敗並提示帳號已停用

#### Scenario: 停用後使用既有 token
- **WHEN** 帳號被停用後使用既有 token 呼叫受保護 API
- **THEN** 請求遭拒

### Requirement: 管理者重設密碼

ADMIN SHALL 可為使用者重設密碼，重設後該使用者可用新密碼登入。

#### Scenario: 重設使用者密碼
- **WHEN** ADMIN 重設使用者密碼
- **THEN** 該使用者可用新密碼成功登入