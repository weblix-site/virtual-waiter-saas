package md.virtualwaiter.api.public_;

import md.virtualwaiter.domain.BillRequest;
import md.virtualwaiter.domain.BillRequestItem;
import md.virtualwaiter.domain.BranchReview;
import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.ChatMessage;
import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.MenuCategory;
import md.virtualwaiter.domain.MenuItem;
import md.virtualwaiter.domain.MenuItemModifierGroup;
import md.virtualwaiter.domain.ModifierGroup;
import md.virtualwaiter.domain.ModifierOption;
import md.virtualwaiter.domain.Order;
import md.virtualwaiter.domain.OrderItem;
import md.virtualwaiter.domain.StaffReview;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.TableParty;
import md.virtualwaiter.domain.WaiterCall;
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
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.ChatMessageRepo;
import md.virtualwaiter.repo.BranchReviewRepo;
import md.virtualwaiter.repo.StaffReviewRepo;
import md.virtualwaiter.otp.OtpService;
import md.virtualwaiter.service.BranchSettingsService;
import md.virtualwaiter.service.PartyService;
import md.virtualwaiter.service.NotificationEventService;
import md.virtualwaiter.service.RateLimitService;
import md.virtualwaiter.config.BillProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;

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
  private final StaffUserRepo staffUserRepo;
  private final StaffReviewRepo staffReviewRepo;
  private final ChatMessageRepo chatMessageRepo;
  private final BranchReviewRepo branchReviewRepo;
  private final QrSignatureService qrSig;
  private final OtpService otpService;
  private final BranchSettingsService settingsService;
  private final NotificationEventService notificationEventService;
  private final PartyService partyService;
  private final RateLimitService rateLimitService;
  private final int otpLimitMax;
  private final int otpLimitWindowSeconds;
  private final int otpVerifyLimitMax;
  private final int otpVerifyLimitWindowSeconds;
  private final int orderLimitMax;
  private final int orderLimitWindowSeconds;
  private final int partyLimitMax;
  private final int partyLimitWindowSeconds;
  private final int waiterCallLimitMax;
  private final int waiterCallLimitWindowSeconds;
  private final int sessionStartLimitMax;
  private final int sessionStartLimitWindowSeconds;
  private final int menuLimitMax;
  private final int menuLimitWindowSeconds;
  private final int chatLimitMax;
  private final int chatLimitWindowSeconds;
  private final BillProperties billProperties;

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
    StaffUserRepo staffUserRepo,
    StaffReviewRepo staffReviewRepo,
    ChatMessageRepo chatMessageRepo,
    BranchReviewRepo branchReviewRepo,
    QrSignatureService qrSig,
    OtpService otpService,
    BranchSettingsService settingsService,
    NotificationEventService notificationEventService,
    PartyService partyService,
    RateLimitService rateLimitService,
    BillProperties billProperties,
    @Value("${app.rateLimit.otp.maxRequests:5}") int otpLimitMax,
    @Value("${app.rateLimit.otp.windowSeconds:300}") int otpLimitWindowSeconds,
    @Value("${app.rateLimit.otpVerify.maxRequests:8}") int otpVerifyLimitMax,
    @Value("${app.rateLimit.otpVerify.windowSeconds:300}") int otpVerifyLimitWindowSeconds,
    @Value("${app.rateLimit.order.maxRequests:10}") int orderLimitMax,
    @Value("${app.rateLimit.order.windowSeconds:60}") int orderLimitWindowSeconds,
    @Value("${app.rateLimit.party.maxRequests:10}") int partyLimitMax,
    @Value("${app.rateLimit.party.windowSeconds:60}") int partyLimitWindowSeconds,
    @Value("${app.rateLimit.waiterCall.maxRequests:10}") int waiterCallLimitMax,
    @Value("${app.rateLimit.waiterCall.windowSeconds:60}") int waiterCallLimitWindowSeconds,
    @Value("${app.rateLimit.sessionStart.maxRequests:30}") int sessionStartLimitMax,
    @Value("${app.rateLimit.sessionStart.windowSeconds:60}") int sessionStartLimitWindowSeconds,
    @Value("${app.rateLimit.menu.maxRequests:60}") int menuLimitMax,
    @Value("${app.rateLimit.menu.windowSeconds:60}") int menuLimitWindowSeconds,
    @Value("${app.rateLimit.chat.maxRequests:15}") int chatLimitMax,
    @Value("${app.rateLimit.chat.windowSeconds:60}") int chatLimitWindowSeconds
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
    this.staffUserRepo = staffUserRepo;
    this.staffReviewRepo = staffReviewRepo;
    this.chatMessageRepo = chatMessageRepo;
    this.branchReviewRepo = branchReviewRepo;
    this.qrSig = qrSig;
    this.otpService = otpService;
    this.settingsService = settingsService;
    this.notificationEventService = notificationEventService;
    this.partyService = partyService;
    this.rateLimitService = rateLimitService;
    this.otpLimitMax = otpLimitMax;
    this.otpLimitWindowSeconds = otpLimitWindowSeconds;
    this.otpVerifyLimitMax = otpVerifyLimitMax;
    this.otpVerifyLimitWindowSeconds = otpVerifyLimitWindowSeconds;
    this.orderLimitMax = orderLimitMax;
    this.orderLimitWindowSeconds = orderLimitWindowSeconds;
    this.partyLimitMax = partyLimitMax;
    this.partyLimitWindowSeconds = partyLimitWindowSeconds;
    this.waiterCallLimitMax = waiterCallLimitMax;
    this.waiterCallLimitWindowSeconds = waiterCallLimitWindowSeconds;
    this.sessionStartLimitMax = sessionStartLimitMax;
    this.sessionStartLimitWindowSeconds = sessionStartLimitWindowSeconds;
    this.menuLimitMax = menuLimitMax;
    this.menuLimitWindowSeconds = menuLimitWindowSeconds;
    this.chatLimitMax = chatLimitMax;
    this.chatLimitWindowSeconds = chatLimitWindowSeconds;
    this.billProperties = billProperties;
  }

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

  public record StartSessionRequest(@NotBlank String tablePublicId, @NotBlank String sig, @NotNull Long ts, @NotBlank String locale) {}
  public record StartSessionResponse(
    long guestSessionId,
    long tableId,
    int tableNumber,
    long branchId,
    String locale,
    boolean otpRequired,
    boolean isVerified,
    String sessionSecret,
    String currencyCode,
    String waiterName,
    String waiterPhotoUrl,
    Integer waiterRating,
    Boolean waiterRecommended,
    Integer waiterExperienceYears,
    List<String> waiterFavoriteItems,
    Double waiterAvgRating,
    Long waiterReviewsCount
  ) {}

  @PostMapping("/session/start")
  public StartSessionResponse startSession(@Valid @RequestBody StartSessionRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("sessionStart:" + clientIp, sessionStartLimitMax, sessionStartLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many session starts from IP");
    }
    if (!qrSig.verifyTablePublicId(req.tablePublicId(), req.sig(), req.ts())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid QR signature");
    }

    CafeTable table = tableRepo.findByPublicId(req.tablePublicId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));

    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    String locale = normalizeLocale(req.locale());
    if ("auto".equalsIgnoreCase(req.locale())) {
      locale = normalizeLocale(settings.defaultLang());
    }
    GuestSession s = new GuestSession();
    s.tableId = table.id;
    s.locale = locale;
    s.expiresAt = Instant.now().plus(12, ChronoUnit.HOURS);
    s.sessionSecret = java.util.UUID.randomUUID().toString().replace("-", "");
    s.createdByIp = clientIp;
    s.createdByUa = getUserAgent(httpReq);
    s = sessionRepo.save(s);

    String waiterName = null;
    String waiterPhoto = null;
    Integer waiterRating = null;
    Boolean waiterRecommended = null;
    Integer waiterExperienceYears = null;
    List<String> waiterFavoriteItems = List.of();
    Double waiterAvgRating = null;
    Long waiterReviewsCount = null;
    if (table.assignedWaiterId != null) {
      StaffUser waiter = staffUserRepo.findById(table.assignedWaiterId).orElse(null);
      if (waiter != null) {
        String first = waiter.firstName != null && !waiter.firstName.isBlank() ? waiter.firstName.trim() : null;
        waiterName = first != null ? first : waiter.username;
        waiterPhoto = waiter.photoUrl;
        waiterRating = waiter.rating;
        waiterRecommended = waiter.recommended;
        waiterExperienceYears = waiter.experienceYears;
        waiterFavoriteItems = parseFavoriteItems(waiter.favoriteItems);
        waiterAvgRating = staffReviewRepo.averageRating(waiter.id);
        waiterReviewsCount = staffReviewRepo.countByStaffUserId(waiter.id);
      }
    }
    return new StartSessionResponse(
      s.id,
      table.id,
      table.number,
      table.branchId,
      s.locale,
      settings.requireOtpForFirstOrder(),
      s.isVerified,
      s.sessionSecret,
      settings.currencyCode(),
      waiterName,
      waiterPhoto,
      waiterRating,
      waiterRecommended,
      waiterExperienceYears,
      waiterFavoriteItems,
      waiterAvgRating,
      waiterReviewsCount
    );
  }

  public record WaiterReviewRequest(@NotNull Long guestSessionId, @NotNull Integer rating, String comment) {}
  public record WaiterReviewResponse(boolean created) {}

  @PostMapping("/waiter-review")
  public WaiterReviewResponse createWaiterReview(
    @Valid @RequestBody WaiterReviewRequest req,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(req.guestSessionId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
    requireSessionSecret(s, httpReq);
    if (staffReviewRepo.findByGuestSessionId(s.id).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists");
    }
    if (req.rating() < 1 || req.rating() > 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be 1..5");
    }
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (table.assignedWaiterId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waiter not assigned");
    }
    StaffReview r = new StaffReview();
    r.branchId = table.branchId;
    r.tableId = table.id;
    r.staffUserId = table.assignedWaiterId;
    r.guestSessionId = s.id;
    r.rating = req.rating();
    String comment = req.comment() != null ? req.comment().trim() : null;
    if (comment != null && comment.length() > 500) {
      comment = comment.substring(0, 500);
    }
    r.comment = (comment == null || comment.isBlank()) ? null : comment;
    staffReviewRepo.save(r);
    return new WaiterReviewResponse(true);
  }

  public record ChatSendRequest(@NotNull Long guestSessionId, @NotBlank String message) {}
  public record ChatMessageDto(long id, String senderRole, String message, String createdAt) {}

  @PostMapping("/chat/send")
  public void sendChatMessage(@Valid @RequestBody ChatSendRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
    requireSessionSecret(s, httpReq);
    if (s.expiresAt != null && s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guest session expired");
    }
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("chat:" + clientIp, chatLimitMax, chatLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many chat messages");
    }
    String msg = req.message() == null ? "" : req.message().trim();
    if (msg.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message empty");
    }
    if (msg.length() > 500) {
      msg = msg.substring(0, 500);
    }
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    ChatMessage m = new ChatMessage();
    m.branchId = table.branchId;
    m.tableId = table.id;
    m.guestSessionId = s.id;
    m.senderRole = "GUEST";
    m.message = msg;
    chatMessageRepo.save(m);
  }

  @GetMapping("/chat/messages")
  public List<ChatMessageDto> listChatMessages(
    @RequestParam("guestSessionId") Long guestSessionId,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
    requireSessionSecret(s, httpReq);
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    List<ChatMessage> msgs = chatMessageRepo.findByBranchIdAndGuestSessionIdOrderByIdAsc(table.branchId, s.id);
    List<ChatMessageDto> out = new ArrayList<>();
    for (ChatMessage m : msgs) {
      out.add(new ChatMessageDto(
        m.id,
        m.senderRole,
        m.message,
        m.createdAt != null ? m.createdAt.toString() : null
      ));
    }
    return out;
  }

  public record BranchReviewRequest(@NotNull Long guestSessionId, @NotNull Integer rating, String comment) {}
  public record BranchReviewResponse(boolean created) {}

  @PostMapping("/branch-review")
  public BranchReviewResponse createBranchReview(
    @Valid @RequestBody BranchReviewRequest req,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(req.guestSessionId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Guest session not found"));
    requireSessionSecret(s, httpReq);
    if (branchReviewRepo.findByGuestSessionId(s.id).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Review already exists");
    }
    if (req.rating() < 1 || req.rating() > 5) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be 1..5");
    }
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchReview r = new BranchReview();
    r.branchId = table.branchId;
    r.guestSessionId = s.id;
    r.rating = req.rating();
    String comment = req.comment() != null ? req.comment().trim() : null;
    if (comment != null && comment.length() > 500) {
      comment = comment.substring(0, 500);
    }
    r.comment = (comment == null || comment.isBlank()) ? null : comment;
    branchReviewRepo.save(r);
    return new BranchReviewResponse(true);
  }

  private static List<String> parseFavoriteItems(String v) {
    if (v == null || v.isBlank()) return List.of();
    String[] parts = v.split(",");
    List<String> out = new ArrayList<>();
    for (String p : parts) {
      String t = p.trim();
      if (!t.isEmpty()) out.add(t);
    }
    return out;
  }

  // --- Menu ---
  public record MenuItemDto(
    long id,
    String name,
    String description,
    String ingredients,
    String allergens,
    String weight,
    Integer kcal,
    Integer proteinG,
    Integer fatG,
    Integer carbsG,
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

  public record MenuItemDetailResponse(
    long id,
    String name,
    String description,
    String ingredients,
    String allergens,
    String weight,
    Integer kcal,
    Integer proteinG,
    Integer fatG,
    Integer carbsG,
    List<String> photos,
    List<String> tags,
    int priceCents,
    String currency
  ) {}

  @GetMapping("/menu-item/{id}")
  public MenuItemDetailResponse getMenuItemDetail(
    @PathVariable("id") long id,
    @RequestParam("tablePublicId") String tablePublicId,
    @RequestParam("sig") String sig,
    @RequestParam("ts") Long ts,
    @RequestParam(value = "locale", required = false) String locale
  ) {
    if (!qrSig.verifyTablePublicId(tablePublicId, sig, ts)) {
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
    return new MenuItemDetailResponse(
      item.id,
      pick(loc, item.nameRu, item.nameRo, item.nameEn),
      pick(loc, item.descriptionRu, item.descriptionRo, item.descriptionEn),
      pick(loc, item.ingredientsRu, item.ingredientsRo, item.ingredientsEn),
      item.allergens,
      item.weight,
      item.kcal,
      item.proteinG,
      item.fatG,
      item.carbsG,
      splitCsv(item.photoUrls),
      splitCsv(item.tags),
      item.priceCents,
      item.currency
    );
  }

  @GetMapping("/menu-item/{id}/modifiers")
  public MenuItemModifiersResponse getMenuItemModifiers(
    @PathVariable("id") long id,
    @RequestParam("tablePublicId") String tablePublicId,
    @RequestParam("sig") String sig,
    @RequestParam("ts") Long ts,
    @RequestParam(value = "locale", required = false) String locale
  ) {
    if (!qrSig.verifyTablePublicId(tablePublicId, sig, ts)) {
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
    @RequestParam("ts") Long ts,
    @RequestParam(value = "locale", required = false) String locale,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("menu:" + clientIp, menuLimitMax, menuLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many menu requests from IP");
    }
    if (!qrSig.verifyTablePublicId(tablePublicId, sig, ts)) {
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
          it.kcal,
          it.proteinG,
          it.fatG,
          it.carbsG,
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
  public record OrderStatusResponse(long orderId, String status, String createdAt) {}
  public record OrderItemSummary(long id, String name, int qty, int unitPriceCents, String comment) {}
  public record OrderSummary(long orderId, String status, String createdAt, List<OrderItemSummary> items) {}

  @PostMapping("/orders")
  public CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("order:" + clientIp, orderLimitMax, orderLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many orders from IP");
    }
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    TableParty activeParty = partyService.getActivePartyOrNull(s, table.id, Instant.now());
    if (settings.requireOtpForFirstOrder() && !s.isVerified) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "OTP required before first order");
    }

    // Basic anti-spam: 1 order per 10 seconds per session
    Instant now = Instant.now();
    if (s.lastOrderAt != null && s.lastOrderAt.isAfter(now.minusSeconds(10))) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many orders");
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
    o.createdByIp = getClientIp(httpReq);
    o.createdByUa = getUserAgent(httpReq);
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

    s.lastOrderAt = now;
    sessionRepo.save(s);

    notificationEventService.emit(table.branchId, "ORDER_NEW", o.id);

    return new CreateOrderResponse(o.id, o.status);
  }

  @GetMapping("/orders/{id}")
  public OrderStatusResponse getOrderStatus(
    @PathVariable("id") long id,
    @RequestParam("guestSessionId") long guestSessionId,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    Order o = orderRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    if (!Objects.equals(o.guestSessionId, s.id)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Order does not belong to session");
    }
    return new OrderStatusResponse(o.id, o.status, o.createdAt.toString());
  }

  @GetMapping("/orders")
  public List<OrderSummary> listOrders(
    @RequestParam("guestSessionId") long guestSessionId,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    List<Order> orders = orderRepo.findByGuestSessionIdOrderByCreatedAtDesc(s.id);
    if (orders.isEmpty()) return List.of();
    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> items = orderItemRepo.findByOrderIdIn(orderIds);
    Map<Long, List<OrderItemSummary>> itemsByOrder = new HashMap<>();
    for (OrderItem it : items) {
      itemsByOrder.computeIfAbsent(it.orderId, k -> new ArrayList<>())
        .add(new OrderItemSummary(it.id, it.nameSnapshot, it.qty, it.unitPriceCents, it.comment));
    }
    List<OrderSummary> out = new ArrayList<>();
    for (Order o : orders) {
      out.add(new OrderSummary(o.id, o.status, o.createdAt.toString(), itemsByOrder.getOrDefault(o.id, List.of())));
    }
    return out;
  }


  // --- Party PIN (group table) ---
  public record CreatePartyRequest(@NotNull Long guestSessionId) {}
  public record CreatePartyResponse(long partyId, String pin, Instant expiresAt) {}

  @PostMapping("/party/create")
  public CreatePartyResponse createParty(@Valid @RequestBody CreatePartyRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("party:" + clientIp, partyLimitMax, partyLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many party requests from IP");
    }
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    TableParty activeParty = partyService.getActivePartyOrNull(s, table.id, Instant.now());
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
  public JoinPartyResponse joinParty(@Valid @RequestBody JoinPartyRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("party:" + clientIp, partyLimitMax, partyLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many party requests from IP");
    }
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }

    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    TableParty activeParty = partyService.getActivePartyOrNull(s, table.id, Instant.now());
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

  public record ClosePartyRequest(@NotNull Long guestSessionId) {}
  public record ClosePartyResponse(long partyId, String status) {}

  @PostMapping("/party/close")
  public ClosePartyResponse closeParty(@Valid @RequestBody ClosePartyRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    if (s.partyId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not in a party");
    }
    TableParty p = partyRepo.findById(s.partyId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Party not found"));
    if (!"ACTIVE".equals(p.status)) {
      return new ClosePartyResponse(p.id, p.status);
    }
    partyService.closeParty(p, Instant.now());
    return new ClosePartyResponse(p.id, p.status);
  }

  @PostMapping("/party/close-expired")
  public Map<String, Object> closeExpiredParties(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
    if (key == null || !key.equals(System.getenv("ADMIN_KEY"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin key required");
    }
    int closed = partyService.closeExpiredParties(Instant.now());
    return Map.of("closed", closed);
  }

  private static String generatePin4() {
    int v = new java.util.Random().nextInt(10000);
    return String.format(java.util.Locale.ROOT, "%04d", v);
  }


  // --- OTP (SMS verification) ---
  public record SendOtpRequest(@NotNull Long guestSessionId, @NotBlank String phoneE164, String locale) {}
  public record SendOtpResponse(long challengeId, int ttlSeconds, String devCode) {}

  @PostMapping("/otp/send")
  public SendOtpResponse sendOtp(@Valid @RequestBody SendOtpRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("otp:" + clientIp, otpLimitMax, otpLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP requests from IP");
    }
    var r = otpService.sendOtp(req.guestSessionId, req.phoneE164, normalizeLocale(req.locale));
    return new SendOtpResponse(r.challengeId(), r.ttlSeconds(), r.devCode());
  }

  public record VerifyOtpRequest(@NotNull Long guestSessionId, @NotNull Long challengeId, @NotBlank String code) {}
  public record VerifyOtpResponse(boolean verified) {}

  @PostMapping("/otp/verify")
  public VerifyOtpResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("otpVerify:" + clientIp, otpVerifyLimitMax, otpVerifyLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many OTP verify attempts from IP");
    }
    otpService.verifyOtp(req.guestSessionId, req.challengeId, req.code);
    return new VerifyOtpResponse(true);
  }


  // --- Waiter call ---
  public record WaiterCallRequest(@NotNull Long guestSessionId) {}
  public record WaiterCallResponse(long waiterCallId, String status) {}
  public record CancelWaiterCallResponse(long waiterCallId, String status) {}
  public record WaiterCallStatusResponse(long waiterCallId, String status) {}

  @PostMapping("/waiter-call")
  public WaiterCallResponse callWaiter(@Valid @RequestBody WaiterCallRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    String clientIp = getClientIp(httpReq);
    if (!rateLimitService.allow("waiterCall:" + clientIp, waiterCallLimitMax, waiterCallLimitWindowSeconds)) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many waiter calls from IP");
    }
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    Instant now = Instant.now();
    if (s.lastWaiterCallAt != null && s.lastWaiterCallAt.isAfter(now.minusSeconds(30))) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many calls");
    }
    WaiterCall wc = new WaiterCall();
    wc.tableId = s.tableId;
    wc.guestSessionId = s.id;
    wc.status = "NEW";
    wc.createdByIp = getClientIp(httpReq);
    wc.createdByUa = getUserAgent(httpReq);
    wc = waiterCallRepo.save(wc);
    s.lastWaiterCallAt = now;
    sessionRepo.save(s);
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    notificationEventService.emit(table.branchId, "WAITER_CALL", wc.id);
    return new WaiterCallResponse(wc.id, wc.status);
  }

  @PostMapping("/waiter-call/cancel")
  public CancelWaiterCallResponse cancelWaiterCall(@Valid @RequestBody WaiterCallRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    List<WaiterCall> calls = waiterCallRepo.findTop100ByTableIdInAndStatusNotOrderByCreatedAtDesc(
      List.of(s.tableId), "CLOSED"
    );
    WaiterCall latest = null;
    for (WaiterCall c : calls) {
      if (Objects.equals(c.guestSessionId, s.id) && !"CLOSED".equals(c.status)) {
        latest = c;
        break;
      }
    }
    if (latest == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active call");
    }
    latest.status = "CLOSED";
    waiterCallRepo.save(latest);
    return new CancelWaiterCallResponse(latest.id, latest.status);
  }

  @GetMapping("/waiter-call/latest")
  public WaiterCallStatusResponse latestWaiterCall(
    @RequestParam("guestSessionId") long guestSessionId,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    List<WaiterCall> calls = waiterCallRepo.findTop100ByTableIdInAndStatusNotOrderByCreatedAtDesc(
      List.of(s.tableId), "CLOSED"
    );
    for (WaiterCall c : calls) {
      if (Objects.equals(c.guestSessionId, s.id) && !"CLOSED".equals(c.status)) {
        return new WaiterCallStatusResponse(c.id, c.status);
      }
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active call");
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

  private static String getClientIp(jakarta.servlet.http.HttpServletRequest req) {
    String xf = req.getHeader("X-Forwarded-For");
    if (xf != null && !xf.isBlank()) {
      return xf.split(",")[0].trim();
    }
    return req.getRemoteAddr();
  }

  private static String getUserAgent(jakarta.servlet.http.HttpServletRequest req) {
    return req.getHeader("User-Agent");
  }

  private static void requireSessionSecret(GuestSession s, jakarta.servlet.http.HttpServletRequest req) {
    String header = req.getHeader("X-Session-Secret");
    if (s.sessionSecret == null || s.sessionSecret.isBlank()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session secret not set");
    }
    if (header == null || header.isBlank() || !s.sessionSecret.equals(header)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid session secret");
    }
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

  private Set<Long> resolvePartySessionIdsOrEmpty(GuestSession s, long tableId, BranchSettingsService.Resolved settings) {
    if (!(settings.allowPayOtherGuestsItems() || settings.allowPayWholeTable())) {
      return Set.of();
    }
    if (s.partyId == null) return Set.of();
    TableParty p = partyService.getActivePartyOrNull(s, tableId, Instant.now());
    if (p == null) return Set.of();
    List<GuestSession> sessions = sessionRepo.findByPartyId(p.id);
    Set<Long> ids = new HashSet<>();
    for (GuestSession gs : sessions) ids.add(gs.id);
    return ids;
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
    String partyStatus,
    Instant partyExpiresAt,
    List<Long> partyGuestSessionIds,
    int partyGuestCount,
    List<BillOptionsItem> myItems,
    List<BillOptionsItem> tableItems,
    boolean payCashEnabled,
    boolean payTerminalEnabled
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

    Set<Long> partySessionIds = resolvePartySessionIdsOrEmpty(s, table.id, settings);
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
      if (!partySessionIds.isEmpty() && !partySessionIds.contains(ownerSessionId)) {
        continue;
      }
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
      if ((settings.allowPayOtherGuestsItems() || settings.allowPayWholeTable()) && !partySessionIds.isEmpty()) {
        tableItems.add(dto);
      }
    }

    boolean partyActive = !partySessionIds.isEmpty();
    String partyStatus = null;
    Instant partyExpiresAt = null;
    List<Long> partyGuestSessionIds = List.of();
    int partyGuestCount = 0;
    TableParty activeParty = partyService.getActivePartyOrNull(s, table.id, Instant.now());
    if (activeParty != null) {
      partyStatus = activeParty.status;
      partyExpiresAt = activeParty.expiresAt;
      List<GuestSession> sessions = sessionRepo.findByPartyId(activeParty.id);
      partyGuestSessionIds = sessions.stream()
        .filter(gs -> gs.expiresAt.isAfter(Instant.now()))
        .map(gs -> gs.id)
        .toList();
      partyGuestCount = partyGuestSessionIds.size();
    }
    return new BillOptionsResponse(
      partyActive && settings.allowPayOtherGuestsItems(),
      partyActive && settings.allowPayWholeTable(),
      settings.tipsEnabled(),
      settings.tipsPercentages(),
      settings.enablePartyPin(),
      s.partyId,
      partyStatus,
      partyExpiresAt,
      partyGuestSessionIds,
      partyGuestCount,
      myItems,
      tableItems,
      settings.payCashEnabled(),
      settings.payTerminalEnabled()
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
  public BillRequestResponse createBillRequest(@Valid @RequestBody CreateBillRequest req, jakarta.servlet.http.HttpServletRequest httpReq) {
    GuestSession s = sessionRepo.findById(req.guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    if (s.expiresAt.isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.GONE, "Session expired");
    }
    Instant now = Instant.now();
    if (s.lastBillRequestAt != null && s.lastBillRequestAt.isAfter(now.minusSeconds(30))) {
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many bill requests");
    }
    CafeTable table = tableRepo.findById(s.tableId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    BranchSettingsService.Resolved settings = settingsService.resolveForBranch(table.branchId);
    TableParty activeParty = partyService.getActivePartyOrNull(s, table.id, Instant.now());

    String mode = req.mode == null ? "" : req.mode.trim().toUpperCase(Locale.ROOT);
    String pay = req.paymentMethod == null ? "" : req.paymentMethod.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("CASH","TERMINAL").contains(pay)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported paymentMethod");
    }
    if ("CASH".equals(pay) && !settings.payCashEnabled()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cash payment is disabled");
    }
    if ("TERMINAL".equals(pay) && !settings.payTerminalEnabled()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terminal payment is disabled");
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
        if (activeParty == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party required to pay other guests items");
        }
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
      if (activeParty == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Party required to pay whole table");
      }
      orders = orderRepo.findByTableIdOrderByCreatedAtDesc(s.tableId);
      if (orders.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No orders for this table");
      }
    }

    List<Long> orderIds = orders.stream().map(o -> o.id).toList();
    List<OrderItem> allItems = orderItemRepo.findByOrderIdIn(orderIds);

    Set<Long> partySessionIds = resolvePartySessionIdsOrEmpty(s, table.id, settings);

    // filter not closed and not already reserved by another bill request
    Map<Long, Long> orderToSession = new HashMap<>();
    for (Order o : orders) orderToSession.put(o.id, o.guestSessionId);

    List<OrderItem> openItems = allItems.stream()
      .filter(oi -> !oi.isClosed && oi.billRequestId == null)
      .filter(oi -> {
        if (partySessionIds.isEmpty()) return true;
        Long ownerSessionId = orderToSession.get(oi.orderId);
        return ownerSessionId != null && partySessionIds.contains(ownerSessionId);
      })
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
    br.createdByIp = getClientIp(httpReq);
    br.createdByUa = getUserAgent(httpReq);

    br = billRequestRepo.save(br);
    s.lastBillRequestAt = now;
    sessionRepo.save(s);
    notificationEventService.emit(table.branchId, "BILL_REQUEST", br.id);

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
  public BillRequestResponse getBillRequest(
    @PathVariable("id") long id,
    @RequestParam("guestSessionId") long guestSessionId,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BillRequest not found"));
    if (!Objects.equals(br.guestSessionId, s.id)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bill request does not belong to session");
    }
    expireBillIfNeeded(br);
    List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
    List<BillItemLine> lines = new ArrayList<>();
    for (BillRequestItem it : items) {
      OrderItem oi = orderItemRepo.findById(it.orderItemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item missing: " + it.orderItemId));
      lines.add(new BillItemLine(oi.id, oi.nameSnapshot, oi.qty, oi.unitPriceCents, it.lineTotalCents));
    }
    return new BillRequestResponse(br.id, br.status, br.paymentMethod, br.mode, br.subtotalCents, br.tipsPercent, br.tipsAmountCents, br.totalCents, lines);
  }

  @GetMapping("/bill-request/latest")
  public BillRequestResponse latestBillRequest(
    @RequestParam("guestSessionId") long guestSessionId,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    BillRequest br = billRequestRepo.findTopByGuestSessionIdOrderByCreatedAtDesc(s.id);
    if (br == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "BillRequest not found");
    }
    expireBillIfNeeded(br);
    List<BillRequestItem> items = billRequestItemRepo.findByBillRequestId(br.id);
    List<BillItemLine> lines = new ArrayList<>();
    for (BillRequestItem it : items) {
      OrderItem oi = orderItemRepo.findById(it.orderItemId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item missing: " + it.orderItemId));
      lines.add(new BillItemLine(oi.id, oi.nameSnapshot, oi.qty, oi.unitPriceCents, it.lineTotalCents));
    }
    return new BillRequestResponse(br.id, br.status, br.paymentMethod, br.mode, br.subtotalCents, br.tipsPercent, br.tipsAmountCents, br.totalCents, lines);
  }

  public record CancelBillRequestResponse(long billRequestId, String status) {}

  @PostMapping("/bill-request/{id}/cancel")
  public CancelBillRequestResponse cancelBillRequest(
    @PathVariable("id") long id,
    @RequestBody Map<String, Object> body,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    Object raw = body == null ? null : body.get("guestSessionId");
    if (!(raw instanceof Number)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guestSessionId required");
    }
    long guestSessionId = ((Number) raw).longValue();
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BillRequest not found"));
    if (!Objects.equals(br.guestSessionId, s.id)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bill request does not belong to session");
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

  public record CloseBillRequestResponse(long billRequestId, String status) {}

  @PostMapping("/bill-request/{id}/close")
  public CloseBillRequestResponse closeBillRequest(
    @PathVariable("id") long id,
    @RequestBody Map<String, Object> body,
    jakarta.servlet.http.HttpServletRequest httpReq
  ) {
    Object raw = body == null ? null : body.get("guestSessionId");
    if (!(raw instanceof Number)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guestSessionId required");
    }
    long guestSessionId = ((Number) raw).longValue();
    GuestSession s = sessionRepo.findById(guestSessionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
    requireSessionSecret(s, httpReq);
    BillRequest br = billRequestRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BillRequest not found"));
    if (!Objects.equals(br.guestSessionId, s.id)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bill request does not belong to session");
    }
    if (!"PAID_CONFIRMED".equals(br.status)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill request is not confirmed");
    }
    br.status = "CLOSED";
    billRequestRepo.save(br);
    return new CloseBillRequestResponse(br.id, br.status);
  }

}
