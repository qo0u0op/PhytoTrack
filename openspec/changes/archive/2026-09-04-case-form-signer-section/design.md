## Context

見 `proposal.md - Why`。現況：防治建議與診斷簽名人同列左右 `col-md-6`，共用 `row g-3`，視覺貼合。

## Goals / Non-Goals

**Goals:**
- 兩區塊視覺分離，仍在同一卡片內，順序不變（建議在上、簽名人在下）。

**Non-Goals:**
- 欄位重排序或其他版面重構。

## Decisions

### D1 簽名人改 `col-12` 獨立列
- **選擇**：診斷簽名人 `div` 由 `col-md-6` 改 `col-12`（保留內部 checkbox 列表，可選改雙欄排以利用寬度——預設維持單欄清單，僅隔開）。
- **替代考慮**：兩欄間加垂直分隔線——併排仍易誤讀，不如上下分列。

## Risks / Trade-offs

- [表單變長] → 一個區塊高度，可接受。

## Migration Plan

1. **部署**：僅前端模板；無影響。
2. **Rollback**：revert 即回併排。

## Open Questions

- 無（採預設獨立整列；若要簽名人清單雙欄排，實作時可順手調整不另立規格）。
