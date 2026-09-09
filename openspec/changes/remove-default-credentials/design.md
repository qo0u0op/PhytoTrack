## Context

`phytotrack.toml.example` 與 `PhytotrackTomlEnvironmentPostProcessor.generateDefaultToml` 目前把 `admin-username`/`admin-password` 等寫入設定檔，`DataInitializer` prod 亦以 `admin123` 建號。使用者要求 prod 仍保留 `admin/admin123` 首啟便利，但設定檔不得含任何帳密可配置項，僅以註釋提醒。

## Goals / Non-Goals

**Goals:**
- `phytotrack.toml` 範例與自動生成零帳密可配置項，僅註釋提醒預設帳密
- prod 首啟仍可用 `admin/admin123`（由程式內建預設，不落地設定檔）

**Non-Goals:**
- 不移除 `DataInitializer` 的 prod 預設建號（維持現行 `admin123` 雜湊建號）
- 不改 `application.yaml` 內建預設值（僅 TOML 層不落地）
- 不做互動輸入或亂數密碼（首啟流程不變）

## Decisions

- **僅清 TOML 可配置項**：`phytotrack.toml.example` 與 `generateDefaultToml` 的 `[app.bootstrap]` 整段改為純註釋（不含任何有效 `admin-username`/`*-password` 行），僅提醒預設帳密與「首次登入後請立即修改」；`loadToml` 仍保留舊帳密讀取以相容既有檔。替代：同步清 `DataInitializer` 與 `application.yaml` → 會破壞首啟便利 → 捨棄。

## Risks / Trade-offs

- [既有 TOML 含明文] 舊檔仍有 `admin123` → Mitigation：`loadToml` 仍讀取，行為不變，文件提示手動刪除或修改
- [掃描誤判] `grep admin123` 於測試/文件歷史段落仍可能命中 → Mitigation：驗證僅限 `backend/phytotrack.toml.example` 與生成檔，無需全庫清掃

## Migration Plan

1. `phytotrack.toml.example` 去密碼 → `generateDefaultToml` 同步 → `mvn test` → `grep` 驗證
2. 文件同步（DEPLOY/README/manual.typ 移除 TOML 明文範例）→ `openspec validate`
3. 回滾：恢復兩檔密碼行即回舊行為

## Open Questions

- 無
