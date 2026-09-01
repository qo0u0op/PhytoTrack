# ADR-002: Spring Boot 4 + Java 21 作為後端框架

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

後端需要一套能快速開發、生態成熟、且能展示「現代 Java」能力的框架。候選有傳統 Spring、Spring Boot 2/3/4。

**選項**:

1. **Spring Boot 4 + Java 21**——自動設定、starter 依賴、內嵌伺服器
2. 傳統 Spring (XML / Java Config 手動設定)
3. 其他語言框架 (Node/Express、Python/FastAPI)——不符合「Java 能力展示」目標

**決策**:

採用 Spring Boot 4.0.6 + Java 21 (LTS)，Maven 建置。

**原因**:

- **auto-configuration**：classpath 出現 `sqlite-jdbc` 就自動配好 DataSource，開發專注在業務
- **starter**：一次引入一組相容依賴，不用自己對版本
- **內嵌伺服器**：`mvn spring-boot:run` (已安裝 mise) 或 `./mvnw spring-boot:run` (Unix/macOS)/ `.\mvnw.cmd spring-boot:run` (Windows) 即起，零部署即跑
- **Java 21**：record、pattern matching 等現代語法，配合 DTO (ADR-005) 乾淨俐落
- Boot 4 將 `spring-boot-starter-web` 更名為 **`spring-boot-starter-webmvc`**，明確區分 MVC (servlet) 與 WebFlux (reactive)——用 Boot 4 本身就是在展示對新版本的理解

**取捨**:

- Boot 的「魔法」多，底層細節被隱藏——需能回答「auto-configuration 靠 `AutoConfiguration.imports` 條件化載入」
- 生態第三方庫跟進 Boot 4 的速度不一 (例：springdoc 需 3.1.0+，見 ADR-008)，版本需鎖定驗證
- 放棄逐步手工設定的完全控制，換取開發速度
