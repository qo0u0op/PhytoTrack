package com.d0w0b.phytotrack.dto;

import java.util.List;

/**
 * AI 診斷相關的資料傳輸物件 (DTO)
 */
public final class AiDtos {

  private AiDtos () {
  }

  /**
   * AI 診斷請求
   *
   * 由診斷表單的欄位組成，後端 AIService 會將這些欄位組進給 LLM 的提示詞 (Prompt)。
   */
  public record AnalyzeRequest (String cropName,
      String cropCategory,
      List<String> damages,
      List<String> pestCategories,
      List<String> pestNotes,
      String caseDescription,
      String pestDescription,
      String cropScale,
      String damageScale,
      String cultivationMethod,
      String hintDescription) {
  }

  /** AI 診斷回應 */
  public record AnalyzeResponse (String suggestion,
      long elapsedMs) {
  }
}