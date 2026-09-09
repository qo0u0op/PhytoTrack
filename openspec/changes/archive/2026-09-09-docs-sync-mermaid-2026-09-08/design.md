## Context

`openspec/specs` 為單一真相源，但 `docs/` 多處文本與圖仍為手寫且未跟隨 `add-remember-me`（`rememberMe` 與雙儲存）、`add-dark-mode`（`ui-theme`）、`remove-default-credentials`（TOML 僅註釋）與 `dev-browser-opens-vite`（dev 開 `:5173`）等變更。規則明確：MD 內圖用 mermaid，外部引用圖用 D2。

## Goals / Non-Goals

**Goals:**
- 單一真相源對齊：`docs/` 文字、表格與圖與 `openspec/specs` 一致（JWT 雙時效、bootstrap 註釋、theme 三態、dev vite 分流）
- 規則落地：MD 內圖一律 ` ```mermaid` 內聯（ER 與架構），外部圖一律 D2 產物（`docs/img/diagnoses.svg`）

**Non-Goals:**
- 不改 `openspec/specs` 行為（已設 `skip_specs: true`）
- 不重繪業務流程圖或補頁面截圖（僅 ER 與架構）
- 不引入付費圖床或外部服務

## Decisions

- **分工：MD 內 mermaid、在外 D2**：`docs/ARCHITECTURE.md` 內聯 ` ```mermaid` ER 與架構圖（GitHub 原生渲染，無需產 SVG）；外部引用僅 `docs/img/diagnoses.svg` 單一 D2 產物（`diagnoses.d2` → `mise run d2`），不另產 `*-mermaid.svg`。替代：D2 與 Mermaid 雙產 SVG → 外圖究竟用誰易混淆 → 捨棄。
- **文件分層**：`ARCHITECTURE.md` 放 Mermaid ER + 架構圖（開發者導向），`REQUIREMENTS.md` 僅文字能力表（不重複圖），`DEPLOY.md` 僅表格式 bootstrap 說明，`manual.typ` 僅操作步驟（不含 ER）。替代：各文件各放一套圖 → 維護重複 → 捨棄。
- **Typst 函式校驗**：`manual.typ`/`diagnoses.typ` 函式呼叫不得有空白（`#set page(margin:` 而非 `#set page (`），以 `typst compile` 為綠燈門檻。

## Risks / Trade-offs

- [規則誤用] MD 內誤用 D2 外圖連結 → Mitigation：`tasks.md` 明確「MD 內 mermaid、在外 D2」，PR 以 `grep '```mermaid' docs/ARCHITECTURE.md` 與 `grep 'docs/img/diagnoses.svg'` 雙檢
- [Typst 空白陷阱] 函式前空白導致編譯失敗 → Mitigation：任務含 `typst compile` 雙檔驗證

## Migration Plan

1. `docs/d2/diagnoses.d2` 更新 → `mise run d2` 重建 `docs/img/diagnoses.svg`（在外圖唯一來源），`docs/ARCHITECTURE.md` 內聯 mermaid 無需額外編譯
2. `ARCHITECTURE.md`/`DEPLOY.md`/`REQUIREMENTS.md`/`README.md` 文本同步 → `manual.typ` 步序補強 → `typst compile` 雙檔
3. `openspec validate --specs --changes` 與 `mise run d2` 綠燈後封存

## Open Questions

- 無
