## Context

現行 `AIService` 以 `spring.ai.openai.base-url=http://localhost:11435/v1` 寫死本機，個人資料經 `SenderService` 的 Viewer 遮蔽僅套用於 `GET /api/cases`，AI prompt 仍可能含明文個資直送外部。

## Goals / Non-Goals

**Goals:**
- `ai.provider` 可選，外送前強制 Viewer 過濾
- 預設 `local` 零破壞，外部僅作可選

**Non-Goals:**
- 不做細粒度欄位授權（僅重用既有 Viewer 遮蔽）
- 不改備份或 SQLite 持久化

## Decisions

- **重用 Viewer 遮蔽而非新策略**：`AIService` 注入 `CaseService` 的 `toViewerDetail` 或抽 `ViewerFilter.filter(Case)`，將 `name/phone/address/displayName` 替換為 `***` 後再組 prompt。替代：為 AI 單獨定義遮蔽規則 → 重複 → 捨棄。
- **TOML 單一 provider 鍵**：`ai.provider` + `ai.base-url/model/api-key`，`PhytotrackTomlEnvironmentPostProcessor` 依 `provider` 決定 `spring.ai.openai.*` 映射；`application.yaml` 僅設 `ai.provider=local` 預設。替代：多 profile (`local`/`external`) → 切換需重啟 profile → 捨棄。
- **日誌與健康檢查**：`GET /api/ai/health` 依 `provider` 探測對應端點，回 `{healthy, provider}`，便於前端顯示；`AIService` 日誌 `provider=external` 時不印 prompt 明文。

## Risks / Trade-offs

- [誤配外部而不知外送] 使用者未讀提示 → Mitigation：UI 明確提示 + 預設 `local`，外部需手動填 `api-key` 才生效
- [Viewer 遮蔽過度] AI 可能因缺電話而診斷品質微降 → Mitigation：可接受，個資優先

## Migration Plan

1. `phytotrack.toml.example` 補 `ai.provider` 註解 → `PhytotrackTomlEnvironmentPostProcessor` 映射 → `AIService` 加 Viewer 過濾 → 前端提示
2. `mvn test`、`openspec validate` 綠燈

## Open Questions

- 無（Viewer 邊界已由 `sender-management` 定義）
