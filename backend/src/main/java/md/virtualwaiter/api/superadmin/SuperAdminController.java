package md.virtualwaiter.api.superadmin;

import md.virtualwaiter.domain.Branch;
import md.virtualwaiter.domain.BranchReview;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.Tenant;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.BranchReviewRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.TenantRepo;
import md.virtualwaiter.service.StatsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

@RestController
@RequestMapping("/api/super")
public class SuperAdminController {
  private static final Logger LOG = LoggerFactory.getLogger(SuperAdminController.class);
  private final StaffUserRepo staffUserRepo;
  private final TenantRepo tenantRepo;
  private final BranchRepo branchRepo;
  private final BranchReviewRepo branchReviewRepo;
  private final CafeTableRepo tableRepo;
  private final PasswordEncoder passwordEncoder;
  private final StatsService statsService;
  private final int maxPhotoUrlLength;
  private final Set<String> allowedPhotoExts;

  public SuperAdminController(
    StaffUserRepo staffUserRepo,
    TenantRepo tenantRepo,
    BranchRepo branchRepo,
    BranchReviewRepo branchReviewRepo,
    CafeTableRepo tableRepo,
    PasswordEncoder passwordEncoder,
    StatsService statsService,
    @Value("${app.media.maxPhotoUrlLength:512}") int maxPhotoUrlLength,
    @Value("${app.media.allowedPhotoExts:jpg,jpeg,png,webp,gif}") String allowedPhotoExts
  ) {
    this.staffUserRepo = staffUserRepo;
    this.tenantRepo = tenantRepo;
    this.branchRepo = branchRepo;
    this.branchReviewRepo = branchReviewRepo;
    this.tableRepo = tableRepo;
    this.passwordEncoder = passwordEncoder;
    this.statsService = statsService;
    this.maxPhotoUrlLength = maxPhotoUrlLength;
    this.allowedPhotoExts = parseExts(allowedPhotoExts);
  }

