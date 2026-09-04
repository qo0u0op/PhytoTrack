## Context

見 proposal.md。現況：`schema.sql` 內建 68 筆作物；縣市鄉鎮無管理端點（`ReferenceDataAdminController` 僅既有 9 類）與管理分頁籤（`ReferenceDataAdminView.vue` TabKey 九項）；測試以 `firstCropId` 取種子作物（`crop-categories[0].crops[0]`），基線移除作物後需自建。

## Goals / Non-Goals

**Goals:**
- 新庫無作物；測試／開發有獨立初始檔；縣市鄉鎮可管；分頁籤依新順序。

**Non-Goals:**
- 既有作物資料遷移（正式庫作物保留，僅新庫受影響）。
- 縣市鄉鎮 id 重編（去重僅刪除多餘列，不重排 id）。

## Decisions

### D1 作物種子刪除＋另立初始檔
- **選擇**：`schema.sql` 刪除 68 筆 `INSERT INTO crops` 列（保留表結構與 `UNIQUE`）；新增 `backend/src/main/resources/schema-baseline.sql`（或 `docs/` 引用）內容＝業務初始（表結構＋參照種子，去作物／業務），供測試／開發建庫；`application-test.yaml` 與開發指引切換至初始檔。
- **替代考慮**：僅刪種子不另立檔——測試／開發仍需一份基線描述，不如成文。

### D2 縣市鄉鎮 CRUD（比照既有參照模式）
- **選擇**：`ReferenceDataAdminController` 新增 `/cities`、`/districts` 的 `POST/PUT/DELETE`（`ADMIN`，刪除走引用檢查 `409`：縣市被鄉鎮／送件人／案件引用、鄉鎮被送件人／案件引用）；`ReferenceDataService` 新增對應方法；鄉鎮寫入 `districtId/cityId` 必填由 `@NotNull` 保證。`GET` 沿用既有 `/api/ref/cities`。
- **替代考慮**：STAFF 可管——與既有刪除僅 ADMIN 一致，維持 ADMIN。

### D3 分頁籤重排
- **選擇**：`TabKey` 依新順序重排並新增 `cities`、`districts` 兩項置首；其餘（作物、害物分類等）接續維持相對順序；預設 tab 改為 `cities`（首位）。
- **替代考慮**：維持預設 `damages`——首位已換，預設跟隨首位較一致。

### D4 未知去重與 DB 重建
- **選擇**：刪除 `(3,未知,2)`（假設待審），保留 `(2,未知,2)`；測試庫刪除重建，開發庫重建（有重要資料先備份）；依賴種子作物的測試改自建（`firstCropId` 改查後建或內聯）。
- **替代考慮**：保留重複——查詢去重鍵不受影響但管理顯示混亂，不如刪除。

## Risks / Trade-offs

- [新庫無作物致舊流程報錯] → 預期變更；管理頁／案件內聯可建，文件載明。
- [测试改自建作物量大] → 集中改 `firstCropId` 等 helper，接受。
- [未知去重假設錯誤] → proposal 已標待審，實作前確認。

## Migration Plan

1. **DB**：測試／開發庫刪除重建；正式庫不動（作物保留）。
2. **部署**：後端（端點＋種子）先上，前端（分頁籤＋順序）後上，型別重生。
3. **Rollback**：revert 即回；已刪作物種子需重跑舊 schema.sql。

## Open Questions

- 無（未知去重列與分頁籤尾段順序已標假設待審）。
