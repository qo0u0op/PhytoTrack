package com.d0w0b.phytotrack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.d0w0b.phytotrack.exception.ApiException;
import com.d0w0b.phytotrack.repository.IdentifierRepository;
import com.d0w0b.phytotrack.service.ReferenceDataService;
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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles ("test")
class SignerEdgeBatch2Test {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ReferenceDataService referenceDataService;

  @Autowired
  private IdentifierRepository identifierRepository;

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
    JsonNode crops = arr.get (0).path ("crops");
    // 業務初始無作物種子：為空時自建（保持測試獨立於種子）
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

  private long firstPestCategoryId (String url, String token) throws Exception {
    MvcResult r = mockMvc.perform (get (url).header (HttpHeaders.AUTHORIZATION, bearer (token)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode arr = objectMapper.readTree (r.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    return arr.get (0).path ("categories").get (0).path ("id").asLong ();
  }

  private Map<String, Object> validCaseBody (String adminToken, List<Long> identifierIds) throws Exception {
    long districtId = firstNestedId ("/api/ref/cities", adminToken, "districts");
    long senderTypeId = firstId ("/api/ref/sender-types", adminToken);
    long methodId = firstId ("/api/ref/methods", adminToken);
    long cropId = firstCropId ("/api/ref/crop-categories", adminToken);
    long serviceId = firstId ("/api/ref/services", adminToken);
    long deliveryId = firstId ("/api/ref/deliveries", adminToken);
    long damageId = firstId ("/api/ref/damages", adminToken);
    long hintId = firstId ("/api/ref/hints", adminToken);
    long pestCategoryId = firstPestCategoryId ("/api/ref/pest-types", adminToken);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put ("receiveDate", "2026-09-03");
    body.put ("senderName", "邊界二批");
    body.put ("senderPhone", "0912000" + (System.nanoTime () % 10000));
    body.put ("senderAddress", "測試路 1 號");
    body.put ("senderDistrictId", districtId);
    body.put ("fieldDistrictId", districtId);
    body.put ("senderTypeId", senderTypeId);
    body.put ("methodId", methodId);
    body.put ("cropId", cropId);
    body.put ("serviceId", serviceId);
    body.put ("deliverId", deliveryId);
    body.put ("damageIds", List.of (damageId));
    body.put ("hintIds", List.of (hintId));
    body.put ("pestCategoryIds", List.of (pestCategoryId));
    body.put ("identifierIds", identifierIds);
    return body;
  }

  private long userIdOf (String adminToken, String username) throws Exception {
    MvcResult list = mockMvc.perform (get ("/api/admin/users").header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ()).andReturn ();
    JsonNode users = objectMapper.readTree (list.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    for (JsonNode u : users) if (username.equals (u.path ("username").asText ())) return u.path ("userId").asLong ();
    throw new IllegalStateException ("找不到使用者：" + username);
  }

  @Test
  void 大小寫全半形視為同名 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String base = "NormAbc%s".formatted (System.nanoTime ());
    mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (base)))
        .andExpect (status ().isCreated ());

    // 小寫變體應 409
    mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (base.toLowerCase (java.util.Locale.ROOT))))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("DISPLAY_NAME_EXISTS"));

    // 全形變體應 409（NFKC 統一）
    StringBuilder full = new StringBuilder ();
    for (char c : base.toCharArray ()) {
      if (c >= 0x21 && c <= 0x7E) full.append ((char) (c + 0xFEE0));
      else full.append (c);
    }
    mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (Map.of ("name", full.toString ()))))
        .andExpect (status ().isConflict ())
        .andExpect (jsonPath ("$.error.code").value ("DISPLAY_NAME_EXISTS"));
  }

  @Test
  void 綁定名實不符被拒 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String signerName = "外部_%s".formatted (System.nanoTime ());
    MvcResult created = mockMvc.perform (post ("/api/admin/ref/identifiers")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"name":"%s"}
                """.formatted (signerName)))
        .andExpect (status ().isCreated ()).andReturn ();
    long signerId = objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    String uname = "bind_" + System.nanoTime ();
    String display = "綁定_%s".formatted (System.nanoTime ());
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, display)))
        .andExpect (status ().isCreated ());
    long uid = userIdOf (adminToken, uname);

    mockMvc.perform (post ("/api/admin/ref/identifiers/{id}/bind", signerId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"userId":%d}
                """.formatted (uid)))
        .andExpect (status ().isBadRequest ())
        .andExpect (jsonPath ("$.error.code").value ("SIGNER_NAME_MISMATCH"));
  }

  @Test
  void 停用後自動帶入重用舊筆不累積 () throws Exception {
    String adminToken = login ("admin", "admin123");
    String uname = "reuse_" + System.nanoTime ();
    String display = "重用_%s".formatted (System.nanoTime ());
    mockMvc.perform (post ("/api/auth/register")
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"username":"%s","displayName":"%s","password":"password123"}
                """.formatted (uname, display)))
        .andExpect (status ().isCreated ());
    long uid = userIdOf (adminToken, uname);
    mockMvc.perform (patch ("/api/admin/users/{id}/role", uid)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"role":"ROLE_STAFF"}
                """))
        .andExpect (status ().isOk ());
    String staffToken = login (uname, "password123");

    MvcResult me = mockMvc.perform (get ("/api/ref/identifiers/me").header (HttpHeaders.AUTHORIZATION, bearer (staffToken)))
        .andExpect (status ().isOk ()).andReturn ();
    long myId = objectMapper.readTree (me.getResponse ().getContentAsString (StandardCharsets.UTF_8)).path ("id").asLong ();

    // ADMIN 停用該簽名人
    mockMvc.perform (patch ("/api/admin/ref/identifiers/{id}/active", myId)
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content ("""
                {"active":false}
                """))
        .andExpect (status ().isOk ());

    // 建案空清單自動帶入應重用同一筆（啟用舊筆），不新增
    MvcResult created = mockMvc.perform (post ("/api/cases")
            .header (HttpHeaders.AUTHORIZATION, bearer (staffToken))
            .contentType (MediaType.APPLICATION_JSON)
            .content (objectMapper.writeValueAsString (validCaseBody (adminToken, List.of ()))))
        .andExpect (status ().isCreated ())
        .andExpect (jsonPath ("$.identifiers[0].id").value (myId))
        .andReturn ();
    assertThat (objectMapper.readTree (created.getResponse ().getContentAsString (StandardCharsets.UTF_8))
        .path ("identifiers").get (0).path ("id").asLong ()).isEqualTo (myId);
  }

  @Test
  @Transactional
  void 最後一個停用被阻擋 () {
    var fresh = referenceDataService.createIdentifier ("最後守衛_%s".formatted (System.nanoTime ()));
    var others = identifierRepository.findByActiveTrue ().stream ()
        .filter (i -> !i.getIdentifierId ().equals (fresh.id ()))
        .toList ();
    for (var o : others) {
      referenceDataService.updateIdentifierActive (o.getIdentifierId (), false);
    }
    assertThatThrownBy (() -> referenceDataService.updateIdentifierActive (fresh.id (), false))
        .isInstanceOf (ApiException.class)
        .satisfies (e -> assertThat (((ApiException) e).getCode ()).isEqualTo ("LAST_ACTIVE_SIGNER"));
  }

  @Test
  void 簽名人清單含身分別與帳號欄位 () throws Exception {
    String adminToken = login ("admin", "admin123");
    MvcResult res = mockMvc.perform (get ("/api/ref/identifiers").param ("includeInactive", "true")
            .header (HttpHeaders.AUTHORIZATION, bearer (adminToken)))
        .andExpect (status ().isOk ())
        .andExpect (jsonPath ("$[0].id").exists ())
        .andExpect (jsonPath ("$[0].name").exists ())
        .andExpect (jsonPath ("$[0].active").exists ())
        .andReturn ();
    JsonNode list = objectMapper.readTree (res.getResponse ().getContentAsString (StandardCharsets.UTF_8));
    assertThat (list.isArray ()).isTrue ();
    // userId/username 欄位存在（可為 null）
    assertThat (list.get (0).has ("userId")).isTrue ();
    assertThat (list.get (0).has ("username")).isTrue ();
  }
}
