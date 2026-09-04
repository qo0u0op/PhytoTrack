package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
 * 縣市鄉鎮管理測試 (baseline-and-city-mgmt)
 *
 * 驗證新增／修改／刪除與引用保護（409）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class CityDistrictAdminTest {

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

  @Test
  void 縣市增改與被引用刪除被拒 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String name = "測試縣_%s".formatted (System.nanoTime ());

    // 新增
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/cities")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (Map.of ("name", name))))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.name").value (name))
        .andReturn ();
    long cityId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();
    assertThat (cityId).isPositive ();

    // 修改
    String renamed = name + "_改";
    mockMvc.perform (put ("/api/admin/ref/cities/{id}", cityId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (Map.of ("name", renamed))))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$.name").value (renamed));

    // 新增鄉鎮歸屬該縣市
    MvcResult dcreated = mockMvc.perform (post ("/api/admin/ref/districts")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (Map.of ("name", "測試鄉", "cityId", cityId))))
        .andExpect (status ().isCreated ())
        .andReturn ();
    long districtId = objectMapper.readTree (dcreated.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // 縣市下仍有鄉鎮：刪除被拒
    mockMvc.perform (delete ("/api/admin/ref/cities/{id}", cityId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("REFERENCE_IN_USE"));

    // 先刪鄉鎮（無引用可刪），再刪縣市
    mockMvc.perform (delete ("/api/admin/ref/districts/{id}", districtId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isNoContent ());
    mockMvc.perform (delete ("/api/admin/ref/cities/{id}", cityId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isNoContent ());
  }

  @Test
  void 鄉鎮新增未選縣市被拒 () throws Exception {
    String adminToken = login ("admin", "admin123");
    mockMvc.perform (post ("/api/admin/ref/districts")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"無縣市鄉"}
                """))
        .andExpect (status ().isBadRequest ());
  }

  @Test
  void 被案件引用的鄉鎮刪除被拒 () throws Exception {
    String adminToken = login ("admin", "admin123");
    // 自建案件鎖定田區鄉鎮（不依賴其他測試殘留資料）
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long cropId = ensureCropId (adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliverId = firstId ("/api/ref/deliveries", adminToken);
    long damageId = firstId ("/api/ref/damages", adminToken);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put ("receiveDate", "2026-09-04");
    body.put ("senderPhone", "0933000" + (System.nanoTime () % 100000));
    body.put ("senderDistrictId", districtId);
    body.put ("fieldDistrictId", districtId);
    body.put ("senderTypeId", senderTypeId);
    body.put ("methodId", methodId);
    body.put ("cropId", cropId);
    body.put ("serviceId", serviceId);
    body.put ("deliverId", deliverId);
    body.put ("damageIds", List.of (damageId));
    body.put ("hintIds", List.of (hintId));
    body.put ("pestCategoryIds", List.of (pestCategoryId));
    body.put ("identifierIds", List.of ());
    mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (body)))
        .andExpect (status ().isCreated ());
    // 該鄉鎮被案件引用：刪除回 409
    mockMvc.perform (delete ("/api/admin/ref/districts/{id}", districtId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("REFERENCE_IN_USE"));
    // 不存在的鄉鎮回 404
    mockMvc.perform (delete ("/api/admin/ref/districts/{id}", 999999999L)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isNotFound ());
  }

  private long firstNestedId (String url, String token, String childField) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path (childField).get (0).path ("id").asLong ();
  }

  private long firstId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("id").asLong ();
  }

  private long firstPestCategoryId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("categories").get (0).path ("id").asLong ();
  }

  private long ensureCropId (String token) throws Exception {
    MvcResult r = mockMvc.perform (get ("/api/ref/crop-categories").header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    JsonNode crops = arr.get (0).path ("crops");
    if (crops.isArray () && crops.size () > 0) return crops.get (0).path ("id").asLong ();
    long categoryId = arr.get (0).path ("id").asLong ();
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/crops")
            .header (HttpHeaders.AUTHORIZATION, bearer (token))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"測試作物_%s","cropCategoryId":%d}
                """.formatted (System.nanoTime (), categoryId)))
        .andExpect (status ().isCreated ()).andReturn ();
    return objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();
  }
}
