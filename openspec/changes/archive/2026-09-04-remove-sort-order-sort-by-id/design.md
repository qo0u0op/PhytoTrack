## Context

見 proposal.md。現況：`City.sortOrder`、`District.sortOrder`（`int`，`nullable=false`）、`CityRepository.findAllByOrderBySortOrderAsc`（`@EntityGraph districts`）、`toCityResponse` 內 `comparingInt(getSortOrder)`、`DistrictItem(id, name, sortOrder)`。種子 `id` 與 `sort_order` 完全同序（縣市 `id==sort_order`；鄉鎮每縣市內 `sort_order` 1..N 且 `district_id` 全域遞增同序），故改 id 排序呈現不變。測試無 `sortOrder` 引用。前端縣市鄉鎮未使用 `sortOrder`（僅害物分類前端型別有，同名不同物，不在範圍）。

## Goals / Non-Goals

**Goals:**
- 移除兩欄位、兩索引、種子值、實體欄位、查詢方法、DTO 欄位，排序一律 id 升冪。

**Non-Goals:**
- 害物分類排序變更（已依 `pestCategoryCode`，不動）。
- 前端縣市鄉鎮呈現邏輯變更（僅重生型別）。

## Decisions

### D1 查詢改 `findAllByOrderByCityIdAsc`，鄉鎮改 `comparingLong(getDistrictId)`
- **選擇**：Repository 衍生查詢改名（`findAllByOrderByCityIdAsc`，保留 `@EntityGraph`）；`toCityResponse` 改 `Comparator.comparing(District::getDistrictId)`；`DistrictItem` 刪第三參數。
- **替代考慮**：`findAll()`＋記憶體排序——衍生查詢語意明確且與原寫法對稱，不改寫風格。

### D2 舊庫孤兒欄位留置（不自動遷移）
- **選擇**：SQLite `ddl-auto: update` 不刪欄，舊庫 `sort_order` 成孤兒欄，JPA 不再映射故無害；`DEPLOY.md` 加註（新庫直接無該欄；如需清理可建表搬資料，參照 7.1 模式，非強制）。
- **替代考慮**：啟動期自動重建表——風險高於效益（欄位無害殘留），不做。

### D3 前端僅重生 `types/api.ts`
- **選擇**：以後端啟動＋`openapi-typescript` 重生，`DistrictItem.sortOrder` 消失；不改元件（無引用）。
- **替代考慮**：手改型別——與生成流程背離，不做。

## Risks / Trade-offs

- [若未來需自訂順序] → 屆時重加欄位；目前種子 id 序即需求序，接受。
- [`DistrictItem` 回應少一欄] → 前端無引用；外部呼叫者以 id 排序即可，`DEPLOY.md`/API 一覽註明。

## Migration Plan

1. **DB**：新庫直接生效；既有庫無需動作（孤兒欄位無害），可選清理。
2. **部署**：後端先上＋前端型別重生；舊前端不受影響（多餘欄位忽略，少用欄位本就未用）。
3. **Rollback**：revert 即回 `sort_order`（舊庫欄位仍在，新庫需重建）。

## Open Questions

- 無。
