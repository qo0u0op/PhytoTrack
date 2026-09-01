# ADR-003: 分層架構 (Controller / Service / Repository)

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

業務涵蓋案件 (Case)、使用者 (User) 與大量參照資料。需要一套一致的組織方式，讓邏輯可測、依賴可換。

**選項**:

1. **三層架構：Controller → Service → Repository**
2. 所有邏輯塞進 Controller (肥胖 Controller)
3. 六邊形架構 (Ports & Adapters)——對本專案規模過重

**決策**:

採用 Controller → Service → Repository 三層，依賴方向**單向**：Controller 依賴 Service，Service 依賴 Repository，禁止反向。

**各層職責 (紅線)**:

- **Controller**：收 HTTP、參數綁定、呼叫 Service、回傳 DTO。不寫業務規則
- **Service**：商業規則 (帳號不得重複、密碼 BCrypt、交易邊界 `@Transactional`)。不拼 SQL
- **Repository**：Spring Data JPA 介面，只管資料存取。不做業務
- **Service 採用「介面 + 實作」** (`UserService` / `UserServiceImpl`)：Controller 依賴介面，測試可注入 mock，實作可被 AOP 代理

**原因**:

- 業務邏輯可獨立於 HTTP 層測試 (@WebMvcTest 不需要啟動 DB)
- 換資料庫只需改 Repository 層 (見 ADR-007)
- 加入 `@Transactional`、事件、快取時有乾淨的落點
- 「依賴方向單一」讓程式易讀、可測、可替換，避免業務邏輯塞在 Controller 難以維護

**取捨**:

- 檔案數變多 (每功能至少 2 個 Java 檔)，對小專案略顯繁瑣——以一致性與可測性換取
- 不用六邊形架構：本專案沒有 adapter 多樣性需求，三層足以，避免過度設計 (YAGNI)
