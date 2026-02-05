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
import md.virtualwaiter.domain.ChatMessage;
import md.virtualwaiter.domain.ChatRead;
import md.virtualwaiter.domain.InventoryItem;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.InventoryItemRepo;
import md.virtualwaiter.repo.OrderItemRepo;
import md.virtualwaiter.repo.OrderRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.ChatMessageRepo;
import md.virtualwaiter.repo.ChatReadRepo;
import md.virtualwaiter.repo.WaiterCallRepo;
import md.virtualwaiter.repo.BillRequestRepo;
import md.virtualwaiter.repo.BillRequestItemRepo;
import md.virtualwaiter.repo.TablePartyRepo;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.RestaurantRepo;
import md.virtualwaiter.repo.BranchHallRepo;
import md.virtualwaiter.repo.HallPlanRepo;
import md.virtualwaiter.security.QrSignatureService;
import md.virtualwaiter.service.PartyService;
import md.virtualwaiter.service.StatsService;
import md.virtualwaiter.service.LoyaltyService;
import md.virtualwaiter.service.AuditService;
import md.virtualwaiter.config.BillProperties;
import md.virtualwaiter.service.StaffNotificationService;
import md.virtualwaiter.repo.NotificationEventRepo;
import md.virtualwaiter.domain.NotificationEvent;
import md.virtualwaiter.repo.StaffDeviceTokenRepo;
import md.virtualwaiter.domain.StaffDeviceToken;
import md.virtualwaiter.repo.AuditLogRepo;
import md.virtualwaiter.domain.AuditLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

  private final StaffUserRepo staffUserRepo;
  private final ChatMessageRepo chatMessageRepo;
  private final ChatReadRepo chatReadRepo;
  private final CafeTableRepo tableRepo;
  private final OrderRepo orderRepo;
  private final OrderItemRepo orderItemRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final BillRequestRepo billRequestRepo;
  private final BillRequestItemRepo billRequestItemRepo;
  private final TablePartyRepo partyRepo;
  private final GuestSessionRepo guestSessionRepo;
  private final BranchRepo branchRepo;
  private final RestaurantRepo restaurantRepo;
  private final BranchHallRepo hallRepo;
  private final HallPlanRepo hallPlanRepo;
  private final QrSignatureService qrSig;
  private final String publicBaseUrl;
  private final StaffNotificationService notificationService;
  private final NotificationEventRepo notificationEventRepo;
  private final StaffDeviceTokenRepo staffDeviceTokenRepo;
  private final InventoryItemRepo inventoryItemRepo;
  private final AuditLogRepo auditLogRepo;
  private final PartyService partyService;
  private final BillProperties billProperties;
  private final StatsService statsService;
  private final LoyaltyService loyaltyService;
  private final AuditService auditService;

  public StaffController(
    StaffUserRepo staffUserRepo,
    ChatMessageRepo chatMessageRepo,
    ChatReadRepo chatReadRepo,
    CafeTableRepo tableRepo,
    OrderRepo orderRepo,
    OrderItemRepo orderItemRepo,
    WaiterCallRepo waiterCallRepo,
    BillRequestRepo billRequestRepo,
    BillRequestItemRepo billRequestItemRepo,
    TablePartyRepo partyRepo,
    GuestSessionRepo guestSessionRepo,
    BranchRepo branchRepo,
    RestaurantRepo restaurantRepo,
    BranchHallRepo hallRepo,
    HallPlanRepo hallPlanRepo,
    QrSignatureService qrSig,
    @Value("${app.publicBaseUrl:http://localhost:3000}") String publicBaseUrl,
    StaffNotificationService notificationService,
    NotificationEventRepo notificationEventRepo,
    StaffDeviceTokenRepo staffDeviceTokenRepo,
    InventoryItemRepo inventoryItemRepo,
    AuditLogRepo auditLogRepo,
    PartyService partyService,
    BillProperties billProperties,
    StatsService statsService,
    LoyaltyService loyaltyService,
    AuditService auditService
  ) {
    this.staffUserRepo = staffUserRepo;
    this.chatMessageRepo = chatMessageRepo;
    this.chatReadRepo = chatReadRepo;
    this.tableRepo = tableRepo;
    this.orderRepo = orderRepo;
    this.orderItemRepo = orderItemRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.billRequestRepo = billRequestRepo;
    this.billRequestItemRepo = billRequestItemRepo;
    this.partyRepo = partyRepo;
    this.guestSessionRepo = guestSessionRepo;
    this.branchRepo = branchRepo;
    this.restaurantRepo = restaurantRepo;
    this.hallRepo = hallRepo;
    this.hallPlanRepo = hallPlanRepo;
    this.qrSig = qrSig;
    this.publicBaseUrl = publicBaseUrl;
    this.notificationService = notificationService;
    this.notificationEventRepo = notificationEventRepo;
    this.staffDeviceTokenRepo = staffDeviceTokenRepo;
    this.inventoryItemRepo = inventoryItemRepo;
    this.auditLogRepo = auditLogRepo;
    this.partyService = partyService;
    this.billProperties = billProperties;
    this.statsService = statsService;
    this.loyaltyService = loyaltyService;
    this.auditService = auditService;
  }

  private static final Set<String> ROLE_ADMIN_LIKE = Set.of("ADMIN", "MANAGER", "SUPER_ADMIN", "OWNER");
  private static final Set<String> ROLE_WAITER_LIKE = Set.of("WAITER", "HOST");
  private static final Set<String> ROLE_KITCHEN_LIKE = Set.of("KITCHEN", "BAR");
  private static final Set<String> ROLE_SHIFT_SCOPED = Set.of("WAITER", "HOST", "KITCHEN", "BAR", "CASHIER");

  private boolean expireBillIfNeeded(BillRequest br) {
    if (br == null) return false;
    if (!"CREATED".equals(br.status)) return false;
    int expireMinutes = billProperties == null ? 120 : billProperties.expireMinutes;
    if (expireMinutes <= 0) return false;
    Instant cutoff = Instant.now().minusSeconds(expireMinutes * 60L);
    if (br.createdAt != null && br.createdAt.isBefore(cutoff)) {
      br.status = "EXPIRED";
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
      return true;
    }
    return false;
  }

  private StaffUser current(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");
    }
    return staffUserRepo.findByUsername(auth.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
  }

  private void requireBranchAccess(StaffUser u, Long branchId) {
    if (u == null || branchId == null || !Objects.equals(u.branchId, branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
  }

  private StaffUser requireRole(Authentication auth, String... roles) {
    StaffUser u = current(auth);
    String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
    for (String r : roles) {
      if (roleMatches(r, role)) return u;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient role");
  }

  private boolean roleMatches(String required, String actual) {
    if (required == null || actual == null) return false;
    String req = required.toUpperCase(Locale.ROOT);
    String act = actual.toUpperCase(Locale.ROOT);
    if ("SUPER_ADMIN".equals(act)) return true;
    if (req.equals(act)) return true;
    if ("ADMIN".equals(req) && ROLE_ADMIN_LIKE.contains(act)) return true;
    if ("WAITER".equals(req) && ROLE_WAITER_LIKE.contains(act)) return true;
    if ("KITCHEN".equals(req) && ROLE_KITCHEN_LIKE.contains(act)) return true;
    return false;
  }

  private Long enforceHallScope(StaffUser u, Long hallId) {
    if (u.hallId == null) return hallId;
    if (hallId != null && !Objects.equals(hallId, u.hallId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong hall");
    }
    return u.hallId;
  }

  private void requireHallScopeForTable(StaffUser u, Long tableHallId) {
    if (u.hallId != null && !Objects.equals(u.hallId, tableHallId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong hall");
    }
  }

  private void requireActiveShiftForStaff(StaffUser u) {
    if (u == null) return;
    String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
    if (ROLE_SHIFT_SCOPED.contains(role) && u.shiftStartedAt == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Shift not started");
    }
  }

  public record MeResponse(
    long id,
    String username,
    String role,
    Long branchId,
    Long hallId,
    String branchName,
    Long restaurantId,
    String restaurantName,
    String firstName,
    String lastName,
    Integer age,
    String gender,
    String photoUrl,
    Integer rating,
    Boolean recommended,
    Integer experienceYears,
    String favoriteItems
  ) {}

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    StaffUser u = current(auth);
    Branch b = u.branchId == null ? null : branchRepo.findById(u.branchId).orElse(null);
    Long restaurantId = b == null ? null : b.restaurantId;
    String restaurantName = null;
    if (restaurantId != null) {
      restaurantName = restaurantRepo.findById(restaurantId).map(r -> r.name).orElse(null);
    }
    return new MeResponse(
      u.id,
      u.username,
      u.role,
      u.branchId,
      u.hallId,
      b == null ? null : b.name,
      restaurantId,
      restaurantName,
      u.firstName,
      u.lastName,
      u.age,
      u.gender,
      u.photoUrl,
      u.rating,
      u.recommended,
      u.experienceYears,
      u.favoriteItems
    );
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
  public record WaiterDto(long id, String username, String firstName, String lastName, String photoUrl) {}
  public record WaiterMotivationRow(
    long staffUserId,
    String username,
    long ordersCount,
    long tipsCents,
    Double avgSlaMinutes
  ) {}

  public record ChatThreadDto(
    long guestSessionId,
    int tableNumber,
    String lastMessage,
    String lastSenderRole,
    String lastAt,
    String lastReadAt,
    boolean unread
  ) {}

  public record ChatMessageDto(long id, String senderRole, String message, String createdAt) {}
  public record ChatSendRequest(long guestSessionId, String message) {}

  @GetMapping("/tables")
  public List<StaffTableDto> tables(
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    hallId = enforceHallScope(u, hallId);
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

  @GetMapping("/waiters")
  public List<WaiterDto> waiters(Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    List<StaffUser> staff = staffUserRepo.findByBranchId(u.branchId);
    List<WaiterDto> out = new ArrayList<>();
    for (StaffUser su : staff) {
      if (su.role == null || !ROLE_WAITER_LIKE.contains(su.role.toUpperCase(Locale.ROOT))) continue;
      out.add(new WaiterDto(su.id, su.username, su.firstName, su.lastName, su.photoUrl));
    }
    return out;
  }

  @GetMapping("/motivation")
  public List<WaiterMotivationRow> motivation(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    hallId = enforceHallScope(u, hallId);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<StatsService.WaiterMotivationRow> rows = statsService.waiterMotivationForBranch(u.branchId, fromTs, toTs, hallId, null, null, null, null);
    List<WaiterMotivationRow> out = new ArrayList<>();
    for (StatsService.WaiterMotivationRow r : rows) {
      out.add(new WaiterMotivationRow(
        r.staffUserId(),
        r.username(),
        r.ordersCount(),
        r.tipsCents(),
        r.avgSlaMinutes()
      ));
    }
    return out;
  }

  @GetMapping("/chat/threads")
  public List<ChatThreadDto> chatThreads(
    @RequestParam(value = "limit", required = false) Integer limit,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    hallId = enforceHallScope(u, hallId);
    int lim = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
    List<ChatMessage> all = chatMessageRepo.findByBranchIdOrderByIdDesc(u.branchId);
    Map<Long, ChatMessage> latestBySession = new LinkedHashMap<>();
    for (ChatMessage m : all) {
      if (latestBySession.size() >= lim) break;
      if (latestBySession.containsKey(m.guestSessionId)) continue;
      if (hallId != null) {
        CafeTable t = tableRepo.findById(m.tableId).orElse(null);
        if (t == null || !Objects.equals(t.hallId, hallId)) continue;
      }
      latestBySession.put(m.guestSessionId, m);
    }
    Map<Long, CafeTable> tableById = new HashMap<>();
    for (ChatMessage m : latestBySession.values()) {
      if (!tableById.containsKey(m.tableId)) {
        tableRepo.findById(m.tableId).ifPresent(t -> tableById.put(t.id, t));
      }
    }
    List<ChatThreadDto> out = new ArrayList<>();
    for (ChatMessage m : latestBySession.values()) {
      CafeTable t = tableById.get(m.tableId);
      ChatRead read = chatReadRepo.findByStaffUserIdAndGuestSessionId(u.id, m.guestSessionId).orElse(null);
      String lastReadAt = read != null && read.lastReadAt != null ? read.lastReadAt.toString() : null;
      boolean unread = "GUEST".equalsIgnoreCase(m.senderRole)
        && (read == null || (m.createdAt != null && read.lastReadAt != null && m.createdAt.isAfter(read.lastReadAt)));
      out.add(new ChatThreadDto(
        m.guestSessionId,
        t != null ? t.number : 0,
        m.message,
        m.senderRole,
        m.createdAt != null ? m.createdAt.toString() : null,
        lastReadAt,
        unread
      ));
    }
    return out;
  }

  @GetMapping("/chat/messages")
  public List<ChatMessageDto> chatMessages(
    @RequestParam("guestSessionId") Long guestSessionId,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    GuestSession s = guestSessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    requireBranchAccess(u, table.branchId);
    requireHallScopeForTable(u, table.hallId);
    List<ChatMessage> msgs = chatMessageRepo.findByBranchIdAndGuestSessionIdOrderByIdAsc(table.branchId, guestSessionId);
    List<ChatMessageDto> out = new ArrayList<>();
    for (ChatMessage m : msgs) {
      out.add(new ChatMessageDto(m.id, m.senderRole, m.message, m.createdAt != null ? m.createdAt.toString() : null));
    }
    return out;
  }

  @PostMapping("/chat/send")
  public void chatSend(@RequestBody ChatSendRequest req, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    requireActiveShiftForStaff(u);
    if (req == null || req.guestSessionId <= 0 || req.message == null || req.message.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid message");
    }
    GuestSession s = guestSessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
    if (s.expiresAt != null && s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest session expired");
    }
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    requireBranchAccess(u, table.branchId);
    requireHallScopeForTable(u, table.hallId);
    String msg = req.message.trim();
    if (msg.length() > 500) msg = msg.substring(0, 500);
    ChatMessage m = new ChatMessage();
    m.branchId = table.branchId;
    m.tableId = table.id;
    m.guestSessionId = s.id;
    m.senderRole = "STAFF";
    m.staffUserId = u.id;
    m.message = msg;
    chatMessageRepo.save(m);
    auditService.log(u, "SEND", "ChatMessage", m.id, "{\"guestSessionId\":" + m.guestSessionId + "}");
  }

  public record ChatReadRequest(long guestSessionId) {}

  @PostMapping("/chat/read")
  public void chatRead(@RequestBody ChatReadRequest req, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    if (req == null || req.guestSessionId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid guestSessionId");
    }
    GuestSession s = guestSessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    requireBranchAccess(u, table.branchId);
    requireHallScopeForTable(u, table.hallId);
    ChatRead read = chatReadRepo.findByStaffUserIdAndGuestSessionId(u.id, req.guestSessionId).orElse(null);
    if (read == null) {
      read = new ChatRead();
      read.branchId = table.branchId;
      read.staffUserId = u.id;
      read.guestSessionId = req.guestSessionId;
    }
    read.lastReadAt = Instant.now();
    chatReadRepo.save(read);
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
    hallId = enforceHallScope(u, hallId);
    if (planId != null) {
      HallPlan p = hallPlanRepo.findById(planId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
      BranchHall h = hallRepo.findById(p.hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!Objects.equals(h.branchId, u.branchId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
      requireHallScopeForTable(u, h.id);
      if (hallId != null && !Objects.equals(hallId, h.id)) {
        BranchHall requestedHall = hallRepo.findById(hallId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
        if (!Objects.equals(requestedHall.branchId, u.branchId)) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
        }
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
    if (u.hallId != null) {
      BranchHall h = hallRepo.findById(u.hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!Objects.equals(h.branchId, u.branchId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
      return List.of(new HallDto(h.id, h.branchId, h.name, h.isActive, h.sortOrder, h.layoutBgUrl, h.layoutZonesJson, h.activePlanId));
    }
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
    if (u.hallId != null && !Objects.equals(u.hallId, hallId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong hall");
    }
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
    requireHallScopeForTable(u, t.hallId);
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
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
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
      requireHallScopeForTable(u, t.hallId);
    }

    List<Order> orders;
    Instant fromTs = parseInstantOrDateOrNull(from, true);
    Instant toTs = parseInstantOrDateOrNull(to, false);
    if (guestSessionId != null && tableId == null) {
      if (fromTs != null && toTs != null) {
        orders = orderRepo.findByGuestSessionIdAndCreatedAtBetweenOrderByCreatedAtDesc(guestSessionId, fromTs, toTs);
      } else {
        orders = orderRepo.findTop200ByGuestSessionIdOrderByCreatedAtDesc(guestSessionId);
      }
    } else if (resolvedTableId != null) {
      if (fromTs != null && toTs != null) {
        orders = orderRepo.findByTableIdAndCreatedAtBetweenOrderByCreatedAtDesc(resolvedTableId, fromTs, toTs);
      } else {
        orders = orderRepo.findTop200ByTableIdOrderByCreatedAtDesc(resolvedTableId);
      }
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

  private static Instant parseInstantOrDateOrNull(String v, boolean isStart) {
    if (v == null || v.isBlank()) return null;
    String s = v.trim();
    try {
      return Instant.parse(s);
    } catch (Exception ignored) {
      LocalDate d = LocalDate.parse(s);
      return isStart ? d.atStartOfDay().toInstant(ZoneOffset.UTC)
        : d.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusSeconds(1);
    }
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
    hallId = enforceHallScope(u, hallId);
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
    hallId = enforceHallScope(u, hallId);
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
    hallId = enforceHallScope(u, hallId);
    if (ids == null || ids.isBlank()) return List.of();
    List<Long> orderIds = new ArrayList<>();
    for (String part : ids.split(",")) {
      if (orderIds.size() >= 200) break;
      String v = part.trim();
      if (v.isEmpty()) continue;
      try {
        orderIds.add(Long.parseLong(v));
      } catch (Exception ignored) {}
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
    hallId = enforceHallScope(u, hallId);
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
    requireActiveShiftForStaff(u);
    Order o = orderRepo.findById(orderId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    // simple branch-level authorization
    CafeTable t = tableRepo.findById(o.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!Objects.equals(t.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    requireHallScopeForTable(u, t.hallId);
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
    String prevStatus = o.status;
    o.status = next;
    Instant now = Instant.now();
    switch (next) {
      case "ACCEPTED" -> {
        if (o.acceptedAt == null) o.acceptedAt = now;
      }
      case "IN_PROGRESS" -> {
        if (o.inProgressAt == null) o.inProgressAt = now;
      }
      case "READY" -> {
        if (o.readyAt == null) o.readyAt = now;
      }
      case "SERVED" -> {
        if (o.servedAt == null) o.servedAt = now;
      }
      case "CLOSED" -> {
        if (o.closedAt == null) o.closedAt = now;
      }
      case "CANCELLED" -> {
        if (o.cancelledAt == null) o.cancelledAt = now;
      }
      default -> {
      }
    }
    if (o.handledByStaffId == null) {
      String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
      if (Set.of("WAITER", "HOST", "ADMIN", "MANAGER").contains(role)) {
        o.handledByStaffId = u.id;
      }
    }
    orderRepo.save(o);
    auditService.log(u, "UPDATE_STATUS", "Order", o.id, "{\"from\":\"" + (prevStatus == null ? "" : prevStatus) + "\",\"to\":\"" + next + "\"}");
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
    if ("ADMIN".equals(r) || "MANAGER".equals(r)) return isAllowedTransition(current, next);
    if ("KITCHEN".equals(r) || "BAR".equals(r)) {
      return isAllowedTransition(current, next) && Set.of("ACCEPTED", "IN_PROGRESS", "READY", "SERVED", "CLOSED").contains(next);
    }
    if ("WAITER".equals(r) || "HOST".equals(r)) {
      return isAllowedTransition(current, next) && Set.of("ACCEPTED", "READY", "SERVED", "CLOSED", "CANCELLED").contains(next);
    }
    return false;
  }

  public record StaffWaiterCallDto(long id, long tableId, int tableNumber, String status, String createdAt) {}

  @GetMapping("/waiter-calls/active")
  public List<StaffWaiterCallDto> activeCalls(@RequestParam(value = "hallId", required = false) Long hallId, Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "ADMIN");
    hallId = enforceHallScope(u, hallId);
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
    requireActiveShiftForStaff(u);
    WaiterCall c = waiterCallRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter call not found"));
    CafeTable t = tableRepo.findById(c.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!Objects.equals(t.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    requireHallScopeForTable(u, t.hallId);
    if (req == null || req.status == null || req.status.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing status");
    }
    String next = req.status.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("NEW", "ACKNOWLEDGED", "CLOSED").contains(next)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status");
    }
    c.status = next;
    waiterCallRepo.save(c);
    auditService.log(u, "UPDATE_STATUS", "WaiterCall", c.id, "{\"to\":\"" + next + "\"}");
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
    requireHallScopeForTable(u, table.hallId);
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
    hallId = enforceHallScope(u, hallId);
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
      if (expireBillIfNeeded(br)) continue;
      CafeTable t = tables.get(br.tableId);
      if (t == null) continue;
      if (u.branchId != null && !Objects.equals(t.branchId, u.branchId)) continue;
      requireHallScopeForTable(u, t.hallId);
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
    requireActiveShiftForStaff(staff);
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill request not found"));
    CafeTable t = tableRepo.findById(br.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (staff.branchId != null && !Objects.equals(t.branchId, staff.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    requireHallScopeForTable(staff, t.hallId);
    if (!"CREATED".equals(br.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill request is not active");
    }
    br.status = "PAID_CONFIRMED";
    br.confirmedAt = java.time.Instant.now();
    br.confirmedByStaffId = staff.id;
    billRequestRepo.save(br);
    loyaltyService.applyBillPaid(br);

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

    auditService.log(staff, "CONFIRM_PAID", "BillRequest", br.id, "{\"status\":\"" + br.status + "\"}");
    return new ConfirmPaidResponse(br.id, br.status);
  }

  public record CancelBillRequestResponse(long billRequestId, String status) {}

  @PostMapping("/bill-requests/{id}/cancel")
  public CancelBillRequestResponse cancelBillRequest(@PathVariable("id") long id, Authentication auth) {
    StaffUser staff = requireRole(auth, "WAITER", "ADMIN");
    requireActiveShiftForStaff(staff);
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bill request not found"));
    CafeTable t = tableRepo.findById(br.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (staff.branchId != null && !Objects.equals(t.branchId, staff.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    requireHallScopeForTable(staff, t.hallId);
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
    auditService.log(staff, "CANCEL", "BillRequest", br.id, "{\"status\":\"" + br.status + "\"}");
    return new CancelBillRequestResponse(br.id, br.status);
  }

  // --- Party close (staff) ---
  public record ClosePartyResponse(long partyId, String status) {}

  @PostMapping("/parties/{id}/close")
  public ClosePartyResponse closeParty(@PathVariable("id") long id, Authentication auth) {
    StaffUser staff = requireRole(auth, "WAITER", "ADMIN");
    requireActiveShiftForStaff(staff);
    TableParty p = partyRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party not found"));
    CafeTable t = tableRepo.findById(p.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (staff.branchId != null && !Objects.equals(t.branchId, staff.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    requireHallScopeForTable(staff, t.hallId);
    if (!"ACTIVE".equals(p.status)) {
      return new ClosePartyResponse(p.id, p.status);
    }
    partyService.closeParty(p, java.time.Instant.now());
    auditService.log(staff, "CLOSE", "Party", p.id, "{\"status\":\"" + p.status + "\"}");
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
  public record RegisterDeviceRequest(String token, String platform, String deviceId, String deviceName) {}
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
    t.deviceId = req.deviceId == null || req.deviceId.isBlank() ? null : req.deviceId.trim();
    t.deviceName = req.deviceName == null || req.deviceName.isBlank() ? null : req.deviceName.trim();
    t.revokedAt = null;
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
    StaffDeviceToken t = staffDeviceTokenRepo.findByToken(req.token.trim()).orElse(null);
    if (t != null) {
      t.revokedAt = Instant.now();
      staffDeviceTokenRepo.save(t);
    }
  }

  // --- Shift control ---
  public record ShiftStatus(String startedAt) {}

  @GetMapping("/shift")
  public ShiftStatus getShift(Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    return new ShiftStatus(u.shiftStartedAt == null ? null : u.shiftStartedAt.toString());
  }

  @PostMapping("/shift/start")
  public ShiftStatus startShift(Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    u.shiftStartedAt = Instant.now();
    staffUserRepo.save(u);
    return new ShiftStatus(u.shiftStartedAt.toString());
  }

  @PostMapping("/shift/clear")
  public void clearShift(Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    u.shiftStartedAt = null;
    staffUserRepo.save(u);
  }

  // --- Ops audit events ---
  public record AuditLogDto(
    long id,
    String action,
    String entityType,
    Long entityId,
    String actorUsername,
    String actorRole,
    String detailsJson,
    String createdAt
  ) {}

  @GetMapping("/ops/audit")
  public List<AuditLogDto> opsAudit(
    @RequestParam(value = "action", required = false) String action,
    @RequestParam(value = "entityType", required = false) String entityType,
    @RequestParam(value = "fromTs", required = false) String fromTs,
    @RequestParam(value = "toTs", required = false) String toTs,
    @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit,
    Authentication auth
  ) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    int safeLimit = limit == null ? 50 : Math.max(1, Math.min(200, limit));
    Instant from = null;
    Instant to = null;
    if (fromTs != null && !fromTs.isBlank()) {
      try { from = Instant.parse(fromTs); } catch (Exception ignored) {}
    }
    if (toTs != null && !toTs.isBlank()) {
      try { to = Instant.parse(toTs); } catch (Exception ignored) {}
    }
    List<AuditLog> logs = auditLogRepo.findFiltered(
      u.branchId,
      action,
      entityType,
      null,
      from,
      to,
      null,
      null,
      PageRequest.of(0, safeLimit)
    );
    List<AuditLogDto> out = new ArrayList<>();
    for (AuditLog a : logs) {
      out.add(new AuditLogDto(
        a.id,
        a.action,
        a.entityType,
        a.entityId,
        a.actorUsername,
        a.actorRole,
        a.detailsJson,
        a.createdAt == null ? null : a.createdAt.toString()
      ));
    }
    return out;
  }

  // --- Inventory (low stock) ---
  public record InventoryItemDto(
    long id,
    String nameRu,
    String nameRo,
    String nameEn,
    String unit,
    Double qtyOnHand,
    Double minQty,
    boolean isActive
  ) {}

  @GetMapping("/inventory/low-stock")
  public List<InventoryItemDto> lowStockInventory(Authentication auth) {
    StaffUser u = requireRole(auth, "WAITER", "KITCHEN", "ADMIN");
    List<InventoryItem> items = inventoryItemRepo.findByBranchIdAndIsActiveTrueOrderByIdDesc(u.branchId);
    List<InventoryItemDto> out = new ArrayList<>();
    for (InventoryItem it : items) {
      double qty = it.qtyOnHand == null ? 0.0 : it.qtyOnHand;
      double min = it.minQty == null ? 0.0 : it.minQty;
      if (min > 0 && qty <= min) {
        out.add(new InventoryItemDto(it.id, it.nameRu, it.nameRo, it.nameEn, it.unit, qty, min, it.isActive));
      }
    }
    return out;
  }

  private static Instant parseInstantOrDate(String v, boolean isStart) {
    if (v == null || v.isBlank()) {
      Instant now = Instant.now();
      return isStart ? now.minusSeconds(30L * 24 * 3600) : now;
    }
    String s = v.trim();
    try {
      return Instant.parse(s);
    } catch (Exception ignored) {
      LocalDate d = LocalDate.parse(s);
      return isStart ? d.atStartOfDay().toInstant(ZoneOffset.UTC)
        : d.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusSeconds(1);
    }
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