  private StaffUser requireSuper(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");
    }
    StaffUser u = staffUserRepo.findByUsername(auth.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    String role = u.role == null ? "" : u.role.toUpperCase(Locale.ROOT);
    if (!"SUPER_ADMIN".equals(role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Super admin required");
    }
    return u;
  }

  // --- Tenants ---
  public record TenantDto(long id, String name, boolean isActive) {}
  public record CreateTenantRequest(@NotBlank String name) {}
  public record UpdateTenantRequest(String name, Boolean isActive) {}

  @GetMapping("/tenants")
  public List<TenantDto> listTenants(@RequestParam(value = "isActive", required = false) Boolean isActive, Authentication auth) {
    requireSuper(auth);
    List<Tenant> list = tenantRepo.findAll();
    if (isActive != null) {
      list = list.stream().filter(t -> t.isActive == isActive).toList();
    }
    List<TenantDto> out = new ArrayList<>();
    for (Tenant t : list) out.add(new TenantDto(t.id, t.name, t.isActive));
    return out;
  }

  @PostMapping("/tenants")
  public TenantDto createTenant(@Valid @RequestBody CreateTenantRequest req, Authentication auth) {
    requireSuper(auth);
    Tenant t = new Tenant();
    t.name = req.name;
    t.isActive = true;
    t = tenantRepo.save(t);
    return new TenantDto(t.id, t.name, t.isActive);
  }

  @PatchMapping("/tenants/{id}")
  public TenantDto updateTenant(@PathVariable long id, @RequestBody UpdateTenantRequest req, Authentication auth) {
    requireSuper(auth);
    Tenant t = tenantRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    if (req.name != null) t.name = req.name;
    if (req.isActive != null) t.isActive = req.isActive;
    t = tenantRepo.save(t);
    return new TenantDto(t.id, t.name, t.isActive);
  }

  @DeleteMapping("/tenants/{id}")
  public void deleteTenant(@PathVariable long id, Authentication auth) {
    requireSuper(auth);
    Tenant t = tenantRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    tenantRepo.delete(t);
  }

  // --- Branches ---
  public record BranchDto(long id, long tenantId, String name, boolean isActive) {}
  public record CreateBranchRequest(@NotBlank String name) {}
  public record UpdateBranchRequest(String name, Boolean isActive) {}

  @GetMapping("/branches")
  public List<BranchDto> listBranches(
    @RequestParam(value = "tenantId", required = false) Long tenantId,
    @RequestParam(value = "isActive", required = false) Boolean isActive,
    Authentication auth
  ) {
    requireSuper(auth);
    List<Branch> list = (tenantId == null) ? branchRepo.findAll() : branchRepo.findByTenantId(tenantId);
    if (isActive != null) {
      list = list.stream().filter(b -> b.isActive == isActive).toList();
    }
    List<BranchDto> out = new ArrayList<>();
    for (Branch b : list) out.add(new BranchDto(b.id, b.tenantId, b.name, b.isActive));
    return out;
  }

  @PostMapping("/tenants/{tenantId}/branches")
  public BranchDto createBranch(@PathVariable long tenantId, @Valid @RequestBody CreateBranchRequest req, Authentication auth) {
    requireSuper(auth);
    Tenant t = tenantRepo.findById(tenantId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    if (!t.isActive) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant is inactive");
    }
    Branch b = new Branch();
    b.tenantId = tenantId;
    b.name = req.name;
    b.isActive = true;
    b = branchRepo.save(b);
    return new BranchDto(b.id, b.tenantId, b.name, b.isActive);
  }

  @PatchMapping("/branches/{id}")
  public BranchDto updateBranch(@PathVariable long id, @RequestBody UpdateBranchRequest req, Authentication auth) {
    requireSuper(auth);
    Branch b = branchRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    if (req.name != null) b.name = req.name;
    if (req.isActive != null) b.isActive = req.isActive;
    b = branchRepo.save(b);
    return new BranchDto(b.id, b.tenantId, b.name, b.isActive);
  }

  @DeleteMapping("/branches/{id}")
  public void deleteBranch(@PathVariable long id, Authentication auth) {
    requireSuper(auth);
    Branch b = branchRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    branchRepo.delete(b);
  }

  // --- Staff users (global create) ---
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
    @NotNull Long branchId,
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

  @PostMapping("/staff")
  public StaffUserDto createStaff(@Valid @RequestBody CreateStaffUserRequest req, Authentication auth) {
    requireSuper(auth);
    String role = req.role.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("WAITER", "KITCHEN", "ADMIN", "SUPER_ADMIN").contains(role)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
    }
    StaffUser su = new StaffUser();
    su.branchId = "SUPER_ADMIN".equals(role) ? null : req.branchId;
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
    return new StaffUserDto(
      su.id, su.branchId, su.username, su.role, su.isActive,
      su.firstName, su.lastName, su.age, su.gender, su.photoUrl,
      su.rating, su.recommended, su.experienceYears, su.favoriteItems
    );
  }

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

