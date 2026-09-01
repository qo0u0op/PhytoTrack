# ADR-004: JWT + Spring Security + BCrypt 認證授權

**日期**: 2026-08-18

**狀態**: 已實作

**背景**:

既有版本使用 raw `HttpSession` 存登入狀態，且**密碼明文儲存並以 `.equals ()` 比對**——這是最高優先級漏洞。前端改為前後分離後 (ADR-001)，登入識別方式需重新設計。

**選項**:

1. **JWT (無狀態)** + Spring Security + BCrypt
2. Session + Cookie (伺服器端 session store)
3. OAuth2 / 第三方登入——對內部工具過重

**決策**:

- 認證：**JWT** (jjwt 0.12.x)，前端以 `Authorization: Bearer <token>` 攜帶，Spring Security 以 filter chain 驗證
- 授權：**RBAC**，沿用 `User.Role` (`ROLE_VIEWER / ROLE_STAFF / ROLE_ADMIN`)，`SecurityFilterChain` 定義 URL 規則，方法級用 `@PreAuthorize`
- 密碼：**BCrypt** (`PasswordEncoder.encode ()` 存、`matches ()` 驗證)
- 設計上保留**短效 access token + 長效 refresh token** 的升級空間

**原因**:

- **為什麼 JWT 而非 Session**：
  - 無狀態，任意節點可驗證，適合前後分離的 API
  - 前端用 header 而非 cookie，**沒有 CSRF 問題**，也不用處理跨域 cookie
  - Session 需要 session store，且跨伺服器要共享，對本架構多餘
- **為什麼 BCrypt**：單向雜湊 + 自動 salt + 可調 cost (故意算得慢)。MD5/SHA 算太快、無 salt，可被彩虹表與暴力破解
- **為什麼 RBAC**：三個角色已存在於實體；宣告式集中管理優於散落的 if-else

**取捨**:

- **JWT 無法主動撤銷**——被盜的 token 在過期前有效；以「短效 token」緩解，這是必須承認的 trade-off
- Spring Security 學習曲線陡峭，但換來宣告式、集中管理且經實務驗證的認證授權 (filter chain、BCrypt、角色授權)，避免重造不安全的輪子
- JWT 密鑰需妥善管理，不放進程式碼與 git

密碼為什麼用 BCrypt？密碼不是「上鎖」，而是「磨碎」。BCrypt 把密碼攪成一團無法還原的粉末 (單向雜湊)，還故意磨得慢 (cost)。即使資料庫被偷，竊賊也無法從粉末還原你的密碼；用 MD5 就像用果汁機卻轉速太快、還沒加鹽，人家可以直接拿果汁配方表 (彩虹表) 比對。