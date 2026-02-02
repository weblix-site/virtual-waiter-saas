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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final StaffUserRepo staffUserRepo;
  private final MenuCategoryRepo categoryRepo;
  private final MenuItemRepo itemRepo;
  private final CafeTableRepo tableRepo;
  private final BranchRepo branchRepo;
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
  private final CurrencyRepo currencyRepo;

  public AdminController(
    StaffUserRepo staffUserRepo,
    MenuCategoryRepo categoryRepo,
    MenuItemRepo itemRepo,
    CafeTableRepo tableRepo,
    BranchRepo branchRepo,
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
    CurrencyRepo currencyRepo
  ) {
    this.staffUserRepo = staffUserRepo;
    this.categoryRepo = categoryRepo;
    this.itemRepo = itemRepo;
    this.tableRepo = tableRepo;
    this.branchRepo = branchRepo;
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
    this.currencyRepo = currencyRepo;
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
    } catch (Exception e) {
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
    boolean payCashEnabled,
    boolean payTerminalEnabled,
    String currencyCode
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
      s.payTerminalEnabled(),
      s.currencyCode()
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
    Boolean payTerminalEnabled,
    String currencyCode
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
    if (req.payCashEnabled != null) s.payCashEnabled = req.payCashEnabled;
    if (req.payTerminalEnabled != null) s.payTerminalEnabled = req.payTerminalEnabled;
    if (req.currencyCode != null && !req.currencyCode.isBlank()) {
      String code = req.currencyCode.trim().toUpperCase(Locale.ROOT);
      Currency cur = currencyRepo.findById(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
      if (!cur.isActive) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is inactive");
      }
      s.currencyCode = code;
    }

    settingsRepo.save(s);
    if (req.currencyCode != null && !req.currencyCode.isBlank()) {
      String nextCurrency = s.currencyCode == null ? "MDL" : s.currencyCode;
      if (!Objects.equals(prevCurrency, nextCurrency)) {
        List<MenuCategory> cats = categoryRepo.findByBranchIdOrderBySortOrderAscIdAsc(bid);
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
      r.payCashEnabled(),
      r.payTerminalEnabled(),
      r.currencyCode()
    );
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
    List<Currency> list = include ? currencyRepo.findAllByOrderByCodeAsc() : currencyRepo.findByIsActiveTrueOrderByCodeAsc();
    List<CurrencyDto> out = new ArrayList<>();
    for (Currency c : list) {
      out.add(new CurrencyDto(c.code, c.name, c.symbol, c.isActive));
    }
    return out;
  }

  @PostMapping("/currencies")
  public CurrencyDto createCurrency(@Valid @RequestBody CreateCurrencyRequest req, Authentication auth) {
    StaffUser u = requireAdmin(auth);
    requireSuperAdmin(u);
    Currency c = new Currency();
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
    Currency c = currencyRepo.findById(k)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Currency not found"));
    if (req.name != null && !req.name.isBlank()) c.name = req.name.trim();
    if (req.symbol != null) c.symbol = req.symbol;
    if (req.isActive != null) c.isActive = req.isActive;
    c = currencyRepo.save(c);
    auditService.log(u, "UPDATE", "Currency", null, c.code);
    return new CurrencyDto(c.code, c.name, c.symbol, c.isActive);
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
    if (req.currency != null && !req.currency.isBlank()) {
      String code = req.currency.trim().toUpperCase(Locale.ROOT);
      Currency cur = currencyRepo.findById(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
      if (!cur.isActive) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is inactive");
      }
      it.currency = code;
    } else {
      it.currency = settingsService.resolveForBranch(bid).currencyCode();
    }
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
    if (req.currency != null && !req.currency.isBlank()) {
      String code = req.currency.trim().toUpperCase(Locale.ROOT);
      Currency cur = currencyRepo.findById(code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
      if (!cur.isActive) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is inactive");
      }
      it.currency = code;
    }
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
    StatsService.Summary s = statsService.summaryForBranchFiltered(bid, fromTs, toTs, tableId, waiterId, hallId);
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
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "planId", required = false) Long planId,
    @RequestParam(value = "waiterId", required = false) Long waiterId,
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
    int lim = limit == null ? 10 : limit;
    List<StatsService.TopItemRow> rows = statsService.topItemsForBranch(bid, fromTs, toTs, tableId, waiterId, hallId, lim);
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
    int lim = limit == null ? 10 : limit;
    List<StatsService.TopCategoryRow> rows = statsService.topCategoriesForBranch(bid, fromTs, toTs, tableId, waiterId, hallId, lim);
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
}
