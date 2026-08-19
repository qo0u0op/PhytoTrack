# 需求總覽（Phase 1）

本文件為 9 份能力契約（`openspec/specs/*/spec.md`）的執行總覽：實作狀態、Phase 1 範圍與能力間依賴。詳細契約以各 spec 為準。

## 能力狀態一覽

| 能力 | spec | 內容摘要 | 狀態 |
|---|---|---|---|
| security-hardening | `security-hardening` | AI 輸出轉義、`open-in-view: false`、JWT fail-fast | ✅ 已實作（Phase 0） |
| api-observability | `api-observability` | 統一錯誤形狀（`details`＋`requestId`）、requestId 進日誌 | ✅ 已實作（僅含已交付項） |
| case-search | `case-search` | 案件列表依作物／服務／送件人／日期區間／狀態篩選 | ✅ 已實作（Phase 1） |
| case-lifecycle | `case-lifecycle` | 狀態列舉（PENDING/RESOLVED/CLOSED）、轉移規則、更新契約補全、int→列舉遷移 | ❌ Phase 1 |
| case-statistics | `case-statistics` | 統計 API（總數／本月／待處理／topN／比例／趨勢）＋ Dashboard 視圖 | ❌ Phase 1 |
| case-report | `case-report` | 案件明細頁、`@media print` 診斷單、CSV 匯出（僅登入） | ❌ Phase 1 |
| user-admin | `user-admin` | ADMIN 改角色、啟停用（含既有 token 拒絕）、重設密碼 | ❌ Phase 1 |
| reference-data-admin | `reference-data-admin` | ADMIN CRUD 參照資料（被引用拒刪）＋管理頁 | ❌ Phase 1 |
| ops-backup | `ops-backup` | SQLite 帶時間戳備份腳本＋文件 | ❌ Phase 1 |

## Phase 1 範圍

- **7 能力**：case-search、case-lifecycle、case-statistics、case-report、user-admin、reference-data-admin、ops-backup
- 每能力一個獨立 OpenSpec change（spec 已在主規格，`skip_specs: true`），建議順序：case-search → case-lifecycle → case-statistics → case-report → user-admin → reference-data-admin → ops-backup
- **排除**：security-hardening（已交付）、api-observability 剩餘項（Actuator 精簡、滾動 logback 日誌）歸 **Phase 2**

## 能力間依賴與遷移注意

- **status 列舉（case-lifecycle）為樞紐**：case-search 與 case-statistics 皆依賴 `status`。case-search 已實作列舉字串（`PENDING`/`RESOLVED`/`CLOSED`）對映既有 `INTEGER`（`0/1/2`，驗證與對照集中於 `CaseService`，`CaseSpecifications` 僅組裝純 SQL 條件）；case-lifecycle 將欄位遷移為列舉後，僅需移除對映，API 契約不變。
- **建議順序**：case-search（已交付）→ case-lifecycle → case-statistics（應於 case-lifecycle 後實作，避免重做對映）→ case-report → user-admin → reference-data-admin → ops-backup
- **更新契約補全（case-lifecycle）**：目前案件更新僅處理純量欄位；送件人、多對多關聯（damage/hint/pestCategory/identifier）與狀態轉移待 case-lifecycle 補全。
- **統計吃 status**：case-statistics 應於 case-lifecycle 完成後實作，避免重做對映。
- **SQLite**：既有資料 `status INTEGER NOT NULL DEFAULT 0`，遷移至列舉時 `0 → PENDING`（見 case-lifecycle spec）。
- **送件人唯一鍵**：`senders.name + phone` UNIQUE，測試資料勿撞值。

## 產出約定

- OpenSpec：`openspec list` / `validate --specs` / `validate --changes`
- 操作手冊：`docs/manual.typ`（`typst compile`）
- 架構：`docs/ARCHITECTURE.md`、ADR 見 `docs/adr/`
