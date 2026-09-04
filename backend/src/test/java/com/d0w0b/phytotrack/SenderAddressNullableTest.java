package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 送件人地址可空測試 (case-sender-address-nullable)
 *
 * 驗證三條寫入路徑皆接受空地址並存 null，非空地址行為不變。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class SenderAddressNullableTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper ();

  private String login (String username, String password) throws Exception {
    MvcResult result = mockMvc.perform (post ("/api/auth/login")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","password":"%s"}
                """.formatted (username, password)))
        .andExpect (status ().isOk ())
        .andReturn ();
    return objectMapper.readTree (result.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("token").asText ();
  }

  private static String bearer (String token) {
    return "Bearer " + token;
  }

  private long firstNestedId (String url, String token, String child) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path (child).get (0).path ("id").asLong ();
  }

  private long firstId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("id").asLong ();
  }

  private long firstCropId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("crops").get (0).path ("id").asLong ();
  }

  private long firstPestCategoryId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("categories").get (0).path ("id").asLong ();
  }

  private Map<String, Object> senderBody (String token, boolean withAddress) throws Exception {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put ("phone", "0922000" + (System.nanoTime () % 100000));
    if (withAddress) body.put ("address", "測試路 1 號");
    body.put ("districtId", firstNestedId ("/api/ref/cities", token, "districts"));
    body.put ("senderTypeId", firstId ("/api/ref/sender-types", token));
    return body;
  }

  @Test
  void 送件人獨立新增可空地址 () throws Exception {
    String adminToken = login ("admin", "admin123");
    MvcResult created = mockMvc.perform (post ("/api/senders")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (senderBody (adminToken, false))))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.address").doesNotExist ())
        .andReturn ();
    long senderId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("senderId").asLong ();

    // 查詢顯示為空
    mockMvc.perform (get ("/api/senders/{id}", senderId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.address").doesNotExist ());
  }

  @Test
  void 送件人更新全空白地址視為null () throws Exception {
    String adminToken = login ("admin", "admin123");
    MvcResult created = mockMvc.perform (post ("/api/senders")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (senderBody (adminToken, true))))
        .andExpect (status ().isCreated ())
        .andReturn ();
    long senderId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("senderId").asLong ();

    Map<String, Object> update = senderBody (adminToken, true);
    update.put ("address", "   ");
    mockMvc.perform (put ("/api/senders/{id}", senderId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (update)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.address").doesNotExist ());
  }

  @Test
  void 案件內新建送件人可空地址 () throws Exception {
    String adminToken = login ("admin", "admin123");
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put ("receiveDate", "2026-09-03");
    body.put ("senderPhone", "0933000" + (System.nanoTime () % 100000));
    // 刻意不傳 senderAddress
    body.put ("senderDistrictId", districtId);
    body.put ("fieldDistrictId", districtId);
    body.put ("senderTypeId", firstId ("/api/ref/sender-types", adminToken));
    body.put ("methodId", firstId ("/api/ref/methods", adminToken));
    body.put ("cropId", firstCropId ("/api/ref/crop-categories", adminToken));
    body.put ("serviceId", firstId ("/api/ref/services", adminToken));
    body.put ("deliverId", firstId ("/api/ref/deliveries", adminToken));
    body.put ("damageIds", List.of (firstId ("/api/ref/damages", adminToken)));
    body.put ("hintIds", List.of (firstId ("/api/ref/hints", adminToken)));
    body.put ("pestCategoryIds", List.of (firstPestCategoryId ("/api/ref/pest-types", adminToken)));
    body.put ("identifierIds", List.of ());
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (body)))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.senderAddress").doesNotExist ());
  }

  @Test
  void 非空地址行為不變 () throws Exception {
    String adminToken = login ("admin", "admin123");
    MvcResult created = mockMvc.perform (post ("/api/senders")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (senderBody (adminToken, true))))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.address").value ("測試路 1 號"))
        .andReturn ();
    assertThat (objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("senderId").asLong ()).isPositive ();
  }
}
