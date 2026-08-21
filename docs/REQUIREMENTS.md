# 需求總覽（Phase 1）

本文件為 10 份能力契約（`openspec/specs/*/spec.md`）的執行總覽：實作狀態、Phase 1 範圍與能力間依賴。詳細契約以各 spec 為準。

## 能力狀態一覽

| 能力 | spec | 內容摘要 | 狀態 |
|---|---|---|---|
| security-hardening | `security-hardening` | AI 輸出轉義、`open-in-view: false`、JWT fail-fast | ✅ 已實作（Phase 0） |
| api-observability | `api-observability` | 統一錯誤形狀（`details`＋`requestId`）、requestId 進日誌 | ✅ 已實作（僅含已交付項） |
| case-search | `case-search` | 案件列表依作物／服務／送件人／日期區間／狀態篩選 | ✅ 已實作（Phase 1，已 archive） |
| case-lifecycle | `case-lifecycle` | 狀態列舉（PENDING/RESOLVED/CLOSED）、轉移規則、更新契約補全、int→列舉遷移 | ✅ 已實作（Phase 1） |
| case-statistics | `case-statistics` | 統計 API（總數／本月／待處理／topN／比例／趨勢）＋ Dashboard 視圖 | ✅ 已實作（Phase 1） |
| case-report | `case-report` | 案件明細頁、`@media print` 診斷單、CSV 匯出（僅登入） | ✅ 已實作（Phase 1） |
| user-admin | `user-admin` | ADMIN 改角色、啟停用（含既有 token 拒絕）、重設密碼 | ✅ 已實作（Phase 1） |
| reference-data-admin | `reference-data-admin` | ADMIN CRUD 參照資料（被引用拒刪）＋管理頁 | ✅ 已實作（Phase 1） |
| sender-management | `sender-management` | 送件人管理：displayName、去重合併（人工確認）、ADMIN 硬刪除（被引用拒刪）、VIEWER 個資遮蔽（保留縣市鄉鎮）、統計去重鍵 `COALESCE(phone, displayName)` | ✅ 已實作（Phase 1） |
| ops-backup | `ops-backup` | SQLite 帶時間戳備份腳本＋文件 | ❌ Phase 1 |

## Phase 1 範圍

- **8 能力**：case-search、case-lifecycle、case-statistics、case-report、user-admin、reference-data-admin、sender-management、ops-backup
- 每能力一個獨立 OpenSpec change（spec 已在主規格，`skip_specs: true`），建議順序：case-search（已交付）→ case-lifecycle（已交付）→ case-statistics（已交付）→ case-report（已交付）→ user-admin（已交付）→ reference-data-admin（已交付）→ sender-management（已交付）→ ops-backup
- **排除**：security-hardening（已交付）、api-observability 剩餘項（Actuator 精簡、滾動 logback 日誌）歸 **Phase 2**

## 能力間依賴與遷移注意

- **status 列舉（case-lifecycle）為樞紐**：case-search 與 case-statistics 皆依賴 `status`。case-lifecycle 已將 `status` 欄位遷移為 `CaseStatus` 列舉（`@Enumerated(EnumType.ORDINAL)`，既有 `INTEGER 0/1/2` 直接對應 `PENDING`/`RESOLVED`/`CLOSED`，無資料遷移）；API 契約為列舉字串，case-statistics 直接使用即可。
- **建議順序**：case-search（已交付）→ case-lifecycle（已交付）→ case-statistics（已交付）→ case-report（已交付）→ user-admin（已交付）→ reference-data-admin（已交付）→ sender-management（已交付）→ ops-backup
- **更新契約補全（case-lifecycle）**：案件更新已涵蓋純量欄位、送件人（senderName/phone/address/districtId/typeId）、多對多關聯整組替換（damage/hint/pestCategory/identifier）與狀態轉移（PENDING→RESOLVED 需 STAFF/ADMIN，RESOLVED→CLOSED 僅 ADMIN）。
- **統計吃 status**：case-statistics 應於 case-lifecycle 完成後實作，避免重做對映。
- **SQLite**：`status` 以 ORDINAL 儲存 `CaseStatus` 列舉，既有 `INTEGER 0/1/2` 直接對應 `PENDING`/`RESOLVED`/`CLOSED`（無資料遷移，見 case-lifecycle）。
- **送件人唯一鍵**：`senders.name + phone` UNIQUE，測試資料勿撞值。
- **送件人（sender-management）**：`name` 可空、`phone`/`displayName` 至少一必填（ADR-011）；統計去重鍵 `COALESCE(phone, display_name)`；VIEWER 遮蔽姓名／電話／地址但保留縣市鄉鎮，與 case-report 的明細輸出需配合（遮蔽由 Service/投影層做）。

