package md.virtualwaiter.api.superadmin;

import md.virtualwaiter.domain.Branch;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.Tenant;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.TenantRepo;
import md.virtualwaiter.service.StatsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/super")
public class SuperAdminController {
  private final StaffUserRepo staffUserRepo;
  private final TenantRepo tenantRepo;
  private final BranchRepo branchRepo;
  private final PasswordEncoder passwordEncoder;
  private final StatsService statsService;

  public SuperAdminController(
    StaffUserRepo staffUserRepo,
    TenantRepo tenantRepo,
    BranchRepo branchRepo,
    PasswordEncoder passwordEncoder,
    StatsService statsService
  ) {
    this.staffUserRepo = staffUserRepo;
    this.tenantRepo = tenantRepo;
    this.branchRepo = branchRepo;
    this.passwordEncoder = passwordEncoder;
    this.statsService = statsService;
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
  public List<TenantDto> listTenants(Authentication auth) {
    requireSuper(auth);
    List<Tenant> list = tenantRepo.findAll();
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

  // --- Branches ---
  public record BranchDto(long id, long tenantId, String name, boolean isActive) {}
  public record CreateBranchRequest(@NotBlank String name) {}
  public record UpdateBranchRequest(String name, Boolean isActive) {}

  @GetMapping("/branches")
  public List<BranchDto> listBranches(@RequestParam(value = "tenantId", required = false) Long tenantId, Authentication auth) {
    requireSuper(auth);
    List<Branch> list = (tenantId == null) ? branchRepo.findAll() : branchRepo.findByTenantId(tenantId);
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

  // --- Staff users (global create) ---
  public record StaffUserDto(long id, Long branchId, String username, String role, boolean isActive) {}
  public record CreateStaffUserRequest(@NotNull Long branchId, @NotBlank String username, @NotBlank String password, @NotBlank String role) {}

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
    su = staffUserRepo.save(su);
    return new StaffUserDto(su.id, su.branchId, su.username, su.role, su.isActive);
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
}