  @PatchMapping("/staff/{id}")
  public StaffUserDto updateStaff(@PathVariable long id, @RequestBody UpdateStaffUserRequest req, Authentication auth) {
    requireSuper(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    if (req.password != null && !req.password.isBlank()) {
      su.passwordHash = passwordEncoder.encode(req.password);
    }
    if (req.role != null) {
      String role = req.role.trim().toUpperCase(Locale.ROOT);
      if (!Set.of("WAITER", "KITCHEN", "ADMIN", "SUPER_ADMIN").contains(role)) {
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
    su = staffUserRepo.save(su);
    return new StaffUserDto(
      su.id, su.branchId, su.username, su.role, su.isActive,
      su.firstName, su.lastName, su.age, su.gender, su.photoUrl,
      su.rating, su.recommended, su.experienceYears, su.favoriteItems
    );
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

  @DeleteMapping("/staff/{id}")
  public void deleteStaff(@PathVariable long id, Authentication auth) {
    requireSuper(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    tableRepo.clearAssignedWaiter(su.id);
    staffUserRepo.delete(su);
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
    double avgBranchRating,
    long branchReviewsCount
  ) {}

  public record BranchSummaryRow(
    long branchId,
    String branchName,
    long ordersCount,
    long callsCount,
    long paidBillsCount,
    long grossCents,
    long tipsCents
  ) {}

  private static class BranchReviewAgg {
    final double avgRating;
    final long count;

    BranchReviewAgg(double avgRating, long count) {
      this.avgRating = avgRating;
      this.count = count;
    }
  }

  @GetMapping("/stats/summary")
  public StatsSummaryResponse getSummary(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    StatsService.Summary s = statsService.summaryForTenant(tenantId, fromTs, toTs);
    BranchReviewAgg reviewsAgg = branchReviewAggForTenant(tenantId, fromTs, toTs);
    return new StatsSummaryResponse(
      s.from().toString(),
      s.to().toString(),
      s.ordersCount(),
      s.callsCount(),
      s.paidBillsCount(),
      s.grossCents(),
      s.tipsCents(),
      s.activeTablesCount(),
      reviewsAgg.avgRating,
      reviewsAgg.count
    );
  }

  private BranchReviewAgg branchReviewAggForTenant(Long tenantId, Instant fromTs, Instant toTs) {
    double sum = 0.0;
    long count = 0;
    List<Branch> branches = branchRepo.findByTenantId(tenantId);
    for (Branch b : branches) {
      List<BranchReview> reviews = branchReviewRepo.findByBranchIdOrderByIdDesc(b.id);
      for (BranchReview r : reviews) {
        if (r.createdAt == null) continue;
        if (r.createdAt.isBefore(fromTs) || r.createdAt.isAfter(toTs)) continue;
        if (r.rating != null) {
          sum += r.rating;
          count++;
        }
      }
    }
    double avg = count == 0 ? 0.0 : (sum / count);
    return new BranchReviewAgg(avg, count);
  }

  @GetMapping("/stats/branches")
  public List<BranchSummaryRow> getBranchSummary(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<StatsService.BranchSummaryRow> rows = statsService.summaryByBranchForTenant(tenantId, fromTs, toTs);
    List<BranchSummaryRow> out = new ArrayList<>();
    for (StatsService.BranchSummaryRow r : rows) {
      out.add(new BranchSummaryRow(r.branchId(), r.branchName(), r.ordersCount(), r.callsCount(), r.paidBillsCount(), r.grossCents(), r.tipsCents()));
    }
    return out;
  }

  @GetMapping("/stats/summary.csv")
  public ResponseEntity<String> getSummaryCsv(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    StatsService.Summary s = statsService.summaryForTenant(tenantId, fromTs, toTs);
    BranchReviewAgg reviewsAgg = branchReviewAggForTenant(tenantId, fromTs, toTs);
    StringBuilder sb = new StringBuilder();
    sb.append("from,to,orders,calls,paid_bills,gross_cents,tips_cents,active_tables,avg_branch_rating,branch_reviews_count\n");
    sb.append(s.from()).append(',')
      .append(s.to()).append(',')
      .append(s.ordersCount()).append(',')
      .append(s.callsCount()).append(',')
      .append(s.paidBillsCount()).append(',')
      .append(s.grossCents()).append(',')
      .append(s.tipsCents()).append(',')
      .append(s.activeTablesCount()).append(',')
      .append(reviewsAgg.avgRating).append(',')
      .append(reviewsAgg.count).append('\n');
    String filename = "tenant-summary-" + tenantId + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
  }

  @GetMapping("/stats/branches.csv")
  public ResponseEntity<String> getBranchSummaryCsv(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    List<StatsService.BranchSummaryRow> rows = statsService.summaryByBranchForTenant(tenantId, fromTs, toTs);
    StringBuilder sb = new StringBuilder();
    sb.append("branch_id,branch_name,orders,calls,paid_bills,gross_cents,tips_cents\n");
    for (StatsService.BranchSummaryRow r : rows) {
      sb.append(r.branchId()).append(',')
        .append(r.branchName()).append(',')
        .append(r.ordersCount()).append(',')
        .append(r.callsCount()).append(',')
        .append(r.paidBillsCount()).append(',')
        .append(r.grossCents()).append(',')
        .append(r.tipsCents()).append('\n');
    }
    String filename = "tenant-branches-" + tenantId + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
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
    return url;
  }

  private void logReject(String reason, String url) {
    int max = 200;
    String safe = url == null ? "" : (url.length() > max ? url.substring(0, max) + "..." : url);
    LOG.warn("Photo URL rejected: {} (url={})", reason, safe);
  }

  private static Integer sanitizeAge(Integer v) {
    if (v == null) return null;
    if (v < 0 || v > 120) return null;
    return v;
  }

  private static String sanitizeGender(String v) {
    if (v == null) return null;
    String t = v.trim().toLowerCase(Locale.ROOT);
    return switch (t) { case "male", "female", "other" -> t; default -> null; };
  }
}
