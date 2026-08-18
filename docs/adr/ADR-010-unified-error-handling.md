# ADR-010: 統一錯誤處理（@RestControllerAdvice）

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

既有版本的問題：login 失敗回傳 `null`、註冊錯誤把 `e.getMessage()` 直接吐到 UI、每支 Controller 各自 try-catch、錯誤格式不一致。前後分離後，前端需要**一致的錯誤形狀**才能統一處理（axios 攔截器 → sweetalert）。

**選項**:

1. **@RestControllerAdvice 全域例外處理**：Service 拋業務例外，advice 統一格式化
2. 每支 Controller 各自 try-catch 回傳錯誤
3. 錯誤以 `null` / 布林值代表失敗（現況）

**決策**:

- **Service 層**拋「有業務語意的例外」：以單一 `ApiException(code, status, message)` 表達（`code` 為機器可讀字串，如 `USERNAME_TAKEN`），替代回傳 `null`，呼叫方不會踩空指標
- **Controller 層**不 try-catch，一律交由 `@RestControllerAdvice` + `@ExceptionHandler` 集中處理
- **requestId 串接日誌**：`RequestIdFilter` 為每個請求產生 requestId 寫入 MDC 並回傳於 `X-Request-Id` 標頭；advice 從 MDC 讀取回傳，與伺服器 log 對照（可觀測性底線）
- 統一錯誤回應格式：

```json
{
  "error": {
    "code": "USERNAME_TAKEN",
    "message": "帳號已存在",
    "details": { "username": "admin" }
  },
  "requestId": "req-abc-123"
}
```

- `details` 於驗證失敗時收集各欄位錯誤；無補充資訊時為空物件
- 業務例外（4xx）與系統例外（5xx）分開處理，皆記錄 log（含 requestId）；資料庫錯誤在 Repository/advice 封裝，不把 SQL 細節洩漏給前端

**原因**:

- **單一格式**：前端 axios 攔截器只看一種錯誤形狀，統一彈 sweetalert
- **消除散落 try-catch**：每個 Controller 乾淨，錯誤邏輯集中一處維護
- **語意化例外**：用 `InvalidCredentialsException` 取代 `null`，呼叫方不會踩空指標
- **不洩漏細節**：5xx 回泛化訊息，詳細資訊只進 log；`requestId` 串接 log 方便除錯（可觀測性底線）

**取捨**:

- advice 集中後，要養成「Service 拋對的例外」紀律，否則錯誤會歸到 500 兜底
- 需要為每個例外決定 HTTP status 與 code——初期成本，換來一致的對外契約
- 暫時不做 message 國際化（i18n）——本系統單一語系（YAGNI），錯誤訊息先寫死中文
