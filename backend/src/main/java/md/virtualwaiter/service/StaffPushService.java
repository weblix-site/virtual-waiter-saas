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
  private final StaffDeviceTokenRepo tokenRepo;
  private final StaffUserRepo staffUserRepo;
  private final OrderRepo orderRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final BillRequestRepo billRequestRepo;
  private final CafeTableRepo tableRepo;
  private final PushProperties pushProperties;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public StaffPushService(
    StaffDeviceTokenRepo tokenRepo,
    StaffUserRepo staffUserRepo,
    OrderRepo orderRepo,
    WaiterCallRepo waiterCallRepo,
    BillRequestRepo billRequestRepo,
    CafeTableRepo tableRepo,
    PushProperties pushProperties
  ) {
    this.tokenRepo = tokenRepo;
    this.staffUserRepo = staffUserRepo;
    this.orderRepo = orderRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.billRequestRepo = billRequestRepo;
    this.tableRepo = tableRepo;
    this.pushProperties = pushProperties;
  }

  public void notifyBranch(long branchId, String type, long refId) {
    List<StaffDeviceToken> tokens = tokenRepo.findByBranchId(branchId);
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
      if ("WAITER".equals(role) && assignedWaiterId != null && !assignedWaiterId.equals(u.id)) {
        continue;
      }
      targets.add(t);
    }

    if (targets.isEmpty()) return;

    String provider = pushProperties.provider == null ? "LOG" : pushProperties.provider.toUpperCase(Locale.ROOT);
    for (StaffDeviceToken t : targets) {
      if ("FCM_LEGACY".equals(provider) && pushProperties.fcmServerKey != null && !pushProperties.fcmServerKey.isBlank()) {
        sendFcmLegacy(t, branchId, type, refId, tableId);
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
      case "ORDER_NEW" -> Set.of("WAITER", "KITCHEN", "ADMIN");
      case "WAITER_CALL", "BILL_REQUEST" -> Set.of("WAITER", "ADMIN");
      default -> Set.of("ADMIN");
    };
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

  private void sendFcmLegacy(StaffDeviceToken t, long branchId, String type, long refId, Long tableId) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("type", type);
      data.put("refId", refId);
      data.put("branchId", branchId);
      if (tableId != null) data.put("tableId", tableId);

      Map<String, Object> payload = new HashMap<>();
      payload.put("to", t.token);
      payload.put("priority", "high");
      payload.put("data", data);
      payload.put("notification", Map.of(
        "title", "Virtual Waiter",
        "body", type + " #" + refId
      ));
      if (pushProperties.dryRun) {
        payload.put("dry_run", true);
      }

      String body = objectMapper.writeValueAsString(payload);
      if (pushProperties.maxPayloadBytes > 0 && body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > pushProperties.maxPayloadBytes) {
        log.warn("[FCM] payload too large ({} bytes > {}), skip send", body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, pushProperties.maxPayloadBytes);
        return;
      }
      HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(pushProperties.fcmApiUrl))
        .header("Authorization", "key=" + pushProperties.fcmServerKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
      HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 400) {
        log.warn("[FCM] status={} body={}", resp.statusCode(), resp.body());
      }
    } catch (Exception e) {
      log.warn("[FCM] send failed: {}", e.getMessage());
    }
  }
}
