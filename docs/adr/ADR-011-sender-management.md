# ADR-011: 送件人管理（Sender Management）

**日期**: 2026-08-19

**狀態**: 已決定

**背景**:

送件人目前僅在建立案件時**隨案建立**：`Sender` 以 `name + phone` 為 UNIQUE，欄位皆不可空（`name`/`phone`/`address`/`district`/`senderType`）。隨之浮現的問題：

1. **同一送件人可來自不同來源**（現場手填、Line、Facebook、Email），各來源的「姓名」未必一致，`name + phone` 唯一鍵無法涵蓋無電話或電話不同的來源
2. **VIEWER 可看到完整個資**（姓名、電話、地址），無遮蔽機制，不符最小揭露原則
3. 送件人**無法獨立管理**：無顯示名稱、無來源標記、無合併既有重複資料的手段
4. 統計分析需要「不重複送件人」的去重鍵，目前僅能靠 `name + phone`，會把同一人多筆來源算成不同人

**選項**:

1. **DB 唯一鍵強制合併**（如 `phone` UNIQUE）：最簡單，但電話可能空白或填錯，且強制合併會擋住正常建案流程
2. **弱識別符 + 人工確認**（本決策）：以多個弱識別符（name / phone / display_name）greedy 搜尋候選，交由使用者人工確認是否為同一人，DB 不設強制唯一鍵
3. **soft delete 供 STAFF/ADMIN 使用**：需要時可「隱藏」送件人，但引入 deleted flag 需貫穿所有查詢，且被引用資料仍有顯示意義
4. **僅 ADMIN 硬刪除（被引用拒刪）**：刪除是真刪除；被案件引用的送件人以「拒絕刪除」保護（與參照資料刪除規則一致）

**決策**:

- **欄位規則**：新增 `displayName`（來源顯示名稱，如 Line/Facebook/Email 暱稱）；`name` 可空，`phone` 與 `display_name` **至少一項必填**；其餘既有欄位不變
- **顯示規則**：同時有 `name` 與 `displayName` 時顯示 `name(displayName)`；只有 `displayName` 時直接顯示 `displayName`
- **去重策略**：輸入時以 name / phone / display_name 組合做 greedy 搜尋，列出候選送件人，由使用者**人工確認**是否合併，不設 DB 唯一鍵強制
- **刪除**：取消 soft delete，僅 ADMIN 可硬刪除；被案件引用的送件人拒刪（回 4xx）
- **VIEWER 遮蔽**：VIEWER 角色不得取得送件人姓名、電話、地址，但**保留縣市鄉鎮**（維持統計與地緣判讀能力）
- **統計去重鍵**：案件統計的「不重複送件人」以 `COALESCE(phone, display_name)` 為鍵
- **案件回應補全**：`CaseResponse` 增加 `senderId` 與送件人縣市、鄉鎮全名（供列表/明細引用）
- **查詢 API**：新增送件人搜尋端點（combobox 依 name / phone / display_name 部分比對），供建案與去重使用

**原因**:

- **不強制唯一鍵**：去重是「資料品質」議題，強制鍵會在電話缺漏或手誤時擋住建案；greedy 候選 + 人工確認兼顧正確性與流暢度
- **取消 soft delete**：送件人去重已在輸入時收斂，soft delete 的價值大減；硬刪除＋引用拒刪與參照資料規則一致，避免 deleted flag 貫穿所有查詢的複雜度
- **遮蔽而非隱藏**：VIEWER 仍需要縣市鄉鎮做統計與判讀，全數隱藏會削弱角色價值
- **單一去重鍵**：統計處統一以 `COALESCE(phone, display_name)` 為鍵，避免各處各自定義「同一人」

**取捨**:

- 人工確認去重依賴使用者操作品質，可能殘留重複資料；換來的是建案流程不被唯一鍵卡住
- `COALESCE(phone, display_name)` 仍可能把「同電話多帳號」當同一人；以候選清單＋人工確認補強
- 取消 soft delete 後，誤刪送件人無法復原；以「被引用拒刪」＋刪除前確認降低風險
- VIEWER 遮蔽僅影響 API 回應與前端呈現，資料庫仍存完整個資（權限控管在 Service/投影層）

**遷移注意**（與既有 `senders` 表）：`name`、`phone` 目前為 NOT NULL 且 `(name, phone)` UNIQUE；遷移列舉為可空＋新增 `display_name` 時需調整 schema 約束，既有資料 `name`/`phone` 值保留。