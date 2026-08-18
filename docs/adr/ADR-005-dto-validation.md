# ADR-005: DTO + Bean Validation（不直接綁定 Entity）

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

既有版本的 Controller 直接把 `User` 實體當表單載體（`register(User user)`）。這帶來兩個問題：

1. **Mass Assignment**：攻擊者可 POST 多餘欄位（如 `role=ROLE_ADMIN`）自動提權
2. **Lazy 序列化地雷**：實體帶 `@ManyToOne` Lazy 關聯，直接回傳會觸發 `LazyInitializationException` 或 N+1 查詢

**選項**:

1. **引入 DTO（record）+ Bean Validation**，Controller 只收 DTO、只回 DTO
2. 繼續綁 Entity，靠 `@JsonIgnore` / DTO 投影補洞
3. 直接回傳 Entity 給前端

**決策**:

- 建立 request / response **record DTO**（`RegisterRequest`、`LoginRequest`、`CaseCreateRequest`、`CaseResponse`…）
- 所有 API 邊界只用 DTO：收進來的欄位 = DTO 定義的欄位；回出去的欄位 = DTO 挑選的欄位
- 搭配 `jakarta.validation`：`@NotBlank`、`@Size`、`@Email`，在 Controller 入口統一驗證
- Service 與 Repository 內部仍用 Entity

**原因**:

- **Security**：DTO 根本沒有 `role` 欄位，攻擊者無處下手——從「事後檢查」變成「結構上不可能」
- **避免 Lazy/序列化問題**：只序列化 DTO，不碰 Lazy 關聯，N+1 也一併被擋在 API 邊界外
- **穩定 API 合約**：Entity 改欄位（DB 演化）不會直接破壞 API 形狀（ADR-008 的單一來源才不會失準）
- record 不可變、自動有 equals/hashCode/toString，是現代 Java 最乾淨的 DTO 載體

**取捨**:

- 程式碼量增加（每功能多 2~4 個 DTO）
- DTO ↔ Entity 轉換需要額外程式碼；本專案以手寫 mapper 處理，若轉換邏輯爆炸再引入 MapStruct（YAGNI）
- 不採用 MapStruct 的理由：本專案 DTO 欄位少、轉換簡單，加 annotation processor 增加建置複雜度卻沒有足夠回報
