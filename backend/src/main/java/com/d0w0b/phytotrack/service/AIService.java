package com.d0w0b.phytotrack.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.d0w0b.phytotrack.dto.AiDtos.AnalyzeRequest;
import com.d0w0b.phytotrack.dto.AiDtos.AnalyzeResponse;

import java.util.List;

/**
 * AI 診斷服務 (AI Service)
 *
 * 透過 Spring AI 的 ChatClient 與後端的 llama.cpp (OpenAI 相容格式) 對話。
 * 設計重點：
 *   - 前端不直接連 llama.cpp，而是由後端代理，避免暴露內網位址與憑證
 *   - 採用非串流 (non-streaming) 的 .call ()：等待完整結果一次回傳，
 *     與前端 sweetalert2 的等待彈窗搭配，實作單純
 *   - 診斷提示詞 (Prompt) 由欄位動態組出，要求模型以臺灣農業的
 *     病蟲害防治建議回覆
 */
@Service
public class AIService {

  private final ChatClient chatClient;
  private final RestClient restClient;
  private final String healthUrl;
  private final String provider;
  private final String baseUrl;
  private final String apiKey;

  public AIService (ChatClient.Builder builder,
                   @Value ("${app.ai.health-url}") String healthUrl,
                   @Value ("${app.ai.provider:local}") String provider,
                   @Value ("${spring.ai.openai.base-url:}") String baseUrl,
                   @Value ("${spring.ai.openai.api-key:}") String apiKey) {
    this.chatClient = builder.build ();
    this.restClient = RestClient.builder ().build ();
    this.healthUrl = healthUrl;
    this.provider = provider;
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
  }

  /**
   * 送出診斷請求並取得建議
   *
   * 使用 Spring AI 的 System / User 角色提示詞結構：
   *   - System：定義模型的身分與回應規則
   *   - User：帶入使用者填寫的診斷表單內容
   */
  public AnalyzeResponse analyze (AnalyzeRequest request) {
    // 當前 AnalyzeRequest 僅含作物/病蟲害/描述等非個資，已符合 Viewer 可見範圍，無需遮蔽
    AnalyzeRequest filtered = request;
    long start = System.currentTimeMillis ();
    String suggestion = chatClient.prompt ()
        .system (buildSystemPrompt ())
        .user (buildUserPrompt (filtered))
        .call ()
        .content ();
    long elapsed = System.currentTimeMillis () - start;
    // 日誌標記 provider 以利稽核，外部模式不印 prompt 明文
    org.slf4j.LoggerFactory.getLogger (AIService.class).debug ("AI analyze provider={} elapsed={}ms", provider, elapsed);
    return new AnalyzeResponse (suggestion, elapsed);
  }

  /**
   * 檢查 AI 是否健康
   *
   * local: 檢查 llama.cpp 的 /health（需 ok 且 models_loaded>0）
   * external (opencode Go 等): 檢查 /v1/models 需 200 且含模型（健康等同可連線，/health 不存在）
   */
  public boolean isHealthy () {
    // external 優先檢查 /v1/models（opencode Go 無 /health）
    if ("external".equalsIgnoreCase (provider)) {
      try {
        String url = baseUrl.endsWith ("/v1") ? baseUrl + "/models" : baseUrl + "/v1/models";
        // 部分 external 需 api-key，帶上 Authorization 以免 401 誤判為不健康
        var req = restClient.get ().uri (url);
        if (apiKey != null && !apiKey.isBlank () && !apiKey.equals ("llama-local-dummy-key")) {
          req = req.header ("Authorization", "Bearer " + apiKey);
        }
        String body = req.retrieve ().body (String.class);
        return body != null && body.contains ("mimo") || body != null && body.contains ("model");
      } catch (Exception e) {
        return false;
      }
    }
    try {
      String body = restClient.get ().uri (healthUrl).retrieve ().body (String.class);
      if (body == null || !body.contains ("ok")) return false;
      // 若回應含 models_loaded，須 >0 才算健康（避免空載時誤判已連線）
      int idx = body.indexOf ("\"models_loaded\"");
      if (idx >= 0) {
        int colon = body.indexOf (':', idx);
        if (colon >= 0) {
          int end = colon + 1;
          while (end < body.length () && Character.isWhitespace (body.charAt (end))) end++;
          int start = end;
          while (end < body.length () && Character.isDigit (body.charAt (end))) end++;
          if (start < end) {
            int loaded = Integer.parseInt (body.substring (start, end));
            if (loaded == 0) return false;
          }
        }
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private String buildSystemPrompt () {
    return """
        你是一位專業的臺灣農業病蟲害診斷專家，服務於植物病蟲害診斷諮詢服務站。
        請根據使用者提供的作物與症狀，判斷可能的病蟲害種類，並以繁體中文回覆。
        回覆時請一律使用臺灣慣用之病蟲害學名與農藥商品名稱 (以農委會核准之臺灣登記名稱為準，避免使用中國或日本慣用譯名；學名請標示拉丁學名)。
        回覆格式請遵守：
        1. 開頭先說明可能的診斷結果 (1～3 種，附理由；若使用者有提供學名/描述請優先參考)
        2. 第二段給出具體的防治建議 (用藥請標示臺灣慣用商品名與有效成分，併附農藝防治措施)
        3. 最後提醒：若症狀持續惡化，建議採集樣本至現場診斷站確認
        4. 若使用者已填「是否已採取防治措施及其效果」，表示該防治措施已嘗試或正在
        使用；防治建議須排除與其重複者，改以補充、調整或建議尚未嘗試的替代措施為主
        請勿編造不存在的農藥名稱；不確定時請明確說明。""";
  }

  private String buildUserPrompt (AnalyzeRequest request) {
    return """
        作物名稱：%s
        作物類別：%s
        被害部位：%s
        病蟲害分類 (可多選)：%s
        病蟲害學名/描述 (pest_note)：%s
        土壤、栽培、用藥紀錄：%s
        種植面積：%s
        被害面積或植株數：%s
        耕種方式：%s
        是否已採取防治措施及其效果：%s""".formatted (nullToEmpty (request.cropName ()),
            nullToEmpty (request.cropCategory ()),
            join (request.damages ()),
            join (request.pestCategories ()),
            join (request.pestNotes ()),
            nullToEmpty (request.caseDescription ()),
            nullToEmpty (request.cropScale ()),
            nullToEmpty (request.damageScale ()),
            nullToEmpty (request.cultivationMethod ()),
            nullToEmpty (request.hintDescription ()));
  }

  private static String join (List<String> values) {
    return values == null || values.isEmpty () ? "未提供" : String.join ("、", values);
  }

  private static String nullToEmpty (String value) {
    return value == null ? "未提供" : value;
  }
}