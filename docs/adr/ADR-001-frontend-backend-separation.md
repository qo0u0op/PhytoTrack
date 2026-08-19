# ADR-001: 前後分離架構（Vue 3 + REST API）

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

原專案為 Spring Boot + Thymeleaf 服務端渲染（MPA）。本次重構要展示 Java、Spring 與 TypeScript 三種能力，並引入 llama.cpp AI 診斷。需要決定前端與後端的耦合方式。

**選項**:

1. **前後分離（Vue 3 SPA + REST API）**——後端只出 JSON，前端獨立 Vue 應用
2. 傳統 MPA（Thymeleaf 服務端渲染 + Bootstrap/axios 漸進增強）——保留既有頁面
3. 全 SPA（React/Vue）搭配 GraphQL——查詢彈性最大

**決策**:

採用**選項 1**：前端 Vue 3 + TypeScript + Bootstrap，後端 Spring Boot 提供 REST API，兩者以 HTTP JSON 為唯一橋樑。

**原因**:

- 後端能力（Spring Security、REST、DTO、OpenAPI）可以完整獨立呈現，不被頁面渲染混雜
- Vue 3 佔前端，正好補上「TypeScript 基礎」的展示缺口
- API 可被 Swagger UI、openapi-typescript、未來其他客戶端共用
- 5 人內 LAN 工具**不需要 SEO**，故不犧牲 MPA 的「首頁直出」優勢
- API 邊界讓前端與後端可平行開發、獨立驗證，互不阻塞交付

**取捨**:

- 付出代價：需處理 CORS、雙端建置（Maven + Vite）、API 版本管理
- 前端登入狀態改用 JWT（見 ADR-004），不再依賴 session cookie，避免 CSRF
- 用 Bootstrap（含 hero 版型）取代 UI 框架，降低 Vue 學習成本，把精力留在後端
