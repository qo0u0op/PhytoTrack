# ADR-006: JPA Auditing 取代 @PrePersist

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

`Case` 實體目前用 `@PrePersist` / `@PreUpdate` 手動填 `createdAt / updatedAt`。缺點：

- 每個實體都要重複寫一次 callback，容易漏
- `createdBy` 沒有自動化來源，誰建立的難以追蹤

**選項**:

1. **Spring Data JPA Auditing**：`@CreatedDate / @LastModifiedDate / @CreatedBy`
2. 繼續每個實體手寫 `@PrePersist`
3. 資料庫層 trigger——與 ORM 脫節，SQLite 支援差

**決策**:

- 加 `@EnableJpaAuditing` + `@EntityListeners (AuditingEntityListener.class)`
- `Case` 改用 `@CreatedDate`、`@LastModifiedDate`，`@CreatedBy` 由 **`AuditorAware`** 從 `SecurityContext` (JWT 登入者) 取出自動填入
- 移除手寫的 `@PrePersist` / `@PreUpdate`

**原因**:

- 宣告式、集中管理：時間與「誰建的」由框架統一填，實體不再重複樣板碼
- **順帶展示 SecurityContext**：`AuditorAware` 讀取目前登入者，剛好把 ADR-004 的認證與資料寫入串起來——這是「橫切關注點」的具體示範
- 「知道框架有這個功能並懂得啟用」比「自己寫 20 遍 callback」更省成本且不易出錯——成熟機制與手寫樣板碼的取捨

**取捨**:

- 依賴 Spring Data 的機制，換框架 (若未來改 MyBatis) 需重做
- 特殊情境 (系統批次匯入、無登入者) 要自己提供 fallback auditor
- 接受：本專案不會有無登入者的寫入路徑，故不處理 fallback (YAGNI)
