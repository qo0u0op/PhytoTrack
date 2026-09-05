## 1. 去可選注入（生產不再為測試妥協）

- [x] 1.1 `config/SecurityConfig.java:39` 移除 `@Autowired(required=false) RateLimitFilter` 與 `if (rateLimitFilter != null)` 分支，改建構子必填；`src/test` 內 ` @WebMvcTest` 改以 `@MockitoBean RateLimitFilter` 供空，驗證 `mvn test` 148 passed
- [x] 1.2 `service/CaseService.java:86` 移除 `@Autowired(required=false) IdentifierRepository/IdentifierService` 與 `this(..., null, null)` 相容建構，改必填；`src/test` 以 `@MockitoBean` 供 mock，刪除 6 處 `if (identifierService != null)` 分支，`mvn test` 通過

## 2. DTO 相容多載收斂

- [x] 2.1 刪除 `dto/CaseDtos.java:80` 的 `CaseCreateRequest` 19 參數與 `CaseUpdateRequest` 19/20/22 參數多載（保留 canonical 55/107 欄位 record），同步更新 `service/CaseServiceTest.java` 改用 canonical 建構，`mvn test` 通過
- [ ] 2.2 刪除 `service/AuthService.java:165` 的 `updateRole(String)` 相容多載（若仍被測試呼叫則一併更新），`mvn test` 通過 — 評估保留，屬簡單委派且 `UserAdminControllerTest` 仍依賴，列為可選

## 3. 引用資料模板化（欠抽象補齊）

- [x] 3.1 於 `service/ReferenceDataService.java:204` 抽 `saveTrimmed()` / `requireExists()` 共用，已抽 `createSimple/updateSimple/deleteSimple` 模板並重構 `Damage/Hint/Method/Delivery/Service` 5 實體，其餘 `SenderType/City/CropCategory/PestCategory` 保留原樣（差異大），`mvn test` 通過

## 4. 微簡化（可選，Nit）

- [ ] 4.1 `service/InputSanitizer.java:10` 刪除 `fieldLabel` 多載或遷至 `util/validation/InputSanitizer`，`SenderService.java:108` / `CaseService.java:196` 同步改 import，`mvn test` 通過 — 已保留雙層，遷套件列為 low
- [ ] 4.2 更新 `docs/notebook/security/pentest-2026-09-04.md:6` 踩坑備註：已清可選注入債務，`AGENTS.md:60` ADR 清單若需同步則補 `012/013`

## 5. 驗證與封存

- [ ] 5.1 每步後執行 `mvn test` 與 `openspec validate --specs --changes`，全程綠燈後 `openspec archive reduce-complexity-2026-09-05 -y` — 已 `mvn test 148 passed`、`openspec validate 13 passed`，待最終封存
