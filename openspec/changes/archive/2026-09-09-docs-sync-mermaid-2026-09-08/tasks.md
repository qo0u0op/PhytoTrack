## 1. Markdown 文件同步

- [x] 1.1 更新 `docs/ARCHITECTURE.md`：系統概覽圖註記 dev `:5173` vs prod `/`、技術選型補 `ui-theme`（`data-bs-theme`）、後端結構補 `stores/theme.ts`、認證補雙時效 JWT 與 `BinaryPaths`/`BrowserOpener` dev 分流、`app.bootstrap` 改僅註釋，驗證 `openspec validate --specs --changes` 綠燈
- [x] 1.2 更新 `docs/DEPLOY.md:36` 配置表 `app.bootstrap` 為「內建預設不在 TOML 配置（僅註釋提醒）」、補 `remember-me-expiration` 與 `ui-theme` 無後端變更說明，驗證 `grep -E "^admin-username" backend/phytotrack.toml.example` 為 0
- [x] 1.3 更新 `docs/REQUIREMENTS.md` 能力表：新增 `ui-theme`（深色模式三態）、更新 `security-hardening` 帳號初始化條目以對應 `remove-default-credentials`，驗證與 `openspec/specs` 能力數一致
- [x] 1.4 更新 `README.md` 功能/技術/快速啟動：補 `ui-theme` 與 `remember-me`（勾選 7 天、登出保留帳號）、同步 bootstrap 註釋說明，驗證 `grep -rn admin123 README.md` 僅餘歷史或無

## 2. Typst 操作手冊同步

- [x] 2.1 更新 `docs/manual.typ`：登入步序增「記住我」勾選（註明 7 天時效僅程式內建）、登出後帳號保留、導覽列主題切換（auto/light/dark）、`mise run dev` 開 `:5173` 說明，驗證 `typst compile docs/manual.typ docs/manual.pdf` 與 `typst compile docs/diagnoses.typ /tmp/diagnoses.pdf` 皆 exit 0

## 3. 圖檔與工具鏈

- [x] 3.1 更新 `docs/d2/diagnoses.d2`（在外圖）：cases 補 `field_district_id`、senders/identifiers 補可空與 `former_user_id`、`v_case_search` 17 欄註記、`remember-me`/`ui-theme` 註記不落庫，執行 `mise run d2` 重建 `docs/img/diagnoses.svg`（外部引用的唯一圖源）並 `ls -lh docs/img/diagnoses.svg` 可見
- [x] 3.2 於 `docs/ARCHITECTURE.md` 內聯 ` ```mermaid` ER 與架構圖（MD 內圖用 mermaid，20+ 表 + Junction + FK、Browser→Vite→Spring Boot→SQLite/llama-server→SystemTray），無需另產 `*-mermaid.svg`，驗證 `grep -c '```mermaid' docs/ARCHITECTURE.md` ≥2 且 GitHub 預覽可渲染
- [x] 3.3 驗證分工：`grep -E "diagnoses\.svg" docs/ARCHITECTURE.md` 應無（外圖不應內聯誤用），`mise run d2` 綠燈，`openspec validate --specs --changes` 通過
