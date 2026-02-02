package md.virtualwaiter.api.admin;

import md.virtualwaiter.domain.*;
import md.virtualwaiter.repo.*;
import md.virtualwaiter.security.QrSignatureService;
import md.virtualwaiter.service.BranchSettingsService;
import md.virtualwaiter.service.StatsService;
import md.virtualwaiter.service.AuditService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final StaffUserRepo staffUserRepo;
  private final MenuCategoryRepo categoryRepo;
  private final MenuItemRepo itemRepo;
  private final CafeTableRepo tableRepo;
  private final BranchSettingsRepo settingsRepo;
  private final BranchSettingsService settingsService;
  private final QrSignatureService qrSig;
  private final PasswordEncoder passwordEncoder;
  private final String publicBaseUrl;
  private final StatsService statsService;
  private final ModifierGroupRepo modifierGroupRepo;
  private final ModifierOptionRepo modifierOptionRepo;
  private final MenuItemModifierGroupRepo menuItemModifierGroupRepo;
  private final AuditService auditService;
  private final AuditLogRepo auditLogRepo;
  private final TablePartyRepo partyRepo;
  private final GuestSessionRepo guestSessionRepo;

  public AdminController(
    StaffUserRepo staffUserRepo,
    MenuCategoryRepo categoryRepo,
    MenuItemRepo itemRepo,
    CafeTableRepo tableRepo,
    BranchSettingsRepo settingsRepo,
    BranchSettingsService settingsService,
    QrSignatureService qrSig,
    PasswordEncoder passwordEncoder,
    @Value("${app.publicBaseUrl:http://localhost:3000}") String publicBaseUrl,
    StatsService statsService,
    ModifierGroupRepo modifierGroupRepo,
    ModifierOptionRepo modifierOptionRepo,
    MenuItemModifierGroupRepo menuItemModifierGroupRepo,
    AuditService auditService,
    AuditLogRepo auditLogRepo,
    TablePartyRepo partyRepo,
    GuestSessionRepo guestSessionRepo
  ) {
    this.staffUserRepo = staffUserRepo;
    this.categoryRepo = categoryRepo;
    this.itemRepo = itemRepo;
    this.tableRepo = tableRepo;
    this.settingsRepo = settingsRepo;
    this.settingsService = settingsService;
    this.qrSig = qrSig;
    this.passwordEncoder = passwordEncoder;
    this.publicBaseUrl = publicBaseUrl;
    this.statsService = statsService;
    this.modifierGroupRepo = modifierGroupRepo;
    this.modifierOptionRepo = modifierOptionRepo;
    this.menuItemModifierGroupRepo = menuItemModifierGroupRepo;
    this.auditService = auditService;
    this.auditLogRepo = auditLogRepo;
    this.partyRepo = partyRepo;
    this.guestSessionRepo = guestSessionRepo;
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
    if (!Set.of("ADMIN", "SUPER_ADMIN").contains(role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
    }
    return u;
  }

  private boolean isSuperAdmin(StaffUser u) {
    return "SUPER_ADMIN".equalsIgnoreCase(u.role);
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

  public record MeResponse(long id, String username, String role, Long branchId) {}

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    StaffUser u = requireAdmin(auth);
    return new MeResponse(u.id, u.username, u.role, u.branchId);
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
    boolean payCashEnabled,
    boolean payTerminalEnabled
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
      s.payCashEnabled(),
      s.payTerminalEnabled()
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
    Boolean payCashEnabled,
    Boolean payTerminalEnabled
  ) {}

  @PutMapping("/branch-settings")
  public BranchSettingsResponse updateBranchSettings(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @Valid @RequestBody UpdateBranchSettingsRequest req,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
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
    if (req.payCashEnabled != null) s.payCashEnabled = req.payCashEnabled;
    if (req.payTerminalEnabled != null) s.payTerminalEnabled = req.payTerminalEnabled;

    settingsRepo.save(s);
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
      r.payCashEnabled(),
      r.payTerminalEnabled()
    );
  }

  // --- Menu categories ---
  public record MenuCategoryDto(long id, String nameRu, String nameRo, String nameEn, int sortOrder, boolean isActive) {}
  public record CreateCategoryRequest(@NotBlank String nameRu, String nameRo, String nameEn, Integer sortOrder, Boolean isActive) {}

  @GetMapping("/menu/categories")
  public List<MenuCategoryDto> listCategories(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<MenuCategory> cats = categoryRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid);
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
    MenuCategory c = new MenuCategory();
    c.branchId = bid;
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
    MenuCategory c = categoryRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, c.branchId);
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
    MenuCategory c = categoryRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, c.branchId);
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
    List<MenuItem> items;
    if (categoryId != null) {
      MenuCategory c = categoryRepo.findById(categoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
      if (!Objects.equals(c.branchId, bid)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
      }
      items = itemRepo.findByCategoryId(categoryId);
    } else {
      List<MenuCategory> cats = categoryRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid);
      List<Long> catIds = cats.stream().map(x -> x.id).toList();
      items = catIds.isEmpty() ? List.of() : itemRepo.findByCategoryIdIn(catIds);
    }
    List<MenuItemDto> out = new ArrayList<>();
    for (MenuItem it : items) out.add(toDto(it));
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
    MenuCategory c = categoryRepo.findById(req.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    if (!Objects.equals(c.branchId, bid)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Wrong branch");
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
    it.photoUrls = req.photoUrls;
    it.kcal = req.kcal;
    it.proteinG = req.proteinG;
    it.fatG = req.fatG;
    it.carbsG = req.carbsG;
    it.priceCents = req.priceCents;
    if (req.currency != null && !req.currency.isBlank()) it.currency = req.currency.trim();
    it.isActive = req.isActive == null || req.isActive;
    it.isStopList = req.isStopList != null && req.isStopList;
    it = itemRepo.save(it);
    auditService.log(u, "CREATE", "MenuItem", it.id, null);
    return toDto(it);
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
  public MenuItemDto updateMenuItem(@PathVariable long id, @RequestBody UpdateMenuItemRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, c.branchId);

    if (req.categoryId != null && !Objects.equals(req.categoryId, it.categoryId)) {
      MenuCategory c2 = categoryRepo.findById(req.categoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
      requireBranchAccess(u, c2.branchId);
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
    if (req.photoUrls != null) it.photoUrls = req.photoUrls;
    if (req.kcal != null) it.kcal = req.kcal;
    if (req.proteinG != null) it.proteinG = req.proteinG;
    if (req.fatG != null) it.fatG = req.fatG;
    if (req.carbsG != null) it.carbsG = req.carbsG;
    if (req.priceCents != null) it.priceCents = req.priceCents;
    if (req.currency != null && !req.currency.isBlank()) it.currency = req.currency.trim();
    if (req.isActive != null) it.isActive = req.isActive;
    if (req.isStopList != null) it.isStopList = req.isStopList;

    it = itemRepo.save(it);
    auditService.log(u, "UPDATE", "MenuItem", it.id, null);
    return toDto(it);
  }

  @DeleteMapping("/menu/items/{id}")
  public void deleteMenuItem(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, c.branchId);
    itemRepo.delete(it);
    auditService.log(u, "DELETE", "MenuItem", it.id, null);
  }

  private static MenuItemDto toDto(MenuItem it) {
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
      it.isActive,
      it.isStopList
    );
  }

  // --- Tables ---
  public record TableDto(long id, int number, String publicId, Long assignedWaiterId) {}
  public record CreateTableRequest(@NotNull Integer number, String publicId, Long assignedWaiterId) {}
  public record UpdateTableRequest(Integer number, String publicId, Long assignedWaiterId) {}

  @GetMapping("/tables")
  public List<TableDto> listTables(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<CafeTable> tables = tableRepo.findByBranchId(bid);
    List<TableDto> out = new ArrayList<>();
    for (CafeTable t : tables) out.add(new TableDto(t.id, t.number, t.publicId, t.assignedWaiterId));
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
    t = tableRepo.save(t);
    auditService.log(u, "CREATE", "CafeTable", t.id, null);
    return new TableDto(t.id, t.number, t.publicId, t.assignedWaiterId);
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
    t = tableRepo.save(t);
    auditService.log(u, "UPDATE", "CafeTable", t.id, null);
    return new TableDto(t.id, t.number, t.publicId, t.assignedWaiterId);
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
  public record StaffUserDto(long id, Long branchId, String username, String role, boolean isActive) {}
  public record CreateStaffUserRequest(@NotBlank String username, @NotBlank String password, @NotBlank String role) {}
  public record UpdateStaffUserRequest(String password, String role, Boolean isActive) {}

  @GetMapping("/staff")
  public List<StaffUserDto> listStaff(@RequestParam(value = "branchId", required = false) Long branchId, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    List<StaffUser> users = staffUserRepo.findByBranchId(bid);
    List<StaffUserDto> out = new ArrayList<>();
    for (StaffUser su : users) {
      out.add(new StaffUserDto(su.id, su.branchId, su.username, su.role, su.isActive));
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
    if (!Set.of("WAITER", "KITCHEN", "ADMIN").contains(role)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
    }
    StaffUser su = new StaffUser();
    su.branchId = bid;
    su.username = req.username.trim();
    su.passwordHash = passwordEncoder.encode(req.password);
    su.role = role;
    su.isActive = true;
    su = staffUserRepo.save(su);
    auditService.log(u, "CREATE", "StaffUser", su.id, null);
    return new StaffUserDto(su.id, su.branchId, su.username, su.role, su.isActive);
  }

  @PatchMapping("/staff/{id}")
  public StaffUserDto updateStaff(@PathVariable long id, @RequestBody UpdateStaffUserRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    requireBranchAccess(u, su.branchId);
    if (req.password != null && !req.password.isBlank()) {
      su.passwordHash = passwordEncoder.encode(req.password);
    }
    if (req.role != null) {
      String role = req.role.trim().toUpperCase(Locale.ROOT);
      if (!Set.of("WAITER", "KITCHEN", "ADMIN").contains(role)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
      }
      su.role = role;
    }
    if (req.isActive != null) su.isActive = req.isActive;
    su = staffUserRepo.save(su);
    auditService.log(u, "UPDATE", "StaffUser", su.id, null);
    return new StaffUserDto(su.id, su.branchId, su.username, su.role, su.isActive);
  }

  @DeleteMapping("/staff/{id}")
  public void deleteStaff(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    requireBranchAccess(u, su.branchId);
    staffUserRepo.delete(su);
    auditService.log(u, "DELETE", "StaffUser", su.id, null);
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
    List<ModifierGroup> groups = modifierGroupRepo.findByBranchIdOrderByIdAsc(bid);
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
    ModifierGroup g = new ModifierGroup();
    g.branchId = bid;
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
    ModifierGroup g = modifierGroupRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    requireBranchAccess(u, g.branchId);
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
    ModifierGroup g = modifierGroupRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
    requireBranchAccess(u, g.branchId);
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
    requireBranchAccess(u, g.branchId);
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
    requireBranchAccess(u, g.branchId);
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
    requireBranchAccess(u, g.branchId);
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
    requireBranchAccess(u, g.branchId);
    modifierOptionRepo.delete(o);
  }

  public record ItemModifierGroupDto(long groupId, boolean isRequired, Integer minSelect, Integer maxSelect, int sortOrder) {}
  public record UpdateItemModifierGroupsRequest(List<ItemModifierGroupDto> groups) {}

  @GetMapping("/menu/items/{id}/modifier-groups")
  public List<ItemModifierGroupDto> getItemModifierGroups(@PathVariable long id, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, c.branchId);
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
    MenuItem it = itemRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    MenuCategory c = categoryRepo.findById(it.categoryId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    requireBranchAccess(u, c.branchId);

    menuItemModifierGroupRepo.deleteByMenuItemId(id);
    List<ItemModifierGroupDto> out = new ArrayList<>();
    if (req.groups != null) {
      for (ItemModifierGroupDto g : req.groups) {
        ModifierGroup mg = modifierGroupRepo.findById(g.groupId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modifier group not found"));
        requireBranchAccess(u, mg.branchId);
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
    long activeTablesCount
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
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
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
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<StatsService.DailyRow> rows = statsService.dailyForBranchFiltered(bid, fromTs, toTs, tableId, waiterId);
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
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
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
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    StatsService.Summary s = statsService.summaryForBranchFiltered(bid, fromTs, toTs, tableId, waiterId);
    return new StatsSummaryResponse(
      s.from().toString(),
      s.to().toString(),
      s.ordersCount(),
      s.callsCount(),
      s.paidBillsCount(),
      s.grossCents(),
      s.tipsCents(),
      s.activeTablesCount()
    );
  }

  @GetMapping("/stats/daily")
  public List<StatsDailyRow> getDaily(
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "tableId", required = false) Long tableId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
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
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<StatsService.DailyRow> rows = statsService.dailyForBranchFiltered(bid, fromTs, toTs, tableId, waiterId);
    List<StatsDailyRow> out = new ArrayList<>();
    for (StatsService.DailyRow r : rows) {
      out.add(new StatsDailyRow(r.day(), r.ordersCount(), r.callsCount(), r.paidBillsCount(), r.grossCents(), r.tipsCents()));
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
    @RequestParam(value = "waiterId", required = false) Long waiterId,
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
    if (waiterId != null) {
      StaffUser w = staffUserRepo.findById(waiterId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter not found"));
      requireBranchAccess(u, w.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    int lim = limit == null ? 10 : limit;
    List<StatsService.TopItemRow> rows = statsService.topItemsForBranch(bid, fromTs, toTs, tableId, waiterId, lim);
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
    @RequestParam(value = "waiterId", required = false) Long waiterId,
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
    if (waiterId != null) {
      StaffUser w = staffUserRepo.findById(waiterId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Waiter not found"));
      requireBranchAccess(u, w.branchId);
    }
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    int lim = limit == null ? 10 : limit;
    List<StatsService.TopCategoryRow> rows = statsService.topCategoriesForBranch(bid, fromTs, toTs, tableId, waiterId, lim);
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
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    String actionVal = action == null || action.isBlank() ? null : action.trim();
    String entityTypeVal = entityType == null || entityType.isBlank() ? null : entityType.trim();
    String actorVal = actorUsername == null || actorUsername.isBlank() ? null : actorUsername.trim();
    Instant fromTs = parseInstantOrDateOrNull(from, true);
    Instant toTs = parseInstantOrDateOrNull(to, false);
    List<AuditLog> logs = auditLogRepo.findFiltered(
      bid,
      actionVal,
      entityTypeVal,
      actorVal,
      fromTs,
      toTs,
      beforeId,
      afterId,
      PageRequest.of(0, 200)
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
    Authentication auth
  ) {
    StaffUser u = requireAdmin(auth);
    long bid = resolveBranchId(u, branchId);
    String actionVal = action == null || action.isBlank() ? null : action.trim();
    String entityTypeVal = entityType == null || entityType.isBlank() ? null : entityType.trim();
    String actorVal = actorUsername == null || actorUsername.isBlank() ? null : actorUsername.trim();
    Instant fromTs = parseInstantOrDateOrNull(from, true);
    Instant toTs = parseInstantOrDateOrNull(to, false);
    List<AuditLog> logs = auditLogRepo.findFiltered(
      bid,
      actionVal,
      entityTypeVal,
      actorVal,
      fromTs,
      toTs,
      beforeId,
      afterId,
      PageRequest.of(0, 200)
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
}
