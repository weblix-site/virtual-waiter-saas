package md.virtualwaiter.api.admin;

import md.virtualwaiter.domain.AuditLog;
import md.virtualwaiter.domain.Branch;
import md.virtualwaiter.domain.BranchMenuItemOverride;
import md.virtualwaiter.domain.MenuTemplate;
import md.virtualwaiter.domain.BranchDiscount;
import md.virtualwaiter.domain.BranchHall;
import md.virtualwaiter.domain.BranchReview;
import md.virtualwaiter.domain.BranchSettings;
import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.ChatMessage;
import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.HallPlan;
import md.virtualwaiter.domain.HallPlanTemplate;
import md.virtualwaiter.domain.HallPlanVersion;
import md.virtualwaiter.domain.InventoryItem;
import md.virtualwaiter.domain.MenuCategory;
import md.virtualwaiter.domain.MenuItem;
import md.virtualwaiter.domain.MenuItemIngredient;
import md.virtualwaiter.domain.MenuItemModifierGroup;
import md.virtualwaiter.domain.ModifierGroup;
import md.virtualwaiter.domain.ModifierOption;
import md.virtualwaiter.domain.StaffReview;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.TableParty;
import md.virtualwaiter.domain.WaiterCall;
import md.virtualwaiter.domain.Order;
import md.virtualwaiter.repo.AuditLogRepo;
import md.virtualwaiter.repo.BranchHallRepo;
import md.virtualwaiter.repo.BranchMenuItemOverrideRepo;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.RestaurantRepo;
import md.virtualwaiter.repo.BranchDiscountRepo;
import md.virtualwaiter.repo.BranchReviewRepo;
import md.virtualwaiter.repo.BranchSettingsRepo;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.ChatMessageRepo;
import md.virtualwaiter.repo.CurrencyRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.GuestOfferRepo;
import md.virtualwaiter.repo.HallPlanRepo;
import md.virtualwaiter.repo.HallPlanTemplateRepo;
import md.virtualwaiter.repo.HallPlanVersionRepo;
import md.virtualwaiter.repo.InventoryItemRepo;
import md.virtualwaiter.repo.MenuCategoryRepo;
import md.virtualwaiter.repo.MenuItemIngredientRepo;
import md.virtualwaiter.repo.MenuItemModifierGroupRepo;
import md.virtualwaiter.repo.MenuTemplateRepo;
import md.virtualwaiter.repo.MenuItemRepo;
import md.virtualwaiter.repo.ModifierGroupRepo;
import md.virtualwaiter.repo.ModifierOptionRepo;
import md.virtualwaiter.repo.StaffReviewRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.TablePartyRepo;
import md.virtualwaiter.repo.WaiterCallRepo;
import md.virtualwaiter.repo.OrderRepo;
import md.virtualwaiter.security.QrSignatureService;
import md.virtualwaiter.service.BranchSettingsService;
import md.virtualwaiter.service.StatsService;
import md.virtualwaiter.service.AuditService;
import md.virtualwaiter.service.LoyaltyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private static final Logger LOG = LoggerFactory.getLogger(AdminController.class);
  private final StaffUserRepo staffUserRepo;
  private final StaffReviewRepo staffReviewRepo;
  private final ChatMessageRepo chatMessageRepo;
  private final BranchReviewRepo branchReviewRepo;
  private final MenuCategoryRepo categoryRepo;
  private final MenuItemRepo itemRepo;
  private final BranchMenuItemOverrideRepo menuItemOverrideRepo;
  private final CafeTableRepo tableRepo;
  private final BranchRepo branchRepo;
  private final MenuTemplateRepo menuTemplateRepo;
  private final RestaurantRepo restaurantRepo;
  private final BranchDiscountRepo branchDiscountRepo;
  private final BranchHallRepo hallRepo;
  private final HallPlanRepo hallPlanRepo;
  private final HallPlanVersionRepo hallPlanVersionRepo;
  private final BranchSettingsRepo settingsRepo;
  private final BranchSettingsService settingsService;
  private final QrSignatureService qrSig;
  private final PasswordEncoder passwordEncoder;
  private final String publicBaseUrl;
  private final StatsService statsService;
  private final ModifierGroupRepo modifierGroupRepo;
  private final ModifierOptionRepo modifierOptionRepo;
  private final MenuItemModifierGroupRepo menuItemModifierGroupRepo;
  private final HallPlanTemplateRepo hallPlanTemplateRepo;
  private final AuditService auditService;
  private final AuditLogRepo auditLogRepo;
  private final TablePartyRepo partyRepo;
  private final GuestSessionRepo guestSessionRepo;
  private final GuestOfferRepo guestOfferRepo;
  private final WaiterCallRepo waiterCallRepo;
  private final OrderRepo orderRepo;
  private final InventoryItemRepo inventoryItemRepo;
  private final MenuItemIngredientRepo ingredientRepo;
  private final CurrencyRepo currencyRepo;
  private final LoyaltyService loyaltyService;
  private final int maxPhotoUrlLength;
  private final int maxPhotoUrlsCount;
  private final Set<String> allowedPhotoExts;
  private final String mediaPublicBaseUrl;

  public AdminController(
    StaffUserRepo staffUserRepo,
    StaffReviewRepo staffReviewRepo,
    ChatMessageRepo chatMessageRepo,
    BranchReviewRepo branchReviewRepo,
    MenuCategoryRepo categoryRepo,
    MenuItemRepo itemRepo,
    BranchMenuItemOverrideRepo menuItemOverrideRepo,
    CafeTableRepo tableRepo,
    BranchRepo branchRepo,
    MenuTemplateRepo menuTemplateRepo,
    RestaurantRepo restaurantRepo,
    BranchDiscountRepo branchDiscountRepo,
    BranchHallRepo hallRepo,
    HallPlanRepo hallPlanRepo,
    HallPlanVersionRepo hallPlanVersionRepo,
    BranchSettingsRepo settingsRepo,
    BranchSettingsService settingsService,
    QrSignatureService qrSig,
    PasswordEncoder passwordEncoder,
    @Value("${app.publicBaseUrl:http://localhost:3000}") String publicBaseUrl,
    StatsService statsService,
    ModifierGroupRepo modifierGroupRepo,
    ModifierOptionRepo modifierOptionRepo,
    MenuItemModifierGroupRepo menuItemModifierGroupRepo,
    HallPlanTemplateRepo hallPlanTemplateRepo,
    AuditService auditService,
    AuditLogRepo auditLogRepo,
    TablePartyRepo partyRepo,
    GuestSessionRepo guestSessionRepo,
    GuestOfferRepo guestOfferRepo,
    WaiterCallRepo waiterCallRepo,
    OrderRepo orderRepo,
    InventoryItemRepo inventoryItemRepo,
    MenuItemIngredientRepo ingredientRepo,
    CurrencyRepo currencyRepo,
    LoyaltyService loyaltyService,
    @Value("${app.media.maxPhotoUrlLength:512}") int maxPhotoUrlLength,
    @Value("${app.media.maxPhotoUrlsCount:6}") int maxPhotoUrlsCount,
    @Value("${app.media.allowedPhotoExts:jpg,jpeg,png,webp,gif}") String allowedPhotoExts,
    @Value("${app.media.publicBaseUrl:http://localhost:8080}") String mediaPublicBaseUrl
  ) {
    this.staffUserRepo = staffUserRepo;
    this.staffReviewRepo = staffReviewRepo;
    this.chatMessageRepo = chatMessageRepo;
    this.branchReviewRepo = branchReviewRepo;
    this.categoryRepo = categoryRepo;
    this.itemRepo = itemRepo;
    this.menuItemOverrideRepo = menuItemOverrideRepo;
    this.tableRepo = tableRepo;
    this.branchRepo = branchRepo;
    this.menuTemplateRepo = menuTemplateRepo;
    this.restaurantRepo = restaurantRepo;
    this.branchDiscountRepo = branchDiscountRepo;
    this.hallRepo = hallRepo;
    this.hallPlanRepo = hallPlanRepo;
    this.hallPlanVersionRepo = hallPlanVersionRepo;
    this.settingsRepo = settingsRepo;
    this.settingsService = settingsService;
    this.qrSig = qrSig;
    this.passwordEncoder = passwordEncoder;
    this.publicBaseUrl = publicBaseUrl;
    this.statsService = statsService;
    this.modifierGroupRepo = modifierGroupRepo;
    this.modifierOptionRepo = modifierOptionRepo;
    this.menuItemModifierGroupRepo = menuItemModifierGroupRepo;
    this.hallPlanTemplateRepo = hallPlanTemplateRepo;
    this.auditService = auditService;
    this.auditLogRepo = auditLogRepo;
    this.partyRepo = partyRepo;
    this.guestSessionRepo = guestSessionRepo;
    this.guestOfferRepo = guestOfferRepo;
    this.waiterCallRepo = waiterCallRepo;
    this.orderRepo = orderRepo;
    this.inventoryItemRepo = inventoryItemRepo;
    this.ingredientRepo = ingredientRepo;
    this.currencyRepo = currencyRepo;
    this.loyaltyService = loyaltyService;
    this.maxPhotoUrlLength = maxPhotoUrlLength;
    this.maxPhotoUrlsCount = maxPhotoUrlsCount;
    this.allowedPhotoExts = parseExts(allowedPhotoExts);
    this.mediaPublicBaseUrl = trimTrailingSlash(mediaPublicBaseUrl);
  }

  private StaffUser current(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");
    }
    return staffUserRepo.findByUsername(auth.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
  }

  private StaffUser requireAdmin(Authentication auth) {
    StaffUser u = current(auth);
    String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
    if (!Set.of("ADMIN", "MANAGER", "SUPER_ADMIN", "OWNER").contains(role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
    }
    return u;
  }

  private boolean isSuperAdmin(StaffUser u) {
    return "SUPER_ADMIN".equalsIgnoreCase(u.role);
  }

  private void requireSuperAdmin(StaffUser u) {
    if (!isSuperAdmin(u)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super admin role required");
    }
  }

  private void requireBranchAccess(StaffUser u, Long resourceBranchId) {
    if (isSuperAdmin(u)) return;
    if (resourceBranchId == null || !resourceBranchId.equals(u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
  }

  private long resolveBranchId(StaffUser u, Long branchIdParam) {
    String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
    if ("SUPER_ADMIN".equals(role)) {
      if (branchIdParam == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId is required");
      }
      return branchIdParam;
    }
    if (u.branchId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User has no branch");
    }
    return u.branchId;
  }

  public record BranchInfoDto(
    long id,
    long tenantId,
    Long restaurantId,
    String restaurantName,
    String name,
    Long menuTemplateId,
    String menuTemplateName
  ) {}

  @GetMapping("/branch")
  public BranchInfoDto getBranch(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    String restaurantName = null;
    if (b.restaurantId != null) {
      restaurantName = restaurantRepo.findById(b.restaurantId).map(r -> r.name).orElse(null);
    }
    String menuTemplateName = null;
    if (b.menuTemplateId != null) {
      menuTemplateName = menuTemplateRepo.findById(b.menuTemplateId).map(t -> t.name).orElse(null);
    }
    return new BranchInfoDto(b.id, b.tenantId, b.restaurantId, restaurantName, b.name, b.menuTemplateId, menuTemplateName);
  }

  private Long resolveHallIdFromPlan(StaffUser u, Long hallId, Long planId) {
    if (planId == null) return hallId;
    HallPlan p = hallPlanRepo.findById(planId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    BranchHall h = hallRepo.findById(p.hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    if (hallId != null && !Objects.equals(hallId, p.hallId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hallId does not match planId");
    }
    return p.hallId;
  }

  private void validateLayoutBounds(Double x, Double y, Double w, Double h) {
    if (x != null && (x.isNaN() || x < 0 || x > 100)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layoutX out of range");
    }
    if (y != null && (y.isNaN() || y < 0 || y > 100)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layoutY out of range");
    }
    if (w != null && (w.isNaN() || w < 0 || w > 100)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layoutW out of range");
    }
    if (h != null && (h.isNaN() || h < 0 || h > 100)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layoutH out of range");
    }
    if (x != null && w != null && x + w > 100.0001) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layoutX + layoutW must be <= 100");
    }
    if (y != null && h != null && y + h > 100.0001) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "layoutY + layoutH must be <= 100");
    }
  }

  private static final int MAX_ZONES_JSON_LEN = 20000;
  private static final int MAX_ZONES_COUNT = 200;
  private static final Pattern COLOR_HEX = Pattern.compile("^#?[0-9a-fA-F]{6}$");

  private void validateZonesJson(String zonesJson) {
    if (zonesJson == null) return;
    if (zonesJson.length() > MAX_ZONES_JSON_LEN) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson too large");
    }
    String trimmed = zonesJson.trim();
    if (trimmed.isEmpty()) return;
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode root = mapper.readTree(trimmed);
      if (!root.isArray()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson must be array");
      }
      if (root.size() > MAX_ZONES_COUNT) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson too many zones");
      }
      for (JsonNode node : root) {
        if (!node.isObject()) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson invalid zone");
        }
        JsonNode id = node.get("id");
        if (id != null && id.isTextual() && id.asText().length() > 64) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson zone id too long");
        }
        JsonNode name = node.get("name");
        if (name != null && name.isTextual() && name.asText().length() > 64) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson zone name too long");
        }
        Double x = node.has("x") ? node.get("x").asDouble() : null;
        Double y = node.has("y") ? node.get("y").asDouble() : null;
        Double w = node.has("w") ? node.get("w").asDouble() : null;
        Double h = node.has("h") ? node.get("h").asDouble() : null;
        if (w != null && w <= 0) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson width must be > 0");
        }
        if (h != null && h <= 0) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson height must be > 0");
        }
        validateLayoutBounds(x, y, w, h);
        JsonNode color = node.get("color");
        if (color != null && color.isTextual() && !color.asText().isBlank()) {
          if (!COLOR_HEX.matcher(color.asText().trim()).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson color invalid");
          }
        }
      }
    } catch (ResponseStatusException e) {
      throw e;
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "zonesJson invalid");
    }
  }

  public record MeResponse(long id, String username, String role, Long branchId) {}

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    StaffUser u = requireAdmin(auth);
    return new MeResponse(u.id, u.username, u.role, u.branchId);
  }

  public record BranchLayoutResponse(String backgroundUrl, String zonesJson) {}
  public record UpdateBranchLayoutRequest(String backgroundUrl, String zonesJson) {}

  @GetMapping("/branch-layout")
  public BranchLayoutResponse getBranchLayout(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    return new BranchLayoutResponse(b.layoutBgUrl, b.layoutZonesJson);
  }

  @PutMapping("/branch-layout")
  public BranchLayoutResponse updateBranchLayout(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestBody UpdateBranchLayoutRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    if (req != null) {
      validateZonesJson(req.zonesJson);
      b.layoutBgUrl = req.backgroundUrl;
      b.layoutZonesJson = req.zonesJson;
    }
    branchRepo.save(b);
    auditService.log(u, "UPDATE", "BranchLayout", b.id, null);
    return new BranchLayoutResponse(b.layoutBgUrl, b.layoutZonesJson);
  }

  // --- Plan signals (operator mode) ---
  public record PlanSignalRow(
    long tableId,
    boolean waiterCallActive,
    String waiterCallStatus,
    String waiterCallCreatedAt,
    String orderStatus,
    String orderCreatedAt
  ) {}

  @GetMapping("/plan-signals")
  public List<PlanSignalRow> getPlanSignals(
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<CafeTable> tables = tableRepo.findByBranchId(bid);
    if (hallId != null) {
      tables = tables.stream().filter(t -> Objects.equals(t.hallId, hallId)).toList();
    }
    if (tables.isEmpty()) return List.of();
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();

    Map<Long, WaiterCall> callByTable = new HashMap<>();
    List<WaiterCall> calls = waiterCallRepo.findTop100ByTableIdInAndStatusNotOrderByCreatedAtDesc(tableIds, "CLOSED");
    for (WaiterCall c : calls) {
      callByTable.putIfAbsent(c.tableId, c);
    }

    Map<Long, Order> orderByTable = new HashMap<>();
    List<Order> orders = orderRepo.findTop100ByTableIdInAndStatusNotInOrderByCreatedAtDesc(
      tableIds,
      List.of("CLOSED", "CANCELLED", "SERVED")
    );
    for (Order o : orders) {
      orderByTable.putIfAbsent(o.tableId, o);
    }

    List<PlanSignalRow> out = new ArrayList<>();
    for (CafeTable t : tables) {
      WaiterCall c = callByTable.get(t.id);
      Order o = orderByTable.get(t.id);
      out.add(new PlanSignalRow(
        t.id,
        c != null,
        c == null ? null : c.status,
        c == null || c.createdAt == null ? null : c.createdAt.toString(),
        o == null ? null : o.status,
        o == null || o.createdAt == null ? null : o.createdAt.toString()
      ));
    }
    return out;
  }

  // --- Halls ---
  public record HallDto(long id, long branchId, String name, boolean isActive, int sortOrder, String backgroundUrl, String zonesJson, Long activePlanId) {}
  public record CreateHallRequest(@NotBlank String name, Integer sortOrder) {}
  public record UpdateHallRequest(String name, Boolean isActive, Integer sortOrder, String backgroundUrl, String zonesJson, Long activePlanId) {}

  @GetMapping("/halls")
  public List<HallDto> listHalls(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<BranchHall> halls = hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid);
    List<HallDto> out = new ArrayList<>();
    for (BranchHall h : halls) {
      out.add(new HallDto(h.id, h.branchId, h.name, h.isActive, h.sortOrder, h.layoutBgUrl, h.layoutZonesJson, h.activePlanId));
    }
    return out;
  }

  @GetMapping("/halls/{id}")
  public HallDto getHall(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    return new HallDto(h.id, h.branchId, h.name, h.isActive, h.sortOrder, h.layoutBgUrl, h.layoutZonesJson, h.activePlanId);
  }

  @PostMapping("/halls")
  public HallDto createHall(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody CreateHallRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    BranchHall h = new BranchHall();
    h.branchId = bid;
    h.name = req.name.trim();
    h.sortOrder = req.sortOrder == null ? 0 : req.sortOrder;
    h = hallRepo.save(h);
    auditService.log(u, "CREATE", "BranchHall", h.id, null);
    return new HallDto(h.id, h.branchId, h.name, h.isActive, h.sortOrder, h.layoutBgUrl, h.layoutZonesJson, h.activePlanId);
  }

  @PatchMapping("/halls/{id}")
  public HallDto updateHall(@PathVariable long id, @RequestBody UpdateHallRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    if (req.name != null && !req.name.isBlank()) h.name = req.name.trim();
    if (req.isActive != null) h.isActive = req.isActive;
    if (req.sortOrder != null) h.sortOrder = req.sortOrder;
    if (req.backgroundUrl != null) h.layoutBgUrl = req.backgroundUrl;
    if (req.zonesJson != null) validateZonesJson(req.zonesJson);
    if (req.zonesJson != null) h.layoutZonesJson = req.zonesJson;
    if (req.activePlanId != null) h.activePlanId = req.activePlanId;
    h = hallRepo.save(h);
    auditService.log(u, "UPDATE", "BranchHall", h.id, null);
    return new HallDto(h.id, h.branchId, h.name, h.isActive, h.sortOrder, h.layoutBgUrl, h.layoutZonesJson, h.activePlanId);
  }

  @DeleteMapping("/halls/{id}")
  public void deleteHall(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    long tableCount = tableRepo.countByHallId(h.id);
    if (tableCount > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Hall has tables assigned");
    }
    hallPlanTemplateRepo.deleteByHallId(h.id);
    hallPlanRepo.deleteByHallId(h.id);
    hallRepo.delete(h);
    auditService.log(u, "DELETE", "BranchHall", h.id, null);
  }

  // --- Hall plans ---
  public record HallPlanDto(long id, long hallId, String name, boolean isActive, int sortOrder, String backgroundUrl, String zonesJson) {}
  public record CreateHallPlanRequest(@NotBlank String name, Integer sortOrder) {}
  public record UpdateHallPlanRequest(String name, Boolean isActive, Integer sortOrder, String backgroundUrl, String zonesJson) {}
  public record DuplicateHallPlanRequest(String name) {}
  public record PlanExportTable(String publicId, int number, Double layoutX, Double layoutY, Double layoutW, Double layoutH, String layoutShape, Integer layoutRotation, String layoutZone) {}
  public record PlanExportResponse(String name, long hallId, String backgroundUrl, String zonesJson, List<PlanExportTable> tables) {}
  public record PlanImportRequest(String name, String backgroundUrl, String zonesJson, List<PlanExportTable> tables, Boolean applyLayouts, Boolean applyTables) {}
  public record HallPlanVersionDto(long id, long planId, String name, boolean isActive, int sortOrder, String backgroundUrl, String zonesJson, String createdAt, Long createdByStaffId, String action) {}
  public record HallPlanTemplateDto(long id, long hallId, String name, String payloadJson, String createdAt, String updatedAt) {}
  public record CreateHallPlanTemplateRequest(@NotBlank String name, @NotBlank String payloadJson) {}

  @GetMapping("/halls/{hallId}/plans")
  public List<HallPlanDto> listHallPlans(@PathVariable long hallId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    List<HallPlan> plans = hallPlanRepo.findByHallIdOrderBySortOrderAscIdAsc(hallId);
    List<HallPlanDto> out = new ArrayList<>();
    for (HallPlan p : plans) {
      out.add(new HallPlanDto(p.id, p.hallId, p.name, p.isActive, p.sortOrder, p.layoutBgUrl, p.layoutZonesJson));
    }
    return out;
  }

  @PostMapping("/halls/{hallId}/plans")
  public HallPlanDto createHallPlan(@PathVariable long hallId, @Valid @RequestBody CreateHallPlanRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    HallPlan p = new HallPlan();
    p.hallId = hallId;
    p.name = req.name.trim();
    p.sortOrder = req.sortOrder == null ? 0 : req.sortOrder;
    p = hallPlanRepo.save(p);
    savePlanVersion(p, u, "CREATE");
    if (h.activePlanId == null) {
      h.activePlanId = p.id;
      hallRepo.save(h);
    }
    auditService.log(u, "CREATE", "HallPlan", p.id, null);
    return new HallPlanDto(p.id, p.hallId, p.name, p.isActive, p.sortOrder, p.layoutBgUrl, p.layoutZonesJson);
  }

  @PatchMapping("/hall-plans/{id}")
  public HallPlanDto updateHallPlan(@PathVariable long id, @RequestBody UpdateHallPlanRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    HallPlan p = hallPlanRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    BranchHall h = hallRepo.findById(p.hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    if (req.name != null && !req.name.isBlank()) p.name = req.name.trim();
    if (req.isActive != null) p.isActive = req.isActive;
    if (req.sortOrder != null) p.sortOrder = req.sortOrder;
    if (req.backgroundUrl != null) p.layoutBgUrl = req.backgroundUrl;
    if (req.zonesJson != null) validateZonesJson(req.zonesJson);
    if (req.zonesJson != null) p.layoutZonesJson = req.zonesJson;
    p = hallPlanRepo.save(p);
    savePlanVersion(p, u, "UPDATE");
    auditService.log(u, "UPDATE", "HallPlan", p.id, null);
    return new HallPlanDto(p.id, p.hallId, p.name, p.isActive, p.sortOrder, p.layoutBgUrl, p.layoutZonesJson);
  }

  @GetMapping("/hall-plans/{id}/export")
  public PlanExportResponse exportHallPlan(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    HallPlan p = hallPlanRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    BranchHall h = hallRepo.findById(p.hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    List<CafeTable> tables = tableRepo.findByBranchId(h.branchId).stream()
      .filter(t -> Objects.equals(t.hallId, h.id))
      .toList();
    List<PlanExportTable> out = new ArrayList<>();
    for (CafeTable t : tables) {
      out.add(new PlanExportTable(
        t.publicId,
        t.number,
        t.layoutX,
        t.layoutY,
        t.layoutW,
        t.layoutH,
        t.layoutShape,
        t.layoutRotation,
        t.layoutZone
      ));
    }
    return new PlanExportResponse(p.name, h.id, p.layoutBgUrl, p.layoutZonesJson, out);
  }

  @PostMapping("/halls/{hallId}/plans/import")
  public HallPlanDto importHallPlan(@PathVariable long hallId, @RequestBody PlanImportRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    if (req == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body required");
    String name = (req.name == null || req.name.isBlank()) ? "Imported" : req.name.trim();
    validateZonesJson(req.zonesJson);
    HallPlan p = new HallPlan();
    p.hallId = hallId;
    p.name = name;
    p.layoutBgUrl = req.backgroundUrl;
    p.layoutZonesJson = req.zonesJson;
    p.sortOrder = 0;
    p = hallPlanRepo.save(p);
    savePlanVersion(p, u, "IMPORT");

    boolean applyLayouts = req.applyLayouts == null || req.applyLayouts;
    boolean applyTables = req.applyTables == null || req.applyTables;
    if (applyLayouts && applyTables && req.tables != null && !req.tables.isEmpty()) {
      List<CafeTable> tables = tableRepo.findByBranchId(h.branchId).stream()
        .filter(t -> Objects.equals(t.hallId, h.id))
        .toList();
      Map<String, CafeTable> byPublicId = new HashMap<>();
      Map<Integer, CafeTable> byNumber = new HashMap<>();
      for (CafeTable t : tables) {
        if (t.publicId != null) byPublicId.put(t.publicId, t);
        byNumber.put(t.number, t);
      }
      for (PlanExportTable it : req.tables) {
        CafeTable t = null;
        if (it.publicId() != null) t = byPublicId.get(it.publicId());
        if (t == null) t = byNumber.get(it.number());
        if (t == null) continue;
        if (it.layoutX() != null || it.layoutY() != null || it.layoutW() != null || it.layoutH() != null) {
          Double nx = it.layoutX() != null ? it.layoutX() : t.layoutX;
          Double ny = it.layoutY() != null ? it.layoutY() : t.layoutY;
          Double nw = it.layoutW() != null ? it.layoutW() : t.layoutW;
          Double nh = it.layoutH() != null ? it.layoutH() : t.layoutH;
          validateLayoutBounds(nx, ny, nw, nh);
        }
        if (it.layoutX() != null) t.layoutX = it.layoutX();
        if (it.layoutY() != null) t.layoutY = it.layoutY();
        if (it.layoutW() != null) t.layoutW = it.layoutW();
        if (it.layoutH() != null) t.layoutH = it.layoutH();
        if (it.layoutShape() != null) t.layoutShape = it.layoutShape();
        if (it.layoutRotation() != null) t.layoutRotation = it.layoutRotation();
        if (it.layoutZone() != null) t.layoutZone = it.layoutZone();
        tableRepo.save(t);
      }
    }
    auditService.log(u, "IMPORT", "HallPlan", p.id, null);
    return new HallPlanDto(p.id, p.hallId, p.name, p.isActive, p.sortOrder, p.layoutBgUrl, p.layoutZonesJson);
  }

  @GetMapping("/halls/{hallId}/plan-templates")
  public List<HallPlanTemplateDto> listPlanTemplates(@PathVariable long hallId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    List<HallPlanTemplate> templates = hallPlanTemplateRepo.findByBranchIdAndHallIdOrderByUpdatedAtDesc(h.branchId, hallId);
    List<HallPlanTemplateDto> out = new ArrayList<>();
    for (HallPlanTemplate t : templates) {
      out.add(new HallPlanTemplateDto(t.id, t.hallId, t.name, t.payloadJson, t.createdAt.toString(), t.updatedAt.toString()));
    }
    return out;
  }

  @PostMapping("/halls/{hallId}/plan-templates")
  public HallPlanTemplateDto createPlanTemplate(
    @PathVariable long hallId,
    @Valid @RequestBody CreateHallPlanTemplateRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    BranchHall h = hallRepo.findById(hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    String name = req.name.trim();
    HallPlanTemplate existing = hallPlanTemplateRepo.findTopByBranchIdAndHallIdAndNameIgnoreCase(h.branchId, hallId, name);
    HallPlanTemplate t = existing == null ? new HallPlanTemplate() : existing;
    if (existing == null) {
      t.branchId = h.branchId;
      t.hallId = hallId;
      t.name = name;
      t.createdAt = Instant.now();
    }
    t.payloadJson = req.payloadJson;
    t.updatedAt = Instant.now();
    t = hallPlanTemplateRepo.save(t);
    auditService.log(u, existing == null ? "CREATE" : "UPDATE", "HallPlanTemplate", t.id, null);
    return new HallPlanTemplateDto(t.id, t.hallId, t.name, t.payloadJson, t.createdAt.toString(), t.updatedAt.toString());
  }

  @DeleteMapping("/hall-plan-templates/{id}")
  public void deletePlanTemplate(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    HallPlanTemplate t = hallPlanTemplateRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
    requireBranchAccess(u, t.branchId);
    hallPlanTemplateRepo.delete(t);
    auditService.log(u, "DELETE", "HallPlanTemplate", t.id, null);
  }

  @DeleteMapping("/hall-plans/{id}")
  public void deleteHallPlan(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    HallPlan p = hallPlanRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    BranchHall h = hallRepo.findById(p.hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    hallPlanRepo.delete(p);
    if (Objects.equals(h.activePlanId, id)) {
      h.activePlanId = null;
      hallRepo.save(h);
    }
    auditService.log(u, "DELETE", "HallPlan", p.id, null);
  }

  @PostMapping("/hall-plans/{id}/duplicate")
  public HallPlanDto duplicateHallPlan(@PathVariable long id, @RequestBody DuplicateHallPlanRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    HallPlan src = hallPlanRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    BranchHall h = hallRepo.findById(src.hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    HallPlan copy = new HallPlan();
    copy.hallId = src.hallId;
    String suffix = " Copy";
    copy.name = (req != null && req.name != null && !req.name.isBlank()) ? req.name.trim() : (src.name + suffix);
    copy.sortOrder = src.sortOrder + 1;
    copy.isActive = src.isActive;
    copy.layoutBgUrl = src.layoutBgUrl;
    copy.layoutZonesJson = src.layoutZonesJson;
    copy = hallPlanRepo.save(copy);
    savePlanVersion(copy, u, "DUPLICATE");
    auditService.log(u, "CREATE", "HallPlan", copy.id, "duplicateFrom:" + src.id);
    return new HallPlanDto(copy.id, copy.hallId, copy.name, copy.isActive, copy.sortOrder, copy.layoutBgUrl, copy.layoutZonesJson);
  }

  @GetMapping("/hall-plans/{id}/versions")
  public List<HallPlanVersionDto> listHallPlanVersions(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    HallPlan p = hallPlanRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    BranchHall h = hallRepo.findById(p.hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    List<HallPlanVersion> versions = hallPlanVersionRepo.findByPlanIdOrderByCreatedAtDesc(id);
    List<HallPlanVersionDto> out = new ArrayList<>();
    for (HallPlanVersion v : versions) {
      out.add(new HallPlanVersionDto(
        v.id,
        v.planId,
        v.name,
        v.isActive,
        v.sortOrder,
        v.layoutBgUrl,
        v.layoutZonesJson,
        v.createdAt == null ? null : v.createdAt.toString(),
        v.createdByStaffId,
        v.action
      ));
    }
    return out;
  }

  @PostMapping("/hall-plans/{id}/versions/{versionId}/restore")
  public HallPlanDto restoreHallPlanVersion(@PathVariable long id, @PathVariable long versionId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    HallPlan p = hallPlanRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
    BranchHall h = hallRepo.findById(p.hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    requireBranchAccess(u, h.branchId);
    HallPlanVersion v = hallPlanVersionRepo.findById(versionId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));
    if (!Objects.equals(v.planId, p.id)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version does not belong to plan");
    }
    p.name = v.name;
    p.sortOrder = v.sortOrder;
    p.isActive = v.isActive;
    p.layoutBgUrl = v.layoutBgUrl;
    p.layoutZonesJson = v.layoutZonesJson;
    p = hallPlanRepo.save(p);
    savePlanVersion(p, u, "RESTORE");
    auditService.log(u, "RESTORE", "HallPlan", p.id, "version:" + v.id);
    return new HallPlanDto(p.id, p.hallId, p.name, p.isActive, p.sortOrder, p.layoutBgUrl, p.layoutZonesJson);
  }

  private void savePlanVersion(HallPlan p, StaffUser u, String action) {
    if (p == null) return;
    BranchHall h = hallRepo.findById(p.hallId).orElse(null);
    if (h == null) return;
    HallPlanVersion v = new HallPlanVersion();
    v.planId = p.id;
    v.hallId = p.hallId;
    v.branchId = h.branchId;
    v.name = p.name;
    v.sortOrder = p.sortOrder;
    v.isActive = p.isActive;
    v.layoutBgUrl = p.layoutBgUrl;
    v.layoutZonesJson = p.layoutZonesJson;
    v.createdByStaffId = u == null ? null : u.id;
    v.action = action;
    hallPlanVersionRepo.save(v);
  }

  public record AdminPartyDto(
    long id,
    long tableId,
    int tableNumber,
    String pin,
    String status,
    String createdAt,
    String expiresAt,
    String closedAt,
    List<Long> guestSessionIds
  ) {}

  @GetMapping("/parties")
  public List<AdminPartyDto> listParties(
    @RequestParam(value = "status", required = false) String status,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<CafeTable> tables = tableRepo.findByBranchId(bid);
    if (tables.isEmpty()) return List.of();
    List<Long> tableIds = tables.stream().map(t -> t.id).toList();
    Map<Long, Integer> tableNumberById = new HashMap<>();
    for (CafeTable t : tables) {
      tableNumberById.put(t.id, t.number);
    }

    String st = status == null || status.isBlank() ? "ACTIVE" : status.trim().toUpperCase(Locale.ROOT);
    List<TableParty> parties = partyRepo.findTop200ByTableIdInAndStatusOrderByCreatedAtDesc(tableIds, st);
    if (parties.isEmpty()) return List.of();

    List<Long> partyIds = parties.stream().map(p -> p.id).toList();
    List<GuestSession> sessions = guestSessionRepo.findByPartyIdIn(partyIds);
    Map<Long, List<Long>> sessionIdsByParty = new HashMap<>();
    for (GuestSession s : sessions) {
      if (s.partyId == null) continue;
      sessionIdsByParty.computeIfAbsent(s.partyId, k -> new ArrayList<>()).add(s.id);
    }

    List<AdminPartyDto> out = new ArrayList<>();
    for (TableParty p : parties) {
      out.add(new AdminPartyDto(
        p.id,
        p.tableId,
        tableNumberById.getOrDefault(p.tableId, 0),
        p.pin,
        p.status,
        p.createdAt == null ? null : p.createdAt.toString(),
        p.expiresAt == null ? null : p.expiresAt.toString(),
        p.closedAt == null ? null : p.closedAt.toString(),
        sessionIdsByParty.getOrDefault(p.id, List.of())
      ));
    }
    return out;
  }

  // --- Branch settings ---
  public record BranchSettingsResponse(
    long branchId,
    boolean requireOtpForFirstOrder,
    int otpTtlSeconds,
    int otpMaxAttempts,
    int otpResendCooldownSeconds,
    int otpLength,
    boolean otpDevEchoCode,
    boolean enablePartyPin,
    boolean allowPayOtherGuestsItems,
    boolean allowPayWholeTable,
    boolean tipsEnabled,
    List<Integer> tipsPercentages,
    int serviceFeePercent,
    int taxPercent,
    boolean inventoryEnabled,
    boolean loyaltyEnabled,
    int loyaltyPointsPer100Cents,
    boolean onlinePayEnabled,
    String onlinePayProvider,
    String onlinePayCurrencyCode,
    String onlinePayRequestUrl,
    String onlinePayCacertPath,
    String onlinePayPcertPath,
    String onlinePayPcertPassword,
    String onlinePayKeyPath,
    String onlinePayRedirectUrl,
    String onlinePayReturnUrl,
    boolean payCashEnabled,
    boolean payTerminalEnabled,
    String currencyCode,
    String defaultLang,
    String commissionModel,
    int commissionMonthlyFixedCents,
    int commissionMonthlyPercent,
    int commissionOrderPercent,
    int commissionOrderFixedCents
  ) {}

  @GetMapping("/branch-settings")
  public BranchSettingsResponse getBranchSettings(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    BranchSettingsService.Resolved s = settingsService.resolveForBranch(bid);
    return new BranchSettingsResponse(
      bid,
      s.requireOtpForFirstOrder(),
      s.otpTtlSeconds(),
      s.otpMaxAttempts(),
      s.otpResendCooldownSeconds(),
      s.otpLength(),
      s.otpDevEchoCode(),
      s.enablePartyPin(),
      s.allowPayOtherGuestsItems(),
      s.allowPayWholeTable(),
      s.tipsEnabled(),
      s.tipsPercentages(),
      s.serviceFeePercent(),
      s.taxPercent(),
      s.inventoryEnabled(),
      s.loyaltyEnabled(),
      s.loyaltyPointsPer100Cents(),
      s.onlinePayEnabled(),
      s.onlinePayProvider(),
      s.onlinePayCurrencyCode(),
      s.onlinePayRequestUrl(),
      s.onlinePayCacertPath(),
      s.onlinePayPcertPath(),
      s.onlinePayPcertPassword(),
      s.onlinePayKeyPath(),
      s.onlinePayRedirectUrl(),
      s.onlinePayReturnUrl(),
      s.payCashEnabled(),
      s.payTerminalEnabled(),
      s.currencyCode(),
      s.defaultLang(),
      s.commissionModel(),
      s.commissionMonthlyFixedCents(),
      s.commissionMonthlyPercent(),
      s.commissionOrderPercent(),
      s.commissionOrderFixedCents()
    );
  }

  public record UpdateBranchSettingsRequest(
    Boolean requireOtpForFirstOrder,
    Integer otpTtlSeconds,
    Integer otpMaxAttempts,
    Integer otpResendCooldownSeconds,
    Integer otpLength,
    Boolean otpDevEchoCode,
    Boolean enablePartyPin,
    Boolean allowPayOtherGuestsItems,
    Boolean allowPayWholeTable,
    Boolean tipsEnabled,
    List<Integer> tipsPercentages,
    Integer serviceFeePercent,
    Integer taxPercent,
    Boolean inventoryEnabled,
    Boolean loyaltyEnabled,
    Integer loyaltyPointsPer100Cents,
    Boolean onlinePayEnabled,
    String onlinePayProvider,
    String onlinePayCurrencyCode,
    String onlinePayRequestUrl,
    String onlinePayCacertPath,
    String onlinePayPcertPath,
    String onlinePayPcertPassword,
    String onlinePayKeyPath,
    String onlinePayRedirectUrl,
    String onlinePayReturnUrl,
    Boolean payCashEnabled,
    Boolean payTerminalEnabled,
    String currencyCode,
    String defaultLang,
    String commissionModel,
    Integer commissionMonthlyFixedCents,
    Integer commissionMonthlyPercent,
    Integer commissionOrderPercent,
    Integer commissionOrderFixedCents
  ) {}

  @PutMapping("/branch-settings")
  @Transactional
  public BranchSettingsResponse updateBranchSettings(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody UpdateBranchSettingsRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    String prevCurrency = settingsService.resolveForBranch(bid).currencyCode();
    BranchSettings s = settingsRepo.findById(bid).orElseGet(() -> {
      BranchSettings ns = new BranchSettings();
      ns.branchId = bid;
      return ns;
    });

    if (req.requireOtpForFirstOrder != null) s.requireOtpForFirstOrder = req.requireOtpForFirstOrder;
    if (req.otpTtlSeconds != null) s.otpTtlSeconds = req.otpTtlSeconds;
    if (req.otpMaxAttempts != null) s.otpMaxAttempts = req.otpMaxAttempts;
    if (req.otpResendCooldownSeconds != null) s.otpResendCooldownSeconds = req.otpResendCooldownSeconds;
    if (req.otpLength != null) s.otpLength = req.otpLength;
    if (req.otpDevEchoCode != null) s.otpDevEchoCode = req.otpDevEchoCode;
    if (req.enablePartyPin != null) s.enablePartyPin = req.enablePartyPin;
    if (req.allowPayOtherGuestsItems != null) s.allowPayOtherGuestsItems = req.allowPayOtherGuestsItems;
    if (req.allowPayWholeTable != null) s.allowPayWholeTable = req.allowPayWholeTable;
    if (req.tipsEnabled != null) s.tipsEnabled = req.tipsEnabled;
    if (req.tipsPercentages != null) {
      s.tipsPercentages = toCsv(req.tipsPercentages);
    }
    if (req.serviceFeePercent != null) s.serviceFeePercent = req.serviceFeePercent;
    if (req.taxPercent != null) s.taxPercent = req.taxPercent;
    if (req.inventoryEnabled != null) s.inventoryEnabled = req.inventoryEnabled;
    if (req.loyaltyEnabled != null) s.loyaltyEnabled = req.loyaltyEnabled;
    if (req.loyaltyPointsPer100Cents != null && req.loyaltyPointsPer100Cents >= 0) {
      s.loyaltyPointsPer100Cents = req.loyaltyPointsPer100Cents;
    }
    if (req.onlinePayEnabled != null) s.onlinePayEnabled = req.onlinePayEnabled;
    if (req.onlinePayProvider != null) {
      String p = req.onlinePayProvider.trim().toUpperCase(Locale.ROOT);
      if (!p.isEmpty() && !Set.of("MAIB", "PAYNET", "MIA").contains(p)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported payment provider");
      }
      s.onlinePayProvider = p.isEmpty() ? null : p;
    }
    if (req.onlinePayCurrencyCode != null && !req.onlinePayCurrencyCode.isBlank()) {
      String code = req.onlinePayCurrencyCode.trim().toUpperCase(Locale.ROOT);
      md.virtualwaiter.domain.Currency cur = currencyRepo.findById(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
      if (!cur.isActive) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is inactive");
      }
      s.onlinePayCurrencyCode = code;
    }
    if (req.onlinePayRequestUrl != null) s.onlinePayRequestUrl = normalizeUrlOrNull(req.onlinePayRequestUrl);
    if (req.onlinePayCacertPath != null) s.onlinePayCacertPath = normalizePathOrNull(req.onlinePayCacertPath);
    if (req.onlinePayPcertPath != null) s.onlinePayPcertPath = normalizePathOrNull(req.onlinePayPcertPath);
    if (req.onlinePayPcertPassword != null) s.onlinePayPcertPassword = normalizeSecretOrNull(req.onlinePayPcertPassword);
    if (req.onlinePayKeyPath != null) s.onlinePayKeyPath = normalizePathOrNull(req.onlinePayKeyPath);
    if (req.onlinePayRedirectUrl != null) s.onlinePayRedirectUrl = normalizeUrlOrNull(req.onlinePayRedirectUrl);
    if (req.onlinePayReturnUrl != null) s.onlinePayReturnUrl = normalizeUrlOrNull(req.onlinePayReturnUrl);
    if (req.payCashEnabled != null) s.payCashEnabled = req.payCashEnabled;
    if (req.payTerminalEnabled != null) s.payTerminalEnabled = req.payTerminalEnabled;
    if (req.currencyCode != null && !req.currencyCode.isBlank()) {
      String code = req.currencyCode.trim().toUpperCase(Locale.ROOT);
      md.virtualwaiter.domain.Currency cur = currencyRepo.findById(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
      if (!cur.isActive) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is inactive");
      }
      s.currencyCode = code;
    }
    if (req.defaultLang != null && !req.defaultLang.isBlank()) {
      String lang = normalizeLocale(req.defaultLang);
      s.defaultLang = lang;
    }
    if (req.commissionModel != null) {
      String model = req.commissionModel.trim().toUpperCase(Locale.ROOT);
      if (!model.isEmpty() && !Set.of("MONTHLY_FIXED", "MONTHLY_PERCENT", "ORDER_PERCENT", "ORDER_FIXED").contains(model)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported commission model");
      }
      s.commissionModel = model.isEmpty() ? null : model;
    }
    if (req.commissionMonthlyFixedCents != null && req.commissionMonthlyFixedCents >= 0) {
      s.commissionMonthlyFixedCents = req.commissionMonthlyFixedCents;
    }
    if (req.commissionMonthlyPercent != null && req.commissionMonthlyPercent >= 0 && req.commissionMonthlyPercent <= 100) {
      s.commissionMonthlyPercent = req.commissionMonthlyPercent;
    }
    if (req.commissionOrderPercent != null && req.commissionOrderPercent >= 0 && req.commissionOrderPercent <= 100) {
      s.commissionOrderPercent = req.commissionOrderPercent;
    }
    if (req.commissionOrderFixedCents != null && req.commissionOrderFixedCents >= 0) {
      s.commissionOrderFixedCents = req.commissionOrderFixedCents;
    }

    if (Boolean.TRUE.equals(s.onlinePayEnabled) && (s.onlinePayProvider == null || s.onlinePayProvider.isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Online payment provider required");
    }
    if (Boolean.TRUE.equals(s.onlinePayEnabled)) {
      if (isBlank(s.onlinePayRequestUrl)
        || isBlank(s.onlinePayCacertPath)
        || isBlank(s.onlinePayPcertPath)
        || isBlank(s.onlinePayPcertPassword)
        || isBlank(s.onlinePayKeyPath)
        || isBlank(s.onlinePayRedirectUrl)
        || isBlank(s.onlinePayReturnUrl)
      ) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Online payment PSP settings required");
      }
    }

    settingsRepo.save(s);
    if (req.currencyCode != null && !req.currencyCode.isBlank()) {
      String nextCurrency = s.currencyCode == null ? "MDL" : s.currencyCode;
      if (!Objects.equals(prevCurrency, nextCurrency)) {
        Branch b = branchRepo.findById(bid)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
        List<MenuCategory> cats = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
        if (!cats.isEmpty()) {
          List<Long> catIds = cats.stream().map(c -> c.id).toList();
          itemRepo.updateCurrencyByCategoryIds(nextCurrency, catIds);
        }
      }
    }
    BranchSettingsService.Resolved r = settingsService.resolveForBranch(bid);
    return new BranchSettingsResponse(
      bid,
      r.requireOtpForFirstOrder(),
      r.otpTtlSeconds(),
      r.otpMaxAttempts(),
      r.otpResendCooldownSeconds(),
      r.otpLength(),
      r.otpDevEchoCode(),
      r.enablePartyPin(),
      r.allowPayOtherGuestsItems(),
      r.allowPayWholeTable(),
      r.tipsEnabled(),
      r.tipsPercentages(),
      r.serviceFeePercent(),
      r.taxPercent(),
      r.inventoryEnabled(),
      r.loyaltyEnabled(),
      r.loyaltyPointsPer100Cents(),
      r.onlinePayEnabled(),
      r.onlinePayProvider(),
      r.onlinePayCurrencyCode(),
      r.onlinePayRequestUrl(),
      r.onlinePayCacertPath(),
      r.onlinePayPcertPath(),
      r.onlinePayPcertPassword(),
      r.onlinePayKeyPath(),
      r.onlinePayRedirectUrl(),
      r.onlinePayReturnUrl(),
      r.payCashEnabled(),
      r.payTerminalEnabled(),
      r.currencyCode(),
      r.defaultLang(),
      r.commissionModel(),
      r.commissionMonthlyFixedCents(),
      r.commissionMonthlyPercent(),
      r.commissionOrderPercent(),
      r.commissionOrderFixedCents()
    );
  }

  // --- Onboarding ---
  public record OnboardingCheckItem(String key, String title, boolean ok, String hint) {}
  public record OnboardingStatusResponse(String branchName, boolean ready, boolean demoSeeded, List<OnboardingCheckItem> items) {}
  public record OnboardingTemplate(
    List<OnboardingHall> halls,
    List<OnboardingTable> tables,
    List<OnboardingCategory> categories,
    List<OnboardingItem> items,
    OnboardingSettings settings
  ) {}
  public record OnboardingHall(String name, String layoutBgUrl, String layoutZonesJson) {}
  public record OnboardingTable(
    int number,
    String publicId,
    String hallName,
    Double layoutX,
    Double layoutY,
    Double layoutW,
    Double layoutH,
    String layoutShape,
    Integer layoutRotation,
    String layoutZone
  ) {}
  public record OnboardingCategory(String nameRu, String nameRo, String nameEn, Integer sortOrder, boolean isActive) {}
  public record OnboardingItem(
    String categoryNameRu,
    String nameRu,
    String nameRo,
    String nameEn,
    String descriptionRu,
    String descriptionRo,
    String descriptionEn,
    int priceCents,
    String currency,
    boolean isActive
  ) {}
  public record OnboardingSettings(String currencyCode, Boolean payCashEnabled, Boolean payTerminalEnabled, Boolean onlinePayEnabled) {}

  @GetMapping("/onboarding/status")
  public OnboardingStatusResponse onboardingStatus(
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    return buildOnboardingStatus(bid);
  }

  @PostMapping("/onboarding/seed")
  @Transactional
  public OnboardingStatusResponse seedOnboarding(
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

    if (hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid).stream().noneMatch(h -> "Демо‑зал".equalsIgnoreCase(h.name))) {
      BranchHall h = new BranchHall();
      h.branchId = bid;
      h.name = "Демо‑зал";
      h.isActive = true;
      h.sortOrder = 0;
      hallRepo.save(h);
    }

    List<CafeTable> existingTables = tableRepo.findByBranchId(bid);
    boolean hasDemoTables = existingTables.stream().anyMatch(t -> "DEMO".equalsIgnoreCase(t.layoutZone));
    if (!hasDemoTables) {
      List<BranchHall> halls = hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid);
      Long hallId = halls.stream().filter(h -> "Демо‑зал".equalsIgnoreCase(h.name)).map(h -> h.id).findFirst()
        .orElse(halls.isEmpty() ? null : halls.get(0).id);
      for (int i = 1; i <= 10; i++) {
        CafeTable t = new CafeTable();
        t.branchId = bid;
        t.number = 900 + i;
        t.publicId = generatePublicId();
        t.hallId = hallId;
        t.layoutX = 20.0 + i * 12.0;
        t.layoutY = 20.0;
        t.layoutW = 50.0;
        t.layoutH = 50.0;
        t.layoutShape = "rect";
        t.layoutZone = "DEMO";
        tableRepo.save(t);
      }
    }

    List<MenuCategory> categories = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    MenuCategory cat = categories.stream().filter(c -> "Демо‑меню".equalsIgnoreCase(c.nameRu)).findFirst().orElse(null);
    if (cat == null) {
      cat = new MenuCategory();
      cat.tenantId = b.tenantId;
      cat.nameRu = "Демо‑меню";
      cat.nameRo = "Meniu demo";
      cat.nameEn = "Demo menu";
      cat.sortOrder = 0;
      cat.isActive = true;
      cat = categoryRepo.save(cat);
    }

    if (itemRepo.findByCategoryId(cat.id).isEmpty()) {
      BranchSettings s = settingsRepo.findById(bid).orElse(null);
      String currency = s != null && s.currencyCode != null && !s.currencyCode.isBlank() ? s.currencyCode : "MDL";
      MenuItem it = new MenuItem();
      it.categoryId = cat.id;
      it.nameRu = "Демо блюдо";
      it.nameRo = "Preparat demo";
      it.nameEn = "Demo dish";
      it.descriptionRu = "Демо‑позиция для быстрого старта";
      it.descriptionRo = "Poziție demo pentru start rapid";
      it.descriptionEn = "Demo item for quick start";
      it.priceCents = 9900;
      it.currency = currency;
      it.isActive = true;
      itemRepo.save(it);
    }

    long staffCount = staffUserRepo.findByBranchId(bid).stream()
      .filter(su -> su.role != null)
      .filter(su -> !Set.of("ADMIN", "SUPER_ADMIN", "MANAGER", "OWNER").contains(su.role.toUpperCase(Locale.ROOT)))
      .count();
    if (staffCount == 0) {
      StaffUser su = new StaffUser();
      su.branchId = bid;
      su.username = "waiter_demo";
      su.passwordHash = passwordEncoder.encode("demo123");
      su.role = "WAITER";
      su.isActive = true;
      staffUserRepo.save(su);
      auditService.log(u, "CREATE", "StaffUser", su.id, "demo-seed");
    }

    BranchSettings s = settingsRepo.findById(bid).orElse(null);
    if (s != null) {
      if (s.currencyCode == null || s.currencyCode.isBlank()) {
        s.currencyCode = "MDL";
      }
      if (s.payCashEnabled == null && s.payTerminalEnabled == null && s.onlinePayEnabled == null) {
        s.payCashEnabled = true;
      }
      settingsRepo.save(s);
    }

    auditService.log(u, "ONBOARDING_SEED", "Branch", bid, "demo-seed");
    return buildOnboardingStatus(bid);
  }

  @GetMapping("/onboarding/template/export")
  public OnboardingTemplate exportOnboardingTemplate(
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);

    List<BranchHall> halls = hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid);
    List<OnboardingHall> hallDtos = new ArrayList<>();
    for (BranchHall h : halls) {
      hallDtos.add(new OnboardingHall(h.name, h.layoutBgUrl, h.layoutZonesJson));
    }

    List<CafeTable> tables = tableRepo.findByBranchId(bid);
    Map<Long, String> hallNameById = new HashMap<>();
    for (BranchHall h : halls) hallNameById.put(h.id, h.name);
    List<OnboardingTable> tableDtos = new ArrayList<>();
    for (CafeTable t : tables) {
      tableDtos.add(new OnboardingTable(
        t.number,
        t.publicId,
        hallNameById.get(t.hallId),
        t.layoutX,
        t.layoutY,
        t.layoutW,
        t.layoutH,
        t.layoutShape,
        t.layoutRotation,
        t.layoutZone
      ));
    }

    List<MenuCategory> cats = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    List<OnboardingCategory> catDtos = new ArrayList<>();
    for (MenuCategory c : cats) {
      catDtos.add(new OnboardingCategory(c.nameRu, c.nameRo, c.nameEn, c.sortOrder, c.isActive));
    }

    List<MenuItem> items = new ArrayList<>();
    if (!cats.isEmpty()) {
      List<Long> catIds = cats.stream().map(c -> c.id).toList();
      items = itemRepo.findByCategoryIdIn(catIds);
    }
    Map<Long, String> catNameById = new HashMap<>();
    for (MenuCategory c : cats) catNameById.put(c.id, c.nameRu);
    List<OnboardingItem> itemDtos = new ArrayList<>();
    Map<Long, BranchMenuItemOverride> overrides = loadMenuItemOverrides(b.id, items.stream().map(i -> i.id).toList());
    for (MenuItem it : items) {
      BranchMenuItemOverride o = overrides.get(it.id);
      boolean active = resolveMenuItemActive(it, o);
      itemDtos.add(new OnboardingItem(
        catNameById.get(it.categoryId),
        it.nameRu,
        it.nameRo,
        it.nameEn,
        it.descriptionRu,
        it.descriptionRo,
        it.descriptionEn,
        it.priceCents,
        it.currency,
        active
      ));
    }

    BranchSettings s = settingsRepo.findById(bid).orElse(null);
    OnboardingSettings set = new OnboardingSettings(
      s == null ? null : s.currencyCode,
      s == null ? null : s.payCashEnabled,
      s == null ? null : s.payTerminalEnabled,
      s == null ? null : s.onlinePayEnabled
    );
    return new OnboardingTemplate(hallDtos, tableDtos, catDtos, itemDtos, set);
  }

  @PostMapping("/onboarding/template/import")
  @Transactional
  public OnboardingStatusResponse importOnboardingTemplate(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestBody OnboardingTemplate tpl,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);

    Map<String, BranchHall> hallByName = new HashMap<>();
    for (BranchHall h : hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid)) hallByName.put(h.name, h);

    if (tpl != null && tpl.halls != null) {
      for (OnboardingHall h : tpl.halls) {
        if (h == null || h.name == null || h.name.isBlank()) continue;
        BranchHall existing = hallByName.get(h.name);
        if (existing == null) {
          BranchHall nh = new BranchHall();
          nh.branchId = bid;
          nh.name = h.name.trim();
          nh.layoutBgUrl = h.layoutBgUrl;
          nh.layoutZonesJson = h.layoutZonesJson;
          nh.isActive = true;
          nh.sortOrder = 0;
          hallRepo.save(nh);
          hallByName.put(nh.name, nh);
        }
      }
    }

    Map<Integer, CafeTable> tableByNumber = new HashMap<>();
    for (CafeTable t : tableRepo.findByBranchId(bid)) tableByNumber.put(t.number, t);

    if (tpl != null && tpl.tables != null) {
      for (OnboardingTable t : tpl.tables) {
        if (t == null) continue;
        CafeTable existing = tableByNumber.get(t.number);
        if (existing != null) continue;
        CafeTable nt = new CafeTable();
        nt.branchId = bid;
        nt.number = t.number;
        nt.publicId = (t.publicId == null || t.publicId.isBlank()) ? generatePublicId() : t.publicId.trim();
        BranchHall hall = t.hallName == null ? null : hallByName.get(t.hallName);
        nt.hallId = hall == null ? null : hall.id;
        nt.layoutX = t.layoutX;
        nt.layoutY = t.layoutY;
        nt.layoutW = t.layoutW;
        nt.layoutH = t.layoutH;
        nt.layoutShape = t.layoutShape;
        nt.layoutRotation = t.layoutRotation;
        nt.layoutZone = t.layoutZone;
        tableRepo.save(nt);
      }
    }

    List<MenuCategory> cats = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    Map<String, MenuCategory> catByName = new HashMap<>();
    for (MenuCategory c : cats) catByName.put(c.nameRu, c);

    if (tpl != null && tpl.categories != null) {
      for (OnboardingCategory c : tpl.categories) {
        if (c == null || c.nameRu == null || c.nameRu.isBlank()) continue;
        MenuCategory existing = catByName.get(c.nameRu);
        if (existing == null) {
          MenuCategory nc = new MenuCategory();
          nc.tenantId = b.tenantId;
          nc.nameRu = c.nameRu.trim();
          nc.nameRo = c.nameRo;
          nc.nameEn = c.nameEn;
          nc.sortOrder = c.sortOrder == null ? 0 : c.sortOrder;
          nc.isActive = c.isActive;
          nc = categoryRepo.save(nc);
          catByName.put(nc.nameRu, nc);
        }
      }
    }

    if (tpl != null && tpl.items != null) {
      for (OnboardingItem it : tpl.items) {
        if (it == null || it.nameRu == null || it.nameRu.isBlank() || it.categoryNameRu == null || it.categoryNameRu.isBlank()) continue;
        MenuCategory cat = catByName.get(it.categoryNameRu);
        if (cat == null) continue;
        MenuItem existingItem = itemRepo.findByCategoryId(cat.id).stream()
          .filter(x -> it.nameRu.equalsIgnoreCase(x.nameRu))
          .findFirst()
          .orElse(null);
        MenuItem ni = existingItem;
        if (ni == null) {
          ni = new MenuItem();
          ni.categoryId = cat.id;
          ni.nameRu = it.nameRu.trim();
          ni.nameRo = it.nameRo;
          ni.nameEn = it.nameEn;
          ni.descriptionRu = it.descriptionRu;
          ni.descriptionRo = it.descriptionRo;
          ni.descriptionEn = it.descriptionEn;
          ni.priceCents = it.priceCents;
          ni.currency = it.currency == null || it.currency.isBlank() ? "MDL" : it.currency.trim().toUpperCase(Locale.ROOT);
          ni.isActive = true;
          ni.isStopList = false;
          ni = itemRepo.save(ni);
        }
        upsertMenuItemOverride(b.id, ni.id, it.isActive, false);
      }
    }

    if (tpl != null && tpl.settings != null) {
      BranchSettings s = settingsRepo.findById(bid).orElseGet(() -> {
        BranchSettings ns = new BranchSettings();
        ns.branchId = bid;
        return ns;
      });
      if ((s.currencyCode == null || s.currencyCode.isBlank()) && tpl.settings.currencyCode != null && !tpl.settings.currencyCode.isBlank()) {
        s.currencyCode = tpl.settings.currencyCode.trim().toUpperCase(Locale.ROOT);
      }
      if (s.payCashEnabled == null && tpl.settings.payCashEnabled != null) s.payCashEnabled = tpl.settings.payCashEnabled;
      if (s.payTerminalEnabled == null && tpl.settings.payTerminalEnabled != null) s.payTerminalEnabled = tpl.settings.payTerminalEnabled;
      if (s.onlinePayEnabled == null && tpl.settings.onlinePayEnabled != null) s.onlinePayEnabled = tpl.settings.onlinePayEnabled;
      settingsRepo.save(s);
    }

    auditService.log(u, "ONBOARDING_IMPORT", "Branch", bid, "template-import");
    AuditLog log = new AuditLog();
    log.branchId = bid;
    log.actorUserId = u.id;
    log.actorUsername = u.username;
    log.actorRole = u.role;
    log.action = "ONBOARDING_IMPORT";
    log.entityType = "ONBOARDING";
    log.detailsJson = "Imported onboarding template";
    log.createdAt = Instant.now();
    auditLogRepo.save(log);
    return buildOnboardingStatus(bid);
  }

  @DeleteMapping("/onboarding/seed")
  @Transactional
  public OnboardingStatusResponse clearOnboardingSeed(
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);

    // delete demo staff
    List<StaffUser> staff = staffUserRepo.findByBranchId(bid);
    for (StaffUser su : staff) {
      if ("waiter_demo".equalsIgnoreCase(su.username)) {
        staffUserRepo.delete(su);
      }
    }

    // disable demo menu for this branch (do not delete shared menu)
    List<MenuCategory> categories = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    for (MenuCategory cat : categories) {
      if ("Демо‑меню".equalsIgnoreCase(cat.nameRu)) {
        List<MenuItem> items = itemRepo.findByCategoryId(cat.id);
        for (MenuItem it : items) {
          if ("Демо блюдо".equalsIgnoreCase(it.nameRu) && "Демо‑позиция для быстрого старта".equals(it.descriptionRu)) {
            upsertMenuItemOverride(b.id, it.id, false, false);
          }
        }
      }
    }

    // delete demo tables (if no orders)
    List<CafeTable> tables = tableRepo.findByBranchId(bid);
    for (CafeTable t : tables) {
      if (!"DEMO".equalsIgnoreCase(t.layoutZone)) continue;
      if (orderRepo.findTop50ByTableIdOrderByCreatedAtDesc(t.id).isEmpty()) {
        tableRepo.delete(t);
      }
    }

    // delete demo hall if empty
    List<BranchHall> halls = hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid);
    for (BranchHall h : halls) {
      if (!"Демо‑зал".equalsIgnoreCase(h.name)) continue;
      boolean hasTables = tableRepo.findByBranchId(bid).stream().anyMatch(t -> Objects.equals(t.hallId, h.id));
      if (!hasTables) {
        hallRepo.delete(h);
      }
    }

    auditService.log(u, "ONBOARDING_CLEAR", "Branch", bid, "demo-clear");
    return buildOnboardingStatus(bid);
  }

  private OnboardingStatusResponse buildOnboardingStatus(long bid) {
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));

    List<OnboardingCheckItem> items = new ArrayList<>();
    boolean hallsOk = !hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid).isEmpty();
    items.add(new OnboardingCheckItem(
      "halls",
      "Залы",
      hallsOk,
      hallsOk ? "Ок" : "Добавьте хотя бы один зал"
    ));

    boolean tablesOk = !tableRepo.findByBranchId(bid).isEmpty();
    items.add(new OnboardingCheckItem(
      "tables",
      "Столы",
      tablesOk,
      tablesOk ? "Ок" : "Добавьте столы"
    ));

    List<MenuCategory> categories = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    boolean catsOk = !categories.isEmpty();
    items.add(new OnboardingCheckItem(
      "menu_categories",
      "Категории меню",
      catsOk,
      catsOk ? "Ок" : "Добавьте категории меню"
    ));

    boolean itemsOk = false;
    if (catsOk) {
      List<Long> catIds = categories.stream().map(c -> c.id).toList();
      List<MenuItem> menuItems = itemRepo.findByCategoryIdIn(catIds);
      List<Long> itemIds = menuItems.stream().map(i -> i.id).toList();
      Map<Long, BranchMenuItemOverride> overrides = loadMenuItemOverrides(bid, itemIds);
      itemsOk = menuItems.stream().anyMatch(it -> resolveMenuItemActive(it, overrides.get(it.id)));
    }
    items.add(new OnboardingCheckItem(
      "menu_items",
      "Позиции меню",
      itemsOk,
      itemsOk ? "Ок" : "Добавьте позиции меню"
    ));

    List<StaffUser> staff = staffUserRepo.findByBranchId(bid);
    long staffCount = staff.stream()
      .filter(su -> su.role != null)
      .filter(su -> !Set.of("ADMIN", "SUPER_ADMIN", "MANAGER", "OWNER").contains(su.role.toUpperCase(Locale.ROOT)))
      .count();
    boolean staffOk = staffCount > 0;
    items.add(new OnboardingCheckItem(
      "staff",
      "Персонал",
      staffOk,
      staffOk ? "Ок" : "Добавьте персонал (официант/повар/бар/хост)"
    ));

    BranchSettings s = settingsRepo.findById(bid).orElse(null);
    boolean currencyOk = s != null && s.currencyCode != null && !s.currencyCode.isBlank();
    items.add(new OnboardingCheckItem(
      "currency",
      "Валюта",
      currencyOk,
      currencyOk ? "Ок" : "Выберите валюту в настройках"
    ));

    boolean paymentsOk = s != null && (
      Boolean.TRUE.equals(s.payCashEnabled) ||
      Boolean.TRUE.equals(s.payTerminalEnabled) ||
      Boolean.TRUE.equals(s.onlinePayEnabled)
    );
    items.add(new OnboardingCheckItem(
      "payments",
      "Методы оплаты",
      paymentsOk,
      paymentsOk ? "Ок" : "Включите хотя бы один способ оплаты"
    ));

    boolean demoSeeded = staffUserRepo.findByBranchId(bid).stream().anyMatch(su -> "waiter_demo".equalsIgnoreCase(su.username))
      || hallRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid).stream().anyMatch(h -> "Демо‑зал".equalsIgnoreCase(h.name))
      || tableRepo.findByBranchId(bid).stream().anyMatch(t -> "DEMO".equalsIgnoreCase(t.layoutZone));

    boolean ready = items.stream().allMatch(OnboardingCheckItem::ok);
    return new OnboardingStatusResponse(b.name, ready, demoSeeded, items);
  }

  // --- Currencies (super admin) ---
  public record CurrencyDto(String code, String name, String symbol, boolean isActive) {}
  public record CreateCurrencyRequest(@NotBlank String code, @NotBlank String name, String symbol, Boolean isActive) {}
  public record UpdateCurrencyRequest(String name, String symbol, Boolean isActive) {}

  @GetMapping("/currencies")
  public List<CurrencyDto> listCurrencies(
    @RequestParam(value = "includeInactive", required = false) Boolean includeInactive,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    boolean include = includeInactive != null && includeInactive && isSuperAdmin(u);
    List<md.virtualwaiter.domain.Currency> list = include ? currencyRepo.findAllByOrderByCodeAsc() : currencyRepo.findByIsActiveTrueOrderByCodeAsc();
    List<CurrencyDto> out = new ArrayList<>();
    for (md.virtualwaiter.domain.Currency c : list) {
      out.add(new CurrencyDto(c.code, c.name, c.symbol, c.isActive));
    }
    return out;
  }

  @PostMapping("/currencies")
  public CurrencyDto createCurrency(@Valid @RequestBody CreateCurrencyRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    requireSuperAdmin(u);
    md.virtualwaiter.domain.Currency c = new md.virtualwaiter.domain.Currency();
    c.code = req.code.trim().toUpperCase(Locale.ROOT);
    c.name = req.name.trim();
    c.symbol = req.symbol;
    c.isActive = req.isActive == null || req.isActive;
    c = currencyRepo.save(c);
    auditService.log(u, "CREATE", "Currency", null, c.code);
    return new CurrencyDto(c.code, c.name, c.symbol, c.isActive);
  }

  @PatchMapping("/currencies/{code}")
  public CurrencyDto updateCurrency(@PathVariable String code, @RequestBody UpdateCurrencyRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    requireSuperAdmin(u);
    String k = code.trim().toUpperCase(Locale.ROOT);
    md.virtualwaiter.domain.Currency c = currencyRepo.findById(k)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found"));
    if (req.name != null && !req.name.isBlank()) c.name = req.name.trim();
    if (req.symbol != null) c.symbol = req.symbol;
    if (req.isActive != null) c.isActive = req.isActive;
    c = currencyRepo.save(c);
    auditService.log(u, "UPDATE", "Currency", null, c.code);
    return new CurrencyDto(c.code, c.name, c.symbol, c.isActive);
  }

  // --- Inventory (MVP) ---
  public record InventoryItemDto(
    long id,
    String nameRu,
    String nameRo,
    String nameEn,
    String unit,
    double qtyOnHand,
    double minQty,
    boolean isActive
  ) {}

  public record InventoryItemRequest(
    @NotBlank String nameRu,
    String nameRo,
    String nameEn,
    String unit,
    Double qtyOnHand,
    Double minQty,
    Boolean isActive
  ) {}

  public record IngredientDto(
    long inventoryItemId,
    double qtyPerItem
  ) {}

  public record IngredientView(
    long inventoryItemId,
    String nameRu,
    String nameRo,
    String nameEn,
    String unit,
    double qtyPerItem
  ) {}

  @GetMapping("/inventory/items")
  public List<InventoryItemDto> listInventoryItems(
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<InventoryItem> items = inventoryItemRepo.findByBranchIdOrderByIdDesc(bid);
    List<InventoryItemDto> out = new ArrayList<>();
    for (InventoryItem it : items) {
      out.add(new InventoryItemDto(
        it.id,
        it.nameRu,
        it.nameRo,
        it.nameEn,
        it.unit,
        it.qtyOnHand == null ? 0.0 : it.qtyOnHand,
        it.minQty == null ? 0.0 : it.minQty,
        it.isActive
      ));
    }
    return out;
  }

  @GetMapping("/inventory/low-stock")
  public List<InventoryItemDto> lowStockInventory(
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<InventoryItem> items = inventoryItemRepo.findByBranchIdAndIsActiveTrueOrderByIdDesc(bid);
    List<InventoryItemDto> out = new ArrayList<>();
    for (InventoryItem it : items) {
      double qty = it.qtyOnHand == null ? 0.0 : it.qtyOnHand;
      double min = it.minQty == null ? 0.0 : it.minQty;
      if (qty <= min) {
        out.add(new InventoryItemDto(it.id, it.nameRu, it.nameRo, it.nameEn, it.unit, qty, min, it.isActive));
      }
    }
    return out;
  }

  @PostMapping("/inventory/items")
  public InventoryItemDto createInventoryItem(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody InventoryItemRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    InventoryItem it = new InventoryItem();
    it.branchId = bid;
    it.nameRu = req.nameRu.trim();
    it.nameRo = req.nameRo;
    it.nameEn = req.nameEn;
    it.unit = req.unit == null || req.unit.isBlank() ? "pcs" : req.unit.trim();
    it.qtyOnHand = req.qtyOnHand == null ? Double.valueOf(0.0) : req.qtyOnHand;
    it.minQty = req.minQty == null ? Double.valueOf(0.0) : req.minQty;
    it.isActive = req.isActive == null || req.isActive;
    inventoryItemRepo.save(it);
    return new InventoryItemDto(it.id, it.nameRu, it.nameRo, it.nameEn, it.unit, it.qtyOnHand, it.minQty, it.isActive);
  }

  @PutMapping("/inventory/items/{id}")
  public InventoryItemDto updateInventoryItem(
    @PathVariable("id") long id,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody InventoryItemRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    InventoryItem it = inventoryItemRepo.findByIdAndBranchId(id, bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
    if (req.nameRu != null && !req.nameRu.isBlank()) it.nameRu = req.nameRu.trim();
    if (req.nameRo != null) it.nameRo = req.nameRo;
    if (req.nameEn != null) it.nameEn = req.nameEn;
    if (req.unit != null && !req.unit.isBlank()) it.unit = req.unit.trim();
    if (req.qtyOnHand != null) it.qtyOnHand = req.qtyOnHand;
    if (req.minQty != null) it.minQty = req.minQty;
    if (req.isActive != null) it.isActive = req.isActive;
    it.updatedAt = Instant.now();
    inventoryItemRepo.save(it);
    return new InventoryItemDto(it.id, it.nameRu, it.nameRo, it.nameEn, it.unit, it.qtyOnHand, it.minQty, it.isActive);
  }

  @DeleteMapping("/inventory/items/{id}")
  public void deleteInventoryItem(
    @PathVariable("id") long id,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    InventoryItem it = inventoryItemRepo.findByIdAndBranchId(id, bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
    inventoryItemRepo.delete(it);
  }

  @GetMapping("/menu/items/{id}/ingredients")
  public List<IngredientView> listMenuItemIngredients(
    @PathVariable("id") long menuItemId,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    MenuItem mi = itemRepo.findById(menuItemId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory cat = categoryRepo.findById(mi.categoryId).orElse(null);
    if (cat == null || !Objects.equals(cat.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Menu item not in tenant");
    }
    List<MenuItemIngredient> ingredients = ingredientRepo.findByMenuItemId(menuItemId);
    Map<Long, InventoryItem> inventory = new HashMap<>();
    for (InventoryItem it : inventoryItemRepo.findByBranchIdOrderByIdDesc(bid)) {
      inventory.put(it.id, it);
    }
    List<IngredientView> out = new ArrayList<>();
    for (MenuItemIngredient ing : ingredients) {
      InventoryItem it = inventory.get(ing.inventoryItemId);
      if (it == null) continue;
      out.add(new IngredientView(
        it.id,
        it.nameRu,
        it.nameRo,
        it.nameEn,
        it.unit,
        ing.qtyPerItem == null ? 0.0 : ing.qtyPerItem
      ));
    }
    return out;
  }

  @PutMapping("/menu/items/{id}/ingredients")
  @Transactional
  public List<IngredientView> replaceMenuItemIngredients(
    @PathVariable("id") long menuItemId,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody List<IngredientDto> req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    MenuItem mi = itemRepo.findById(menuItemId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory cat = categoryRepo.findById(mi.categoryId).orElse(null);
    if (cat == null || !Objects.equals(cat.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Menu item not in tenant");
    }
    ingredientRepo.deleteByMenuItemId(menuItemId);
    List<IngredientView> out = new ArrayList<>();
    if (req != null) {
      for (IngredientDto dto : req) {
        if (dto.qtyPerItem <= 0) continue;
        InventoryItem it = inventoryItemRepo.findByIdAndBranchId(dto.inventoryItemId, bid)
          .orElse(null);
        if (it == null) continue;
        MenuItemIngredient ing = new MenuItemIngredient();
        ing.menuItemId = menuItemId;
        ing.inventoryItemId = it.id;
        ing.qtyPerItem = dto.qtyPerItem;
        ingredientRepo.save(ing);
        out.add(new IngredientView(it.id, it.nameRu, it.nameRo, it.nameEn, it.unit, dto.qtyPerItem));
      }
    }
    return out;
  }

  // --- Loyalty / CRM ---
  public record LoyaltyProfileResponse(
    String phone,
    int pointsBalance,
    List<LoyaltyService.FavoriteItemDto> favorites,
    List<LoyaltyService.OfferDto> offers
  ) {}

  public record CreateOfferRequest(
    @NotBlank String phone,
    @NotBlank String title,
    String body,
    String discountCode,
    String startsAt,
    String endsAt,
    Boolean isActive
  ) {}

  public record UpdateOfferRequest(
    String phone,
    String title,
    String body,
    String discountCode,
    String startsAt,
    String endsAt,
    Boolean isActive
  ) {}

  public record OfferDto(
    long id,
    String phone,
    String title,
    String body,
    String discountCode,
    String startsAt,
    String endsAt,
    boolean isActive
  ) {}

  @GetMapping("/loyalty/profile")
  public LoyaltyProfileResponse getLoyaltyProfile(
    @RequestParam("phone") String phone,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    LoyaltyService.LoyaltyProfile p = loyaltyService.getProfile(bid, phone);
    return new LoyaltyProfileResponse(p.phone(), p.pointsBalance(), p.favorites(), p.offers());
  }

  @GetMapping("/loyalty/offers")
  public List<OfferDto> listLoyaltyOffers(
    @RequestParam("phone") String phone,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    String p = phone == null ? "" : phone.trim();
    if (p.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "phone required");
    List<md.virtualwaiter.domain.GuestOffer> list = guestOfferRepo.findTop100ByBranchIdAndPhoneOrderByIdDesc(bid, p);
    List<OfferDto> out = new ArrayList<>();
    for (md.virtualwaiter.domain.GuestOffer o : list) {
      out.add(new OfferDto(
        o.id,
        o.phone,
        o.title,
        o.body,
        o.discountCode,
        o.startsAt == null ? null : o.startsAt.toString(),
        o.endsAt == null ? null : o.endsAt.toString(),
        o.isActive
      ));
    }
    return out;
  }

  @PostMapping("/loyalty/offers")
  public OfferDto createLoyaltyOffer(
    @Valid @RequestBody CreateOfferRequest req,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    md.virtualwaiter.domain.GuestOffer o = new md.virtualwaiter.domain.GuestOffer();
    o.branchId = bid;
    o.phone = req.phone.trim();
    o.title = req.title.trim();
    o.body = req.body;
    o.discountCode = req.discountCode == null ? null : req.discountCode.trim();
    o.startsAt = parseInstantOrDateOrNull(req.startsAt, true);
    o.endsAt = parseInstantOrDateOrNull(req.endsAt, false);
    o.isActive = req.isActive == null || req.isActive;
    o = guestOfferRepo.save(o);
    return new OfferDto(
      o.id, o.phone, o.title, o.body, o.discountCode,
      o.startsAt == null ? null : o.startsAt.toString(),
      o.endsAt == null ? null : o.endsAt.toString(),
      o.isActive
    );
  }

  @PatchMapping("/loyalty/offers/{id}")
  public OfferDto updateLoyaltyOffer(
    @PathVariable("id") long id,
    @RequestBody UpdateOfferRequest req,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    md.virtualwaiter.domain.GuestOffer o = guestOfferRepo.findByIdAndBranchId(id, bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
    if (req.phone != null && !req.phone.isBlank()) o.phone = req.phone.trim();
    if (req.title != null && !req.title.isBlank()) o.title = req.title.trim();
    if (req.body != null) o.body = req.body;
    if (req.discountCode != null) o.discountCode = req.discountCode.trim();
    if (req.startsAt != null) o.startsAt = parseInstantOrDateOrNull(req.startsAt, true);
    if (req.endsAt != null) o.endsAt = parseInstantOrDateOrNull(req.endsAt, false);
    if (req.isActive != null) o.isActive = req.isActive;
    o.updatedAt = Instant.now();
    o = guestOfferRepo.save(o);
    return new OfferDto(
      o.id, o.phone, o.title, o.body, o.discountCode,
      o.startsAt == null ? null : o.startsAt.toString(),
      o.endsAt == null ? null : o.endsAt.toString(),
      o.isActive
    );
  }

  @DeleteMapping("/loyalty/offers/{id}")
  public Map<String, Object> deleteLoyaltyOffer(
    @PathVariable("id") long id,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    md.virtualwaiter.domain.GuestOffer o = guestOfferRepo.findByIdAndBranchId(id, bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found"));
    guestOfferRepo.delete(o);
    Map<String, Object> out = new HashMap<>();
    out.put("deleted", true);
    out.put("id", id);
    return out;
  }

  // --- Menu templates ---
  public record MenuTemplateDto(
    long id,
    long tenantId,
    Long restaurantId,
    String name,
    boolean isActive,
    String scope
  ) {}

  public record SaveMenuTemplateRequest(@NotBlank String name, @NotBlank String scope) {}
  public record ApplyMenuTemplateRequest(@NotNull Long templateId, Boolean replaceExisting) {}

  private record MenuTemplateCategoryPayload(
    String nameRu,
    String nameRo,
    String nameEn,
    int sortOrder,
    boolean isActive
  ) {}

  private record MenuTemplateItemPayload(
    String categoryNameRu,
    String nameRu,
    String nameRo,
    String nameEn,
    String descriptionRu,
    String descriptionRo,
    String descriptionEn,
    String ingredientsRu,
    String ingredientsRo,
    String ingredientsEn,
    String allergens,
    String weight,
    String tags,
    String photoUrls,
    Integer kcal,
    Integer proteinG,
    Integer fatG,
    Integer carbsG,
    int priceCents,
    String currency,
    boolean isActive,
    boolean isStopList
  ) {}

  @GetMapping("/menu-templates")
  public List<MenuTemplateDto> listMenuTemplates(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "includeInactive", required = false) Boolean includeInactive,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    List<MenuTemplate> out = new ArrayList<>();
    out.addAll(menuTemplateRepo.findByTenantIdAndRestaurantIdIsNullOrderByIdAsc(b.tenantId));
    if (b.restaurantId != null) {
      out.addAll(menuTemplateRepo.findByTenantIdAndRestaurantIdOrderByIdAsc(b.tenantId, b.restaurantId));
    }
    boolean showInactive = includeInactive != null && includeInactive;
    return out.stream()
      .filter(t -> showInactive || t.isActive)
      .map(t -> new MenuTemplateDto(
        t.id,
        t.tenantId,
        t.restaurantId,
        t.name,
        t.isActive,
        t.restaurantId == null ? "TENANT" : "RESTAURANT"
      ))
      .toList();
  }

  @PostMapping("/menu-templates/save")
  public MenuTemplateDto saveMenuTemplate(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody SaveMenuTemplateRequest req,
    Authentication auth
  ) throws IOException {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    boolean scopeRestaurant = "RESTAURANT".equalsIgnoreCase(req.scope);
    if (scopeRestaurant && b.restaurantId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Branch has no restaurant");
    }
    List<MenuCategory> categories = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    List<Long> catIds = categories.stream().map(c -> c.id).toList();
    List<MenuItem> items = catIds.isEmpty() ? List.of() : itemRepo.findByCategoryIdIn(catIds);
    Map<Long, BranchMenuItemOverride> overrides = loadMenuItemOverrides(b.id, items.stream().map(i -> i.id).toList());
    Map<Long, MenuCategory> catMap = categories.stream().collect(Collectors.toMap(c -> c.id, c -> c));
    List<MenuTemplateCategoryPayload> categoryPayloads = new ArrayList<>();
    for (MenuCategory c : categories) {
      categoryPayloads.add(new MenuTemplateCategoryPayload(c.nameRu, c.nameRo, c.nameEn, c.sortOrder, c.isActive));
    }
    List<MenuTemplateItemPayload> itemPayloads = new ArrayList<>();
    for (MenuItem it : items) {
      MenuCategory c = catMap.get(it.categoryId);
      String catNameRu = c == null ? null : c.nameRu;
      if (catNameRu == null || catNameRu.isBlank()) continue;
      BranchMenuItemOverride o = overrides.get(it.id);
      boolean active = resolveMenuItemActive(it, o);
      boolean stop = resolveMenuItemStopList(it, o);
      itemPayloads.add(new MenuTemplateItemPayload(
        catNameRu,
        it.nameRu,
        it.nameRo,
        it.nameEn,
        it.descriptionRu,
        it.descriptionRo,
        it.descriptionEn,
        it.ingredientsRu,
        it.ingredientsRo,
        it.ingredientsEn,
        it.allergens,
        it.weight,
        it.tags,
        it.photoUrls,
        it.kcal,
        it.proteinG,
        it.fatG,
        it.carbsG,
        it.priceCents,
        it.currency,
        active,
        stop
      ));
    }
    ObjectMapper mapper = new ObjectMapper();
    String payloadJson = mapper.writeValueAsString(Map.of(
      "categories", categoryPayloads,
      "items", itemPayloads
    ));
    MenuTemplate t = new MenuTemplate();
    t.tenantId = b.tenantId;
    t.restaurantId = scopeRestaurant ? b.restaurantId : null;
    t.name = req.name;
    t.payloadJson = payloadJson;
    t.isActive = true;
    t.createdAt = Instant.now();
    t.updatedAt = Instant.now();
    t = menuTemplateRepo.save(t);
    auditService.log(u, "CREATE", "MenuTemplate", t.id, null);
    return new MenuTemplateDto(t.id, t.tenantId, t.restaurantId, t.name, t.isActive, t.restaurantId == null ? "TENANT" : "RESTAURANT");
  }

  @PostMapping("/menu-templates/apply")
  @Transactional
  public void applyMenuTemplate(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody ApplyMenuTemplateRequest req,
    Authentication auth
  ) throws IOException {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    MenuTemplate t = menuTemplateRepo.findById(req.templateId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
    if (!Objects.equals(t.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template does not belong to tenant");
    }
    if (t.restaurantId != null && !Objects.equals(t.restaurantId, b.restaurantId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template does not belong to restaurant");
    }
    boolean replaceExisting = req.replaceExisting == null || req.replaceExisting;
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(t.payloadJson);
    JsonNode categoriesNode = root.get("categories");
    JsonNode itemsNode = root.get("items");
    if (categoriesNode == null || !categoriesNode.isArray()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template categories missing");
    }
    List<MenuCategory> tenantCategories = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    Map<String, Long> categoryIdByName = new HashMap<>();
    for (JsonNode c : categoriesNode) {
      String nameRu = c.get("nameRu").asText(null);
      if (nameRu == null || nameRu.isBlank()) continue;
      MenuCategory existing = tenantCategories.stream()
        .filter(cat -> nameRu.equalsIgnoreCase(cat.nameRu))
        .findFirst()
        .orElse(null);
      MenuCategory mc = existing;
      if (mc == null) {
        mc = new MenuCategory();
        mc.tenantId = b.tenantId;
        mc.nameRu = nameRu;
        mc.nameRo = c.get("nameRo").asText(null);
        mc.nameEn = c.get("nameEn").asText(null);
        mc.sortOrder = c.has("sortOrder") ? c.get("sortOrder").asInt(0) : 0;
        mc.isActive = !c.has("isActive") || c.get("isActive").asBoolean(true);
        mc = categoryRepo.save(mc);
      }
      categoryIdByName.put(nameRu, mc.id);
    }
    Set<Long> templateItemIds = new HashSet<>();
    if (itemsNode != null && itemsNode.isArray()) {
      for (JsonNode it : itemsNode) {
        String catNameRu = it.get("categoryNameRu").asText(null);
        Long catId = catNameRu == null ? null : categoryIdByName.get(catNameRu);
        if (catId == null) continue;
        String itemNameRu = it.get("nameRu").asText(null);
        if (itemNameRu == null || itemNameRu.isBlank()) continue;
        MenuItem mi = itemRepo.findByCategoryId(catId).stream()
          .filter(x -> itemNameRu.equalsIgnoreCase(x.nameRu))
          .findFirst()
          .orElse(null);
        if (mi == null) {
          mi = new MenuItem();
          mi.categoryId = catId;
          mi.nameRu = itemNameRu;
          mi.nameRo = it.get("nameRo").asText(null);
          mi.nameEn = it.get("nameEn").asText(null);
          mi.descriptionRu = it.get("descriptionRu").asText(null);
          mi.descriptionRo = it.get("descriptionRo").asText(null);
          mi.descriptionEn = it.get("descriptionEn").asText(null);
          mi.ingredientsRu = it.get("ingredientsRu").asText(null);
          mi.ingredientsRo = it.get("ingredientsRo").asText(null);
          mi.ingredientsEn = it.get("ingredientsEn").asText(null);
          mi.allergens = it.get("allergens").asText(null);
          mi.weight = it.get("weight").asText(null);
          mi.tags = it.get("tags").asText(null);
          mi.photoUrls = it.get("photoUrls").asText(null);
          mi.kcal = it.has("kcal") && !it.get("kcal").isNull() ? it.get("kcal").asInt() : null;
          mi.proteinG = it.has("proteinG") && !it.get("proteinG").isNull() ? it.get("proteinG").asInt() : null;
          mi.fatG = it.has("fatG") && !it.get("fatG").isNull() ? it.get("fatG").asInt() : null;
          mi.carbsG = it.has("carbsG") && !it.get("carbsG").isNull() ? it.get("carbsG").asInt() : null;
          mi.priceCents = it.has("priceCents") ? it.get("priceCents").asInt(0) : 0;
          mi.currency = it.get("currency").asText("MDL");
          mi.isActive = true;
          mi.isStopList = false;
          mi = itemRepo.save(mi);
        }
        boolean nextActive = !it.has("isActive") || it.get("isActive").asBoolean(true);
        boolean nextStop = it.has("isStopList") && it.get("isStopList").asBoolean(false);
        upsertMenuItemOverride(b.id, mi.id, nextActive, nextStop);
        templateItemIds.add(mi.id);
      }
    }
    if (replaceExisting) {
      List<Long> allCatIds = tenantCategories.stream().map(c -> c.id).toList();
      List<MenuItem> allItems = allCatIds.isEmpty() ? List.of() : itemRepo.findByCategoryIdIn(allCatIds);
      for (MenuItem mi : allItems) {
        if (!templateItemIds.contains(mi.id)) {
          upsertMenuItemOverride(b.id, mi.id, false, resolveMenuItemStopList(mi, null));
        }
      }
    }
    b.menuTemplateId = t.id;
    branchRepo.save(b);
    auditService.log(u, "APPLY_TEMPLATE", "Branch", b.id, "templateId=" + t.id);
  }

  // --- Menu categories ---
  public record MenuCategoryDto(long id, String nameRu, String nameRo, String nameEn, int sortOrder, boolean isActive) {}
  public record CreateCategoryRequest(@NotBlank String nameRu, String nameRo, String nameEn, Integer sortOrder, Boolean isActive) {}

  @GetMapping("/menu/categories")
  public List<MenuCategoryDto> listCategories(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    List<MenuCategory> cats = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
    List<MenuCategoryDto> out = new ArrayList<>();
    for (MenuCategory c : cats) {
      out.add(new MenuCategoryDto(c.id, c.nameRu, c.nameRo, c.nameEn, c.sortOrder, c.isActive));
    }
    return out;
  }

  @PostMapping("/menu/categories")
  public MenuCategoryDto createCategory(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody CreateCategoryRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    MenuCategory c = new MenuCategory();
    c.tenantId = b.tenantId;
    c.nameRu = req.nameRu;
    c.nameRo = req.nameRo;
    c.nameEn = req.nameEn;
    c.sortOrder = req.sortOrder == null ? 0 : req.sortOrder;
    c.isActive = req.isActive == null || req.isActive;
    c = categoryRepo.save(c);
    auditService.log(u, "CREATE", "MenuCategory", c.id, null);
    return new MenuCategoryDto(c.id, c.nameRu, c.nameRo, c.nameEn, c.sortOrder, c.isActive);
  }

  public record UpdateCategoryRequest(String nameRu, String nameRo, String nameEn, Integer sortOrder, Boolean isActive) {}

  @PatchMapping("/menu/categories/{id}")
  public MenuCategoryDto updateCategory(@PathVariable long id, @RequestBody UpdateCategoryRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    MenuCategory c = categoryRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(c.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    if (req.nameRu != null) c.nameRu = req.nameRu;
    if (req.nameRo != null) c.nameRo = req.nameRo;
    if (req.nameEn != null) c.nameEn = req.nameEn;
    if (req.sortOrder != null) c.sortOrder = req.sortOrder;
    if (req.isActive != null) c.isActive = req.isActive;
    c = categoryRepo.save(c);
    auditService.log(u, "UPDATE", "MenuCategory", c.id, null);
    return new MenuCategoryDto(c.id, c.nameRu, c.nameRo, c.nameEn, c.sortOrder, c.isActive);
  }

  @DeleteMapping("/menu/categories/{id}")
  public void deleteCategory(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    MenuCategory c = categoryRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(c.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    categoryRepo.delete(c);
    auditService.log(u, "DELETE", "MenuCategory", c.id, null);
  }

  // --- Menu items ---
  public record MenuItemDto(
    long id,
    long categoryId,
    String nameRu,
    String nameRo,
    String nameEn,
    String descriptionRu,
    String descriptionRo,
    String descriptionEn,
    String ingredientsRu,
    String ingredientsRo,
    String ingredientsEn,
    String allergens,
    String weight,
    String tags,
    String photoUrls,
    Integer kcal,
    Integer proteinG,
    Integer fatG,
    Integer carbsG,
    int priceCents,
    String currency,
    boolean isActive,
    boolean isStopList
  ) {}

  public record CreateMenuItemRequest(
    @NotNull Long categoryId,
    @NotBlank String nameRu,
    String nameRo,
    String nameEn,
    String descriptionRu,
    String descriptionRo,
    String descriptionEn,
    String ingredientsRu,
    String ingredientsRo,
    String ingredientsEn,
    String allergens,
    String weight,
    String tags,
    String photoUrls,
    Integer kcal,
    Integer proteinG,
    Integer fatG,
    Integer carbsG,
    @NotNull Integer priceCents,
    String currency,
    Boolean isActive,
    Boolean isStopList
  ) {}

  @GetMapping("/menu/items")
  public List<MenuItemDto> listMenuItems(
    @RequestParam(value = "categoryId", required = false) Long categoryId,
    @RequestParam(value = "branchId", required = false) Long branchId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    List<MenuItem> items;
    if (categoryId != null) {
      MenuCategory c = categoryRepo.findById(categoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
      if (!Objects.equals(c.tenantId, b.tenantId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
      }
      items = itemRepo.findByCategoryId(categoryId);
    } else {
      List<MenuCategory> cats = categoryRepo.findByTenantIdOrderBySortOrderAscIdAsc(b.tenantId);
      List<Long> catIds = cats.stream().map(x -> x.id).toList();
      items = catIds.isEmpty() ? List.of() : itemRepo.findByCategoryIdIn(catIds);
    }
    List<Long> itemIds = items.stream().map(it -> it.id).toList();
    Map<Long, BranchMenuItemOverride> overrides = loadMenuItemOverrides(b.id, itemIds);
    List<MenuItemDto> out = new ArrayList<>();
    for (MenuItem it : items) {
      BranchMenuItemOverride o = overrides.get(it.id);
      out.add(toDto(it, o));
    }
    return out;
  }

  @PostMapping("/menu/items")
  public MenuItemDto createMenuItem(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody CreateMenuItemRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    MenuCategory c = categoryRepo.findById(req.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    if (!Objects.equals(c.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    MenuItem it = new MenuItem();
    it.categoryId = req.categoryId;
    it.nameRu = req.nameRu;
    it.nameRo = req.nameRo;
    it.nameEn = req.nameEn;
    it.descriptionRu = req.descriptionRu;
    it.descriptionRo = req.descriptionRo;
    it.descriptionEn = req.descriptionEn;
    it.ingredientsRu = req.ingredientsRu;
    it.ingredientsRo = req.ingredientsRo;
    it.ingredientsEn = req.ingredientsEn;
    it.allergens = req.allergens;
    it.weight = req.weight;
    it.tags = req.tags;
    it.photoUrls = sanitizePhotoUrls(req.photoUrls);
    it.kcal = req.kcal;
    it.proteinG = req.proteinG;
    it.fatG = req.fatG;
    it.carbsG = req.carbsG;
    it.priceCents = req.priceCents;
    if (req.currency != null && !req.currency.isBlank()) {
      String code = req.currency.trim().toUpperCase(Locale.ROOT);
      md.virtualwaiter.domain.Currency cur = currencyRepo.findById(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
      if (!cur.isActive) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is inactive");
      }
      it.currency = code;
    } else {
      it.currency = settingsService.resolveForBranch(bid).currencyCode();
    }
    it.isActive = true;
    it.isStopList = false;
    it = itemRepo.save(it);
    if (req.isActive != null || req.isStopList != null) {
      upsertMenuItemOverride(b.id, it.id, req.isActive == null || req.isActive, req.isStopList != null && req.isStopList);
    }
    auditService.log(u, "CREATE", "MenuItem", it.id, null);
    BranchMenuItemOverride o = menuItemOverrideRepo.findByBranchIdAndMenuItemId(b.id, it.id).orElse(null);
    return toDto(it, o);
  }

  public record UpdateMenuItemRequest(
    Long categoryId,
    String nameRu,
    String nameRo,
    String nameEn,
    String descriptionRu,
    String descriptionRo,
    String descriptionEn,
    String ingredientsRu,
    String ingredientsRo,
    String ingredientsEn,
    String allergens,
    String weight,
    String tags,
    String photoUrls,
    Integer kcal,
    Integer proteinG,
    Integer fatG,
    Integer carbsG,
    Integer priceCents,
    String currency,
    Boolean isActive,
    Boolean isStopList
  ) {}

  @PatchMapping("/menu/items/{id}")
  public MenuItemDto updateMenuItem(
    @PathVariable long id,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestBody UpdateMenuItemRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    if (!Objects.equals(c.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }

    if (req.categoryId != null && !Objects.equals(req.categoryId, it.categoryId)) {
      MenuCategory c2 = categoryRepo.findById(req.categoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
      if (!Objects.equals(c2.tenantId, b.tenantId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
      }
      it.categoryId = req.categoryId;
    }
    if (req.nameRu != null) it.nameRu = req.nameRu;
    if (req.nameRo != null) it.nameRo = req.nameRo;
    if (req.nameEn != null) it.nameEn = req.nameEn;
    if (req.descriptionRu != null) it.descriptionRu = req.descriptionRu;
    if (req.descriptionRo != null) it.descriptionRo = req.descriptionRo;
    if (req.descriptionEn != null) it.descriptionEn = req.descriptionEn;
    if (req.ingredientsRu != null) it.ingredientsRu = req.ingredientsRu;
    if (req.ingredientsRo != null) it.ingredientsRo = req.ingredientsRo;
    if (req.ingredientsEn != null) it.ingredientsEn = req.ingredientsEn;
    if (req.allergens != null) it.allergens = req.allergens;
    if (req.weight != null) it.weight = req.weight;
    if (req.tags != null) it.tags = req.tags;
    if (req.photoUrls != null) it.photoUrls = sanitizePhotoUrls(req.photoUrls);
    if (req.kcal != null) it.kcal = req.kcal;
    if (req.proteinG != null) it.proteinG = req.proteinG;
    if (req.fatG != null) it.fatG = req.fatG;
    if (req.carbsG != null) it.carbsG = req.carbsG;
    if (req.priceCents != null) it.priceCents = req.priceCents;
    if (req.currency != null && !req.currency.isBlank()) {
      String code = req.currency.trim().toUpperCase(Locale.ROOT);
      md.virtualwaiter.domain.Currency cur = currencyRepo.findById(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
      if (!cur.isActive) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is inactive");
      }
      it.currency = code;
    }
    if (req.isActive != null || req.isStopList != null) {
      BranchMenuItemOverride existingOverride = menuItemOverrideRepo.findByBranchIdAndMenuItemId(b.id, it.id).orElse(null);
      boolean nextActive = req.isActive == null ? resolveMenuItemActive(it, existingOverride) : req.isActive;
      boolean nextStop = req.isStopList == null ? resolveMenuItemStopList(it, existingOverride) : req.isStopList;
      upsertMenuItemOverride(b.id, it.id, nextActive, nextStop);
    }

    it = itemRepo.save(it);
    auditService.log(u, "UPDATE", "MenuItem", it.id, null);
    BranchMenuItemOverride o = menuItemOverrideRepo.findByBranchIdAndMenuItemId(b.id, it.id).orElse(null);
    return toDto(it, o);
  }

  @DeleteMapping("/menu/items/{id}")
  public void deleteMenuItem(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(c.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    itemRepo.delete(it);
    auditService.log(u, "DELETE", "MenuItem", it.id, null);
  }

  private static MenuItemDto toDto(MenuItem it, BranchMenuItemOverride override) {
    boolean active = resolveMenuItemActive(it, override);
    boolean stop = resolveMenuItemStopList(it, override);
    return new MenuItemDto(
      it.id,
      it.categoryId,
      it.nameRu,
      it.nameRo,
      it.nameEn,
      it.descriptionRu,
      it.descriptionRo,
      it.descriptionEn,
      it.ingredientsRu,
      it.ingredientsRo,
      it.ingredientsEn,
      it.allergens,
      it.weight,
      it.tags,
      it.photoUrls,
      it.kcal,
      it.proteinG,
      it.fatG,
      it.carbsG,
      it.priceCents,
      it.currency,
      active,
      stop
    );
  }

  private static boolean resolveMenuItemActive(MenuItem it, BranchMenuItemOverride override) {
    return override != null ? override.isActive : it.isActive;
  }

  private static boolean resolveMenuItemStopList(MenuItem it, BranchMenuItemOverride override) {
    return override != null ? override.isStopList : it.isStopList;
  }

  private Map<Long, BranchMenuItemOverride> loadMenuItemOverrides(long branchId, List<Long> itemIds) {
    if (itemIds.isEmpty()) return Map.of();
    List<BranchMenuItemOverride> overrides = menuItemOverrideRepo.findByBranchIdAndMenuItemIdIn(branchId, itemIds);
    Map<Long, BranchMenuItemOverride> out = new HashMap<>();
    for (BranchMenuItemOverride o : overrides) out.put(o.menuItemId, o);
    return out;
  }

  private void upsertMenuItemOverride(long branchId, long menuItemId, boolean isActive, boolean isStopList) {
    BranchMenuItemOverride o = menuItemOverrideRepo.findByBranchIdAndMenuItemId(branchId, menuItemId)
      .orElseGet(() -> {
        BranchMenuItemOverride next = new BranchMenuItemOverride();
        next.branchId = branchId;
        next.menuItemId = menuItemId;
        return next;
      });
    o.isActive = isActive;
    o.isStopList = isStopList;
    o.updatedAt = Instant.now();
    menuItemOverrideRepo.save(o);
  }

  // --- Tables ---
  public record TableDto(
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
  public record CreateTableRequest(@NotNull Integer number, String publicId, Long assignedWaiterId, Long hallId) {}
  public record UpdateTableRequest(
    Integer number,
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

  @GetMapping("/tables")
  public List<TableDto> listTables(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<CafeTable> tables = tableRepo.findByBranchId(bid);
    List<TableDto> out = new ArrayList<>();
    for (CafeTable t : tables) {
      out.add(new TableDto(
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

  @PostMapping("/tables")
  public TableDto createTable(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody CreateTableRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    CafeTable t = new CafeTable();
    t.branchId = bid;
    t.number = req.number;
    t.publicId = (req.publicId == null || req.publicId.isBlank()) ? generatePublicId() : req.publicId.trim();
    t.assignedWaiterId = req.assignedWaiterId;
    t.hallId = req.hallId;
    t = tableRepo.save(t);
    auditService.log(u, "CREATE", "CafeTable", t.id, null);
    return new TableDto(
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
    );
  }

  @PatchMapping("/tables/{id}")
  public TableDto updateTable(@PathVariable long id, @RequestBody UpdateTableRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    CafeTable t = tableRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    requireBranchAccess(u, t.branchId);
    if (req.number != null) t.number = req.number;
    if (req.publicId != null && !req.publicId.isBlank()) t.publicId = req.publicId.trim();
    if (req.assignedWaiterId != null) t.assignedWaiterId = req.assignedWaiterId;
    if (req.hallId != null) t.hallId = req.hallId;
    if (req.layoutX != null || req.layoutY != null || req.layoutW != null || req.layoutH != null) {
      Double nx = req.layoutX != null ? req.layoutX : t.layoutX;
      Double ny = req.layoutY != null ? req.layoutY : t.layoutY;
      Double nw = req.layoutW != null ? req.layoutW : t.layoutW;
      Double nh = req.layoutH != null ? req.layoutH : t.layoutH;
      validateLayoutBounds(nx, ny, nw, nh);
    }
    if (req.layoutX != null) t.layoutX = req.layoutX;
    if (req.layoutY != null) t.layoutY = req.layoutY;
    if (req.layoutW != null) t.layoutW = req.layoutW;
    if (req.layoutH != null) t.layoutH = req.layoutH;
    if (req.layoutShape != null) t.layoutShape = req.layoutShape;
    if (req.layoutRotation != null) t.layoutRotation = req.layoutRotation;
    if (req.layoutZone != null) t.layoutZone = req.layoutZone;
    t = tableRepo.save(t);
    auditService.log(u, "UPDATE", "CafeTable", t.id, null);
    return new TableDto(
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
    );
  }

  public record TableLayoutItem(
    @NotNull Long id,
    Long hallId,
    Double layoutX,
    Double layoutY,
    Double layoutW,
    Double layoutH,
    String layoutShape,
    Integer layoutRotation,
    String layoutZone
  ) {}
  public record UpdateTableLayoutRequest(@NotNull List<TableLayoutItem> tables) {}

  @PostMapping("/tables/layout")
  public Map<String, Object> updateTableLayout(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody UpdateTableLayoutRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    if (req.tables == null || req.tables.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tables required");
    }
    int updated = 0;
    for (TableLayoutItem item : req.tables) {
      CafeTable t = tableRepo.findById(item.id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
      if (!Objects.equals(t.branchId, bid)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
      if (item.layoutX != null || item.layoutY != null || item.layoutW != null || item.layoutH != null) {
        Double nx = item.layoutX != null ? item.layoutX : t.layoutX;
        Double ny = item.layoutY != null ? item.layoutY : t.layoutY;
        Double nw = item.layoutW != null ? item.layoutW : t.layoutW;
        Double nh = item.layoutH != null ? item.layoutH : t.layoutH;
        validateLayoutBounds(nx, ny, nw, nh);
      }
      if (item.layoutX != null) t.layoutX = item.layoutX;
      if (item.layoutY != null) t.layoutY = item.layoutY;
      if (item.layoutW != null) t.layoutW = item.layoutW;
      if (item.layoutH != null) t.layoutH = item.layoutH;
      if (item.layoutShape != null) t.layoutShape = item.layoutShape;
      if (item.layoutRotation != null) t.layoutRotation = item.layoutRotation;
      if (item.layoutZone != null) t.layoutZone = item.layoutZone;
      if (item.hallId != null) t.hallId = item.hallId;
      tableRepo.save(t);
      updated++;
    }
    auditService.log(u, "UPDATE", "CafeTableLayout", (long) updated, null);
    return Map.of("updated", updated);
  }

  public record BulkAssignHallRequest(@NotNull List<Long> tableIds, Long hallId) {}

  @PostMapping("/tables/bulk-assign-hall")
  public Map<String, Object> bulkAssignHall(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody BulkAssignHallRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    if (req.tableIds == null || req.tableIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tableIds required");
    }
    int updated = 0;
    for (Long id : req.tableIds) {
      CafeTable t = tableRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
      if (!Objects.equals(t.branchId, bid)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
      t.hallId = req.hallId;
      tableRepo.save(t);
      updated++;
    }
    auditService.log(u, "UPDATE", "CafeTable", (long) updated, "bulkAssignHall");
    return Map.of("updated", updated);
  }

  @DeleteMapping("/tables/{id}")
  public void deleteTable(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    CafeTable t = tableRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    requireBranchAccess(u, t.branchId);
    tableRepo.delete(t);
    auditService.log(u, "DELETE", "CafeTable", t.id, null);
  }

  public record SignedTableUrlResponse(String tablePublicId, String sig, long ts, String url) {}

  @GetMapping("/tables/{tablePublicId}/signed-url")
  public SignedTableUrlResponse getSignedTableUrl(@PathVariable String tablePublicId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    CafeTable table = tableRepo.findByPublicId(tablePublicId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    if (!isSuperAdmin(u) && !Objects.equals(table.branchId, u.branchId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
    }
    long ts = java.time.Instant.now().getEpochSecond();
    String sig = qrSig.signTablePublicId(tablePublicId, ts);
    String url = publicBaseUrl + "/t/" + table.publicId + "?sig=" + sig + "&ts=" + ts;
    return new SignedTableUrlResponse(table.publicId, sig, ts, url);
  }

  // --- Staff users ---
  public record StaffUserDto(
    long id,
    Long branchId,
    String username,
    String role,
    boolean isActive,
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
  public record CreateStaffUserRequest(
    @NotBlank String username,
    @NotBlank String password,
    @NotBlank String role,
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
  public record UpdateStaffUserRequest(
    String password,
    String role,
    Boolean isActive,
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
  public record BulkStaffUpdateRequest(
    List<Long> ids,
    UpdateStaffUserRequest patch
  ) {}
  public record BulkStaffUpdateResponse(int updated) {}

  public record StaffReviewDto(
    long id,
    long staffUserId,
    String staffUsername,
    Integer tableNumber,
    long guestSessionId,
    int rating,
    String comment,
    String createdAt
  ) {}

  public record BranchReviewDto(
    long id,
    long guestSessionId,
    int rating,
    String comment,
    String createdAt
  ) {}

  public record BranchReviewSummary(double avgRating, long count) {}

  public record ChatExportRow(
    long id,
    long guestSessionId,
    long tableId,
    Integer tableNumber,
    String senderRole,
    Long staffUserId,
    String message,
    String createdAt
  ) {}

  @GetMapping("/staff")
  public List<StaffUserDto> listStaff(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<StaffUser> users = staffUserRepo.findByBranchId(bid);
    List<StaffUserDto> out = new ArrayList<>();
    for (StaffUser su : users) {
      out.add(new StaffUserDto(
        su.id, su.branchId, su.username, su.role, su.isActive,
        su.firstName, su.lastName, su.age, su.gender, su.photoUrl,
        su.rating, su.recommended, su.experienceYears, su.favoriteItems
      ));
    }
    return out;
  }

  @PostMapping("/staff")
  public StaffUserDto createStaff(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody CreateStaffUserRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    String role = req.role.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("WAITER", "KITCHEN", "ADMIN", "HOST", "BAR", "MANAGER", "OWNER").contains(role)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
    }
    StaffUser su = new StaffUser();
    su.branchId = bid;
    su.username = req.username.trim();
    su.passwordHash = passwordEncoder.encode(req.password);
    su.role = role;
    su.isActive = true;
    su.firstName = trimOrNull(req.firstName);
    su.lastName = trimOrNull(req.lastName);
    su.age = sanitizeAge(req.age);
    su.gender = sanitizeGender(req.gender);
    su.photoUrl = sanitizePhotoUrl(req.photoUrl);
    su.rating = sanitizeRating(req.rating);
    su.recommended = req.recommended != null ? req.recommended : Boolean.FALSE;
    su.experienceYears = sanitizeExperienceYears(req.experienceYears);
    su.favoriteItems = sanitizeFavoriteItems(req.favoriteItems);
    su = staffUserRepo.save(su);
    auditService.log(u, "CREATE", "StaffUser", su.id, null);
    return new StaffUserDto(
      su.id, su.branchId, su.username, su.role, su.isActive,
      su.firstName, su.lastName, su.age, su.gender, su.photoUrl,
      su.rating, su.recommended, su.experienceYears, su.favoriteItems
    );
  }

  @PatchMapping("/staff/{id}")
  public StaffUserDto updateStaff(@PathVariable long id, @RequestBody UpdateStaffUserRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    requireBranchAccess(u, su.branchId);
    applyStaffPatch(su, req);
    su = staffUserRepo.save(su);
    auditService.log(u, "UPDATE", "StaffUser", su.id, null);
    return new StaffUserDto(
      su.id, su.branchId, su.username, su.role, su.isActive,
      su.firstName, su.lastName, su.age, su.gender, su.photoUrl,
      su.rating, su.recommended, su.experienceYears, su.favoriteItems
    );
  }

  @PostMapping("/staff/bulk")
  @Transactional
  public BulkStaffUpdateResponse bulkUpdateStaff(@RequestBody BulkStaffUpdateRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    if (req == null || req.ids == null || req.ids.isEmpty() || req.patch == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids and patch are required");
    }
    int updated = 0;
    for (Long id : req.ids) {
      if (id == null) continue;
      StaffUser su = staffUserRepo.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
      requireBranchAccess(u, su.branchId);
      applyStaffPatch(su, req.patch);
      staffUserRepo.save(su);
      updated++;
    }
    if (updated > 0) {
      auditService.log(u, "BULK_UPDATE", "StaffUser", null, "{\"updated\":" + updated + "}");
    }
    return new BulkStaffUpdateResponse(updated);
  }

  @DeleteMapping("/staff/{id}")
  public void deleteStaff(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    requireBranchAccess(u, su.branchId);
    int cleared = tableRepo.clearAssignedWaiter(su.id);
    staffUserRepo.delete(su);
    if (cleared > 0) {
      auditService.log(u, "UNASSIGN_WAITER", "CafeTable", null, "{\"clearedTables\":" + cleared + ",\"waiterId\":" + su.id + "}");
    }
    auditService.log(u, "DELETE", "StaffUser", su.id, null);
  }

  @GetMapping("/staff-reviews")
  public List<StaffReviewDto> listStaffReviews(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "staffUserId", required = false) Long staffUserId,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    int lim = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
    List<StaffReview> reviews = (staffUserId != null)
      ? staffReviewRepo.findByBranchIdAndStaffUserIdOrderByIdDesc(bid, staffUserId)
      : staffReviewRepo.findByBranchIdOrderByIdDesc(bid);
    if (reviews.size() > lim) {
      reviews = reviews.subList(0, lim);
    }
    Set<Long> staffIds = new HashSet<>();
    Set<Long> tableIds = new HashSet<>();
    for (StaffReview r : reviews) {
      staffIds.add(r.staffUserId);
      tableIds.add(r.tableId);
    }
    Map<Long, StaffUser> staffById = new HashMap<>();
    if (!staffIds.isEmpty()) {
      staffUserRepo.findAllById(staffIds).forEach(s -> staffById.put(s.id, s));
    }
    Map<Long, CafeTable> tablesById = new HashMap<>();
    if (!tableIds.isEmpty()) {
      tableRepo.findAllById(tableIds).forEach(t -> tablesById.put(t.id, t));
    }
    List<StaffReviewDto> out = new ArrayList<>();
    for (StaffReview r : reviews) {
      StaffUser su = staffById.get(r.staffUserId);
      CafeTable t = tablesById.get(r.tableId);
      out.add(new StaffReviewDto(
        r.id,
        r.staffUserId,
        su != null ? su.username : ("#" + r.staffUserId),
        t != null ? t.number : null,
        r.guestSessionId,
        r.rating != null ? r.rating : 0,
        r.comment,
        r.createdAt != null ? r.createdAt.toString() : null
      ));
    }
    return out;
  }

  @GetMapping("/branch-reviews")
  public List<BranchReviewDto> listBranchReviews(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    int lim = limit == null ? 50 : Math.min(Math.max(limit, 1), 200);
    List<BranchReview> reviews = branchReviewRepo.findByBranchIdOrderByIdDesc(bid);
    reviews = filterBranchReviews(reviews, tableId, hallId, null, null);
    if (reviews.size() > lim) reviews = reviews.subList(0, lim);
    List<BranchReviewDto> out = new ArrayList<>();
    for (BranchReview r : reviews) {
      out.add(new BranchReviewDto(
        r.id,
        r.guestSessionId,
        r.rating != null ? r.rating : 0,
        r.comment,
        r.createdAt != null ? r.createdAt.toString() : null
      ));
    }
    return out;
  }

  @GetMapping("/branch-reviews/summary")
  public BranchReviewSummary branchReviewSummary(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    List<BranchReview> reviews = branchReviewRepo.findByBranchIdOrderByIdDesc(bid);
    reviews = filterBranchReviews(reviews, tableId, hallId, null, null);
    if (reviews.isEmpty()) return new BranchReviewSummary(0.0, 0);
    double sum = 0.0;
    long count = 0;
    for (BranchReview r : reviews) {
      if (r.rating != null) {
        sum += r.rating;
        count++;
      }
    }
    double avg = count == 0 ? 0.0 : (sum / count);
    return new BranchReviewSummary(avg, count);
  }

  @GetMapping("/branch-reviews/export.csv")
  public ResponseEntity<String> exportBranchReviewsCsv(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<BranchReview> reviews = branchReviewRepo.findByBranchIdOrderByIdDesc(bid);
    List<BranchReview> filtered = filterBranchReviews(reviews, tableId, hallId, fromTs, toTs);
    StringBuilder sb = new StringBuilder();
    sb.append("id,guest_session_id,rating,comment,created_at\n");
    for (BranchReview r : filtered) {
      sb.append(r.id).append(',')
        .append(r.guestSessionId).append(',')
        .append(r.rating != null ? r.rating : 0).append(',')
        .append('"').append((r.comment == null ? "" : r.comment).replace("\"", "\"\"")).append('"').append(',')
        .append(r.createdAt != null ? r.createdAt.toString() : "")
        .append('\n');
    }
    String filename = "branch-reviews-" + bid + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
  }

  private List<BranchReview> filterBranchReviews(
    List<BranchReview> reviews,
    Long tableId,
    Long hallId,
    Instant fromTs,
    Instant toTs
  ) {
    if ((tableId == null && hallId == null && fromTs == null && toTs == null) || reviews.isEmpty()) {
      return reviews;
    }
    Set<Long> sessionIds = new HashSet<>();
    for (BranchReview r : reviews) {
      if (r.guestSessionId != null) sessionIds.add(r.guestSessionId);
    }
    Map<Long, GuestSession> sessionsById = new HashMap<>();
    if (!sessionIds.isEmpty()) {
      guestSessionRepo.findAllById(sessionIds).forEach(s -> sessionsById.put(s.id, s));
    }
    Set<Long> tableIds = new HashSet<>();
    for (GuestSession s : sessionsById.values()) {
      if (s.tableId != null) tableIds.add(s.tableId);
    }
    Map<Long, CafeTable> tablesById = new HashMap<>();
    if (!tableIds.isEmpty()) {
      tableRepo.findAllById(tableIds).forEach(t -> tablesById.put(t.id, t));
    }
    List<BranchReview> out = new ArrayList<>();
    for (BranchReview r : reviews) {
      if (fromTs != null || toTs != null) {
        if (r.createdAt == null) continue;
        if (fromTs != null && r.createdAt.isBefore(fromTs)) continue;
        if (toTs != null && r.createdAt.isAfter(toTs)) continue;
      }
      GuestSession s = sessionsById.get(r.guestSessionId);
      if (s == null) continue;
      if (tableId != null && !tableId.equals(s.tableId)) continue;
      if (hallId != null) {
        CafeTable t = tablesById.get(s.tableId);
        if (t == null || t.hallId == null || !t.hallId.equals(hallId)) continue;
      }
      out.add(r);
    }
    return out;
  }

  @GetMapping("/chat/export.csv")
  public ResponseEntity<String> exportChatCsv(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<ChatMessage> msgs = chatMessageRepo.findByBranchIdAndCreatedAtBetweenOrderByIdAsc(bid, fromTs, toTs);
    Map<Long, CafeTable> tableById = new HashMap<>();
    for (ChatMessage m : msgs) {
      if (!tableById.containsKey(m.tableId)) {
        tableRepo.findById(m.tableId).ifPresent(t -> tableById.put(t.id, t));
      }
    }
    if (waiterId != null) {
      msgs = msgs.stream().filter(m -> {
        if (m.staffUserId != null && Objects.equals(m.staffUserId, waiterId)) return true;
        CafeTable t = tableById.get(m.tableId);
        return t != null && Objects.equals(t.assignedWaiterId, waiterId);
      }).toList();
    }
    StringBuilder sb = new StringBuilder();
    sb.append("id,guest_session_id,table_id,table_number,sender_role,staff_user_id,message,created_at\n");
    for (ChatMessage m : msgs) {
      CafeTable t = tableById.get(m.tableId);
      sb.append(m.id).append(',')
        .append(m.guestSessionId).append(',')
        .append(m.tableId).append(',')
        .append(t != null ? t.number : "").append(',')
        .append(m.senderRole).append(',')
        .append(m.staffUserId != null ? m.staffUserId : "").append(',')
        .append('"').append(m.message.replace("\"", "\"\"")).append('"').append(',')
        .append(m.createdAt != null ? m.createdAt.toString() : "")
        .append('\n');
    }
    String filename = "chat-export-" + bid + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
  }

  private static String generatePublicId() {
    return "TBL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
  }

  private static String toCsv(List<Integer> vals) {
    if (vals == null || vals.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < vals.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append(vals.get(i));
    }
    return sb.toString();
  }

  // --- Modifiers ---
  public record ModifierGroupDto(long id, String nameRu, String nameRo, String nameEn, boolean isActive) {}
  public record CreateModifierGroupRequest(@NotBlank String nameRu, String nameRo, String nameEn, Boolean isActive) {}
  public record UpdateModifierGroupRequest(String nameRu, String nameRo, String nameEn, Boolean isActive) {}

  @GetMapping("/modifier-groups")
  public List<ModifierGroupDto> listModifierGroups(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    List<ModifierGroup> groups = modifierGroupRepo.findByTenantIdOrderByIdAsc(b.tenantId);
    List<ModifierGroupDto> out = new ArrayList<>();
    for (ModifierGroup g : groups) {
      out.add(new ModifierGroupDto(g.id, g.nameRu, g.nameRo, g.nameEn, g.isActive));
    }
    return out;
  }

  @PostMapping("/modifier-groups")
  public ModifierGroupDto createModifierGroup(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody CreateModifierGroupRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    ModifierGroup g = new ModifierGroup();
    g.tenantId = b.tenantId;
    g.nameRu = req.nameRu;
    g.nameRo = req.nameRo;
    g.nameEn = req.nameEn;
    g.isActive = req.isActive == null || req.isActive;
    g = modifierGroupRepo.save(g);
    return new ModifierGroupDto(g.id, g.nameRu, g.nameRo, g.nameEn, g.isActive);
  }

  @PatchMapping("/modifier-groups/{id}")
  public ModifierGroupDto updateModifierGroup(@PathVariable long id, @RequestBody UpdateModifierGroupRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    ModifierGroup g = modifierGroupRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(g.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    if (req.nameRu != null) g.nameRu = req.nameRu;
    if (req.nameRo != null) g.nameRo = req.nameRo;
    if (req.nameEn != null) g.nameEn = req.nameEn;
    if (req.isActive != null) g.isActive = req.isActive;
    g = modifierGroupRepo.save(g);
    return new ModifierGroupDto(g.id, g.nameRu, g.nameRo, g.nameEn, g.isActive);
  }

  @DeleteMapping("/modifier-groups/{id}")
  public void deleteModifierGroup(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    ModifierGroup g = modifierGroupRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(g.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    modifierGroupRepo.delete(g);
  }

  public record ModifierOptionDto(long id, long groupId, String nameRu, String nameRo, String nameEn, int priceCents, boolean isActive) {}
  public record CreateModifierOptionRequest(@NotBlank String nameRu, String nameRo, String nameEn, Integer priceCents, Boolean isActive) {}
  public record UpdateModifierOptionRequest(String nameRu, String nameRo, String nameEn, Integer priceCents, Boolean isActive) {}

  @GetMapping("/modifier-options")
  public List<ModifierOptionDto> listModifierOptions(@RequestParam("groupId") long groupId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    ModifierGroup g = modifierGroupRepo.findById(groupId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(g.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    List<ModifierOption> list = modifierOptionRepo.findByGroupId(groupId);
    List<ModifierOptionDto> out = new ArrayList<>();
    for (ModifierOption o : list) {
      out.add(new ModifierOptionDto(o.id, o.groupId, o.nameRu, o.nameRo, o.nameEn, o.priceCents, o.isActive));
    }
    return out;
  }

  @PostMapping("/modifier-options")
  public ModifierOptionDto createModifierOption(@RequestParam("groupId") long groupId, @Valid @RequestBody CreateModifierOptionRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    ModifierGroup g = modifierGroupRepo.findById(groupId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(g.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    ModifierOption o = new ModifierOption();
    o.groupId = groupId;
    o.nameRu = req.nameRu;
    o.nameRo = req.nameRo;
    o.nameEn = req.nameEn;
    o.priceCents = req.priceCents == null ? 0 : req.priceCents;
    o.isActive = req.isActive == null || req.isActive;
    o = modifierOptionRepo.save(o);
    return new ModifierOptionDto(o.id, o.groupId, o.nameRu, o.nameRo, o.nameEn, o.priceCents, o.isActive);
  }

  @PatchMapping("/modifier-options/{id}")
  public ModifierOptionDto updateModifierOption(@PathVariable long id, @RequestBody UpdateModifierOptionRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    ModifierOption o = modifierOptionRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier option not found"));
    ModifierGroup g = modifierGroupRepo.findById(o.groupId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(g.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    if (req.nameRu != null) o.nameRu = req.nameRu;
    if (req.nameRo != null) o.nameRo = req.nameRo;
    if (req.nameEn != null) o.nameEn = req.nameEn;
    if (req.priceCents != null) o.priceCents = req.priceCents;
    if (req.isActive != null) o.isActive = req.isActive;
    o = modifierOptionRepo.save(o);
    return new ModifierOptionDto(o.id, o.groupId, o.nameRu, o.nameRo, o.nameEn, o.priceCents, o.isActive);
  }

  @DeleteMapping("/modifier-options/{id}")
  public void deleteModifierOption(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    ModifierOption o = modifierOptionRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier option not found"));
    ModifierGroup g = modifierGroupRepo.findById(o.groupId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(g.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    modifierOptionRepo.delete(o);
  }

  public record ItemModifierGroupDto(long groupId, boolean isRequired, Integer minSelect, Integer maxSelect, int sortOrder) {}
  public record UpdateItemModifierGroupsRequest(List<ItemModifierGroupDto> groups) {}

  @GetMapping("/menu/items/{id}/modifier-groups")
  public List<ItemModifierGroupDto> getItemModifierGroups(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(c.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }
    List<MenuItemModifierGroup> list = menuItemModifierGroupRepo.findByMenuItemIdOrderBySortOrderAscIdAsc(id);
    List<ItemModifierGroupDto> out = new ArrayList<>();
    for (MenuItemModifierGroup mg : list) {
      out.add(new ItemModifierGroupDto(mg.groupId, mg.isRequired, mg.minSelect, mg.maxSelect, mg.sortOrder));
    }
    return out;
  }

  @PutMapping("/menu/items/{id}/modifier-groups")
  public List<ItemModifierGroupDto> updateItemModifierGroups(
    @PathVariable long id,
    @Valid @RequestBody UpdateItemModifierGroupsRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, null);
    Branch b = branchRepo.findById(bid)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, b.id);
    if (!Objects.equals(c.tenantId, b.tenantId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
    }

    menuItemModifierGroupRepo.deleteByMenuItemId(id);
    List<ItemModifierGroupDto> out = new ArrayList<>();
    if (req.groups != null) {
      for (ItemModifierGroupDto g : req.groups) {
        ModifierGroup mg = modifierGroupRepo.findById(g.groupId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
        if (!Objects.equals(mg.tenantId, b.tenantId)) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong tenant");
        }
        MenuItemModifierGroup link = new MenuItemModifierGroup();
        link.menuItemId = id;
        link.groupId = g.groupId;
        link.isRequired = g.isRequired;
        link.minSelect = g.minSelect;
        link.maxSelect = g.maxSelect;
        link.sortOrder = g.sortOrder;
        menuItemModifierGroupRepo.save(link);
        out.add(new ItemModifierGroupDto(link.groupId, link.isRequired, link.minSelect, link.maxSelect, link.sortOrder));
      }
    }
    return out;
  }

  // --- Stats ---
  public record StatsSummaryResponse(
    String from,
    String to,
    long ordersCount,
    long callsCount,
    long paidBillsCount,
    long grossCents,
    long tipsCents,
    long activeTablesCount,
    long avgCheckCents,
    Double avgSlaMinutes,
    double avgBranchRating,
    long branchReviewsCount
  ) {}

  public record WaiterMotivationRow(
    long staffUserId,
    String username,
    long ordersCount,
    long tipsCents,
    Double avgSlaMinutes
  ) {}

  public record StatsDailyRow(
    String day,
    long ordersCount,
    long callsCount,
    long paidBillsCount,
    long grossCents,
    long tipsCents
  ) {}

  @GetMapping("/stats/daily.csv")
  public ResponseEntity<String> getDailyCsv(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    hallId = resolveHallIdFromPlan(u, hallId, planId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (waiterId != null) {
      StaffUser w = staffUserRepo.findById(waiterId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter not found"));
      requireBranchAccess(u, w.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<StatsService.DailyRow> rows = statsService.dailyForBranchFiltered(bid, fromTs, toTs, tableId, waiterId, hallId);
    StringBuilder sb = new StringBuilder();
    sb.append("day,orders,calls,paid_bills,gross_cents,tips_cents\n");
    for (StatsService.DailyRow r : rows) {
      sb.append(r.day()).append(',')
        .append(r.ordersCount()).append(',')
        .append(r.callsCount()).append(',')
        .append(r.paidBillsCount()).append(',')
        .append(r.grossCents()).append(',')
        .append(r.tipsCents()).append('\n');
    }
    String filename = "stats-daily-" + bid + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
  }

  @GetMapping("/stats/summary")
  public StatsSummaryResponse getSummary(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    hallId = resolveHallIdFromPlan(u, hallId, planId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (waiterId != null) {
      StaffUser w = staffUserRepo.findById(waiterId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter not found"));
      requireBranchAccess(u, w.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    StatsService.Summary s = statsService.summaryForBranchFiltered(
      bid,
      fromTs,
      toTs,
      tableId,
      waiterId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
    List<BranchReview> reviews = branchReviewRepo.findByBranchIdOrderByIdDesc(bid);
    reviews = filterBranchReviews(reviews, tableId, hallId, fromTs, toTs);
    double sumRating = 0.0;
    long ratingCount = 0;
    for (BranchReview r : reviews) {
      if (r.rating != null) {
        sumRating += r.rating;
        ratingCount++;
      }
    }
    double avgRating = ratingCount == 0 ? 0.0 : (sumRating / ratingCount);
    long avgCheckCents = s.paidBillsCount() == 0 ? 0L : (s.grossCents() / s.paidBillsCount());
    return new StatsSummaryResponse(
      s.from().toString(),
      s.to().toString(),
      s.ordersCount(),
      s.callsCount(),
      s.paidBillsCount(),
      s.grossCents(),
      s.tipsCents(),
      s.activeTablesCount(),
      avgCheckCents,
      s.avgSlaMinutes(),
      avgRating,
      ratingCount
    );
  }

  @GetMapping("/stats/daily")
  public List<StatsDailyRow> getDaily(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    hallId = resolveHallIdFromPlan(u, hallId, planId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (waiterId != null) {
      StaffUser w = staffUserRepo.findById(waiterId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter not found"));
      requireBranchAccess(u, w.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    List<StatsService.DailyRow> rows = statsService.dailyForBranchFiltered(
      bid,
      fromTs,
      toTs,
      tableId,
      waiterId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
    List<StatsDailyRow> out = new ArrayList<>();
    for (StatsService.DailyRow r : rows) {
      out.add(new StatsDailyRow(r.day(), r.ordersCount(), r.callsCount(), r.paidBillsCount(), r.grossCents(), r.tipsCents()));
    }
    return out;
  }

  @GetMapping("/stats/waiters-motivation")
  public List<WaiterMotivationRow> getWaiterMotivation(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    hallId = resolveHallIdFromPlan(u, hallId, planId);
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    List<StatsService.WaiterMotivationRow> rows = statsService.waiterMotivationForBranch(
      bid,
      fromTs,
      toTs,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
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

  public record TopItemRow(long menuItemId, String name, long qty, long grossCents) {}
  public record TopCategoryRow(long categoryId, String name, long qty, long grossCents) {}

  @GetMapping("/stats/top-items")
  public List<TopItemRow> topItems(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    hallId = resolveHallIdFromPlan(u, hallId, planId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (waiterId != null) {
      StaffUser w = staffUserRepo.findById(waiterId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter not found"));
      requireBranchAccess(u, w.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    int lim = limit == null ? 10 : limit;
    List<StatsService.TopItemRow> rows = statsService.topItemsForBranch(
      bid,
      fromTs,
      toTs,
      tableId,
      waiterId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs,
      lim
    );
    List<TopItemRow> out = new ArrayList<>();
    for (StatsService.TopItemRow r : rows) {
      out.add(new TopItemRow(r.menuItemId(), r.name(), r.qty(), r.grossCents()));
    }
    return out;
  }

  @GetMapping("/stats/top-categories")
  public List<TopCategoryRow> topCategories(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    hallId = resolveHallIdFromPlan(u, hallId, planId);
    if (tableId != null) {
      CafeTable t = tableRepo.findById(tableId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
      requireBranchAccess(u, t.branchId);
    }
    if (waiterId != null) {
      StaffUser w = staffUserRepo.findById(waiterId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter not found"));
      requireBranchAccess(u, w.branchId);
    }
    if (hallId != null) {
      BranchHall h = hallRepo.findById(hallId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
      if (!h.branchId.equals(bid)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall does not belong to branch");
      }
      requireBranchAccess(u, h.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    int lim = limit == null ? 10 : limit;
    List<StatsService.TopCategoryRow> rows = statsService.topCategoriesForBranch(
      bid,
      fromTs,
      toTs,
      tableId,
      waiterId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs,
      lim
    );
    List<TopCategoryRow> out = new ArrayList<>();
    for (StatsService.TopCategoryRow r : rows) {
      out.add(new TopCategoryRow(r.categoryId(), r.name(), r.qty(), r.grossCents()));
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

  // --- Audit logs ---
  public record AuditLogDto(
    long id,
    String createdAt,
    Long actorUserId,
    String actorUsername,
    String actorRole,
    Long branchId,
    String action,
    String entityType,
    Long entityId,
    String detailsJson
  ) {}

  @GetMapping("/audit-logs")
  public List<AuditLogDto> listAuditLogs(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "action", required = false) String action,
    @RequestParam(value = "entityType", required = false) String entityType,
    @RequestParam(value = "actorUsername", required = false) String actorUsername,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "beforeId", required = false) Long beforeId,
    @RequestParam(value = "afterId", required = false) Long afterId,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    String actionVal = action == null || action.isBlank() ? null : action.trim();
    String entityTypeVal = entityType == null || entityType.isBlank() ? null : entityType.trim();
    String actorVal = actorUsername == null || actorUsername.isBlank() ? null : actorUsername.trim();
    Instant fromTs = parseInstantOrDateOrNull(from, true);
    Instant toTs = parseInstantOrDateOrNull(to, false);
    int lim = limit == null ? 200 : Math.max(1, Math.min(limit, 500));
    List<AuditLog> logs = auditLogRepo.findFiltered(
      bid,
      actionVal,
      entityTypeVal,
      actorVal,
      fromTs,
      toTs,
      beforeId,
      afterId,
      PageRequest.of(0, lim)
    );
    List<AuditLogDto> out = new ArrayList<>();
    for (AuditLog a : logs) {
      out.add(new AuditLogDto(
        a.id,
        a.createdAt.toString(),
        a.actorUserId,
        a.actorUsername,
        a.actorRole,
        a.branchId,
        a.action,
        a.entityType,
        a.entityId,
        a.detailsJson
      ));
    }
    return out;
  }

  @GetMapping("/audit-logs.csv")
  public ResponseEntity<String> exportAuditLogsCsv(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "action", required = false) String action,
    @RequestParam(value = "entityType", required = false) String entityType,
    @RequestParam(value = "actorUsername", required = false) String actorUsername,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "beforeId", required = false) Long beforeId,
    @RequestParam(value = "afterId", required = false) Long afterId,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    String actionVal = action == null || action.isBlank() ? null : action.trim();
    String entityTypeVal = entityType == null || entityType.isBlank() ? null : entityType.trim();
    String actorVal = actorUsername == null || actorUsername.isBlank() ? null : actorUsername.trim();
    Instant fromTs = parseInstantOrDateOrNull(from, true);
    Instant toTs = parseInstantOrDateOrNull(to, false);
    int lim = limit == null ? 1000 : Math.max(1, Math.min(limit, 5000));
    List<AuditLog> logs = auditLogRepo.findFiltered(
      bid,
      actionVal,
      entityTypeVal,
      actorVal,
      fromTs,
      toTs,
      beforeId,
      afterId,
      PageRequest.of(0, lim)
    );
    StringBuilder sb = new StringBuilder();
    sb.append("id,created_at,actor_user_id,actor_username,actor_role,branch_id,action,entity_type,entity_id,details_json\n");
    for (AuditLog a : logs) {
      sb.append(a.id).append(',')
        .append(a.createdAt).append(',')
        .append(a.actorUserId == null ? "" : a.actorUserId).append(',')
        .append(csv(a.actorUsername)).append(',')
        .append(csv(a.actorRole)).append(',')
        .append(a.branchId == null ? "" : a.branchId).append(',')
        .append(csv(a.action)).append(',')
        .append(csv(a.entityType)).append(',')
        .append(a.entityId == null ? "" : a.entityId).append(',')
        .append(csv(a.detailsJson)).append('\n');
    }
    String filename = "audit-logs-" + bid + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
  }

  private static String csv(String v) {
    if (v == null) return "";
    String s = v.replace("\"", "\"\"");
    if (s.contains(",") || s.contains("\n") || s.contains("\r")) {
      return "\"" + s + "\"";
    }
    return s;
  }

  private static String normalizeLocale(String raw) {
    if (raw == null) return "ru";
    String v = raw.trim().toLowerCase(Locale.ROOT);
    if (v.startsWith("ro")) return "ro";
    if (v.startsWith("en")) return "en";
    return "ru";
  }

  private static String trimOrNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private static Set<String> parseExts(String raw) {
    if (raw == null) return Set.of();
    Set<String> out = new HashSet<>();
    for (String p : raw.split(",")) {
      String t = p.trim().toLowerCase(Locale.ROOT);
      if (!t.isEmpty()) out.add(t);
    }
    return out;
  }

  private String sanitizePhotoUrl(String raw) {
    String url = trimOrNull(raw);
    if (url == null) return null;
    if (maxPhotoUrlLength > 0 && url.length() > maxPhotoUrlLength) {
      logReject("Photo URL too long", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo URL too long");
    }
    if (url.startsWith("/media/")) {
      validatePhotoPath(url, url);
      return url;
    }
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      logReject("Invalid photo URL", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid photo URL");
    }
    String scheme = uri.getScheme();
    if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
      logReject("Photo URL must be http/https", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo URL must be http/https");
    }
    if (uri.getHost() == null || uri.getHost().isBlank()) {
      logReject("Photo URL host is required", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo URL host is required");
    }
    String path = uri.getPath();
    if (path == null || path.isBlank()) {
      logReject("Photo URL path is required", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo URL path is required");
    }
    if (!mediaPublicBaseUrl.isBlank() && !url.startsWith(mediaPublicBaseUrl + "/media/")) {
      logReject("External photo URL is not allowed", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "External photo URL is not allowed");
    }
    validatePhotoPath(path, url);
    return url;
  }

  private void validatePhotoPath(String path, String url) {
    int dot = path.lastIndexOf('.');
    if (dot < 0 || dot == path.length() - 1) {
      logReject("Photo URL must include file extension", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo URL must include file extension");
    }
    String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (!allowedPhotoExts.isEmpty() && !allowedPhotoExts.contains(ext)) {
      logReject("Unsupported photo type", url);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported photo type");
    }
  }

  private String sanitizePhotoUrls(String raw) {
    String csv = trimOrNull(raw);
    if (csv == null) return null;
    String[] parts = csv.split(",");
    List<String> cleaned = new ArrayList<>();
    for (String p : parts) {
      String u = sanitizePhotoUrl(p);
      if (u != null) cleaned.add(u);
    }
    if (maxPhotoUrlsCount > 0 && cleaned.size() > maxPhotoUrlsCount) {
      logReject("Too many photo URLs", csv);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many photo URLs");
    }
    return String.join(",", cleaned);
  }

  private void logReject(String reason, String url) {
    int max = 200;
    String safe = url == null ? "" : (url.length() > max ? url.substring(0, max) + "..." : url);
    LOG.warn("Photo URL rejected: {} (url={})", reason, safe);
  }

  private static String trimTrailingSlash(String v) {
    if (v == null) return "";
    String s = v.trim();
    if (s.endsWith("/")) return s.substring(0, s.length() - 1);
    return s;
  }

  private static Integer sanitizeAge(Integer v) {
    if (v == null) return null;
    if (v < 0 || v > 120) return null;
    return v;
  }

  private static Integer sanitizeRating(Integer v) {
    if (v == null) return null;
    if (v < 0 || v > 5) return null;
    return v;
  }

  private static Integer sanitizeExperienceYears(Integer v) {
    if (v == null) return null;
    if (v < 0 || v > 80) return null;
    return v;
  }

  private static String sanitizeFavoriteItems(String v) {
    if (v == null) return null;
    String t = v.trim();
    if (t.isEmpty()) return null;
    if (t.length() > 500) t = t.substring(0, 500);
    return t;
  }

  private static String sanitizeGender(String v) {
    if (v == null) return null;
    String t = v.trim().toLowerCase(Locale.ROOT);
    return switch (t) { case "male", "female", "other" -> t; default -> null; };
  }

  private void applyStaffPatch(StaffUser su, UpdateStaffUserRequest req) {
    if (req.password != null && !req.password.isBlank()) {
      su.passwordHash = passwordEncoder.encode(req.password);
    }
    if (req.role != null) {
      String role = req.role.trim().toUpperCase(Locale.ROOT);
      if (!Set.of("WAITER", "KITCHEN", "ADMIN", "HOST", "BAR", "MANAGER", "OWNER").contains(role)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
      }
      su.role = role;
    }
    if (req.isActive != null) su.isActive = req.isActive;
    if (req.firstName != null) su.firstName = trimOrNull(req.firstName);
    if (req.lastName != null) su.lastName = trimOrNull(req.lastName);
    if (req.age != null) su.age = sanitizeAge(req.age);
    if (req.gender != null) su.gender = sanitizeGender(req.gender);
    if (req.photoUrl != null) su.photoUrl = sanitizePhotoUrl(req.photoUrl);
    if (req.rating != null) su.rating = sanitizeRating(req.rating);
    if (req.recommended != null) su.recommended = req.recommended;
    if (req.experienceYears != null) su.experienceYears = sanitizeExperienceYears(req.experienceYears);
    if (req.favoriteItems != null) su.favoriteItems = sanitizeFavoriteItems(req.favoriteItems);
  }

  public record DiscountDto(
    long id,
    long branchId,
    String scope,
    String code,
    String type,
    int value,
    String label,
    boolean active,
    Integer maxUses,
    int usedCount,
    Instant startsAt,
    Instant endsAt,
    Integer daysMask,
    Integer startMinute,
    Integer endMinute,
    Integer tzOffsetMinutes
  ) {}

  public record CreateDiscountRequest(
    Long branchId,
    String scope,
    String code,
    String type,
    Integer value,
    String label,
    Boolean active,
    Integer maxUses,
    Instant startsAt,
    Instant endsAt,
    Integer daysMask,
    Integer startMinute,
    Integer endMinute,
    Integer tzOffsetMinutes
  ) {}

  public record UpdateDiscountRequest(
    String scope,
    String code,
    String type,
    Integer value,
    String label,
    Boolean active,
    Integer maxUses,
    Integer usedCount,
    Instant startsAt,
    Instant endsAt,
    Integer daysMask,
    Integer startMinute,
    Integer endMinute,
    Integer tzOffsetMinutes
  ) {}

  @GetMapping("/discounts")
  public List<DiscountDto> listDiscounts(Authentication auth, @RequestParam(required = false) Long branchId) {
    StaffUser u = requireAdmin(auth);
    long bId = resolveBranchId(u, branchId);
    List<BranchDiscount> items = branchDiscountRepo.findByBranchIdOrderByIdDesc(bId);
    return items.stream().map(this::toDiscountDto).toList();
  }

  @PostMapping("/discounts")
  public DiscountDto createDiscount(Authentication auth, @Valid @RequestBody CreateDiscountRequest req) {
    StaffUser u = requireAdmin(auth);
    long bId = resolveBranchId(u, req.branchId);
    BranchDiscount d = new BranchDiscount();
    d.branchId = bId;
    d.scope = normalizeScope(req.scope);
    d.code = trimOrNull(req.code);
    d.type = normalizeType(req.type);
    d.value = req.value == null ? 0 : req.value;
    d.label = trimOrNull(req.label);
    d.active = req.active == null || req.active;
    d.maxUses = req.maxUses;
    d.startsAt = req.startsAt;
    d.endsAt = req.endsAt;
    d.daysMask = req.daysMask;
    d.startMinute = req.startMinute;
    d.endMinute = req.endMinute;
    d.tzOffsetMinutes = req.tzOffsetMinutes;
    validateDiscount(d, true);
    if ("COUPON".equalsIgnoreCase(d.scope) && d.code != null) {
      if (branchDiscountRepo.findFirstByBranchIdAndCodeIgnoreCase(d.branchId, d.code).isPresent()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Promo code already exists");
      }
    }
    d = branchDiscountRepo.save(d);
    return toDiscountDto(d);
  }

  @PutMapping("/discounts/{id}")
  public DiscountDto updateDiscount(
    Authentication auth,
    @PathVariable("id") long id,
    @Valid @RequestBody UpdateDiscountRequest req
  ) {
    StaffUser u = requireAdmin(auth);
    BranchDiscount d = branchDiscountRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount not found"));
    requireBranchAccess(u, d.branchId);
    if (req.scope != null) d.scope = normalizeScope(req.scope);
    if (req.code != null) d.code = trimOrNull(req.code);
    if (req.type != null) d.type = normalizeType(req.type);
    if (req.value != null) d.value = req.value;
    if (req.label != null) d.label = trimOrNull(req.label);
    if (req.active != null) d.active = req.active;
    if (req.maxUses != null) d.maxUses = req.maxUses;
    if (req.usedCount != null) d.usedCount = Math.max(0, req.usedCount);
    if (req.startsAt != null) d.startsAt = req.startsAt;
    if (req.endsAt != null) d.endsAt = req.endsAt;
    if (req.daysMask != null) d.daysMask = req.daysMask;
    if (req.startMinute != null) d.startMinute = req.startMinute;
    if (req.endMinute != null) d.endMinute = req.endMinute;
    if (req.tzOffsetMinutes != null) d.tzOffsetMinutes = req.tzOffsetMinutes;
    validateDiscount(d, "COUPON".equalsIgnoreCase(d.scope));
    if ("COUPON".equalsIgnoreCase(d.scope) && d.code != null) {
      final Long currentId = d.id;
      branchDiscountRepo.findFirstByBranchIdAndCodeIgnoreCase(d.branchId, d.code).ifPresent(existing -> {
        if (!existing.id.equals(currentId)) {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Promo code already exists");
        }
      });
    }
    d = branchDiscountRepo.save(d);
    return toDiscountDto(d);
  }

  @DeleteMapping("/discounts/{id}")
  public Map<String, Object> deleteDiscount(Authentication auth, @PathVariable("id") long id) {
    StaffUser u = requireAdmin(auth);
    BranchDiscount d = branchDiscountRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Discount not found"));
    requireBranchAccess(u, d.branchId);
    branchDiscountRepo.delete(d);
    return Map.of("deleted", true, "id", id);
  }

  private DiscountDto toDiscountDto(BranchDiscount d) {
    return new DiscountDto(
      d.id,
      d.branchId,
      d.scope,
      d.code,
      d.type,
      d.value,
      d.label,
      d.active,
      d.maxUses,
      d.usedCount,
      d.startsAt,
      d.endsAt,
      d.daysMask,
      d.startMinute,
      d.endMinute,
      d.tzOffsetMinutes
    );
  }

  private void validateDiscount(BranchDiscount d, boolean requireCode) {
    if (d.scope == null || (!"COUPON".equalsIgnoreCase(d.scope) && !"HAPPY_HOUR".equalsIgnoreCase(d.scope))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported discount scope");
    }
    if (requireCode) {
      if (d.code == null || d.code.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code invalid");
      }
    }
    if (d.type == null || (!"PERCENT".equalsIgnoreCase(d.type) && !"FIXED".equalsIgnoreCase(d.type))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported discount type");
    }
    if ("PERCENT".equalsIgnoreCase(d.type)) {
      if (d.value <= 0 || d.value > 100) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code invalid");
      }
    } else if (d.value <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Promo code invalid");
    }
    if ("HAPPY_HOUR".equalsIgnoreCase(d.scope)) {
      if (d.startMinute == null || d.endMinute == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Happy hour time window required");
      }
      if (d.startMinute < 0 || d.startMinute > 1439 || d.endMinute < 0 || d.endMinute > 1439) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Happy hour time window invalid");
      }
    }
    if (d.daysMask != null && (d.daysMask < 1 || d.daysMask > 127)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Happy hour days mask invalid");
    }
    if (d.tzOffsetMinutes != null && (d.tzOffsetMinutes < -840 || d.tzOffsetMinutes > 840)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Happy hour tz offset invalid");
    }
  }

  private String normalizeScope(String v) {
    String t = v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    return t.isEmpty() ? "COUPON" : t;
  }

  private String normalizeType(String v) {
    String t = v == null ? "" : v.trim().toUpperCase(Locale.ROOT);
    return t.isEmpty() ? "PERCENT" : t;
  }

  private static boolean isBlank(String v) {
    return v == null || v.isBlank();
  }

  private static String normalizeUrlOrNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private static String normalizePathOrNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private static String normalizeSecretOrNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }
}
