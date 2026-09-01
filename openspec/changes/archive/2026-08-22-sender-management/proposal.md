## Why

現有送件人隨案件建立且以 `name+phone UNIQUE` 強合併，無法支援 Line/Facebook 等僅有 `displayName` 的來源，也缺乏去重候選、硬刪除與 VIEWER 個資遮蔽。依 `openspec/specs/sender-management/spec.md` 與 ADR-011 需補齊獨立管理與權限控管，並為統計提供 `COALESCE (phone, displayName)` 去重鍵。

## What Changes

- 後端送件人欄位放寬：`name` 可空、`phone` 與 `displayName` 至少一必填；`displayName` 用於來源暱稱顯示 (`name (displayName)` 規則)
- 建案去重：新增 `GET /api/senders/search?q=` 依 `name/phone/displayName` 部分比對回候選，改建案流程為「列候選→人工確認沿用或新建」，不再以 DB `UNIQUE` 強制合併 (移除 `name+phone UNIQUE`，改 `phone` 非空時唯一性視情況保留)
- 送件人管理端點 (ADMIN 硬刪除)：`GET /api/senders` 列表/搜尋、`GET /api/senders/{id}` 詳細、`DELETE /api/senders/{id}` 硬刪除 (被案件引用回 409 `REFERENCE_IN_USE`，無 soft delete)
- 權限遮蔽：`CaseResponse`/`CaseSummaryResponse` 依角色回應——VIEWER 隱藏 `senderName/phone/address` 但保留 `district/city`，STAFF/ADMIN 回完整
- `CaseResponse` 補 `senderId`、`senderCityName`/`senderDistrictName` 明確縣市鄉鎮；列表與詳細皆同
- 統計：`CaseService.statistics` 的不重複送件人改 `COALESCE (phone, displayName)` 聚合
- 前端新增 `frontend/src/views/SendersView.vue` (ADMIN 列表/搜尋/刪除)、建案表單整合去重候選彈窗、列表/詳細的 VIEWER 遮蔽顯示

## Capabilities

### New Capabilities

<!-- 主規格已含 sender-management，本 change 採 skip_specs: true，不新增 capability 檔案 -->

### Modified Capabilities

<!-- 本 change 不修改 spec 需求，僅實作主規格已定義的 sender-management 需求 -->

## Impact

- 後端：`models/Sender.java` (欄位放寬與約束)、`models/Case.java` 無變、`repository/SenderRepository.java`、`service/SenderService.java` (新增)、`controller/SenderController.java` (新增)、`service/CaseService.java` (統計去重、遮蔽邏輯、`toDetail/toSummary`)、`dto/CaseDtos.java` (`senderId` 與縣市鄉鎮)、`security` 無變、遷移腳本 (移除 UNIQUE)
- 前端：`views/SendersView.vue`、`views/CasesView.vue`/`CaseDetailView.vue`/`CaseFormView.vue` (遮蔽與候選)、`api/index.ts` (`senderApi`)、`types/api.ts` (重新生成)、`router/index.ts` (ADMIN 路由)
- 文件與測試：`docs/ARCHITECTURE.md` (補端點與遮蔽說明)、`docs/REQUIREMENTS.md`、`openspec validate`、新增 `SenderControllerTest`、`SenderServiceTest` 與整合測試
