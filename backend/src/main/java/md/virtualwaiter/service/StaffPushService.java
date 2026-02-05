package md.virtualwaiter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import md.virtualwaiter.config.PushProperties;
import md.virtualwaiter.domain.BillRequest;
import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.Order;
import md.virtualwaiter.domain.StaffDeviceToken;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.WaiterCall;
import md.virtualwaiter.repo.BillRequestRepo;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.OrderRepo;
import md.virtualwaiter.repo.StaffDeviceTokenRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.WaiterCallRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import md.virtualwaiter.util.PayloadGuard;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class StaffPushService {
  private static final Logger log = LoggerFactory.getLogger(StaffPushService.class);
  private static final Set<String> WAITER_ROLES = Set.of("WAITER", "HOST");
  private static final Set<String> KITCHEN_ROLES = Set.of("KITCHEN", "BAR");
  private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "MANAGER");
  private final StaffDeviceTokenRepo tokenRepo;
  private final StaffUserRepo staffUserRepo;
  private final OrderRepo orderRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final BillRequestRepo billRequestRepo;
  private final CafeTableRepo tableRepo;
  private final PushProperties pushProperties;
  private final int maxLogPayloadChars;
  private final int maxEventPayloadBytes;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public StaffPushService(
    StaffDeviceTokenRepo tokenRepo,
    StaffUserRepo staffUserRepo,
    OrderRepo orderRepo,
    WaiterCallRepo waiterCallRepo,
    BillRequestRepo billRequestRepo,
    CafeTableRepo tableRepo,
    PushProperties pushProperties,
    @Value("${app.log.maxPayloadChars:2000}") int maxLogPayloadChars,
    @Value("${app.payload.maxBytes:4096}") int maxEventPayloadBytes
  ) {
    this.tokenRepo = tokenRepo;
    this.staffUserRepo = staffUserRepo;
    this.orderRepo = orderRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.billRequestRepo = billRequestRepo;
    this.tableRepo = tableRepo;
    this.pushProperties = pushProperties;
    this.maxLogPayloadChars = maxLogPayloadChars;
    this.maxEventPayloadBytes = maxEventPayloadBytes;
  }

  public void notifyBranch(long branchId, String type, long refId) {
    notifyBranch(branchId, type, refId, Map.of());
  }

  public void notifyBranch(long branchId, String type, long refId, Map<String, Object> extraData) {
    List<StaffDeviceToken> tokens = tokenRepo.findByBranchIdAndRevokedAtIsNull(branchId);
    if (tokens.isEmpty()) return;

    Long tableId = resolveTableId(type, refId);
    Long assignedWaiterId = resolveAssignedWaiterId(tableId);
    Set<String> roles = rolesForEvent(type);

    Map<Long, StaffUser> staffById = new HashMap<>();
    for (StaffDeviceToken t : tokens) {
      staffById.computeIfAbsent(t.staffUserId, id -> staffUserRepo.findById(id).orElse(null));
    }

    List<StaffDeviceToken> targets = new ArrayList<>();
    for (StaffDeviceToken t : tokens) {
      StaffUser u = staffById.get(t.staffUserId);
      if (u == null || !u.isActive) continue;
      if (u.branchId != null && !u.branchId.equals(branchId)) continue;
      String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
      if (!roles.contains(role)) continue;
      if (WAITER_ROLES.contains(role) && assignedWaiterId != null && !assignedWaiterId.equals(u.id)) {
        continue;
      }
      targets.add(t);
    }

    if (targets.isEmpty()) return;

    String provider = pushProperties.getProvider() == null ? "LOG" : pushProperties.getProvider().toUpperCase(Locale.ROOT);
    for (StaffDeviceToken t : targets) {
      if ("FCM_LEGACY".equals(provider) && pushProperties.getFcmServerKey() != null && !pushProperties.getFcmServerKey().isBlank()) {
        sendFcmLegacy(t, branchId, type, refId, tableId, extraData);
      } else {
        log.info("[PUSH] branch={} token={} platform={} type={} refId={} tableId={}", branchId, t.token, t.platform, type, refId, tableId);
      }
      t.lastSeenAt = Instant.now();
      tokenRepo.save(t);
    }
  }

  private Set<String> rolesForEvent(String type) {
    String t = type == null ? "" : type.toUpperCase(Locale.ROOT);
    return switch (t) {
      case "ORDER_NEW" -> mergeRoles(WAITER_ROLES, KITCHEN_ROLES, ADMIN_ROLES);
      case "WAITER_CALL", "BILL_REQUEST" -> mergeRoles(WAITER_ROLES, ADMIN_ROLES);
      case "SLA_ALERT" -> mergeRoles(WAITER_ROLES, KITCHEN_ROLES, ADMIN_ROLES);
      default -> ADMIN_ROLES;
    };
  }

  private Set<String> mergeRoles(Set<String> a, Set<String> b) {
    Set<String> out = new java.util.HashSet<>();
    out.addAll(a);
    out.addAll(b);
    return out;
  }

  private Set<String> mergeRoles(Set<String> a, Set<String> b, Set<String> c) {
    Set<String> out = new java.util.HashSet<>();
    out.addAll(a);
    out.addAll(b);
    out.addAll(c);
    return out;
  }

  private Long resolveTableId(String type, long refId) {
    String t = type == null ? "" : type.toUpperCase(Locale.ROOT);
    if ("ORDER_NEW".equals(t)) {
      Optional<Order> o = orderRepo.findById(refId);
      return o.map(order -> order.tableId).orElse(null);
    }
    if ("WAITER_CALL".equals(t)) {
      Optional<WaiterCall> wc = waiterCallRepo.findById(refId);
      return wc.map(call -> call.tableId).orElse(null);
    }
    if ("BILL_REQUEST".equals(t)) {
      Optional<BillRequest> br = billRequestRepo.findById(refId);
      return br.map(bill -> bill.tableId).orElse(null);
    }
    return null;
  }

  private Long resolveAssignedWaiterId(Long tableId) {
    if (tableId == null) return null;
    Optional<CafeTable> t = tableRepo.findById(tableId);
    return t.map(table -> table.assignedWaiterId).orElse(null);
  }

  private void sendFcmLegacy(StaffDeviceToken t, long branchId, String type, long refId, Long tableId, Map<String, Object> extraData) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("type", type);
      data.put("refId", refId);
      data.put("branchId", branchId);
      if (tableId != null) data.put("tableId", tableId);
      if (extraData != null && !extraData.isEmpty()) {
        data.putAll(extraData);
      }

      String title = extraData != null && extraData.get("title") != null ? extraData.get("title").toString() : "Virtual Waiter";
      String bodyText = extraData != null && extraData.get("body") != null ? extraData.get("body").toString() : (type + " #" + refId);

      Map<String, Object> payload = new HashMap<>();
      payload.put("to", t.token);
      payload.put("priority", "high");
      payload.put("data", data);
      payload.put("notification", Map.of(
        "title", title,
        "body", bodyText
      ));
      if (pushProperties.isDryRun()) {
        payload.put("dry_run", true);
      }

      String body = objectMapper.writeValueAsString(payload);
      int effectiveMax = pushProperties.getMaxPayloadBytes() > 0
        ? Math.min(pushProperties.getMaxPayloadBytes(), maxEventPayloadBytes)
        : maxEventPayloadBytes;
      if (effectiveMax > 0 && body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > effectiveMax) {
        log.warn("[FCM] payload too large ({} bytes > {}), skip send", body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, effectiveMax);
        return;
      }
      HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(pushProperties.getFcmApiUrl()))
        .header("Authorization", "key=" + pushProperties.getFcmServerKey())
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
      HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 400) {
        log.warn("[FCM] status={} body={}", resp.statusCode(), PayloadGuard.truncate(resp.body(), maxLogPayloadChars));
      }
    } catch (Exception e) {
      log.warn("[FCM] send failed: {}", e.getMessage());
    }
  }
}
