package md.virtualwaiter.api.staff;

import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.Order;
import md.virtualwaiter.domain.OrderItem;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.WaiterCall;
import md.virtualwaiter.domain.BillRequest;
import md.virtualwaiter.domain.BillRequestItem;
import md.virtualwaiter.domain.TableParty;
import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.Branch;
import md.virtualwaiter.domain.BranchHall;
import md.virtualwaiter.domain.HallPlan;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.OrderItemRepo;
import md.virtualwaiter.repo.OrderRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.WaiterCallRepo;
import md.virtualwaiter.repo.BillRequestRepo;
import md.virtualwaiter.repo.BillRequestItemRepo;
import md.virtualwaiter.repo.TablePartyRepo;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.BranchHallRepo;
import md.virtualwaiter.repo.HallPlanRepo;
import md.virtualwaiter.security.QrSignatureService;
import md.virtualwaiter.service.PartyService;
import md.virtualwaiter.service.StaffNotificationService;
import md.virtualwaiter.repo.NotificationEventRepo;
import md.virtualwaiter.domain.NotificationEvent;
import md.virtualwaiter.repo.StaffDeviceTokenRepo;
import md.virtualwaiter.domain.StaffDeviceToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

  private final StaffUserRepo staffUserRepo;
  private final CafeTableRepo tableRepo;
  private final OrderRepo orderRepo;
  private final OrderItemRepo orderItemRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final BillRequestRepo billRequestRepo;
  private final BillRequestItemRepo billRequestItemRepo;
  private final TablePartyRepo partyRepo;
  private final GuestSessionRepo guestSessionRepo;
  private final BranchRepo branchRepo;
  private final BranchHallRepo hallRepo;
  private final HallPlanRepo hallPlanRepo;
  private final QrSignatureService qrSig;
  private final String publicBaseUrl;
  private final StaffNotificationService notificationService;
  private final NotificationEventRepo notificationEventRepo;
  private final StaffDeviceTokenRepo staffDeviceTokenRepo;
  private final PartyService partyService;

  public StaffController(
    StaffUserRepo staffUserRepo,
    CafeTableRepo tableRepo,
    OrderRepo orderRepo,
    OrderItemRepo orderItemRepo,
    WaiterCallRepo waiterCallRepo,
    BillRequestRepo billRequestRepo,
    BillRequestItemRepo billRequestItemRepo,
    TablePartyRepo partyRepo,
    GuestSessionRepo guestSessionRepo,
    BranchRepo branchRepo,
    BranchHallRepo hallRepo,
    HallPlanRepo hallPlanRepo,
    QrSignatureService qrSig,
    @Value("${app.publicBaseUrl:http://localhost:3000}") String publicBaseUrl,
    StaffNotificationService notificationService,
    NotificationEventRepo notificationEventRepo,
    StaffDeviceTokenRepo staffDeviceTokenRepo,
    PartyService partyService
  ) {
    this.staffUserRepo = staffUserRepo;
    this.tableRepo = tableRepo;
    this.orderRepo = orderRepo;
    this.orderItemRepo = orderItemRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.billRequestRepo = billRequestRepo;
    this.billRequestItemRepo = billRequestItemRepo;
    this.partyRepo = partyRepo;
    this.guestSessionRepo = guestSessionRepo;
    this.branchRepo = branchRepo;
    this.hallRepo = hallRepo;
    this.hallPlanRepo = hallPlanRepo;
    this.qrSig = qrSig;
    this.publicBaseUrl = publicBaseUrl;
    this.notificationService = notificationService;
    this.notificationEventRepo = notificationEventRepo;
    this.staffDeviceTokenRepo = staffDeviceTokenRepo;
    this.partyService = partyService;
  }

  private StaffUser current(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");
    }
    return staffUserRepo.findByUsername(auth.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
  }

  private StaffUser requireRole(Authentication auth, String... roles) {
    StaffUser u = current(auth);
    String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
    for (String r : roles) {
      if (role.equals(r)) return u;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
  }

  public record MeResponse(long id, String username, String role, long branchId) {}

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    StaffUser u = current(auth);
    return new MeResponse(u.id, u.username, u.role, u.branchId);
  }

  public record StaffTableDto(
    long id,
    int number,
    String publicId,
    Long assignedWaiterId,
    Long hallId,
    Double layoutX,
    Double layoutY,
    Double layoutW,
    Double layoutH,
    String layoutShape,
    Integer layoutRotation,
    String layoutZone
  ) {}

  public record BranchLayoutResponse(String backgroundUrl, String zonesJson) {}
  public record HallDto(long id, long branchId, String name, boolean isActive, int sortOrder, String backgroundUrl, String zonesJson, Long activePlanId) {}

  @GetMapping("/tables")
  public List<StaffTableDto> tables(
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    if (hallId != null) {
      tables = tables.stream().filter(t -> Objects.equals(t.hallId, hallId)).toList();
    }
    List<StaffTableDto> out = new ArrayList<>();
    for (CafeTable t : tables) {
      out.add(new StaffTableDto(
        t.id,
        t.number,
        t.publicId,
        t.assignedWaiterId,
        t.hallId,
        t.layoutX,
        t.layoutY,
        t.layoutW,
        t.layoutH,
        t.layoutShape,
        t.layoutRotation,
        t.layoutZone
      ));
    }
    return out;
  }

  @GetMapping("/branch-layout")
  public BranchLayoutResponse branchLayout(
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    if (u.branchId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no branch");
    }
    if (planId != null) {
      HallPlan p = hallPlanRepo.findById(planId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
      BranchHall h = hallRepo.findById(p.hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!Objects.equals(h.branchId, u.branchId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
      if (hallId != null && !Objects.equals(hallId, h.id)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan does not belong to hall");
      }
      return new BranchLayoutResponse(p.layoutBgUrl, p.layoutZonesJson);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!Objects.equals(h.branchId, u.branchId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
      if (h.activePlanId != null) {
        HallPlan p = hallPlanRepo.findById(h.activePlanId).orElse(null);
        if (p != null) {
          return new BranchLayoutResponse(p.layoutBgUrl, p.layoutZonesJson);
        }
      }
      return new BranchLayoutResponse(h.layoutBgUrl, h.layoutZonesJson);
    }
    Branch b = branchRepo.findById(u.branchId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    return new BranchLayoutResponse(b.layoutBgUrl, b.layoutZonesJson);
  }

  @GetMapping("/halls")
  public List<HallDto> halls(Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    List<BranchHall> halls = hallRepo.findByBranchIdAndIsActiveTrueOrderBySortOrderAscIdAsc(u.branchId);
    List<HallDto> out = new ArrayList<>();
    for (BranchHall h : halls) {
      out.add(new HallDto(h.id, h.branchId, h.name, h.isActive, h.sortOrder, h.layoutBgUrl, h.layoutZonesJson, h.activePlanId));
    }
    return out;
  }

  public record StaffHallPlanDto(long id, long hallId, String name, boolean isActive, int sortOrder) {}

  @GetMapping("/halls/{hallId}/plans")
  public List<StaffHallPlanDto> hallPlans(@PathVariable long hallId, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    BranchHall h = hallRepo.findById(hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    if (!Objects.equals(h.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    List<HallPlan> plans = hallPlanRepo.findByHallIdOrderBySortOrderAscIdAsc(hallId);
    List<StaffHallPlanDto> out = new ArrayList<>();
    for (HallPlan p : plans) {
      out.add(new StaffHallPlanDto(p.id, p.hallId, p.name, p.isActive, p.sortOrder));
    }
    return out;
  }

  public record StaffGuestSessionDto(
    long id,
    long tableId,
    Long partyId,
    boolean isVerified,
    String lastOrderAt,
    String lastBillRequestAt,
    String lastWaiterCallAt
  ) {}

  @GetMapping("/guest-sessions")
  public List<StaffGuestSessionDto> guestSessions(
    @RequestParam("tableId") long tableId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    CafeTable t = tableRepo.findById(tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!Objects.equals(t.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    List<GuestSession> sessions = guestSessionRepo.findByTableIdOrderByIdDesc(tableId);
    List<StaffGuestSessionDto> out = new ArrayList<>();
    for (GuestSession s : sessions) {
      out.add(new StaffGuestSessionDto(
        s.id,
        s.tableId,
        s.partyId,
        s.isVerified,
        s.lastOrderAt == null ? null : s.lastOrderAt.toString(),
        s.lastBillRequestAt == null ? null : s.lastBillRequestAt.toString(),
        s.lastWaiterCallAt == null ? null : s.lastWaiterCallAt.toString()
      ));
    }
    return out;
  }

  public record StaffHistoryOrderDto(
    long id,
    long tableId,
    int tableNumber,
    long guestSessionId,
    String status,
    String createdAt,
    List<StaffOrderItemDto> items
  ) {}

  @GetMapping("/orders/history")
  public List<StaffHistoryOrderDto> orderHistory(
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "guestSessionId", required = false) Long guestSessionId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    if (tableId == null && guestSessionId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tableId or guestSessionId required");
    }
    Long resolvedTableId = tableId;
    if (guestSessionId != null) {
      GuestSession gs = guestSessionRepo.findById(guestSessionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
      if (resolvedTableId == null) {
        resolvedTableId = gs.tableId;
      } else if (!Objects.equals(resolvedTableId, gs.tableId)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guestSessionId does not belong to tableId");
      }
    }
    if (resolvedTableId != null) {
      CafeTable t = tableRepo.findById(resolvedTableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      if (!Objects.equals(t.branchId, u.branchId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
    }

    List<Order> orders;
    if (guestSessionId != null && tableId == null) {
      orders = orderRepo.findTop200ByGuestSessionIdOrderByCreatedAtDesc(guestSessionId);
    } else if (resolvedTableId != null) {
      orders = orderRepo.findTop200ByTableIdOrderByCreatedAtDesc(resolvedTableId);
      if (guestSessionId != null) {
        orders = orders.stream().filter(o -> Objects.equals(o.guestSessionId, guestSessionId)).toList();
      }
    } else {
      orders = List.of();
    }
    if (orders.isEmpty()) return List.of();

    Map<Long, Integer> tableNumberById = new HashMap<>();
    if (resolvedTableId != null) {
      CafeTable t = tableRepo.findById(resolvedTableId).orElse(null);
      if (t != null) tableNumberById.put(t.id, t.number);
    }

    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> items = orderItemRepo.findByOrderIdIn(orderIds);
    Map<Long, List<StaffOrderItemDto>> itemsByOrder = new HashMap<>();
    for (OrderItem it : items) {
      itemsByOrder.computeIfAbsent(it.orderId, k -> new ArrayList<>()).add(
        new StaffOrderItemDto(it.id, it.menuItemId, it.nameSnapshot, it.unitPriceCents, it.qty, it.comment)
      );
    }

    List<StaffHistoryOrderDto> out = new ArrayList<>();
    for (Order o : orders) {
      out.add(new StaffHistoryOrderDto(
        o.id,
        o.tableId,
        tableNumberById.getOrDefault(o.tableId, 0),
        o.guestSessionId,
        o.status,
        o.createdAt.toString(),
        itemsByOrder.getOrDefault(o.id, List.of())
      ));
    }
    return out;
  }

  public record StaffOrderItemDto(long id, long menuItemId, String name, int unitPriceCents, int qty, String comment) {}
  public record StaffOrderDto(long id, long tableId, int tableNumber, Long assignedWaiterId, String status, String createdAt, List<StaffOrderItemDto> items) {}
  public record StaffOrderStatusDto(long id, long tableId, String status) {}

  public record KitchenOrderDto(long id, long tableId, int tableNumber, Long assignedWaiterId, String status, String createdAt, long ageSeconds, List<StaffOrderItemDto> items) {}

  @GetMapping("/orders/active")
  public List<StaffOrderDto> activeOrders(
    @RequestParam(value = "statusIn", required = false) String statusIn,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    if (hallId != null) {
      tables = tables.stream().filter(t -> Objects.equals(t.hallId, hallId)).toList();
    }
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();
    if (tableIds.isEmpty()) return List.of();

    List<String> closed = List.of("CLOSED", "CANCELLED");
    List<Order> orders = orderRepo.findTop100ByTableIdInAndStatusNotInOrderByCreatedAtDesc(tableIds, closed);
    if (statusIn != null && !statusIn.isBlank()) {
      Set<String> allow = new HashSet<>();
      for (String s : statusIn.split(",")) {
        String v = s.trim().toUpperCase(Locale.ROOT);
        if (!v.isEmpty()) allow.add(v);
      }
      if (!allow.isEmpty()) {
        orders = orders.stream().filter(o -> allow.contains(o.status)).toList();
      }
    }
    if (orders.isEmpty()) return List.of();

    Map<Long, Integer> tableNumberById = new HashMap<>();
    Map<Long, Long> assignedById = new HashMap<>();
    for (CafeTable t : tables) {
      tableNumberById.put(t.id, t.number);
      assignedById.put(t.id, t.assignedWaiterId);
    }

    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> items = orderItemRepo.findByOrderIdIn(orderIds);
    Map<Long, List<StaffOrderItemDto>> itemsByOrder = new HashMap<>();
    for (OrderItem it : items) {
      itemsByOrder.computeIfAbsent(it.orderId, k -> new ArrayList<>()).add(
        new StaffOrderItemDto(it.id, it.menuItemId, it.nameSnapshot, it.unitPriceCents, it.qty, it.comment)
      );
    }

    List<StaffOrderDto> out = new ArrayList<>();
    for (Order o : orders) {
      out.add(new StaffOrderDto(
        o.id,
        o.tableId,
        tableNumberById.getOrDefault(o.tableId, 0),
        assignedById.get(o.tableId),
        o.status,
        o.createdAt.toString(),
        itemsByOrder.getOrDefault(o.id, List.of())
      ));
    }
    return out;
  }

  @GetMapping("/orders/active/updates")
  public List<StaffOrderDto> activeOrderUpdates(
    @RequestParam("since") String since,
    @RequestParam(value = "statusIn", required = false) String statusIn,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    Instant sinceTs;
    try {
      sinceTs = Instant.parse(since);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid since");
    }
    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    if (hallId != null) {
      tables = tables.stream().filter(t -> Objects.equals(t.hallId, hallId)).toList();
    }
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();
    if (tableIds.isEmpty()) return List.of();

    List<String> closed = List.of("CLOSED", "CANCELLED");
    List<Order> orders = orderRepo.findByTableIdInAndStatusNotInAndCreatedAtAfterOrderByCreatedAtDesc(tableIds, closed, sinceTs);
    if (statusIn != null && !statusIn.isBlank()) {
      Set<String> allow = new HashSet<>();
      for (String s : statusIn.split(",")) {
        String v = s.trim().toUpperCase(Locale.ROOT);
        if (!v.isEmpty()) allow.add(v);
      }
      if (!allow.isEmpty()) {
        orders = orders.stream().filter(o -> allow.contains(o.status)).toList();
      }
    }
    if (orders.isEmpty()) return List.of();

    Map<Long, Integer> tableNumberById = new HashMap<>();
    Map<Long, Long> assignedById = new HashMap<>();
    for (CafeTable t : tables) {
      tableNumberById.put(t.id, t.number);
      assignedById.put(t.id, t.assignedWaiterId);
    }

    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> items = orderItemRepo.findByOrderIdIn(orderIds);
    Map<Long, List<StaffOrderItemDto>> itemsByOrder = new HashMap<>();
    for (OrderItem it : items) {
      itemsByOrder.computeIfAbsent(it.orderId, k -> new ArrayList<>()).add(
        new StaffOrderItemDto(it.id, it.menuItemId, it.nameSnapshot, it.unitPriceCents, it.qty, it.comment)
      );
    }

    List<StaffOrderDto> out = new ArrayList<>();
    for (Order o : orders) {
      out.add(new StaffOrderDto(
        o.id,
        o.tableId,
        tableNumberById.getOrDefault(o.tableId, 0),
        assignedById.get(o.tableId),
        o.status,
        o.createdAt.toString(),
        itemsByOrder.getOrDefault(o.id, List.of())
      ));
    }
    return out;
  }

  @GetMapping("/orders/active/status")
  public List<StaffOrderStatusDto> activeOrderStatuses(
    @RequestParam("ids") String ids,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    if (ids == null || ids.isBlank()) return List.of();
    List<Long> orderIds = new ArrayList<>();
    for (String part : ids.split(",")) {
      if (orderIds.size() >= 200) break;
      String v = part.trim();
      if (v.isEmpty()) continue;
      try {
        orderIds.add(Long.parseLong(v));
      } catch (_) {}
    }
    if (orderIds.isEmpty()) return List.of();

    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    if (hallId != null) {
      tables = tables.stream().filter(t -> Objects.equals(t.hallId, hallId)).toList();
    }
    Set<Long> tableIds = new HashSet<>();
    for (CafeTable t : tables) tableIds.add(t.id);
    if (tableIds.isEmpty()) return List.of();

    List<Order> orders = orderRepo.findByIdIn(orderIds);
    List<StaffOrderStatusDto> out = new ArrayList<>();
    for (Order o : orders) {
      if (!tableIds.contains(o.tableId)) continue;
      out.add(new StaffOrderStatusDto(o.id, o.tableId, o.status));
    }
    return out;
  }

  @GetMapping("/orders/kitchen")
  public List<KitchenOrderDto> kitchenQueue(
    @RequestParam(value = "statusIn", required = false) String statusIn,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "KITCHEN", "ADMIN");
    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    if (hallId != null) {
      tables = tables.stream().filter(t -> Objects.equals(t.hallId, hallId)).toList();
    }
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();
    if (tableIds.isEmpty()) return List.of();

    List<String> closed = List.of("CLOSED", "CANCELLED");
    List<Order> orders = orderRepo.findTop100ByTableIdInAndStatusNotInOrderByCreatedAtDesc(tableIds, closed);
    if (statusIn != null && !statusIn.isBlank()) {
      Set<String> allow = new HashSet<>();
      for (String s : statusIn.split(",")) {
        String v = s.trim().toUpperCase(Locale.ROOT);
        if (!v.isEmpty()) allow.add(v);
      }
      if (!allow.isEmpty()) {
        orders = orders.stream().filter(o -> allow.contains(o.status)).toList();
      }
    }
    if (orders.isEmpty()) return List.of();

    Map<Long, Integer> tableNumberById = new HashMap<>();
    Map<Long, Long> assignedById = new HashMap<>();
    for (CafeTable t : tables) {
      tableNumberById.put(t.id, t.number);
      assignedById.put(t.id, t.assignedWaiterId);
    }

    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> items = orderItemRepo.findByOrderIdIn(orderIds);
    Map<Long, List<StaffOrderItemDto>> itemsByOrder = new HashMap<>();
    for (OrderItem it : items) {
      itemsByOrder.computeIfAbsent(it.orderId, k -> new ArrayList<>()).add(
        new StaffOrderItemDto(it.id, it.menuItemId, it.nameSnapshot, it.unitPriceCents, it.qty, it.comment)
      );
    }

    long now = java.time.Instant.now().getEpochSecond();
    List<KitchenOrderDto> out = new ArrayList<>();
    for (Order o : orders) {
      long age = now - o.createdAt.getEpochSecond();
      out.add(new KitchenOrderDto(
        o.id,
        o.tableId,
        tableNumberById.getOrDefault(o.tableId, 0),
        assignedById.get(o.tableId),
        o.status,
        o.createdAt.toString(),
        age,
        itemsByOrder.getOrDefault(o.id, List.of())
      ));
    }
    return out;
  }

  public record UpdateStatusReq(String status) {}

  @PostMapping("/orders/{orderId}/status")
  public void updateStatus(@PathVariable long orderId, @RequestBody UpdateStatusReq req, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    Order o = orderRepo.findById(orderId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    // simple branch-level authorization
    CafeTable t = tableRepo.findById(o.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!Objects.equals(t.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    if (req == null || req.status == null || req.status.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing status");
    }
    String next = req.status.trim().toUpperCase(Locale.ROOT);
    if ("COOKING".equals(next)) {
      next = "IN_PROGRESS";
    }
    if (!isAllowedOrderStatus(next)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status");
    }
    if (!isAllowedRoleTransition(u.role, o.status, next)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status transition");
    }
    o.status = next;
    if (o.handledByStaffId == null) {
      String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
      if (Set.of("WAITER", "ADMIN").contains(role)) {
        o.handledByStaffId = u.id;
      }
    }
    orderRepo.save(o);
  }

  private boolean isAllowedOrderStatus(String s) {
    return Set.of("NEW", "ACCEPTED", "IN_PROGRESS", "READY", "SERVED", "CLOSED", "CANCELLED").contains(s);
  }

  private boolean isAllowedTransition(String current, String next) {
    String cur = current == null ? "NEW" : current.toUpperCase(Locale.ROOT);
    if (cur.equals(next)) return true;
    return switch (cur) {
      case "NEW" -> Set.of("ACCEPTED", "IN_PROGRESS", "READY", "CANCELLED").contains(next);
      case "ACCEPTED" -> Set.of("IN_PROGRESS", "READY", "CANCELLED").contains(next);
      case "IN_PROGRESS" -> Set.of("READY", "CANCELLED").contains(next);
      case "READY" -> Set.of("SERVED", "CLOSED").contains(next);
      case "SERVED" -> Set.of("CLOSED").contains(next);
      case "CLOSED", "CANCELLED" -> false;
      default -> false;
    };
  }

  private boolean isAllowedRoleTransition(String role, String current, String next) {
    String r = role == null ? "" : role.toUpperCase(Locale.ROOT);
    if ("ADMIN".equals(r)) return isAllowedTransition(current, next);
    if ("KITCHEN".equals(r)) {
      return isAllowedTransition(current, next) && Set.of("ACCEPTED", "IN_PROGRESS", "READY", "SERVED").contains(next);
    }
    if ("WAITER".equals(r)) {
      return isAllowedTransition(current, next) && Set.of("ACCEPTED", "READY", "SERVED", "CLOSED", "CANCELLED").contains(next);
    }
    return false;
  }

  public record StaffWaiterCallDto(long id, long tableId, int tableNumber, String status, String createdAt) {}

  @GetMapping("/waiter-calls/active")
  public List<StaffWaiterCallDto> activeCalls(@RequestParam(value = "hallId", required = false) Long hallId, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "ADMIN");
    List<CafeTable> tables = tableRepo.findByBranchId(u.branchId);
    if (hallId != null) {
      tables = tables.stream().filter(t -> Objects.equals(t.hallId, hallId)).toList();
    }
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();
    if (tableIds.isEmpty()) return List.of();
    Map<Long, Integer> tableNumberById = new HashMap<>();
    for (CafeTable t : tables) tableNumberById.put(t.id, t.number);

    List<WaiterCall> calls = waiterCallRepo.findTop100ByTableIdInAndStatusNotOrderByCreatedAtDesc(tableIds, "CLOSED");
    List<StaffWaiterCallDto> out = new ArrayList<>();
    for (WaiterCall c : calls) {
      out.add(new StaffWaiterCallDto(c.id, c.tableId, tableNumberById.getOrDefault(c.tableId, 0), c.status, c.createdAt.toString()));
    }
    return out;
  }

  public record UpdateWaiterCallStatusReq(String status) {}
  public record UpdateWaiterCallStatusRes(long id, String status) {}

  @PostMapping("/waiter-calls/{id}/status")
  public UpdateWaiterCallStatusRes updateWaiterCallStatus(
    @PathVariable("id") long id,
    @RequestBody UpdateWaiterCallStatusReq req,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "ADMIN");
    WaiterCall c = waiterCallRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter call not found"));
    CafeTable t = tableRepo.findById(c.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!Objects.equals(t.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    if (req == null || req.status == null || req.status.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing status");
    }
    String next = req.status.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("NEW", "ACKNOWLEDGED", "CLOSED").contains(next)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status");
    }
    c.status = next;
    waiterCallRepo.save(c);
    return new UpdateWaiterCallStatusRes(c.id, c.status);
  }
  // --- QR helper (for admin/staff tooling) ---
  public record SignedTableUrlResponse(String tablePublicId, String sig, long ts, String url) {}

  @GetMapping("/tables/{tablePublicId}/signed-url")
  public SignedTableUrlResponse getSignedTableUrl(@PathVariable String tablePublicId, Authentication auth) {
    StaffUser u = requireRole(auth, "ADMIN", "WAITER");
    CafeTable table = tableRepo.findByPublicId(tablePublicId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!Objects.equals(table.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    long ts = java.time.Instant.now().getEpochSecond();
    String sig = qrSig.signTablePublicId(tablePublicId, ts);
    String url = publicBaseUrl + "/t/" + table.publicId + "?sig=" + sig + "&ts=" + ts;
    return new SignedTableUrlResponse(table.publicId, sig, ts, url);
  }


  // --- Bill requests (offline payment) ---
  public record StaffBillLine(long orderItemId, String name, int qty, int unitPriceCents, int lineTotalCents) {}
  public record StaffBillRequestDto(
    long billRequestId,
    int tableNumber,
    Long partyId,
    String paymentMethod,
    String mode,
    String status,
    String createdAt,
    int subtotalCents,
    Integer tipsPercent,
    int tipsAmountCents,
    int totalCents,
    List<StaffBillLine> items
  ) {}

  @GetMapping("/bill-requests/active")
  public List<StaffBillRequestDto> activeBillRequests(@RequestParam(value = "hallId", required = false) Long hallId, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "ADMIN");
    List<BillRequest> reqs = billRequestRepo.findByStatusOrderByCreatedAtAsc("CREATED");
    if (reqs.isEmpty()) return List.of();

    // Load tables
    Map<Long, CafeTable> tables = new HashMap<>();
    for (BillRequest br : reqs) {
      tables.computeIfAbsent(br.tableId, id -> tableRepo.findById(id).orElse(null));
    }

    // For each bill request load items
    List<StaffBillRequestDto> out = new ArrayList<>();
    for (BillRequest br : reqs) {
      CafeTable t = tables.get(br.tableId);
      if (t == null) continue;
      if (u.branchId != null && !Objects.equals(t.branchId, u.branchId)) continue;
      if (hallId != null && !Objects.equals(t.hallId, hallId)) continue;
      List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
      List<StaffBillLine> lines = new ArrayList<>();
      for (BillRequestItem it : items) {
        OrderItem oi = orderItemRepo.findById(it.orderItemId).orElse(null);
        if (oi == null) continue;
        lines.add(new StaffBillLine(oi.id, oi.nameSnapshot, oi.qty, oi.unitPriceCents, it.lineTotalCents));
      }
      out.add(new StaffBillRequestDto(br.id, t.number, br.partyId, br.paymentMethod, br.mode, br.status, br.createdAt.toString(), br.subtotalCents, br.tipsPercent, br.tipsAmountCents, br.totalCents, lines));
    }
    return out;
  }

  public record ConfirmPaidResponse(long billRequestId, String status) {}

  @PostMapping("/bill-requests/{id}/confirm-paid")
  public ConfirmPaidResponse confirmPaid(@PathVariable("id") long id, Authentication auth) {
    StaffUser staff = requireRole(auth, "WAITER", "ADMIN");
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill request not found"));
    CafeTable t = tableRepo.findById(br.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (staff.branchId != null && !Objects.equals(t.branchId, staff.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    if (!"CREATED".equals(br.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill request is not active");
    }
    br.status = "PAID_CONFIRMED";
    br.confirmedAt = java.time.Instant.now();
    br.confirmedByStaffId = staff.id;
    billRequestRepo.save(br);

    // Close order items
    List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
    for (BillRequestItem it : items) {
      OrderItem oi = orderItemRepo.findById(it.orderItemId).orElse(null);
      if (oi == null) continue;
      oi.isClosed = true;
      oi.closedAt = java.time.Instant.now();
      oi.billRequestId = br.id;
      orderItemRepo.save(oi);
    }

    // Auto-close party on WHOLE_TABLE payments
    if ("WHOLE_TABLE".equals(br.mode) && br.partyId != null) {
      TableParty p = partyRepo.findById(br.partyId).orElse(null);
      if (p != null) {
        partyService.closeParty(p, java.time.Instant.now());
      }
    }

    return new ConfirmPaidResponse(br.id, br.status);
  }

  public record CancelBillRequestResponse(long billRequestId, String status) {}

  @PostMapping("/bill-requests/{id}/cancel")
  public CancelBillRequestResponse cancelBillRequest(@PathVariable("id") long id, Authentication auth) {
    StaffUser staff = requireRole(auth, "WAITER", "ADMIN");
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill request not found"));
    CafeTable t = tableRepo.findById(br.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (staff.branchId != null && !Objects.equals(t.branchId, staff.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    if (!"CREATED".equals(br.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill request is not active");
    }
    br.status = "CANCELLED";
    billRequestRepo.save(br);

    List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
    for (BillRequestItem it : items) {
      OrderItem oi = orderItemRepo.findById(it.orderItemId).orElse(null);
      if (oi == null) continue;
      if (Objects.equals(oi.billRequestId, br.id)) {
        oi.billRequestId = null;
        orderItemRepo.save(oi);
      }
    }
    return new CancelBillRequestResponse(br.id, br.status);
  }

  // --- Party close (staff) ---
  public record ClosePartyResponse(long partyId, String status) {}

  @PostMapping("/parties/{id}/close")
  public ClosePartyResponse closeParty(@PathVariable("id") long id, Authentication auth) {
    StaffUser staff = requireRole(auth, "WAITER", "ADMIN");
    TableParty p = partyRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party not found"));
    CafeTable t = tableRepo.findById(p.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (staff.branchId != null && !Objects.equals(t.branchId, staff.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    if (!"ACTIVE".equals(p.status)) {
      return new ClosePartyResponse(p.id, p.status);
    }
    partyService.closeParty(p, java.time.Instant.now());
    return new ClosePartyResponse(p.id, p.status);
  }

  // --- Notifications feed ---
  public record NotificationEventDto(long id, String type, long refId, String createdAt) {}
  public record NotificationFeedResponse(long lastId, List<NotificationEventDto> events) {}

  @GetMapping("/notifications/feed")
  public NotificationFeedResponse feed(@RequestParam(value = "sinceId", required = false) Long sinceId, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    long from = sinceId == null ? 0L : sinceId;
    List<NotificationEvent> list = notificationEventRepo.findTop200ByBranchIdAndIdGreaterThanOrderByIdAsc(u.branchId, from);
    List<NotificationEventDto> out = new ArrayList<>();
    long last = from;
    for (NotificationEvent e : list) {
      out.add(new NotificationEventDto(e.id, e.eventType, e.refId, e.createdAt.toString()));
      if (e.id > last) last = e.id;
    }
    return new NotificationFeedResponse(last, out);
  }

  // --- Device tokens (push) ---
  public record RegisterDeviceRequest(String token, String platform) {}
  public record RegisterDeviceResponse(boolean registered) {}

  @PostMapping("/devices/register")
  public RegisterDeviceResponse registerDevice(@RequestBody RegisterDeviceRequest req, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    if (req == null || req.token == null || req.token.isBlank() || req.platform == null || req.platform.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token/platform required");
    }
    StaffDeviceToken t = staffDeviceTokenRepo.findByToken(req.token.trim()).orElseGet(StaffDeviceToken::new);
    t.token = req.token.trim();
    t.platform = req.platform.trim().toUpperCase(Locale.ROOT);
    t.staffUserId = u.id;
    t.branchId = u.branchId;
    t.lastSeenAt = java.time.Instant.now();
    staffDeviceTokenRepo.save(t);
    return new RegisterDeviceResponse(true);
  }

  public record UnregisterDeviceRequest(String token) {}

  @PostMapping("/devices/unregister")
  public void unregisterDevice(@RequestBody UnregisterDeviceRequest req, Authentication auth) {
    requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    if (req == null || req.token == null || req.token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "token required");
    }
    staffDeviceTokenRepo.deleteByToken(req.token.trim());
  }

  // --- Notifications (polling) ---
  public record NotificationCounts(long newOrders, long newCalls, long newBills, String sinceOrders, String sinceCalls, String sinceBills) {}

  @GetMapping("/notifications")
  public NotificationCounts notifications(
    @RequestParam("sinceOrders") String sinceOrders,
    @RequestParam("sinceCalls") String sinceCalls,
    @RequestParam("sinceBills") String sinceBills,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    try {
      Instant so = Instant.parse(sinceOrders);
      Instant sc = Instant.parse(sinceCalls);
      Instant sb = Instant.parse(sinceBills);
      StaffNotificationService.Counts c = notificationService.countsSince(u.branchId, so, sc, sb);
      return new NotificationCounts(c.newOrders(), c.newCalls(), c.newBills(), sinceOrders, sinceCalls, sinceBills);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid since");
    }
  }

}
