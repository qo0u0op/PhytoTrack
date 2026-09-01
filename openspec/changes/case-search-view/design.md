# Design: case-search-view 案件篩選視圖化

## Context

- 契約：`openspec/specs/case-search/spec.md`（現 6 欄），本次擴至 14 欄：送件人三欄合一（`name/displayName/phone`）、服務類別、送件方式、**田區**縣市/鄉鎮、作物類別、害物、害物類別、建議類別，皆 `AND` 組合，維持分頁。
- 現況：`CaseService.list` 以 `CaseSpecifications.build()` 對實表 `cases` 組 `Specification`，`CasesView.vue` 篩選卡 6 控件；`schema.sql` 無視圖，多對多篩選需 `EXISTS` 且 N+1 風險隨維度增加。
- 約束：`spring.jpa.open-in-view:false`、`Hikari pool 1`、`SQLite`、多對多頓號顯示、縣市必先選、`hint_id` 定版、`dashboard` 另案；**田區位置 `field_district_id NOT NULL` 與診斷卡片同交易原子寫入**。

## Goals / Non-Goals

- **Goals**：以 `CREATE VIEW v_case_search` 收斂 9 維度篩選與頓號聚合，`LEFT OUTER JOIN` 涵蓋可空關聯，提供 `CaseFilter` 14 欄透傳與前端 4 列篩選卡（**田區**縣市→鄉鎮聯動）；田區位置必填且與縣市/鄉鎮篩選語意一致。
- **Non-Goals**：不引入物化視圖；`dashboard` 統計由 `case-statistics`（已歸檔）另案交付，本 change 僅保留視圖命名共用。

## Decisions

### D1. 視圖 `v_case_search` 以 LEFT OUTER JOIN + GROUP_CONCAT（田區語意）

```sql
CREATE VIEW IF NOT EXISTS v_case_search AS
SELECT c.case_id, c.receive_date, c.status, c.created_at,
       s.name AS sender_name, s.display_name, s.phone,
       fd.district_id AS district_id, fd.city_id AS city_id,
       cr.crop_id, cc.crop_category_id,
       c.service_id, c.deliver_id, c.method_id,
       CAST(COUNT(DISTINCT cpc.cpc_id) AS INTEGER) AS pest_category_count,
       CAST(REPLACE(GROUP_CONCAT(DISTINCT pc.pest_category), ',', '、') AS TEXT) AS pest_category_names,
       CAST(REPLACE(GROUP_CONCAT(DISTINCT h.hint), ',', '、') AS TEXT) AS hint_names,
       CAST(REPLACE(GROUP_CONCAT(DISTINCT dm.damage), ',', '、') AS TEXT) AS damage_names
FROM cases c
LEFT JOIN senders s ON s.sender_id=c.sender_id
LEFT JOIN districts fd ON fd.district_id=c.field_district_id
LEFT JOIN crops cr ON cr.crop_id=c.crop_id
LEFT JOIN crop_categories cc ON cc.crop_category_id=cr.crop_category_id
LEFT JOIN case_pest_categories cpc ON cpc.case_id=c.case_id
LEFT JOIN pest_categories pc ON pc.pest_category_id=cpc.pest_category_id
LEFT JOIN case_hints ch ON ch.case_id=c.case_id
LEFT JOIN hints h ON h.hint_id=ch.hint_id
LEFT JOIN case_damages cd ON cd.case_id=c.case_id
LEFT JOIN damages dm ON dm.damage_id=cd.damage_id
GROUP BY c.case_id, c.receive_date, c.status, c.created_at, s.name, s.display_name, s.phone, fd.district_id, fd.city_id, cr.crop_id, cc.crop_category_id, c.service_id, c.deliver_id, c.method_id;
```

- 多對多篩選以 `EXISTS` 精確匹配（避免 `GROUP_CONCAT LIKE` 誤判），顯示以頓號聚合一次產生。
- 替代捨棄：實表 `Specification` 逐欄 `JOIN`（維度爆炸）、`GROUP_CONCAT` 篩選（無法精確 `pestCategoryId`）、物化視圖（SQLite 無）。

### D2. 實體 `CaseSearchView` 映射視圖

`@Entity @Table(name="v_case_search") @Immutable`，僅 `case_id` 為 `@Id`，其餘為 `@Column` 只讀；`CaseSearchViewRepository` 繼承 `JpaSpecificationExecutor<CaseSearchView>` 提供分頁。

### D3. Service 先視圖分頁再回補實體

`CaseService.list(CaseFilter, Pageable)`：`filter.isEmpty()` 仍走實表 `findAll(Pageable)`；否則 `viewRepo.findAll(buildView(filter), pageable)` 取 `case_id` 頁，再 `caseRepo.findAllById(ids)` + `@EntityGraph` 回補 `toSummary`（保留 `isViewer()` 遮蔽），`exportCsv` 同切視圖；`buildView` 對 `senderQuery` 三欄 `OR LIKE`（跳脫 `%/_`），`city/district/cropCategory/service/delivery/pestType/pestCategory/hint` 為 `equal/EXISTS`。

### D4. 前端篩選卡 4 列與必選縣市（田區語意）

`CasesView.vue`：`CaseFilters` 擴 14 欄，縣市/鄉鎮標示為「田區縣市/田區鄉鎮」並以 `v_case_search` 的 `fd.city_id/district_id` 篩選；`loadFilterOptions()` 併取 `cities/cropCategories/pestTypes/hints/deliveries`，鄉鎮 `disabled` 直至縣市選定（`watch` 聯動同 `CaseFormView.vue:149`），害物→害物類別聯動；`caseApi.list/exportCsv` 同步參數，`clearFilters()` 重置 14 欄。`CaseFormView.vue` 田區位置必填（`@NotNull` + `fieldSameAsSender` 同步）與診斷卡片同交易。

## Risks / Trade-offs

- [視圖 `GROUP BY` 分頁 `count` 準確性] → 以 `viewRepo` 的 `Specification` 配合 `Pageable` 驗證，fallback 為子查詢取 `case_id` 再分頁。
- [SQLite VIEW 只讀] → 寫入仍走實表，無影響。
- [頓號聚合與篩選分離] → 顯示用聚合、篩選用 `EXISTS`，避免顯示與篩選耦合。

## Migration Plan

- `schema.sql`：`cases.field_district_id INTEGER NOT NULL REFERENCES districts`（`Case.java nullable=false` + `CaseCreateRequest @NotNull` + `CaseService` 單一 `@Transactional` 同寫卡片）；既有 `diagnoses.db` 需遷移：`PRAGMA foreign_keys=off` → `cases_new` 重建 → 80% 同 `senders.district_id`、20% 同縣市他鄉鎮回填 → 重建 `v_case_search`（田區語意）→ `PRAGMA foreign_keys=on`（腳本 `scripts/migrate-field-district.sh`，詳 `docs/DEPLOY.md#既有資料庫遷移`）。
- `v_case_search`：`DROP VIEW IF EXISTS` + `CREATE VIEW` 以田區 `fd` 投影 `city_id/district_id`，`ddl-auto: update` 首次啟動自動建立；測試庫 `target/phytotrack-test.db` 刪除重建即可。

## Open Questions

- 無（`deliveryId` 新增、`LEFT JOIN`、`hint_id`、`v_case_search` 命名已於提問確認）。
