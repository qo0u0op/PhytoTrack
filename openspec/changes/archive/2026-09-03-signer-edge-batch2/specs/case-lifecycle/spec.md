## ADDED Requirements

### Requirement: 案件表單簽名人可辨同名

`CaseFormView` 簽名人候選 SHALL 與管理頁一致顯示 `身分別＋帳號`（如 `使用者 · wang123`），同名時可以帳號區分選擇，提交仍以 `id` 為準。

#### Scenario: 同名以帳號區分
- **WHEN** 存在兩筆 `王小明`（帳號 `wang1`、`wang2`）
- **THEN** 案件表單顯示兩列可辨帳號，勾選後以各自 `id` 提交
