## ADDED Requirements

### Requirement: 簽名人併發冪等與正規化比對

同名 `active` 簽名人 SHALL 在併發下仍只保留一筆（DB 部分唯一索引或異常轉 `409 DISPLAY_NAME_EXISTS`）；名稱比對 SHALL 經正規化（trim、連續空白摺疊、全半形統一、NFC、英文大小寫不敏感）後比較。

#### Scenario: 併發同名只存一筆
- **WHEN** 兩請求同時新建同名 `active` 簽名人
- **THEN** 一筆成功、一筆回 `409 DISPLAY_NAME_EXISTS`，不產生重複

#### Scenario: 大小寫全半形視為同名
- **WHEN** 已有 `王小明` 而新建 `王小明` 的大小寫/全半形變體
- **THEN** 回 `409 DISPLAY_NAME_EXISTS`

### Requirement: 綁定名實一致與重建循環

`bindToUser` SHALL 要求來源簽名人名稱與目標使用者 `displayName` 一致（經同上正規化），不一致 SHALL 拒絕並提示先改名；使用者無 `active` 但有同名 `inactive` 時，`ensureForUser` SHALL 優先提示啟用舊筆而非直接新建。

#### Scenario: 綁定名實不符被拒
- **WHEN** 將外部 `陳建宏` 綁給 `displayName=王小明` 的使用者
- **THEN** 回 4xx 並提示名稱不一致，不改名綁定

#### Scenario: 停用後優先啟用舊筆
- **WHEN** 使用者僅有同名 `inactive` 簽名人而請求自動帶入
- **THEN** 系統提示啟用舊筆，確認後 `active=true` 而非新建第二筆

### Requirement: 重名碰撞範圍界定

重名檢查（新建、改名、自動帶入）SHALL 僅針對非使用者 `active` 同名；同名同姓的使用者簽名人 SHALL 允許並存，以 `帳號` 區分，提權撞非使用者同名時走綁定流程。

#### Scenario: 同名使用者簽名人並存
- **WHEN** 兩位不同 `username` 的 STAFF 使用相同 `displayName`
- **THEN** 各自擁有同名 `active` 簽名人，管理頁與案件表單以帳號區分，不報 `DISPLAY_NAME_EXISTS`

### Requirement: 最後 active 保護

停用全域最後一個 `active` 簽名人 SHALL 需二次確認或被阻擋，避免新案無候選。

#### Scenario: 停用最後一個被阻擋或警告
- **WHEN** ADMIN 停用全域最後一個 `active` 簽名人
- **THEN** 回 4xx 或前端二次確認，確認後才執行
