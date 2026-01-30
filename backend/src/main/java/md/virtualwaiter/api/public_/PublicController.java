package md.virtualwaiter.api.public_;

import md.virtualwaiter.domain.*;
import md.virtualwaiter.security.QrSignatureService;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.MenuCategoryRepo;
import md.virtualwaiter.repo.MenuItemRepo;
import md.virtualwaiter.repo.OrderItemRepo;
import md.virtualwaiter.repo.OrderRepo;
import md.virtualwaiter.repo.WaiterCallRepo;
import md.virtualwaiter.repo.TablePartyRepo;
import md.virtualwaiter.repo.BillRequestRepo;
import md.virtualwaiter.repo.BillRequestItemRepo;
import md.virtualwaiter.repo.ModifierGroupRepo;
import md.virtualwaiter.repo.ModifierOptionRepo;
import md.virtualwaiter.repo.MenuItemModifierGroupRepo;
import md.virtualwaiter.otp.OtpService;
import md.virtualwaiter.service.BranchSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.*;

@RestController
@RequestMapping("/api/public")
public class PublicController {

  private final CafeTableRepo tableRepo;
  private final GuestSessionRepo sessionRepo;
  private final MenuCategoryRepo categoryRepo;
  private final MenuItemRepo itemRepo;
  private final OrderRepo orderRepo;
  private final OrderItemRepo orderItemRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final TablePartyRepo partyRepo;
  private final BillRequestRepo billRequestRepo;
  private final BillRequestItemRepo billRequestItemRepo;
  private final ModifierGroupRepo modifierGroupRepo;
  private final ModifierOptionRepo modifierOptionRepo;
  private final MenuItemModifierGroupRepo menuItemModifierGroupRepo;
  private final QrSignatureService qrSig;
  private final OtpService otpService;
  private final BranchSettingsService settingsService;

  public PublicController(
    CafeTableRepo tableRepo,
    GuestSessionRepo sessionRepo,
    MenuCategoryRepo categoryRepo,
    MenuItemRepo itemRepo,
    OrderRepo orderRepo,
    OrderItemRepo orderItemRepo,
    WaiterCallRepo waiterCallRepo,
    TablePartyRepo partyRepo,
    BillRequestRepo billRequestRepo,
    BillRequestItemRepo billRequestItemRepo,
    ModifierGroupRepo modifierGroupRepo,
    ModifierOptionRepo modifierOptionRepo,
    MenuItemModifierGroupRepo menuItemModifierGroupRepo,
    QrSignatureService qrSig,
    OtpService otpService,
    BranchSettingsService settingsService
  ) {
    this.tableRepo = tableRepo;
    this.sessionRepo = sessionRepo;
    this.categoryRepo = categoryRepo;
    this.itemRepo = itemRepo;
    this.orderRepo = orderRepo;
    this.orderItemRepo = orderItemRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.partyRepo = partyRepo;
    this.billRequestRepo = billRequestRepo;
    this.billRequestItemRepo = billRequestItemRepo;
    this.modifierGroupRepo = modifierGroupRepo;
    this.modifierOptionRepo = modifierOptionRepo;
    this.menuItemModifierGroupRepo = menuItemModifierGroupRepo;
    this.qrSig = qrSig;
    this.otpService = otpService;
    this.settingsService = settingsService;
  }

  public record StartSessionRequest(@NotBlank String tablePublicId, @NotBlank String sig, @NotBlank String locale) {}
  public record StartSessionResponse(long guestSessionId, long tableId, int tableNumber, long branchId, String locale, boolean otpRequired, boolean isVerified) {}

  @PostMapping("/session/start")
  public StartSessionResponse startSession(@Valid @RequestBody StartSessionRequest req) {
    if (!qrSig.verifyTablePublicId(req.tablePublicId(), req.sig())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid QR signature");
    }

    CafeTable table = tableRepo.findByPublicId(req.tablePublicId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));

    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    GuestSession s = new GuestSession();
    s.tableId = table.id;
    s.locale = normalizeLocale(req.locale());
    s.expiresAt = Instant.now().plus(12, ChronoUnit.HOURS);
    s = sessionRepo.save(s);

