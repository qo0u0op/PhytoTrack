package com.d0w0b.phytotrack.dto;

import java.util.List;

/**
 * 案件統計（Case Statistics）相關的資料傳輸物件（DTO）
 */
public final class StatisticsDtos {

  private StatisticsDtos() {
  }

  /** 統計總覽回應：總數、本月新增、待處理、topN、狀態比例與近 N 月趨勢 */
  public record CaseStatisticsResponse(
      long totalCases,
      long monthNewCases,
      long pendingCases,
      long distinctSenders,
      List<CountName> topCrops,
      List<CountName> topPestCategories,
      List<StatusCount> statusRatio,
      List<MonthCount> monthlyTrend) {

    // 相容舊版 7 參數建構
    public CaseStatisticsResponse(long totalCases, long monthNewCases, long pendingCases,
        List<CountName> topCrops, List<CountName> topPestCategories,
        List<StatusCount> statusRatio, List<MonthCount> monthlyTrend) {
      this(totalCases, monthNewCases, pendingCases, 0L, topCrops, topPestCategories, statusRatio, monthlyTrend);
    }
  }

  /** 具名計數（topN 用）：名稱與件數 */
  public record CountName(String name, long count) {
  }

  /** 狀態計數：狀態字串（PENDING/RESOLVED/CLOSED）與件數 */
  public record StatusCount(String status, long count) {
  }

  /** 月份計數：YYYY-MM 與件數 */
  public record MonthCount(String month, long count) {
  }
}