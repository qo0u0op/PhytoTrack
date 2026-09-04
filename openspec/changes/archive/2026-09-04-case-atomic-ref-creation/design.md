## Context

見 `proposal.md - Why`：`CaseFormView.vue:365` 與 `396` 的「新增」直接 `POST /admin/ref/*` 立即落庫，後續放棄案件則孤兒殘留。`spring.jpa.open-in-view: false` 下 `CaseService` 已以單交易建立案件與多對多關聯，但作物／簽名人為交易外前置步驟，未納入同一 ` @Transactional`。`signer-lifecycle` 引入 `Identifier.active` 後孤兒更需 `active=false` 人工清理。

## Goals / Non-Goals

**Goals:**
- 表單內新增的作物／簽名人在案件未提交前不落庫，放棄即丟棄
- 提交時與案件於同一交易內建立，去重後關聯，失敗全回滾
- 獨立管理頁仍可直建參照，與內聯路徑共用建表邏輯與校驗

**Non-Goals:**
- 既有 `DELETE` 保護與 `active` 停用語意變更
- 跨頁暫存（重新整理丟棄可接受，符合 5 人內 LAN 表單流程）
- 作物／簽名人的硬一對一約束或快照冗餘

## Decisions

### D1 後端：案件 DTO 內聯欄位並於 `CaseService` 同交易建立

- **選擇**：`CaseDtos.CaseCreateRequest` / `CaseUpdateRequest` 新增 `InlineCrop inlineCrop`（`String name, Long cropCategoryId`）與 `List<String> inlineIdentifiers`；`CaseService.create/update` 開頭若 `inlineCrop != null` 則 `ReferenceDataService.createCrop(name, categoryId)` 或復用（`existsByCropIgnoreCase...` 回 `409` 時查回既有 `id`），若 `inlineIdentifiers` 非空則逐名 `trim` 後 `Identifier` 同名 `active=true` 復用否則 `new Identifier(name, user=null, active=true)`，皆在 `@Transactional` 內，取得 `id` 後併入 `cropId` / `identifierIds` 再走原有參照校驗與 `add*` 關聯。若同時提供顯式 `cropId` 與 `inlineCrop`，以 `inline` 為準；`identifierIds` 與 `inlineIdentifiers` 為並集去重。
- **替代**：前端先建暫存 `id` 再併入案件（兩階段提交）需補償刪除，失敗時孤兒仍可能殘留；純前端暫存不涉交易則後端無需改，但無法保證 ACID，故選後端內聯單交易。

### D2 前端：本地暫存不打 API

- **選擇**：`CaseFormView.vue` 將 `handleCreateCrop` / `handleCreateIdentifier` 改為 `pendingCrops: {name, categoryId}[]` 與 `pendingIdentifiers: string[]` 記憶體暫存，下拉由 `computed mergedCrops = [...cropCategories.flatMap, ...pendingCrops]` 與 `mergedIdentifiers = [...identifiers, ...pendingIdentifiers.map]` 合併顯示（標記 `（待提交）`）。`submit` 時將暫存映射為 `inlineCrop` / `inlineIdentifiers` 併入 `caseApi.create/update` 請求體；成功後清空暫存並 `await refApi.cropCategories()/identifiers()` 重新載入；取消/`router.push /cases` 直接丟棄暫存。
- **替代**：維持立即 `POST` 但放棄時 `DELETE` 補償，需處理併發與已被引用後 `409` 無法刪，且增加請求；本地暫存最簡且無後端副作用。

### D3 去重與復用：同層共用建表邏輯

- **選擇**：`ReferenceDataService.createCrop/createIdentifier` 抽 `findExistingOrCreate` 內部方法，內聯路徑復用該方法：同分類同名作物（`existsByCropIgnoreCaseAnd...`）或同名活躍簽名人（`findByIdentifierAndActiveTrueIgnoreCase`）存在時回既有 `id`，否則新建。管理頁直建仍回 `409` 供前端提示，案件內聯則靜默復用，避免併發重複。
- **替代**：內聯一律新建允重名，會污染 `crops` 唯讀去重與 `Identifier` 去重，違背參照管理。

### D4 交易邊界與交易外查詢

- **選擇**：`CaseService` 整個 `create/update` 保持單 `@Transactional`，內聯建表與關聯寫入同庫事務，`spring.jpa.open-in-view: false` 下不影響 `toDetail` 的 DTO 映射。暫存僅記憶體，不入 `localStorage`，重新整理丟棄符合預期「放棄即無」。
- **替代**：置於 `Converter` 或觸發器處理則脫離 Service 交易，不可控。

## Risks / Trade-offs

- [重新整理丟棄暫存] → 接受：符合放棄語意，5 人內表單填寫時間短，重新整理即視為放棄；緩解：提示「重新整理將丟棄未提交的暫存」可選。
- [內聯與顯式同時提供語意模糊] → 明文化：`inlineCrop` 覆蓋 `cropId`，`inlineIdentifiers` 與 `identifierIds` 並集；緩解：前端提交時若有暫存則清空對應顯式選擇，避免混淆。
- [同名復用可能誤復用非預期同名] → 去重本就要求同名視為同一參照（既有 `REFERENCE_DUPLICATE`），管理頁亦同，故一致；緩解：前端暫存顯示時標記待提交，同名已存在時直接選既有項，不再入暫存。
- [併發兩人同時內聯同名新建] → 首個提交新建，後者事務內復用（`find` 後 `exists` 檢查），`UNIQUE` 衝突時捕捉 `DataIntegrityViolationException` 後查回既有 `id`。

## Migration Plan

1. **DB**：無遷移，`active` 已由 `signer-lifecycle` 引入。
2. **部署**：`mvn test` 後 `CaseFormView` 手測：暫存作物／簽名人後放棄再進表單下拉無殘留；提交後下拉可見且案件詳情含關聯。
3. **前端**：無環境變數，`caseApi` 型別由 `openapi-typescript` 重生（如需）。
4. **Rollback**：移除 `inline*` 欄位與前端暫存即回立即 `POST` 舊路徑，無資料回滾需求。

## Open Questions

- `inlineCrop` 是否需支援 `cropScale` 等擴充屬性？本批僅 `name + cropCategoryId`，後續依表單需求擴。
- `inlineIdentifiers` 是否需攜 `active` 覆蓋？預設 `true`，停用簽名人不經內聯重建。
