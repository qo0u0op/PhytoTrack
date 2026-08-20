package com.d0w0b.phytotrack.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.d0w0b.phytotrack.dto.AiDtos.AnalyzeRequest;
import com.d0w0b.phytotrack.dto.AiDtos.AnalyzeResponse;

import java.util.List;

/**
 * AI 診斷服務（AI Service）
 *
 * 透過 Spring AI 的 ChatClient 與後端的 llama.cpp（OpenAI 相容格式）對話。
 * 設計重點：
 *   - 前端不直接連 llama.cpp，而是由後端代理，避免暴露內網位址與憑證
 *   - 採用非串流（non-streaming）的 .call()：等待完整結果一次回傳，
 *     與前端 sweetalert2 的等待彈窗搭配，實作單純
 *   - 診斷提示詞（Prompt）由欄位動態組出，要求模型以臺灣農業的
 *     病蟲害防治建議回覆
 */
@Service
public class AIService {

  private final ChatClient chatClient;
  private final RestClient restClient;
  private final String healthUrl;

  public AIService(ChatClient.Builder builder,
                   @Value("${app.ai.health-url}") String healthUrl) {
    this.chatClient = builder.build();
    this.restClient = RestClient.builder().build();
    this.healthUrl = healthUrl;
  }

  /**
   * 送出診斷請求並取得建議
   *
   * 使用 Spring AI 的 System / User 角色提示詞結構：
   *   - System：定義模型的身分與回應規則
   *   - User：帶入使用者填寫的診斷表單內容
   */
  public AnalyzeResponse analyze(AnalyzeRequest request) {
    long start = System.currentTimeMillis();
    String suggestion = chatClient.prompt()
        .system(buildSystemPrompt())
        .user(buildUserPrompt(request))
        .call()
        .content();
    long elapsed = System.currentTimeMillis() - start;
    return new AnalyzeResponse(suggestion, elapsed);
  }

  /**
   * 檢查 llama.cpp 是否存活
   *
   * llama.cpp 提供 /health 端點回傳 "ok"。後端以 RestClient 主動檢查，
   * 前端可藉此在呼叫前先顯示模型狀態。
   */
  public boolean isHealthy() {
    try {
      String body = restClient.get().uri(healthUrl).retrieve().body(String.class);
      return body != null && body.contains("ok");
    } catch (Exception e) {
      // 模型未啟動或連線失敗都視為不健康
      return false;
    }
  }

  private String buildSystemPrompt() {
    return """
        你是一位專業的臺灣農業病蟲害診斷專家，服務於植物病蟲害診斷諮詢服務站。
        請根據使用者提供的作物與症狀，判斷可能的病蟲害種類，並以繁體中文回覆。
        回覆格式請遵守：
        1. 開頭先說明可能的診斷結果（1～3 種，附理由）
        2. 第二段給出具體的防治建議（用藥或農藝措施）
        3. 最後提醒：若症狀持續惡化，建議採集樣本至現場診斷站確認
         4. 若使用者已填「是否已採取防治措施及其效果」，表示該防治措施已嘗試或正在
        使用；防治建議須排除與其重複者，改以補充、調整或建議尚未嘗試的替代措施為主
        請勿編造不存在的農藥名稱；不確定時請明確說明。""";
  }

  private String buildUserPrompt(AnalyzeRequest request) {
    return """
        作物名稱：%s
        作物類別：%s
        被害部位：%s
        病蟲害分類（可多選）：%s
        病害情形描述：%s
        種植面積：%s
        被害面積或植株數：%s
        耕種方式：%s
        是否已採取防治措施及其效果：%s""".formatted(
            nullToEmpty(request.cropName()),
            nullToEmpty(request.cropCategory()),
            join(request.damages()),
            join(request.pestCategories()),
            nullToEmpty(request.pestDescription()),
            nullToEmpty(request.cropScale()),
            nullToEmpty(request.damageScale()),
            nullToEmpty(request.cultivationMethod()),
            nullToEmpty(request.hintDescription()));
  }

  private static String join(List<String> values) {
    return values == null || values.isEmpty() ? "未提供" : String.join("、", values);
  }

  private static String nullToEmpty(String value) {
    return value == null ? "未提供" : value;
  }
}