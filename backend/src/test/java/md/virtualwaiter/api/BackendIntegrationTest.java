package md.virtualwaiter.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
    .withDatabaseName("vw")
    .withUsername("vw")
    .withPassword("vw");

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry registry) {
    if (!POSTGRES.isRunning()) {
      POSTGRES.start();
    }
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.qr.hmacSecret", () -> "test_qr_hmac_secret");
    registry.add("app.auth.cookieSecret", () -> "test_auth_cookie_secret");
    registry.add("app.otp.devEchoCode", () -> "true");
  }

  @LocalServerPort
  int port;

  @Autowired
  TestRestTemplate rest;

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void otpOrderBillPartyHallFlows() throws Exception {
    String adminAuth = basicAuth("admin1", "demo123");

    // Ensure table exists
    List<Map<String, Object>> tables = getJsonList("/api/admin/tables", adminAuth);
    String tablePublicId;
    if (tables.isEmpty()) {
      Map<String, Object> created = postJson("/api/admin/tables", adminAuth, Map.of("number", 1));
      tablePublicId = created.get("publicId").toString();
    } else {
      tablePublicId = tables.get(0).get("publicId").toString();
    }
    assertThat(tablePublicId).isNotBlank();

    // Signed URL
    Map<String, Object> signed = getJson("/api/admin/tables/" + tablePublicId + "/signed-url", adminAuth);
    String signedUrl = signed.get("url").toString();
    Map<String, String> query = parseQuery(signedUrl);
    String sig = query.get("sig");
    String ts = query.get("ts");
    assertThat(sig).isNotBlank();
    assertThat(ts).isNotBlank();

    // Start session
    Map<String, Object> session = postJson("/api/public/session/start", null, Map.of(
      "tablePublicId", tablePublicId,
      "sig", sig,
      "ts", Long.parseLong(ts),
      "locale", "ru"
    ));
    int guestSessionId = ((Number) session.get("guestSessionId")).intValue();
    String sessionSecret = session.get("sessionSecret").toString();
    boolean otpRequired = Boolean.TRUE.equals(session.get("otpRequired"));

    // OTP flow (only if required)
    if (otpRequired) {
      Map<String, Object> otpSend = postJson("/api/public/otp/send", sessionSecret, Map.of(
        "guestSessionId", guestSessionId,
        "phoneE164", "+37369000000",
        "locale", "ru"
      ));
      String challengeId = otpSend.get("challengeId").toString();
      String devCode = otpSend.getOrDefault("devCode", "").toString();
      assertThat(devCode).isNotBlank();
      postJson("/api/public/otp/verify", sessionSecret, Map.of(
        "guestSessionId", guestSessionId,
        "challengeId", challengeId,
        "code", devCode
      ));
    }

    // Menu + ensure item
    Map<String, Object> menu = getJson("/api/public/menu?tablePublicId=" + tablePublicId + "&sig=" + sig + "&ts=" + ts + "&locale=ru", null);
    int itemId = firstMenuItemId(menu);
    if (itemId == 0) {
      Map<String, Object> cat = postJson("/api/admin/menu/categories", adminAuth, Map.of(
        "nameRu", "Тест",
        "nameRo", "Test",
        "nameEn", "Test",
        "sortOrder", 1,
        "isActive", true
      ));
      int catId = ((Number) cat.get("id")).intValue();
      Map<String, Object> item = postJson("/api/admin/menu/items", adminAuth, Map.of(
        "categoryId", catId,
        "nameRu", "Тест",
        "nameRo", "Test",
        "nameEn", "Test",
        "descriptionRu", "",
        "descriptionRo", "",
        "descriptionEn", "",
        "price", 10,
        "isActive", true
      ));
      itemId = ((Number) item.get("id")).intValue();
    }
    assertThat(itemId).isGreaterThan(0);

    // Order
    Map<String, Object> order = postJson("/api/public/orders", sessionSecret, Map.of(
      "guestSessionId", guestSessionId,
      "items", List.of(Map.of("menuItemId", itemId, "qty", 1))
    ));
    assertThat(order.get("orderId")).isNotNull();

    // Party
    Map<String, Object> party = postJson("/api/public/party/create", sessionSecret, Map.of("guestSessionId", guestSessionId));
    assertThat(party.get("partyId")).isNotNull();

    // Bill request (tips only if enabled)
    Map<String, Object> settings = getJson("/api/admin/branch-settings", adminAuth);
    boolean tipsEnabled = Boolean.TRUE.equals(settings.get("tipsEnabled"));
    Map<String, Object> billPayload = new java.util.HashMap<>();
    billPayload.put("guestSessionId", guestSessionId);
    billPayload.put("mode", "MY");
    billPayload.put("paymentMethod", "CASH");
    if (tipsEnabled) billPayload.put("tipsPercent", 5);
    postJson("/api/public/bill-request/create", sessionSecret, billPayload);

    // Halls + plans
    List<Map<String, Object>> halls = getJsonList("/api/admin/halls", adminAuth);
    int hallId;
    if (halls.isEmpty()) {
      Map<String, Object> hall = postJson("/api/admin/halls", adminAuth, Map.of("name", "Main", "sortOrder", 0));
      hallId = ((Number) hall.get("id")).intValue();
    } else {
      hallId = ((Number) halls.get(0).get("id")).intValue();
    }
    assertThat(hallId).isGreaterThan(0);

    List<Map<String, Object>> plans = getJsonList("/api/admin/halls/" + hallId + "/plans", adminAuth);
    int planId;
    if (plans.isEmpty()) {
      Map<String, Object> plan = postJson("/api/admin/halls/" + hallId + "/plans", adminAuth, Map.of("name", "Default", "sortOrder", 0));
      planId = ((Number) plan.get("id")).intValue();
    } else {
      planId = ((Number) plans.get(0).get("id")).intValue();
    }
    assertThat(planId).isGreaterThan(0);

    ResponseEntity<String> versions = exchange("/api/admin/hall-plans/" + planId + "/versions", adminAuth, null, HttpMethod.GET);
    assertThat(versions.getStatusCode().value()).isEqualTo(200);
  }

  private Map<String, Object> postJson(String path, String authOrSecret, Object payload) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (authOrSecret != null) {
      if (authOrSecret.startsWith("Basic ")) {
        headers.set("Authorization", authOrSecret);
      } else {
        headers.set("X-Session-Secret", authOrSecret);
      }
    }
    String body = mapper.writeValueAsString(payload);
    ResponseEntity<String> res = rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    assertThat(res.getStatusCode().value())
      .withFailMessage("POST %s failed: %s", path, res.getBody())
      .isEqualTo(200);
    return mapper.readValue(res.getBody(), new TypeReference<Map<String, Object>>() {});
  }

  private Map<String, Object> getJson(String path, String auth) throws Exception {
    ResponseEntity<String> res = exchange(path, auth, null, HttpMethod.GET);
    assertThat(res.getStatusCode().value())
      .withFailMessage("GET %s failed: %s", path, res.getBody())
      .isEqualTo(200);
    return mapper.readValue(res.getBody(), new TypeReference<Map<String, Object>>() {});
  }

  private List<Map<String, Object>> getJsonList(String path, String auth) throws Exception {
    ResponseEntity<String> res = exchange(path, auth, null, HttpMethod.GET);
    assertThat(res.getStatusCode().value())
      .withFailMessage("GET %s failed: %s", path, res.getBody())
      .isEqualTo(200);
    return mapper.readValue(res.getBody(), new TypeReference<List<Map<String, Object>>>() {});
  }

  private ResponseEntity<String> exchange(String path, String auth, String secret, HttpMethod method) {
    HttpHeaders headers = new HttpHeaders();
    if (auth != null) headers.set("Authorization", auth);
    if (secret != null) headers.set("X-Session-Secret", secret);
    return rest.exchange(url(path), method, new HttpEntity<>(headers), String.class);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }

  private static String basicAuth(String user, String pass) {
    String raw = user + ":" + pass;
    return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  private static Map<String, String> parseQuery(String url) throws URISyntaxException {
    URI uri = new URI(url);
    String query = uri.getQuery();
    return java.util.Arrays.stream(query.split("&"))
      .map(s -> s.split("=", 2))
      .collect(java.util.stream.Collectors.toMap(a -> a[0], a -> a.length > 1 ? a[1] : ""));
  }

  private static int firstMenuItemId(Map<String, Object> menu) {
    Object catsObj = menu.get("categories");
    if (!(catsObj instanceof List<?> cats)) return 0;
    for (Object c : cats) {
      if (!(c instanceof Map<?, ?> cat)) continue;
      Object itemsObj = cat.get("items");
      if (!(itemsObj instanceof List<?> items)) continue;
      if (!items.isEmpty() && items.get(0) instanceof Map<?, ?> item) {
        Object id = item.get("id");
        if (id instanceof Number n) return n.intValue();
      }
    }
    return 0;
  }
}
