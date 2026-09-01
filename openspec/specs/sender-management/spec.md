# Sender Management Specification

## Purpose

送件人資料的獨立管理與權限控管：支援多來源送件人 (現場、Line、Facebook、Email) 的顯示、去重與合併，VIEWER 角色遮蔽個資但保留縣市鄉鎮，並提供統計用的一致去重鍵。設計決策見 ADR-011。

## Requirements

### Requirement: 送件人欄位規則

送件人 SHALL 支援 `name`、`displayName`、`phone`、`address`、`district` (含所屬 `city`) 與 `senderType`；`name` MAY 為空，`phone` 與 `displayName` 之間 SHALL 至少一項有值；`displayName` 用於標記來源顯示名稱 (Line/Facebook/Email 暱稱)。

#### Scenario: 有姓名且有顯示名稱
- **WHEN** 送件人同時有 `name` 與 `displayName`
- **THEN** 顯示為 `name (displayName)`

#### Scenario: 只有顯示名稱
- **WHEN** 送件人只有 `displayName`、無 `name`
- **THEN** 直接顯示 `displayName`

#### Scenario: 兩者皆空
- **WHEN** 建立送件人時 `phone` 與 `displayName` 皆未提供
- **THEN** 回應 4xx，且不建立資料

### Requirement: 送件人去重與合併

送件人辨識 SHALL 以多個弱識別符 (name / phone / displayName) 組合進行，建立案件時列出候選送件人並由使用者人工確認是否沿用或建立新送件人；系統 SHALL NOT 以 DB 唯一鍵強制合併。

#### Scenario: 同識別符候選
- **WHEN** 輸入的姓名或電話與既有送件人部分相符
- **THEN** 系統列出候選清單供使用者確認合併

#### Scenario: 無候選
- **WHEN** 輸入資料與既有送件人皆不符
- **THEN** 系統建立新送件人

### Requirement: 送件人刪除權限

送件人刪除 SHALL 僅 ADMIN 可執行且為硬刪除；被案件引用的送件人 SHALL 拒絕刪除並回 4xx。系統 SHALL NOT 提供 soft delete。

#### Scenario: 刪除未被引用送件人
- **WHEN** ADMIN 刪除未被任何案件引用的送件人
- **THEN** 送件人自資料庫移除

#### Scenario: 刪除被引用送件人
- **WHEN** ADMIN 刪除已被案件引用的送件人
- **THEN** 回應 4xx，且資料保留

### Requirement: VIEWER 個資遮蔽

VIEWER 角色 SHALL NOT 取得送件人姓名、電話與地址，但 SHALL 可取得縣市鄉鎮。

#### Scenario: VIEWER 查詢案件
- **WHEN** VIEWER 查詢案件列表或詳細
- **THEN** 回應不含送件人姓名／電話／地址，但含縣市鄉鎮

#### Scenario: STAFF/ADMIN 查詢案件
- **WHEN** STAFF 或 ADMIN 查詢案件
- **THEN** 回應含完整送件人資料

### Requirement: 統計去重鍵

案件統計的「不重複送件人」SHALL 以 `COALESCE (phone, displayName)` 為鍵計算。

#### Scenario: 依電話去重
- **WHEN** 統計不重複送件人
- **THEN** 有電話者依電話歸併，無電話者依顯示名稱歸併

### Requirement: 送件人查詢 API

系統 SHALL 提供送件人搜尋端點，依 name / phone / displayName 部分比對，供建案表單與去重候選使用；CaseResponse SHALL 包含 `senderId` 與送件人縣市、鄉鎮名稱。

#### Scenario: 依關鍵字搜尋
- **WHEN** 使用者輸入關鍵字搜尋送件人
- **THEN** 回傳姓名或電話或顯示名稱相符的送件人候選

#### Scenario: 案件回應帶送件人識別
- **WHEN** 查詢案件詳細或列表
- **THEN** 回應包含 `senderId` 及送件人縣市、鄉鎮名稱