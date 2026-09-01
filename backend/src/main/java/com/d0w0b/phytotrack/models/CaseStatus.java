package com.d0w0b.phytotrack.models;

/**
 * 案件狀態列舉 (Case Status)
 *
 * 以 @Enumerated (EnumType.ORDINAL) 儲存於 status INTEGER 欄位 (0/1/2)，
 * 與既有資料直接對應、無需遷移。**警告：序數即持久化值，不得重排或插值**。
 *
 * 轉移規則 (見 case-lifecycle design)：
 *   PENDING → RESOLVED：STAFF/ADMIN
 *   RESOLVED → CLOSED ：僅 ADMIN
 *   其餘變動為非法轉移 (400 INVALID_STATUS_TRANSITION)
 */
public enum CaseStatus {
  PENDING,
  RESOLVED,
  CLOSED,
}