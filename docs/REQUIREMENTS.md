# 需求總覽（Phase 1）

本文件為 10 份能力契約（`openspec/specs/*/spec.md`）的執行總覽：實作狀態、Phase 1 範圍與能力間依賴。詳細契約以各 spec 為準。

## 能力狀態一覽

| 能力 | spec | 內容摘要 | 狀態 |
|---|---|---|---|
| security-hardening | `security-hardening` | AI 輸出轉義、`open-in-view: false`、JWT fail-fast | ✅ 已實作（Phase 0） |
| api-observability | `api-observability` | 統一錯誤形狀（`details`＋`requestId`）、requestId 進日誌 | ✅ 已實作（僅含已交付項） |
| case-search | `case-search` | 案件列表依作物／服務／送件人／日期區間／狀態篩選 | ✅ 已實作（Phase 1，已 archive） |
| case-lifecycle | `case-lifecycle` | 狀態列舉（PENDING/RESOLVED/CLOSED）、轉移規則、更新契約補全、int→列舉遷移 | ✅ 已實作（Phase 1） |
| case-statistics | `case-statistics` | 統計 API（總數／本月／待處理／topN／比例／趨勢）＋ Dashboard 視圖 | ❌ Phase 1 |
| case-report | `case-report` | 案件明細頁、`@media print` 診斷單、CSV 匯出（僅登入） | ❌ Phase 1 |
| user-admin | `user-admin` | ADMIN 改角色、啟停用（含既有 token 拒絕）、重設密碼 | ❌ Phase 1 |
| reference-data-admin | `reference-data-admin` | ADMIN CRUD 參照資料（被引用拒刪）＋管理頁 | ❌ Phase 1 |
| sender-management | `sender-management` | 送件人管理：displayName、去重合併（人工確認）、ADMIN 硬刪除（被引用拒刪）、VIEWER 個資遮蔽（保留縣市鄉鎮）、統計去重鍵 `COALESCE(phone, displayName)` | ❌ Phase 1（規劃中，見 ADR-011） |
| ops-backup | `ops-backup` | SQLite 帶時間戳備份腳本＋文件 | ❌ Phase 1 |

## Phase 1 範圍

- **8 能力**：case-search、case-lifecycle、case-statistics、case-report、user-admin、reference-data-admin、sender-management、ops-backup
- 每能力一個獨立 OpenSpec change（spec 已在主規格，`skip_specs: true`），建議順序：case-search（已交付）→ case-lifecycle（已交付）→ case-statistics → case-report → user-admin → reference-data-admin → sender-management → ops-backup
- **排除**：security-hardening（已交付）、api-observability 剩餘項（Actuator 精簡、滾動 logback 日誌）歸 **Phase 2**

## 能力間依賴與遷移注意

- **status 列舉（case-lifecycle）為樞紐**：case-search 與 case-statistics 皆依賴 `status`。case-lifecycle 已將 `status` 欄位遷移為 `CaseStatus` 列舉（`@Enumerated(EnumType.ORDINAL)`，既有 `INTEGER 0/1/2` 直接對應 `PENDING`/`RESOLVED`/`CLOSED`，無資料遷移）；API 契約為列舉字串，case-statistics 直接使用即可。
- **建議順序**：case-search（已交付）→ case-lifecycle（已交付）→ case-statistics → case-report → user-admin → reference-data-admin → sender-management → ops-backup
- **更新契約補全（case-lifecycle）**：案件更新已涵蓋純量欄位、送件人（senderName/phone/address/districtId/typeId）、多對多關聯整組替換（damage/hint/pestCategory/identifier）與狀態轉移（PENDING→RESOLVED 需 STAFF/ADMIN，RESOLVED→CLOSED 僅 ADMIN）。
- **統計吃 status**：case-statistics 應於 case-lifecycle 完成後實作，避免重做對映。
- **SQLite**：`status` 以 ORDINAL 儲存 `CaseStatus` 列舉，既有 `INTEGER 0/1/2` 直接對應 `PENDING`/`RESOLVED`/`CLOSED`（無資料遷移，見 case-lifecycle）。
- **送件人唯一鍵**：`senders.name + phone` UNIQUE，測試資料勿撞值。
- **送件人（sender-management）**：`name` 可空、`phone`/`displayName` 至少一必填（ADR-011）；統計去重鍵 `COALESCE(phone, display_name)`；VIEWER 遮蔽姓名／電話／地址但保留縣市鄉鎮，與 case-report 的明細輸出需配合（遮蔽由 Service/投影層做）。

## 產出約定

- OpenSpec：`openspec list` / `validate --specs` / `validate --changes`
- 操作手冊：`docs/manual.typ`（`typst compile`）
- 架構：`docs/ARCHITECTURE.md`、ADR 見 `docs/adr/`
