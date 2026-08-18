# ADR-009: llama.cpp 後端代理（Spring AI）

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

系統要提供 AI 診斷建議（輸入作物、被害部位、症狀、病蟲害類別，輸出診斷與防治建議）。AI 引擎是本機 llama.cpp（`llama-server`，提供 OpenAI 相容的 `/v1/chat/completions`）。需要決定前端怎麼接、後端用什麼方式呼叫。

**選項**:

1. **後端代理 + Spring AI**：後端用 Spring AI 的 `ChatClient`（OpenAI 相容格式）呼叫 llama-server，包成 `/api/ai/*`
2. 後端代理 + 手寫 `RestClient` 直接呼叫
3. 前端 axios 直連 llama-server
4. 直接整合 OpenAI 雲端 API

**決策**:

- 後端提供 **`POST /api/ai/analyze`**（限 STAFF+）：接收案件欄位 DTO → 用 Spring AI 組 System/User 提示詞（Prompt）→ 呼叫 llama-server（**非串流** `.call()`）→ 回傳診斷建議與耗時
- 提供 **`GET /api/ai/health`**（公開）檢查 llama-server 存活（打 `/health` 端點）
- 使用 **Spring AI 2.0**（`spring-ai-starter-model-openai`），設定集中在 `application.yaml` 的 `spring.ai.openai.*`：`base-url: http://localhost:8081/v1`、`api-key` 填 dummy、`model: llama`
- llama-server 掛 **8081**（避開 Spring Boot 預設 8080）

**原因**:

- **統一邊界**：認證、權限、輸入清洗都在後端做一次，前端永遠不碰 llama
- **隱藏細節**：llama-server 位址、模型、prompt 組裝封在後端，換模型/換供應商前端無感
- **帶領域上下文**：後端把表單欄位（作物、被害部位、症狀、病蟲害類別）組進 prompt，AI 才給得出貼合領域的回答
- **為何用 Spring AI**：OpenAI 相容格式是事實標準，Spring AI 提供統一抽象（`ChatClient`），未來切換 OpenAI/其他供應商只需改設定，不用重寫呼叫層；同時可展現「串接 AI 服務」的實務經驗
- **為何非串流**：先求穩定、可測、好講；SSE 串流（打字機效果）留作後續進階項目
- **為何不直連**：CORS 處理 + 沒有認證權限 + prompt 組裝外洩到前端，訊號弱又危險

**取捨**:

- 每次呼叫是 blocking 且耗時（本地 LLM 可能數秒）——對 <5 人 LAN 可接受；若未來要並發，再考慮快取或非同步/串流
- llama-server 與 Spring Boot 都預設 8080 會**撞 port**，需以設定錯開（llama 掛 8081）
- 模型品質依賴本機硬體與所選 GGUF——這是硬體限制，不是架構問題
- llama.cpp 不驗證 API Key，`api-key` 僅為滿足 Spring AI 必填欄位，不代表真實授權
