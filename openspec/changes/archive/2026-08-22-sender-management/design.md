# Design: sender-management 送件人獨立管理與遮蔽

## Context

見 `proposal.md` (Why)。現況：`Sender` 以 `name+phone UNIQUE` 強約束，隨 `CaseService.create` 去重或新建 (`findByNameAndPhone`)，欄位皆 `nullable=false` 無 `displayName`；`Case` 以 `sender_id` 直連，刪除無獨立端點；`CaseResponse` 含送件人姓名/電話/地址但無 `senderId` 與縣市名稱分離；`VIEWER` 可見完整個資；統計以案件計數為主，未對送件人去重。需在不破壞既有案件完整性下，放寬欄位、引入弱識別去重與權限遮蔽 (見 ADR-011)。

## Goals / Non-Goals

**Goals：**
- 送件人 `name` 可空、`displayName` 新增、`phone/displayName` 至少一必填；顯示規則 `name (displayName)` / `displayName`
- 建案時 `GET /api/senders/search?q=` 回候選 (`name/phone/displayName` 部分比對)，由前端彈窗人工確認沿用或新建，不再以 DB `UNIQUE` 強制合併
- ADMIN 可 `GET /api/senders`、`GET /api/senders/{id}`、`DELETE /api/senders/{id}` (硬刪除，`CaseRepository.existsBySenderSenderId` 則 409 `REFERENCE_IN_USE`)
- VIEWER 對案件列表/詳細的送件人姓名/電話/地址遮蔽，保留 `city/district`
- `CaseResponse` 補 `senderId` 與 `senderCityName`/`senderDistrictName`；統計去重鍵改 `COALESCE (phone, displayName)`

**Non-Goals：**
- 送件人完整 CRUD 的 `PUT /api/senders/{id}` 編輯 (本次僅列表/搜尋/刪除，新增由建案流程建立)
- 送件人合併 (merge) 與去重自動化 (本次僅列候選、人工確認)
- 縣市/鄉鎮寫入 (沿用 reference-data-admin 範圍外)
- `displayName` 的來源標記自動化 (Line/Facebook 串接)

## Decisions

1. **欄位與約束放寬**
   - `Sender.name` 改 `nullable=true`、`displayName` 新增 `nullable=true`、`phone` 改 `nullable=true`；實體層以 Bean Validation (`@NotBlank` 分組) 或 Service 檢查「`phone` 與 `displayName` 至少一有值」拋 `VALIDATION_ERROR` 400。DB 層移除 `UNIQUE (name, phone)`，改 `CREATE UNIQUE INDEX idx_sender_phone ON senders (phone) WHERE phone IS NOT NULL AND phone <> ''` (SQLite 部分索引，`phone` 非空時唯一，允許空 `phone` 多筆)。
   - 理由：支援僅有 `displayName` 的來源，同時避免 `phone` 重複的帳號氾濫。替代方案捨棄：保留 `UNIQUE (name,phone)` —— 無法支援 `displayName` 單獨去重。

2. **去重搜尋**
   - `SenderRepository.search (String q)` 以 `WHERE name LIKE %q% OR phone LIKE %q% OR displayName LIKE %q%` (大小寫不敏感，`LOWER`)，分頁或限前 10 筆，供 `CaseFormView` 在輸入姓名/電話時即時查詢。
   - `CaseService.create` 改為：若前端傳 `senderId` 則沿用，否則依 `search` 回候選 → 前端彈窗 → 使用者選「沿用」或「新建」。Service 不再自行 `findByNameAndPhone` 強合併。

3. **硬刪除與引用檢查**
   - `SenderService.delete (id)` 先 `senderRepository.findById` 404，再 `caseRepository.existsBySenderSenderId (id)` 若 true 拋 `REFERENCE_IN_USE` 409，否則 `senderRepository.delete`。
   - 理由：符合 spec「硬刪除且被引用拒刪」；不引入 soft delete。

4. **VIEWER 遮蔽**
   - `CaseService.toDetail/toSummary` 內以 `SecurityContextHolder` 取得當前 `UserPrincipal.role`，若 `ROLE_VIEWER` 則將回應 DTO 的 `senderName/phone/address` 置 `null` (縣市鄉鎮保留)，`senderId` 仍回 (供統計去重鍵的前端顯示？依 spec 遮蔽僅姓名/電話/地址)。
   - 替代方案捨棄：於 Controller 層遮蔽 —— Service 層統一更不易遺漏。

5. **CaseResponse 擴充**
   - 新增 `senderId` (`Long`)、`senderCityName` (`city.city`)、`senderDistrictName` 已有 (沿用)、`senderDisplayName` (可選)。`toDetail` 以 `sender.getDistrict ().getCity ()` 取縣市。
   - 統計：`CaseService.statistics` 中「不重複送件人」以 `cases.stream ().map (c -> c.getSender ().getPhone ()!=null?phone:displayName).distinct ().count ()` 計算 (記憶體聚合，資料量小可接受)。

6. **前端**
   - `views/SendersView.vue` (`ADMIN` 表格 + 搜尋 + 刪除)
   - `CaseFormView` 在送件人區塊加入「搜尋候選」按鈕，調 `senderApi.search` 後以 `Swal` 列候選供選擇
   - `CasesView`/`CaseDetailView` 依 `auth.isViewer` 遮蔽顯示

## Risks / Trade-offs

- 移除 `UNIQUE (name,phone)` 後歷史重複資料共存 → 遷移時保留既有資料，後續以 `senderId` 為準，不回溯合併
- `LIKE %q%` 全表掃描 → 送件人量小 (<數千) 可接受；日後可加 `GIN` 或前端防抖
- 統計 `COALESCE` 記憶體 distinct → 與 `case-statistics` 的 Java 聚合一致，現階段不引入 SQL `COUNT (DISTINCT COALESCE...)`
- VIEWER 遮蔽在 Service 層做，單元測試需 mock `SecurityContextHolder` (同 `CaseControllerTest` 的 `TestSecurityContextHolderStrategyAdapter` 模式)

## Migration Plan

- DB：`ALTER TABLE senders ADD COLUMN display_name TEXT`；`DROP INDEX` 原 `UNIQUE (name,phone)` (若為隱式索引需重建表或 `CREATE TABLE ...` 搬移，開發期可直接重建 SQLite)；新增部分唯一索引 `idx_sender_phone`；`name` 改 `NULL` 允許
- 部署：後端先上線 (舊案件 `displayName` 為空，顯示回退為 `name`)；前端隨後發布
- 回滾：還原 `Sender` 實體與索引，`displayName` 欄位保留空值不影響舊版

## Open Questions

- `displayName` 是否需唯一？ (本次不唯一，允許同暱稱多筆，由人工確認去重)
- `Sender` 編輯是否納入本次？ (不納入，僅刪除與搜尋；編輯另案)
