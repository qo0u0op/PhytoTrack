package com.d0w0b.phytotrack.dto;

import java.util.List;

/**
 * 案件統計（Case Statistics）相關的資料傳輸物件（DTO）
 */
public final class StatisticsDtos {

  private StatisticsDtos() {
  }

  /** 統計總覽回應：總數、本月新增、待處理、topN、狀態比例與近 N 月趨勢 + 複合案件 + 期別 breakdown */
  public record CaseStatisticsResponse(
      long totalCases,
      long monthNewCases,
      long pendingCases,
      long distinctSenders,
      List<CountName> topCrops,
      List<CountName> topPestCategories,
      List<StatusCount> statusRatio,
      List<MonthCount> monthlyTrend,
      long compositeCases,
      List<CountName> cropCategoryBreakdown,
      List<CountName> pestTypeBreakdown,
      List<CountName> deliveryBreakdown,
      List<CountName> methodBreakdown,
      List<CountName> hintBreakdown,
      long compositeFactorCases,
      long compositeHintCases,
      List<Integer> availableYears,
      String period,
      Integer periodYear,
      Integer periodMonth,
      long periodTotal) {

    // 相容 8 參數建構（無 breakdown，ControllerTest 仍使用）
    public CaseStatisticsResponse(long totalCases, long monthNewCases, long pendingCases, long distinctSenders,
        List<CountName> topCrops, List<CountName> topPestCategories,
        List<StatusCount> statusRatio, List<MonthCount> monthlyTrend) {
      this(totalCases, monthNewCases, pendingCases, distinctSenders, topCrops, topPestCategories, statusRatio, monthlyTrend, 0L,
          List.of(), List.of(), List.of(), List.of(), List.of(), 0L, 0L, List.of(), "HISTORICAL", null, null, totalCases);
    }
  }

  /** 具名計數（top10 用）：名稱與件數 */
  public record CountName(String name, long count) {
  }

  /** 狀態計數：狀態字串（PENDING/RESOLVED/CLOSED）與件數 */
  public record StatusCount(String status, long count) {
  }

  /** 月份計數：YYYY-MM 與件數 */
  public record MonthCount(String month, long count) {
  }
}