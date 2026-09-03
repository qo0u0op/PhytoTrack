## 1. 後端收斂

- [x] 1.1 `CaseService.create` 逐筆檢查簽名人 `active`，停用回 409 並驗證新建被拒、歷史更新放行
- [x] 1.2 `IdentifierService.ensureForUser` 新建前全域查重，撞名拋 `DISPLAY_NAME_EXISTS` 並驗證不靜默重複
- [x] 1.3 帳號停用/降級連動停用名下簽名人，並驗證歷史案件仍顯示原名
- [x] 1.4 `findByUserUserIdAndActiveTrue` 加 `OrderByIdentifierIdAsc`，呼叫方以首筆為準並驗證確定性

## 2. 驗證

- [x] 2.1 新增 `SignerEdgeBatch1Test` 覆蓋上述四場景並驗證全綠
- [x] 2.2 執行既有 `SignerLifecycleTest`、`CaseSignerAutoFillTest` 回歸並驗證無退化
