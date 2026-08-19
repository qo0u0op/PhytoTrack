# ADR（Architecture Decision Records）

本目錄記錄 PhytoTrack 重構過程中的架構決策。每一份 ADR 說明：**背景 → 選項 → 決策 → 原因 → 取捨**。

狀態用語：

- `已決定`：方向已確認，尚未或正在實作
- `已實作`：程式碼已落地
- `已取代`：已被後續 ADR 推翻

## 索引

| 編號 | 主題 | 狀態 |
|------|------|------|
| [ADR-001](ADR-001-frontend-backend-separation.md) | 前後分離架構（Vue 3 + REST API） | 已實作 |
| [ADR-002](ADR-002-spring-boot-4-java-21.md) | Spring Boot 4 + Java 21 作為後端框架 | 已實作 |
| [ADR-003](ADR-003-layered-architecture.md) | 分層架構（Controller / Service / Repository） | 已實作 |
| [ADR-004](ADR-004-jwt-security-bcrypt.md) | JWT + Spring Security + BCrypt 認證授權 | 已實作 |
| [ADR-005](ADR-005-dto-validation.md) | DTO + Bean Validation（不直接綁定 Entity） | 已實作 |
| [ADR-006](ADR-006-jpa-auditing.md) | JPA Auditing 取代 @PrePersist | 已實作 |
| [ADR-007](ADR-007-sqlite-to-postgresql.md) | SQLite 起步、預留 PostgreSQL 升級路徑 | 已實作 |
| [ADR-008](ADR-008-openapi-springdoc.md) | OpenAPI（springdoc）作為 API 規格單一來源 | 已實作 |
| [ADR-009](ADR-009-llama-backend-proxy.md) | llama.cpp 後端代理（Spring AI） | 已實作 |
| [ADR-010](ADR-010-unified-error-handling.md) | 統一錯誤處理（@RestControllerAdvice） | 已實作 |

## 為何要寫 ADR

- 記錄「為什麼選這個，而不是另一個」——程式碼只會告訴你「怎麼做」，ADR 告訴你「為什麼」
- 對每個決策都能講出 trade-off，而不是只會背名詞
- 未來重構時，先看 ADR 才知道哪些「看似不合理」的選擇其實是刻意為之
