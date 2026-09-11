## Context

見 `proposal.md - Why`。現狀：`backend/phytotrack.toml.example` 約 30 行註釋，平均每欄位 1 行；`PhytotrackTomlEnvironmentPostProcessor.generateDefaultToml()` 內嵌 `String content` 與範例約 80% 重疊但節順序與缺漏（先前 `app.tray` 未生成、`ai.headers` 僅註釋）已分叉。`docs/DEPLOY.md` 與 `docs/ARCHITECTURE.md` 另有重複的欄位表，導致三處文檔需同步。PhytoTrack 需達「config as a manual」的完整手冊等級。約束：TOML 註釋以 `#` 起始，不影響解析；範例與生成檔需同源，否則使用者首次生成檔與倉庫範例體驗不一致。

## Goals / Non-Goals

**Goals:**
- 單檔離線手冊：`phytotrack.toml.example` 在無網路環境可獨立完成常見改配（port、ai 開關/provider、tray/ui、CORS/限流）
- 同源一致：範例與 `generateDefaultToml()` 生成檔節/欄/註釋一致，差異僅 `app.jwt.secret` 隨機值
- 手冊級註釋：檔案頭路徑契約 + 每節節說明塊 + 每欄位五要素（用途/類型/預設/取值/生效）
- 關聯收斂：DEPLOY/ARCHITECTURE 改為指向檔內手冊，不重複欄位表

**Non-Goals:**
- 不新增/刪除配置鍵（僅註釋，`spring.datasource.url` / `logging.file.name` 已在 `loadToml` 支援，僅補註釋）
- 不改變執行期預設值或驗證邏輯（`AI_API_KEY` env 覆蓋、`app.bootstrap` 不可配口令等行為不變）
- 不做多語言範例（僅繁中，與專案慣例一致）

## Decisions

**D1 — 範例為主、生成為鏡像**
- 選擇：以 `phytotrack.toml.example` 為唯一手寫源，`generateDefaultToml()` 保持字面量與範例逐節同步（透過測試 `diff` 範例與生成字串）。
- 替代：二者各自手寫 → 已證分叉且難察覺 → 捨棄。
- 理由：單源可被 `grep` 與單元測試守護。

**D2 — 註釋結構：檔頭 + 節塊 + 欄位五要素**
- 檔頭（0-15 行）：`# PhytoTrack 配置手冊` + 四路徑契約（Windows 可攜 `config/`、AppImage 同目錄、XDG、系統級 `/etc`）+ 優先順序 + 首次生成與冪等 + 保存起點 `cp backend/phytotrack.toml.example ~/.config/phytotrack/phytotrack.toml` + `→ docs/DEPLOY.md#...` 索引。
- 節塊：每 `[section]` 前 2-4 行 `# ---` 或 `# [section] — 用途`，說明節職責與關聯程式（例：`[ai] → AIService`）。
- 欄位：每鍵上 1-2 行 `# 用途 | 類型 | 預設 | 取值/範例 | 生效`，敏感/關聯欄位加警告（`secret` 首次亂數、`ai.api-key` env 覆蓋、`bootstrap` 不可配）。
- 替代：每欄位長段落 → TOML 橫向過寬、生成字串難維護 → 捨棄。

**D3 — 缺漏節補齊與排序**
- 依 `loadToml` 可讀鍵補齊並固定節順序：`server → app.jwt → app.bootstrap → ai → ai.headers → app.cors → app.rate-limit → app.security-headers → app.tray → app.ui → springdoc → spring.datasource/logging`，與 `application.yaml` 鏡像，便於對照。
- 替代：按字母序 → 與現有範例與文件慣例衝突 → 捨棄。

**D4 — 文件收斂而非重複**
- `docs/DEPLOY.md`「配置」章節由欄位表改為「以 `phytotrack.toml` 檔內手冊為準，詳見 `phytotrack.toml.example`」+ 僅保留部署特有關聯（如防火牆、XDG 權限）。
- `docs/ARCHITECTURE.md` 僅保留「TOML 為單一真相、BinaryPaths 落點」一句，移除重複表。
- 替代：三處重複維護 → 已導致本次分叉 → 捨棄。

## Risks / Trade-offs

- [長註釋撐寬檔案] 單檔由 ~70 行增至 ~120 行 → Mitigation：五要素用短句、範例折行，`cat` 仍可讀；`generateDefaultToml()` 字串用 `"""` + `formatted` 保持可讀。
- [生成字串轉義] `#` 與 `"` 在 Java text block 需轉義 → Mitigation：單元測試對比生成檔與範例，CI 攔截分叉。
- [註釋誤導預設] 手冊寫「預設 8080」但 `application.yaml` 實為回落預設 → Mitigation：註釋預設取自 `application.yaml` 與 `loadToml` 實際預設，並在 `→ docs/DEPLOY.md` 註記「未配時回落 application.yaml」。

## Migration Plan

- 無資料遷移。部署：合併後下次 `tag` 的 `phytotrack.toml.example` 即含新手冊；已存在使用者的舊 `phytotrack.toml` 不覆蓋（冪等，需自行比對範例更新或刪檔重生成）。回滾：還原單檔即可，無 schema 變更。

## Open Questions

- 是否需在 `toml.example` 頂部加 `version` 註記以利使用者比對生成檔版本？可延後，不影響本次節/欄結構。
