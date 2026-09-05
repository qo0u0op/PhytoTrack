## Context

團隊 <5 人、LAN 單機，`CaseService` 卻因「測試相容」承載 `@Autowired(required=false)` 與 3 個 DTO 多載，使生產代碼為測試讓步；`ReferenceDataService` 反向因「不敢抽象」而複製 24 方法。兩者皆推高新增欄位/實體成本，與 YAGNI/KISS 相悖。

## Goals / Non-Goals

**Goals:**
- 零行為變更下減少分支與重複，`mvn test 148` 全程綠燈。
- 以最小可審步長落地，每步可單獨 revert。

**Non-Goals:**
- 不拆 `CaseService` 為多服務（需跨交易與 `findByIdOrThrow` 共用，大改風險高）。
- 不改 `InputSanitizer` 雙層語意與 `GlobalExceptionHandler` 四映射（已驗為合理）。

## Decisions

- **去可選注入 — 建構子必填 + 測試供 bean**：`SecurityConfig` 與 `CaseService` 刪除 `required=false`，`@WebMvcTest` 改 `@MockitoBean RateLimitFilter` / `IdentifierService`。替代：生產 `if (x != null)` 保留，成本為每新增依賴複製分支 → 捨棄。
- **DTO 多載 — 刪多載改測試**：`CaseDtos` 多載僅為 `CaseControllerTest` 不改而設，改測試為 canonical 建構（`record` 帶名參數）後刪除多載，`sed` 一次性成本 < 永久多載維護。替代：保留多載，債務永續 → 捨棄。
- **RefService — 模板而非策略**：以 `AbstractRefService<T,ID>` 抽 `trim→exists→save` 模板，`Damage/Hint/...` 各匿名子類僅供 `existsBy` 函數，無需策略工廠。替代：完全無抽象則 24 方法持續膨脹 → 捨棄。
- **InputSanitizer — 保留雙層，僅微簡化**：刪除 `fieldLabel` 多載或遷 `util` 為 Nit，留待本 change 後半段 `good-first-issue`，避免與 CaseService 大改同 PR 衝突。

## Risks / Trade-offs

- [測試改動噪音] `CaseControllerTest` 多處建構更新 → 以單 commit 集中改測試，diff 雖大但語意單一，可 `git diff -w` 審。
- [模板抽象風險] `AbstractRefService` 若過早泛型化可能束縛差異化校驗 → 僅抽 `saveTrimmed/exists`，保留各實體差異點可覆寫。
- [套件遷移] `InputSanitizer` 遷 `util` 需改 4 檔案 import → 低風險，列為可選。

## Migration Plan

1. 去可選注入（`SecurityConfig` → `CaseService`）→ `mvn test`。
2. 刪 `CaseDtos` 多載並同步改測試 → `mvn test`。
3. 抽 `ReferenceDataService` 模板 → `mvn test`。
4. （可選）`InputSanitizer` 精簡/遷套件 → `mvn test`。
5. `openspec validate --specs --changes` 通過後封存。

## Open Questions

- 無。
