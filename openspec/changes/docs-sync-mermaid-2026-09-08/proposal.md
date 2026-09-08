## Why

`openspec/specs` 已演進至 `ui-theme`（深色模式）、`remember-me`（雙時效 JWT）、`bootstrap` 去明文（`phytotrack.toml` 僅註釋）與 `dev-browser`（dev 開 `:5173`），但 `docs/ARCHITECTURE.md`、`docs/DEPLOY.md`、`docs/REQUIREMENTS.md`、`docs/manual.typ`/`diagnoses.typ` 與 `docs/d2/diagnoses.d2`/`docs/img/diagnoses.svg` 仍停留在 2026-09-04 前後狀態，資料模型、JWT 設定與前端主題等圖文與實作脫節，需一次性同步並以 mermaid 補齊可維護的 ER/架構圖。

## What Changes

- **Markdown 同步**：`docs/ARCHITECTURE.md` 補 `stores/theme.ts` 與 `data-bs-theme`、雙時效 JWT（`expiration-ms`/`remember-me-expiration-ms`）、`BinaryPaths` 三模式與 `BrowserOpener` dev 分流、`app.bootstrap` 僅註釋；`docs/DEPLOY.md` 改 `app.bootstrap` 表為「內建預設不在 TOML 配置」；`docs/REQUIREMENTS.md` 補 `ui-theme` 能力與 `security-hardening` 設定檔條目；`README.md` 投影片/stack 與快速啟動提示同步
- **Typst 同步**：`docs/manual.typ` 操作步驟增「記住我」勾選與登出保留帳號、`dark mode` 切換、`dev :5173` 說明；`docs/diagnoses.typ` 紙本欄位已於前期完成，僅驗證 `typst compile` 綠燈
- **圖檔同步（外圖用 D2）**：`docs/d2/diagnoses.d2` 更新欄位（`field_district_id`、`sender.district_id` 可空策略、`remember-me`/`ui-theme` 不落庫註記）、`identifier.former_user_id`、`v_case_search` 17 欄註記，執行 `mise run d2` 重建 `docs/img/diagnoses.svg`（外部引用的唯一圖源）
- **Mermaid 增補（MD 內圖用 Mermaid）**：`docs/ARCHITECTURE.md` 以 ` ```mermaid` 內聯 ER 圖（20+ 表、Junction、FK）與系統架構圖（Browser→Vite→Spring Boot→SQLite/llama-server），不另產 `*-mermaid.svg`；渲染由 Markdown 預覽直接完成，規則「MD 內則 mermaid、在外則 D2」

## Capabilities

### New Capabilities
<!-- 無 — 純文件同步，不改 spec 行為，已設 skip_specs: true -->

### Modified Capabilities
<!-- 無 -->

## Impact

- 文件：`docs/ARCHITECTURE.md`（內聯 mermaid）、`docs/DEPLOY.md`、`docs/REQUIREMENTS.md`、`docs/manual.typ`、`docs/diagnoses.typ`、`docs/d2/diagnoses.d2`、`docs/img/diagnoses.svg`（D2 產物）、`README.md`
- 工具：`mise.toml:[tasks.d2]` 保留（外部圖唯一來源），MD 內圖依賴原生 mermaid 渲染無需額外編譯
- 驗證：`openspec validate --specs --changes`、`mise run d2`、`typst compile docs/manual.typ` / `diagnoses.typ`、MD 預覽 mermaid 可渲染
