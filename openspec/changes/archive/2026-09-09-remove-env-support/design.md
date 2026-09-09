## Context

設定已收斂至 `phytotrack.toml`（`PhytotrackTomlEnvironmentPostProcessor` 載入並於首次生成），但 `application.yaml` 仍保留 `${ENV_VAR:default}` 作為備援，且文件仍以 env 為主要說明，與單一真相源衝突。

## Goals / Non-Goals

**Goals:**
- 應用層零 `System.getenv`（`BinaryPaths` 的 XDG/AppImage 除外，屬 OS 契約）
- `application.yaml` 純預設，不含 `${ENV}`；所有覆寫經 `phytotrack.toml`
- 文件一致指向 TOML 鍵

**Non-Goals:**
- 不移除 `BinaryPaths` 的 XDG/AppImage/OWD 讀取（OS 路徑，非應用 env）
- 不改資料庫或 API 行為

## Decisions

- **硬編碼預設而非 env**：`app.jwt.secret` 回 `phytotrack-dev-secret-please-change-in-production-0123456789` 明文、`app.bootstrap.*` 回內建預設，`spring.ai.openai.*` 回 `http://localhost:11435` 等。替代：保留 `${ENV:default}` → 設定分散 → 捨棄。
- **刪除 AI_API_KEY env 覆蓋**：`PhytotrackTomlEnvironmentPostProcessor.loadToml` 刪除 `System.getenv("AI_API_KEY")` 分支，`.env` WARN 一併刪除。保留 `BinaryPaths` 的 XDG/AppImage 因屬系統路徑，非應用 env。
- **提示文案改 TOML**：`JwtSecretValidator` 改提示 `phytotrack.toml:app.jwt.secret`。

## Risks / Trade-offs

- [既有部署依賴 env] 以 `JWT_SECRET` 啟動者將失效 → Mitigation：**BREAKING**，文件與 Release notes 明示遷移至 `phytotrack.toml`
- [容器編排] 無 env 即需掛載 TOML → Mitigation：`phytotrack.toml` 支援 `/etc/phytotrack/phytotrack.toml` 系統級（XDG 已處理），容器可 volume mount

## Migration Plan

1. `application.yaml` 去 `${ENV}` → `PhytotrackTomlEnvironmentPostProcessor` 刪 env 分支 → `JwtSecretValidator` 文案 → 文件批次替換
2. `mvn test`、`openspec validate`、`grep -r "JWT_SECRET\|CORS_ALLOWED_ORIGINS\|AI_API_KEY" backend/src/main docs/` 為 0（XDG 除外）
3. 回滾：恢復 `${ENV:default}` 即回 env 支援

## Open Questions

- 無