    return new StartSessionResponse(s.id, table.id, table.number, table.branchId, s.locale, settings.requireOtpForFirstOrder(), s.isVerified);
  }

  // --- Menu ---
  public record MenuItemDto(
    long id,
    String name,
    String description,
    String ingredients,
    String allergens,
    String weight,
    List<String> photos,
    List<String> tags,
    int priceCents,
    String currency
  ) {}

  public record MenuCategoryDto(
    long id,
    String name,
    int sortOrder,
    List<MenuItemDto> items
  ) {}

  public record MenuResponse(
    long branchId,
    String locale,
    List<MenuCategoryDto> categories
  ) {}

  // --- Modifiers public ---
  public record ModifierOptionDto(long id, String name, int priceCents) {}
  public record ModifierGroupDto(long id, String name, boolean isRequired, Integer minSelect, Integer maxSelect, List<ModifierOptionDto> options) {}
  public record MenuItemModifiersResponse(long menuItemId, List<ModifierGroupDto> groups) {}

  @GetMapping("/menu-item/{id}/modifiers")
  public MenuItemModifiersResponse getMenuItemModifiers(
    @PathVariable("id") long id,
    @RequestParam("tablePublicId") String tablePublicId,
    @RequestParam("sig") String sig,
    @RequestParam(value = "locale", required = false) String locale
  ) {
    if (!qrSig.verifyTablePublicId(tablePublicId, sig)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid QR signature");
    }
    CafeTable table = tableRepo.findByPublicId(tablePublicId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    MenuItem item = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory cat = categoryRepo.findById(item.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    if (!Objects.equals(cat.branchId, table.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }

    String loc = normalizeLocale(locale);
    List<MenuItemModifierGroup> links = menuItemModifierGroupRepo.findByMenuItemIdOrderBySortOrderAscIdAsc(id);
    if (links.isEmpty()) return new MenuItemModifiersResponse(id, List.of());

    List<Long> groupIds = links.stream().map(l -> l.groupId).toList();
    Map<Long, ModifierGroup> groupById = new HashMap<>();
    for (ModifierGroup g : modifierGroupRepo.findAllById(groupIds)) groupById.put(g.id, g);
    List<ModifierOption> options = modifierOptionRepo.findByGroupIdIn(groupIds);
    Map<Long, List<ModifierOption>> optionsByGroup = new HashMap<>();
    for (ModifierOption o : options) {
      if (!o.isActive) continue;
      optionsByGroup.computeIfAbsent(o.groupId, k -> new ArrayList<>()).add(o);
    }

    List<ModifierGroupDto> out = new ArrayList<>();
    for (MenuItemModifierGroup link : links) {
      ModifierGroup g = groupById.get(link.groupId);
      if (g == null || !g.isActive) continue;
      List<ModifierOptionDto> opt = new ArrayList<>();
      for (ModifierOption o : optionsByGroup.getOrDefault(g.id, List.of())) {
        opt.add(new ModifierOptionDto(o.id, pick(loc, o.nameRu, o.nameRo, o.nameEn), o.priceCents));
      }
      out.add(new ModifierGroupDto(
        g.id,
        pick(loc, g.nameRu, g.nameRo, g.nameEn),
        link.isRequired,
        link.minSelect,
        link.maxSelect,
        opt
      ));
    }
    return new MenuItemModifiersResponse(id, out);
  }

  @GetMapping("/menu")
  public MenuResponse getMenu(
    @RequestParam("tablePublicId") String tablePublicId,
    @RequestParam("sig") String sig,
    @RequestParam(value = "locale", required = false) String locale
  ) {
    if (!qrSig.verifyTablePublicId(tablePublicId, sig)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid QR signature");
    }
    CafeTable table = tableRepo.findByPublicId(tablePublicId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));

    String loc = normalizeLocale(locale);
    List<MenuCategory> cats = categoryRepo.findByBranchIdAndIsActiveOrderBySortOrderAscIdAsc(table.branchId, true);
    List<Long> catIds = cats.stream().map(c -> c.id).toList();
    List<MenuItem> items = catIds.isEmpty() ? List.of() : itemRepo.findByCategoryIdInAndIsActiveAndIsStopList(catIds, true, false);

    Map<Long, List<MenuItemDto>> itemsByCat = new HashMap<>();
    for (MenuItem it : items) {
      itemsByCat.computeIfAbsent(it.categoryId, k -> new ArrayList<>())
        .add(new MenuItemDto(
          it.id,
          pick(loc, it.nameRu, it.nameRo, it.nameEn),
          pick(loc, it.descriptionRu, it.descriptionRo, it.descriptionEn),
          pick(loc, it.ingredientsRu, it.ingredientsRo, it.ingredientsEn),
          it.allergens,
          it.weight,
          splitCsv(it.photoUrls),
          splitCsv(it.tags),
          it.priceCents,
          it.currency
        ));
    }

    List<MenuCategoryDto> out = new ArrayList<>();
    for (MenuCategory c : cats) {
      out.add(new MenuCategoryDto(
        c.id,
        pick(loc, c.nameRu, c.nameRo, c.nameEn),
        c.sortOrder,
        itemsByCat.getOrDefault(c.id, List.of())
      ));
    }

    return new MenuResponse(table.branchId, loc, out);
  }

  // --- Orders ---
  public record CreateOrderItemReq(
    @NotNull Long menuItemId,
    @Positive int qty,
    String comment,
    String modifiersJson
  ) {}

  public record CreateOrderRequest(
    @NotNull Long guestSessionId,
    @NotNull List<CreateOrderItemReq> items
  ) {}

  public record CreateOrderResponse(long orderId, String status) {}

  @PostMapping("/orders")
  public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest req) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    if (settings.requireOtpForFirstOrder() && !s.isVerified) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "OTP required before first order");
    }

    if (req.items == null || req.items.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order items empty");
    }

    // Load menu items for snapshot/prices
    List<Long> ids = req.items.stream().map(i -> i.menuItemId).toList();
    Map<Long, MenuItem> menu = new HashMap<>();
    for (MenuItem mi : itemRepo.findAllById(ids)) {
      menu.put(mi.id, mi);
    }
    for (Long id : ids) {
      if (!menu.containsKey(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown menu item: " + id);
    }

    Order o = new Order();
    o.tableId = table.id;
    o.guestSessionId = s.id;
    o.status = "NEW";
    o = orderRepo.save(o);

    for (CreateOrderItemReq i : req.items) {
      MenuItem mi = menu.get(i.menuItemId);
      ModSelection sel = parseAndValidateModifiers(mi.id, i.modifiersJson);
      int basePrice = mi.priceCents;
      int modifiersPrice = sel.totalPriceCents;
      int unitPrice = basePrice + modifiersPrice;
      OrderItem oi = new OrderItem();
      oi.orderId = o.id;
      oi.menuItemId = mi.id;
      oi.nameSnapshot = pick(s.locale, mi.nameRu, mi.nameRo, mi.nameEn);
      oi.unitPriceCents = unitPrice;
      oi.basePriceCents = basePrice;
      oi.modifiersPriceCents = modifiersPrice;
      oi.qty = i.qty;
      oi.comment = i.comment;
      oi.modifiersJson = sel.rawJson;
      orderItemRepo.save(oi);
    }

    return new CreateOrderResponse(o.id, o.status);
  }


  // --- Party PIN (group table) ---
  public record CreatePartyRequest(@NotNull Long guestSessionId) {}
  public record CreatePartyResponse(long partyId, String pin, Instant expiresAt) {}

  @PostMapping("/party/create")
  public CreatePartyResponse createParty(@Valid @RequestBody CreatePartyRequest req) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    if (settings.requireOtpForFirstOrder() && !s.isVerified) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "OTP required before joining a party");
    }
    if (!settings.enablePartyPin()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party PIN is disabled");
    }

    // If already in an active party, return it (idempotent UX)
    if (s.partyId != null) {
      Optional<TableParty> existing = partyRepo.findById(s.partyId);
      if (existing.isPresent() && "ACTIVE".equals(existing.get().status) && existing.get().expiresAt.isAfter(Instant.now())) {
        TableParty p = existing.get();
        return new CreatePartyResponse(p.id, p.pin, p.expiresAt);
      }
    }

    String pin = generatePin4();
    Instant now = Instant.now();
    Instant exp = now.plus(2, ChronoUnit.HOURS);

    // Ensure PIN uniqueness for the table among active parties
    for (int tries = 0; tries < 10; tries++) {
      if (partyRepo.findActiveByTableIdAndPin(s.tableId, pin, now).isEmpty()) break;
      pin = generatePin4();
    }
    if (partyRepo.findActiveByTableIdAndPin(s.tableId, pin, now).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Failed to allocate PIN");
    }

    TableParty p = new TableParty();
    p.tableId = s.tableId;
    p.pin = pin;
    p.status = "ACTIVE";
    p.expiresAt = exp;
    p = partyRepo.save(p);

    s.partyId = p.id;
    sessionRepo.save(s);

    return new CreatePartyResponse(p.id, p.pin, p.expiresAt);
  }

  public record JoinPartyRequest(@NotNull Long guestSessionId, @NotBlank String pin) {}
  public record JoinPartyResponse(long partyId, String pin, Instant expiresAt) {}

  @PostMapping("/party/join")
  public JoinPartyResponse joinParty(@Valid @RequestBody JoinPartyRequest req) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    if (settings.requireOtpForFirstOrder() && !s.isVerified) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "OTP required before joining a party");
    }
    if (!settings.enablePartyPin()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party PIN is disabled");
    }

    String pin = req.pin().trim();
    if (pin.length() != 4 || !pin.chars().allMatch(Character::isDigit)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN must be 4 digits");
    }

    TableParty p = partyRepo.findActiveByTableIdAndPin(s.tableId, pin, Instant.now())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party not found or expired"));

    s.partyId = p.id;
    sessionRepo.save(s);

    return new JoinPartyResponse(p.id, p.pin, p.expiresAt);
  }

  private static String generatePin4() {
    int v = new java.util.Random().nextInt(10000);
    return String.format(java.util.Locale.ROOT, "%04d", v);
  }


  // --- OTP (SMS verification) ---
  public record SendOtpRequest(@NotNull Long guestSessionId, @NotBlank String phoneE164, String locale) {}
  public record SendOtpResponse(long challengeId, int ttlSeconds, String devCode) {}

  @PostMapping("/otp/send")
  public SendOtpResponse sendOtp(@Valid @RequestBody SendOtpRequest req) {
    var r = otpService.sendOtp(req.guestSessionId, req.phoneE164, normalizeLocale(req.locale));
    return new SendOtpResponse(r.challengeId(), r.ttlSeconds(), r.devCode());
  }

  public record VerifyOtpRequest(@NotNull Long guestSessionId, @NotNull Long challengeId, @NotBlank String code) {}
  public record VerifyOtpResponse(boolean verified) {}

  @PostMapping("/otp/verify")
  public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
    otpService.verifyOtp(req.guestSessionId, req.challengeId, req.code);
    return new VerifyOtpResponse(true);
  }


  // --- Waiter call ---
  public record WaiterCallRequest(@NotNull Long guestSessionId) {}
  public record WaiterCallResponse(long waiterCallId, String status) {}

  @PostMapping("/waiter-call")
  public WaiterCallResponse callWaiter(@Valid @RequestBody WaiterCallRequest req) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    WaiterCall wc = new WaiterCall();
    wc.tableId = s.tableId;
    wc.guestSessionId = s.id;
    wc.status = "NEW";
    wc = waiterCallRepo.save(wc);
    return new WaiterCallResponse(wc.id, wc.status);
  }

  private static String normalizeLocale(String s) {
    if (s == null) return "ru";
    String v = s.trim().toLowerCase(Locale.ROOT);
    return switch (v) { case "ru", "ro", "en" -> v; default -> "ru"; };
  }

  private static String pick(String locale, String ru, String ro, String en) {
    if (locale == null) locale = "ru";
    return switch (locale) {
      case "ro" -> (ro != null && !ro.isBlank()) ? ro : ru;
      case "en" -> (en != null && !en.isBlank()) ? en : ru;
      default -> ru;
    };
  }

  private static List<String> splitCsv(String s) {
    if (s == null || s.isBlank()) return List.of();
    String[] parts = s.split(",");
    List<String> out = new ArrayList<>();
    for (String p : parts) {
      String v = p.trim();
      if (!v.isEmpty()) out.add(v);
    }
    return out;
  }

  private static class ModSelection {
    final String rawJson;
    final Set<Long> optionIds;
    final int totalPriceCents;

    ModSelection(String rawJson, Set<Long> optionIds, int totalPriceCents) {
      this.rawJson = rawJson;
      this.optionIds = optionIds;
      this.totalPriceCents = totalPriceCents;
    }
  }

  private ModSelection parseAndValidateModifiers(long menuItemId, String modifiersJson) {
    if (modifiersJson == null || modifiersJson.isBlank()) {
      // still need to validate required groups
      List<MenuItemModifierGroup> links = menuItemModifierGroupRepo.findByMenuItemIdOrderBySortOrderAscIdAsc(menuItemId);
      for (MenuItemModifierGroup link : links) {
        int min = link.minSelect != null ? link.minSelect : (link.isRequired ? 1 : 0);
        if (min > 0) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required modifiers for group " + link.groupId);
        }
      }
      return new ModSelection(null, Set.of(), 0);
    }

    Set<Long> selected = new HashSet<>();
    String raw = modifiersJson;
    try {
      // Expect JSON with { optionIds: [1,2,3] }
      int idx = raw.indexOf('[');
      int end = raw.indexOf(']');
      if (idx >= 0 && end > idx) {
        String inner = raw.substring(idx + 1, end);
        for (String part : inner.split(",")) {
          String t = part.trim();
          if (t.isEmpty()) continue;
          selected.add(Long.parseLong(t));
        }
      }
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid modifiersJson");
    }

    List<MenuItemModifierGroup> links = menuItemModifierGroupRepo.findByMenuItemIdOrderBySortOrderAscIdAsc(menuItemId);
    if (links.isEmpty()) {
      if (!selected.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modifiers not allowed for item");
      }
      return new ModSelection(raw, Set.of(), 0);
    }

    Map<Long, MenuItemModifierGroup> linkByGroup = new HashMap<>();
    for (MenuItemModifierGroup link : links) linkByGroup.put(link.groupId, link);
    List<Long> groupIds = links.stream().map(l -> l.groupId).toList();
    List<ModifierOption> options = modifierOptionRepo.findByGroupIdIn(groupIds);
    Map<Long, ModifierOption> optionById = new HashMap<>();
    for (ModifierOption o : options) {
      if (!o.isActive) continue;
      optionById.put(o.id, o);
    }

    Map<Long, Integer> countByGroup = new HashMap<>();
    int totalPrice = 0;
    for (Long optId : selected) {
      ModifierOption opt = optionById.get(optId);
      if (opt == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown modifier option: " + optId);
      }
      MenuItemModifierGroup link = linkByGroup.get(opt.groupId);
      if (link == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modifier option not allowed for this item: " + optId);
      }
      countByGroup.put(opt.groupId, countByGroup.getOrDefault(opt.groupId, 0) + 1);
      totalPrice += opt.priceCents;
    }

    for (MenuItemModifierGroup link : links) {
      int count = countByGroup.getOrDefault(link.groupId, 0);
      int min = link.minSelect != null ? link.minSelect : (link.isRequired ? 1 : 0);
      Integer max = link.maxSelect;
      if (count < min) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough modifiers for group " + link.groupId);
      }
      if (max != null && count > max) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many modifiers for group " + link.groupId);
      }
    }

    return new ModSelection(raw, selected, totalPrice);
  }

  // --- Bill request / offline payment ---
  public record BillOptionsItem(
    long orderItemId,
    long guestSessionId,
    String name,
    int qty,
    int unitPriceCents,
    int lineTotalCents
  ) {}

  public record BillOptionsResponse(
    boolean allowPayOtherGuestsItems,
    boolean allowPayWholeTable,
    boolean tipsEnabled,
    List<Integer> tipsPercentages,
    boolean enablePartyPin,
    Long partyId,
    List<BillOptionsItem> myItems,
    List<BillOptionsItem> tableItems
  ) {}

  @GetMapping("/bill-options")
  public BillOptionsResponse getBillOptions(@RequestParam("guestSessionId") long guestSessionId) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);

    List<Order> orders = orderRepo.findByTableIdOrderByCreatedAtDesc(s.tableId);
    Map<Long, Long> orderToSession = new HashMap<>();
    for (Order o : orders) {
      orderToSession.put(o.id, o.guestSessionId);
    }
    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> allItems = orderIds.isEmpty() ? List.of() : orderItemRepo.findByOrderIdIn(orderIds);
    List<OrderItem> openItems = allItems.stream()
      .filter(oi -> !oi.isClosed && oi.billRequestId == null)
      .toList();

    List<BillOptionsItem> myItems = new ArrayList<>();
    List<BillOptionsItem> tableItems = new ArrayList<>();
    for (OrderItem oi : openItems) {
      long ownerSessionId = orderToSession.getOrDefault(oi.orderId, 0L);
      BillOptionsItem dto = new BillOptionsItem(
        oi.id,
        ownerSessionId,
        oi.nameSnapshot,
        oi.qty,
        oi.unitPriceCents,
        oi.unitPriceCents * oi.qty
      );
      if (ownerSessionId == s.id) {
        myItems.add(dto);
      }
      if (settings.allowPayOtherGuestsItems() || settings.allowPayWholeTable()) {
        tableItems.add(dto);
      }
    }

    return new BillOptionsResponse(
      settings.allowPayOtherGuestsItems(),
      settings.allowPayWholeTable(),
      settings.tipsEnabled(),
      settings.tipsPercentages(),
      settings.enablePartyPin(),
      s.partyId,
      myItems,
      tableItems
    );
  }

  public record CreateBillRequest(
    @NotNull Long guestSessionId,
    @NotBlank String mode, // MY | SELECTED | WHOLE_TABLE
    List<Long> orderItemIds, // for SELECTED
    @NotBlank String paymentMethod, // CASH | TERMINAL
    Integer tipsPercent
  ) {}

  public record BillItemLine(long orderItemId, String name, int qty, int unitPriceCents, int lineTotalCents) {}

  public record BillRequestResponse(
    long billRequestId,
    String status,
    String paymentMethod,
    String mode,
    int subtotalCents,
    Integer tipsPercent,
    int tipsAmountCents,
    int totalCents,
    List<BillItemLine> items
  ) {}

  @PostMapping("/bill-request/create")
  public BillRequestResponse createBillRequest(@Valid @RequestBody CreateBillRequest req) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);

    String mode = req.mode == null ? "" : req.mode.trim().toUpperCase(Locale.ROOT);
    String pay = req.paymentMethod == null ? "" : req.paymentMethod.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("CASH","TERMINAL").contains(pay)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported paymentMethod");
    }
    if (!Set.of("MY","SELECTED","WHOLE_TABLE").contains(mode)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported mode");
    }

    List<Order> orders;
    if ("MY".equals(mode)) {
      orders = orderRepo.findByGuestSessionIdOrderByCreatedAtDesc(s.id);
      if (orders.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No orders for this session");
      }
    } else if ("SELECTED".equals(mode)) {
      if (req.orderItemIds == null || req.orderItemIds.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderItemIds required for SELECTED");
      }
      if (settings.allowPayOtherGuestsItems()) {
        orders = orderRepo.findByTableIdOrderByCreatedAtDesc(s.tableId);
        if (orders.isEmpty()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No orders for this table");
        }
      } else {
        orders = orderRepo.findByGuestSessionIdOrderByCreatedAtDesc(s.id);
        if (orders.isEmpty()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No orders for this session");
        }
      }
    } else {
      // WHOLE_TABLE
      if (!settings.allowPayWholeTable()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Whole table payment is disabled");
      }
      orders = orderRepo.findByTableIdOrderByCreatedAtDesc(s.tableId);
      if (orders.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No orders for this table");
      }
    }

    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> allItems = orderItemRepo.findByOrderIdIn(orderIds);

    // filter not closed and not already reserved by another bill request
    List<OrderItem> openItems = allItems.stream()
      .filter(oi -> !oi.isClosed && oi.billRequestId == null)
      .toList();
    if (openItems.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No unpaid items");
    }

    List<OrderItem> selected;
    if ("MY".equals(mode) || "WHOLE_TABLE".equals(mode)) {
      selected = openItems;
    } else {
      // SELECTED
      Map<Long, OrderItem> byId = new HashMap<>();
      for (OrderItem oi : openItems) byId.put(oi.id, oi);
      List<OrderItem> out = new ArrayList<>();
      for (Long id : req.orderItemIds) {
        OrderItem oi = byId.get(id);
        if (oi == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order item not available: " + id);
        }
        out.add(oi);
      }
      selected = out;
    }

    int subtotal = 0;
    List<BillItemLine> lines = new ArrayList<>();
    for (OrderItem oi : selected) {
      int lineTotal = oi.unitPriceCents * oi.qty;
      subtotal += lineTotal;
      lines.add(new BillItemLine(oi.id, oi.nameSnapshot, oi.qty, oi.unitPriceCents, lineTotal));
    }

    Integer tipsPercent = req.tipsPercent;
    int tipsAmount = 0;
    if (tipsPercent != null) {
      if (!settings.tipsEnabled()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tips are disabled");
      }
      if (!settings.tipsPercentages().contains(tipsPercent)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported tips percent");
      }
      tipsAmount = (int) Math.round(subtotal * (tipsPercent / 100.0));
    }

    BillRequest br = new BillRequest();
    br.tableId = s.tableId;
    br.guestSessionId = s.id;
    br.partyId = s.partyId;
    br.mode = mode;
    br.paymentMethod = pay;
    br.subtotalCents = subtotal;
    br.tipsPercent = tipsPercent;
    br.tipsAmountCents = tipsAmount;
    br.totalCents = subtotal + tipsAmount;

    br = billRequestRepo.save(br);

    for (OrderItem oi : selected) {
      BillRequestItem bri = new BillRequestItem();
      bri.billRequestId = br.id;
      bri.orderItemId = oi.id;
      bri.lineTotalCents = oi.unitPriceCents * oi.qty;
      billRequestItemRepo.save(bri);

      // mark reserved to this bill request (but not closed yet)
      oi.billRequestId = br.id;
      orderItemRepo.save(oi);
    }

    return new BillRequestResponse(br.id, br.status, br.paymentMethod, br.mode, br.subtotalCents, br.tipsPercent, br.tipsAmountCents, br.totalCents, lines);
  }

  @GetMapping("/bill-request/{id}")
  public BillRequestResponse getBillRequest(@PathVariable("id") long id) {
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BillRequest not found"));
    List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
    List<BillItemLine> lines = new ArrayList<>();
    for (BillRequestItem it : items) {
      OrderItem oi = orderItemRepo.findById(it.orderItemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item missing: " + it.orderItemId));
      lines.add(new BillItemLine(oi.id, oi.nameSnapshot, oi.qty, oi.unitPriceCents, it.lineTotalCents));
    }
    return new BillRequestResponse(br.id, br.status, br.paymentMethod, br.mode, br.subtotalCents, br.tipsPercent, br.tipsAmountCents, br.totalCents, lines);
  }

}