## 登入／JWT 安全審查待辦（2026-08-20）

依 security 技能檢核（OWASP Top 10）對登入、Access Token、帳號狀態的審查結果。分三組：可即修、歸屬能力 change、維持現狀。

### A 可即修（低風險高報酬，尚未實作）

| 項目 | 位置 | 修法 |
|---|---|---|
| DataInitializer 於所有 profile（含 production）以預設密碼建 admin 帳號，`app.bootstrap.*` 硬編碼無法 env 覆寫 | `service/DataInitializer.java`、`application.yaml` | 帳號初始化限定 `@Profile("dev \| test")`（比照 `JwtSecretValidator` 的 fail-fast 精神） |
| 停用帳號登入落 generic handler → 500 | `exception/GlobalExceptionHandler.java` | 新增 `@ExceptionHandler(DisabledException.class)` → 403 `ACCOUNT_DISABLED` |
| BCrypt strength 10（檢核要求 ≥12） | `config/SecurityConfig.java` | `new BCryptPasswordEncoder(12)`（既有 hash 相容） |
| JWT 無 issuer | `security/JwtTokenProvider.java` | `.issuer("phytotrack")` + `requireIssuer` |
| Swagger UI 於所有 profile 公開 | `config/SecurityConfig.java` | 非 dev 關閉 `springdoc.api-docs/swagger-ui.enabled` |
| `X-Request-Id` 客戶端可控、原樣回寫 header/log | `config/RequestIdFilter.java` | 限長度 ≤64 並過濾不可列印字元 |

### B 歸屬能力 change（需人工同意後於對應 change 實作）

- **user-admin**：JWT filter 不查資料庫 → 停用／刪除／降級後既有 token 最長 1 小時仍有效（stale role）；✅ 已於 user-admin 實作 filter 以 `userId` 查 DB 重載 role/active（`security/JwtAuthenticationFilter.java`、`security/UserPrincipal.java`）。
- **sender-management**：VIEWER 目前可讀送件人完整個資（電話／地址）；遮蔽邏輯未實作（`service/CaseService.toDetail`、`controller/CaseController.java`，spec 已 SHALL 要求）。✅ 已於 sender-management 實作 Service 層遮蔽。
- **獨立或 ops**：登入／註冊無 rate limiting（暴力破解、大量註冊），需新依賴（Bucket4j）或閘道層。
- **部署層**：token 存 localStorage（XSS 竊取面，目前無 XSS 注入點故風險低；遷移 httpOnly cookie 屬 auth 流程變更，需恢復 CSRF）；CORS wildcard `*`（allowCredentials=false 無 cookie 風險，改 env 白名單）；無 CSP/HSTS（注意 Swagger inline style 相容）。

### C 維持現狀（審查判定安全）

CSRF off（Bearer header 無 cookie 面）、無狀態登出（前端丟 token）、登入錯誤訊息統一（防帳號列舉）、HS256 簽章（jjwt 固定 HMAC key 無 alg confusion）、`JWT_SECRET` fail-fast、500 泛化訊息不洩內部、`npm audit` 0 漏洞。

## 產出約定

- OpenSpec：`openspec list` / `validate --specs` / `validate --changes`
- 操作手冊：`docs/manual.typ`（`typst compile`）
- 架構：`docs/ARCHITECTURE.md`、ADR 見 `docs/adr/`
