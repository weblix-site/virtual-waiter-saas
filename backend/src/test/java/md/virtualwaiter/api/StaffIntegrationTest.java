package md.virtualwaiter.api;

import static org.assertj.core.api.Assertions.assertThat;

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
class StaffIntegrationTest {

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
  void staffHallsAndNotificationsWork() throws Exception {
    String adminCookie = loginCookie("admin1", "demo123");
    ensureHall(adminCookie);

    String staffAuth = basicAuth("waiter1", "demo123");

    ResponseEntity<String> hallsRes = exchange("/api/staff/halls", staffAuth, null, HttpMethod.GET, true);
    assertThat(hallsRes.getStatusCode().value()).isEqualTo(200);
    List<Map<String, Object>> halls = mapper.readValue(hallsRes.getBody(), new TypeReference<>() {});
    assertThat(halls).isNotEmpty();

    ResponseEntity<String> notifRes = exchange("/api/staff/notifications?sinceOrders=2000-01-01T00:00:00Z&sinceCalls=2000-01-01T00:00:00Z&sinceBills=2000-01-01T00:00:00Z", staffAuth, null, HttpMethod.GET, true);
    assertThat(notifRes.getStatusCode().value()).isEqualTo(200);
    Map<String, Object> notif = mapper.readValue(notifRes.getBody(), new TypeReference<>() {});
    assertThat(notif).containsKeys("newOrders", "newCalls", "newBills");

    ResponseEntity<String> feedRes = exchange("/api/staff/notifications/feed?sinceId=0", staffAuth, null, HttpMethod.GET, true);
    assertThat(feedRes.getStatusCode().value()).isEqualTo(200);
    Map<String, Object> feed = mapper.readValue(feedRes.getBody(), new TypeReference<>() {});
    assertThat(feed).containsKey("events");
  }

  private void ensureHall(String adminCookie) throws Exception {
    ResponseEntity<String> hallsRes = exchange("/api/admin/halls", adminCookie, null, HttpMethod.GET, false);
    List<Map<String, Object>> halls = mapper.readValue(hallsRes.getBody(), new TypeReference<>() {});
    if (!halls.isEmpty()) return;
    Map<String, Object> created = postJson("/api/admin/halls", adminCookie, Map.of("name", "Main", "sortOrder", 0));
    assertThat(created.get("id")).isNotNull();
  }

  private String loginCookie(String username, String password) throws Exception {
    Map<String, Object> body = Map.of("username", username, "password", password);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    ResponseEntity<String> res = rest.exchange(url("/api/auth/login"), HttpMethod.POST, new HttpEntity<>(mapper.writeValueAsString(body), headers), String.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    List<String> cookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
    assertThat(cookies).isNotNull();
    return cookies.get(0).split(";", 2)[0];
  }

  private String basicAuth(String username, String password) {
    String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    return "Basic " + token;
  }

  private Map<String, Object> postJson(String path, String authOrCookie, Object payload) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (authOrCookie != null) {
      if (authOrCookie.startsWith("vw_auth=")) {
        headers.add(HttpHeaders.COOKIE, authOrCookie);
      } else {
        headers.set(HttpHeaders.AUTHORIZATION, authOrCookie);
      }
    }
    ResponseEntity<String> res = rest.exchange(url(path), HttpMethod.POST, new HttpEntity<>(mapper.writeValueAsString(payload), headers), String.class);
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    return mapper.readValue(res.getBody(), new TypeReference<>() {});
  }

  private ResponseEntity<String> exchange(String path, String authOrCookie, Object payload, HttpMethod method, boolean authHeader) throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (authOrCookie != null) {
      if (authOrCookie.startsWith("vw_auth=")) {
        headers.add(HttpHeaders.COOKIE, authOrCookie);
      } else if (authHeader) {
        headers.set(HttpHeaders.AUTHORIZATION, authOrCookie);
      } else {
        headers.set("X-Session-Secret", authOrCookie);
      }
    }
    String body = payload == null ? null : mapper.writeValueAsString(payload);
    return rest.exchange(url(path), method, new HttpEntity<>(body, headers), String.class);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
