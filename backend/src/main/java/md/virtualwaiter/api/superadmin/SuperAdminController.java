package md.virtualwaiter.api.superadmin;

import md.virtualwaiter.domain.Branch;
import md.virtualwaiter.domain.BranchReview;
import md.virtualwaiter.domain.BranchHall;
import md.virtualwaiter.domain.Restaurant;
import md.virtualwaiter.domain.StaffDeviceToken;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.domain.Tenant;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.BranchHallRepo;
import md.virtualwaiter.repo.BranchReviewRepo;
import md.virtualwaiter.repo.RestaurantRepo;
import md.virtualwaiter.repo.StaffDeviceTokenRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import md.virtualwaiter.repo.TenantRepo;
import md.virtualwaiter.service.StatsService;
import md.virtualwaiter.service.AuditService;
import md.virtualwaiter.security.AuthzService;
import md.virtualwaiter.security.Permission;
import md.virtualwaiter.security.PermissionUtils;
import md.virtualwaiter.security.RolePermissions;
import md.virtualwaiter.security.TotpService;
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
import java.util.HashSet;
import java.util.Set;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/super")
public class SuperAdminController {
  private static final Logger LOG = LoggerFactory.getLogger(SuperAdminController.class);
  private final StaffUserRepo staffUserRepo;
  private final TenantRepo tenantRepo;
  private final RestaurantRepo restaurantRepo;
  private final BranchRepo branchRepo;
  private final BranchHallRepo hallRepo;
  private final BranchReviewRepo branchReviewRepo;
  private final CafeTableRepo tableRepo;
  private final StaffDeviceTokenRepo staffDeviceTokenRepo;
  private final PasswordEncoder passwordEncoder;
  private final StatsService statsService;
  private final AuditService auditService;
  private final AuthzService authzService;
  private final TotpService totpService;
  private final int maxPhotoUrlLength;
  private final Set<String> allowedPhotoExts;
  private final String mediaPublicBaseUrl;

  public SuperAdminController(
    StaffUserRepo staffUserRepo,
    TenantRepo tenantRepo,
    RestaurantRepo restaurantRepo,
    BranchRepo branchRepo,
    BranchHallRepo hallRepo,
    BranchReviewRepo branchReviewRepo,
    CafeTableRepo tableRepo,
    StaffDeviceTokenRepo staffDeviceTokenRepo,
    PasswordEncoder passwordEncoder,
    StatsService statsService,
    AuditService auditService,
    AuthzService authzService,
    TotpService totpService,
    @Value("${app.media.maxPhotoUrlLength:512}") int maxPhotoUrlLength,
    @Value("${app.media.allowedPhotoExts:jpg,jpeg,png,webp,gif}") String allowedPhotoExts,
    @Value("${app.media.publicBaseUrl:http://localhost:8080}") String mediaPublicBaseUrl
  ) {
    this.staffUserRepo = staffUserRepo;
    this.tenantRepo = tenantRepo;
    this.restaurantRepo = restaurantRepo;
    this.branchRepo = branchRepo;
    this.hallRepo = hallRepo;
    this.branchReviewRepo = branchReviewRepo;
    this.tableRepo = tableRepo;
    this.staffDeviceTokenRepo = staffDeviceTokenRepo;
    this.passwordEncoder = passwordEncoder;
    this.statsService = statsService;
    this.auditService = auditService;
    this.authzService = authzService;
    this.totpService = totpService;
    this.maxPhotoUrlLength = maxPhotoUrlLength;
    this.allowedPhotoExts = parseExts(allowedPhotoExts);
    this.mediaPublicBaseUrl = trimTrailingSlash(mediaPublicBaseUrl);
  }

  private StaffUser requireSuper(Authentication auth) {
    if (auth == null || auth.getName() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No auth");
    }
    StaffUser u = staffUserRepo.findByUsername(auth.getName())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    authzService.require(u, Permission.SUPERADMIN_ACCESS);
    return u;
  }

  public record MeResponse(long id, String username, String role, Long branchId, Set<String> permissions) {}

  @GetMapping("/me")
  public MeResponse me(Authentication auth) {
    StaffUser u = requireSuper(auth);
    return new MeResponse(u.id, u.username, u.role, u.branchId, effectivePermissions(u));
  }

  public record TotpStatusResponse(boolean enabled, boolean hasSecret) {}
  public record TotpSetupResponse(String secret, String otpauthUrl, boolean enabled) {}
  public record TotpVerifyRequest(@NotBlank String code) {}

  @GetMapping("/2fa/status")
  public TotpStatusResponse totpStatus(Authentication auth) {
    StaffUser u = requireSuper(auth);
    return new TotpStatusResponse(u.totpEnabled, u.totpSecret != null && !u.totpSecret.isBlank());
  }

  @PostMapping("/2fa/setup")
  public TotpSetupResponse totpSetup(Authentication auth) {
    StaffUser u = requireSuper(auth);
    String secret = totpService.generateSecret();
    u.totpSecret = secret;
    u.totpEnabled = false;
    staffUserRepo.save(u);
    String otpauth = totpService.buildOtpauthUrl(u.username, "VirtualWaiter", secret);
    auditService.log(u, "UPDATE", "StaffUser2FA", u.id, "setup");
    return new TotpSetupResponse(secret, otpauth, false);
  }

  @PostMapping("/2fa/enable")
  public TotpStatusResponse totpEnable(@Valid @RequestBody TotpVerifyRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    if (u.totpSecret == null || u.totpSecret.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA is not initialized");
    }
    if (!totpService.verifyCode(u.totpSecret, req.code(), System.currentTimeMillis())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
    }
    u.totpEnabled = true;
    staffUserRepo.save(u);
    auditService.log(u, "UPDATE", "StaffUser2FA", u.id, "enable");
    return new TotpStatusResponse(true, true);
  }

  @PostMapping("/2fa/disable")
  public TotpStatusResponse totpDisable(@Valid @RequestBody TotpVerifyRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    if (u.totpSecret == null || u.totpSecret.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "2FA is not initialized");
    }
    if (!totpService.verifyCode(u.totpSecret, req.code(), System.currentTimeMillis())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
    }
    u.totpEnabled = false;
    u.totpSecret = null;
    staffUserRepo.save(u);
    auditService.log(u, "UPDATE", "StaffUser2FA", u.id, "disable");
    return new TotpStatusResponse(false, false);
  }

  public record DeviceSessionDto(
    long id,
    long staffUserId,
    String username,
    Long branchId,
    String platform,
    String deviceId,
    String deviceName,
    String tokenMasked,
    String createdAt,
    String lastSeenAt,
    String revokedAt
  ) {}

  public record RevokeDevicesResponse(int revoked) {}
  public record RevokeByUserRequest(@NotNull Long staffUserId) {}
  public record RevokeByBranchRequest(Long branchId) {}

  @GetMapping("/devices")
  public List<DeviceSessionDto> listDeviceSessions(
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "staffUserId", required = false) Long staffUserId,
    @RequestParam(value = "includeRevoked", required = false) Boolean includeRevoked,
    Authentication auth
  ) {
    requireSuper(auth);
    boolean include = includeRevoked != null && includeRevoked;
    List<StaffDeviceToken> tokens;
    if (staffUserId != null) {
      tokens = include
        ? staffDeviceTokenRepo.findByStaffUserId(staffUserId)
        : staffDeviceTokenRepo.findByStaffUserIdAndRevokedAtIsNull(staffUserId);
    } else if (branchId != null) {
      tokens = include
        ? staffDeviceTokenRepo.findByBranchId(branchId)
        : staffDeviceTokenRepo.findByBranchIdAndRevokedAtIsNull(branchId);
    } else {
      tokens = staffDeviceTokenRepo.findAll();
      if (!include) {
        tokens = tokens.stream().filter(t -> t.revokedAt == null).toList();
      }
    }
    List<Long> staffIds = tokens.stream().map(t -> t.staffUserId).distinct().toList();
    Map<Long, String> usernames = staffUserRepo.findAllById(staffIds).stream()
      .collect(Collectors.toMap(su -> su.id, su -> su.username));
    return tokens.stream()
      .sorted((a, b) -> {
        Instant al = a.lastSeenAt;
        Instant bl = b.lastSeenAt;
        if (al == null && bl == null) return 0;
        if (al == null) return 1;
        if (bl == null) return -1;
        return bl.compareTo(al);
      })
      .map(t -> new DeviceSessionDto(
        t.id,
        t.staffUserId,
        usernames.get(t.staffUserId),
        t.branchId,
        t.platform,
        t.deviceId,
        t.deviceName,
        maskToken(t.token),
        t.createdAt == null ? null : t.createdAt.toString(),
        t.lastSeenAt == null ? null : t.lastSeenAt.toString(),
        t.revokedAt == null ? null : t.revokedAt.toString()
      ))
      .toList();
  }

  @PostMapping("/devices/{id}/revoke")
  public void revokeDevice(@PathVariable long id, Authentication auth) {
    requireSuper(auth);
    StaffDeviceToken token = staffDeviceTokenRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device session not found"));
    if (token.revokedAt == null) {
      token.revokedAt = Instant.now();
      staffDeviceTokenRepo.save(token);
    }
  }

  @PostMapping("/devices/revoke-by-user")
  public RevokeDevicesResponse revokeDevicesByUser(@Valid @RequestBody RevokeByUserRequest req, Authentication auth) {
    requireSuper(auth);
    List<StaffDeviceToken> tokens = staffDeviceTokenRepo.findByStaffUserIdAndRevokedAtIsNull(req.staffUserId);
    int revoked = 0;
    Instant now = Instant.now();
    for (StaffDeviceToken t : tokens) {
      t.revokedAt = now;
      revoked++;
    }
    staffDeviceTokenRepo.saveAll(tokens);
    return new RevokeDevicesResponse(revoked);
  }

  @PostMapping("/devices/revoke-by-branch")
  public RevokeDevicesResponse revokeDevicesByBranch(@RequestBody RevokeByBranchRequest req, Authentication auth) {
    requireSuper(auth);
    if (req == null || req.branchId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "branchId is required");
    }
    List<StaffDeviceToken> tokens = staffDeviceTokenRepo.findByBranchIdAndRevokedAtIsNull(req.branchId);
    int revoked = 0;
    Instant now = Instant.now();
    for (StaffDeviceToken t : tokens) {
      t.revokedAt = now;
      revoked++;
    }
    staffDeviceTokenRepo.saveAll(tokens);
    return new RevokeDevicesResponse(revoked);
  }

  // --- Tenants ---
  public record TenantDto(
    long id,
    String name,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson,
    boolean isActive
  ) {}
  public record CreateTenantRequest(
    @NotBlank String name,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson
  ) {}
  public record UpdateTenantRequest(
    String name,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson,
    Boolean isActive
  ) {}

  @GetMapping("/tenants")
  public List<TenantDto> listTenants(@RequestParam(value = "isActive", required = false) Boolean isActive, Authentication auth) {
    requireSuper(auth);
    List<Tenant> list = tenantRepo.findAll();
    if (isActive != null) {
      list = list.stream().filter(t -> t.isActive == isActive).toList();
    }
    List<TenantDto> out = new ArrayList<>();
    for (Tenant t : list) {
      out.add(new TenantDto(
        t.id,
        t.name,
        t.logoUrl,
        t.country,
        t.address,
        t.phone,
        t.contactPerson,
        t.isActive
      ));
    }
    return out;
  }

  @PostMapping("/tenants")
  public TenantDto createTenant(@Valid @RequestBody CreateTenantRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Tenant t = new Tenant();
    t.name = req.name;
    t.logoUrl = sanitizePhotoUrl(req.logoUrl);
    t.country = trimOrNull(req.country);
    t.address = trimOrNull(req.address);
    t.phone = trimOrNull(req.phone);
    t.contactPerson = trimOrNull(req.contactPerson);
    t.isActive = true;
    t = tenantRepo.save(t);
    auditService.log(u, "CREATE", "Tenant", t.id, null);
    return new TenantDto(
      t.id,
      t.name,
      t.logoUrl,
      t.country,
      t.address,
      t.phone,
      t.contactPerson,
      t.isActive
    );
  }

  @PatchMapping("/tenants/{id}")
  public TenantDto updateTenant(@PathVariable long id, @RequestBody UpdateTenantRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Tenant t = tenantRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    if (req.name != null) t.name = req.name;
    if (req.logoUrl != null) t.logoUrl = sanitizePhotoUrl(req.logoUrl);
    if (req.country != null) t.country = trimOrNull(req.country);
    if (req.address != null) t.address = trimOrNull(req.address);
    if (req.phone != null) t.phone = trimOrNull(req.phone);
    if (req.contactPerson != null) t.contactPerson = trimOrNull(req.contactPerson);
    if (req.isActive != null) t.isActive = req.isActive;
    t = tenantRepo.save(t);
    auditService.log(u, "UPDATE", "Tenant", t.id, null);
    return new TenantDto(
      t.id,
      t.name,
      t.logoUrl,
      t.country,
      t.address,
      t.phone,
      t.contactPerson,
      t.isActive
    );
  }

  @DeleteMapping("/tenants/{id}")
  public void deleteTenant(@PathVariable long id, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Tenant t = tenantRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    tenantRepo.delete(t);
    auditService.log(u, "DELETE", "Tenant", t.id, null);
  }

  // --- Restaurants ---
  public record RestaurantDto(
    long id,
    long tenantId,
    String name,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson,
    boolean isActive
  ) {}
  public record CreateRestaurantRequest(
    @NotBlank String name,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson
  ) {}
  public record UpdateRestaurantRequest(
    String name,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson,
    Boolean isActive
  ) {}

  @GetMapping("/restaurants")
  public List<RestaurantDto> listRestaurants(
    @RequestParam(value = "tenantId", required = false) Long tenantId,
    @RequestParam(value = "isActive", required = false) Boolean isActive,
    Authentication auth
  ) {
    requireSuper(auth);
    List<Restaurant> list = tenantId == null ? restaurantRepo.findAll() : restaurantRepo.findByTenantId(tenantId);
    if (isActive != null) {
      list = list.stream().filter(r -> r.isActive == isActive).toList();
    }
    List<RestaurantDto> out = new ArrayList<>();
    for (Restaurant r : list) {
      out.add(new RestaurantDto(
        r.id,
        r.tenantId,
        r.name,
        r.logoUrl,
        r.country,
        r.address,
        r.phone,
        r.contactPerson,
        r.isActive
      ));
    }
    return out;
  }

  @PostMapping("/tenants/{tenantId}/restaurants")
  public RestaurantDto createRestaurant(@PathVariable long tenantId, @Valid @RequestBody CreateRestaurantRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Tenant t = tenantRepo.findById(tenantId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    if (!t.isActive) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant is inactive");
    }
    Restaurant r = new Restaurant();
    r.tenantId = tenantId;
    r.name = req.name;
    r.logoUrl = sanitizePhotoUrl(req.logoUrl);
    r.country = trimOrNull(req.country);
    r.address = trimOrNull(req.address);
    r.phone = trimOrNull(req.phone);
    r.contactPerson = trimOrNull(req.contactPerson);
    r.isActive = true;
    r = restaurantRepo.save(r);
    auditService.log(u, "CREATE", "Restaurant", r.id, null);
    return new RestaurantDto(
      r.id,
      r.tenantId,
      r.name,
      r.logoUrl,
      r.country,
      r.address,
      r.phone,
      r.contactPerson,
      r.isActive
    );
  }

  @PatchMapping("/restaurants/{id}")
  public RestaurantDto updateRestaurant(@PathVariable long id, @RequestBody UpdateRestaurantRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Restaurant r = restaurantRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    if (req.name != null) r.name = req.name;
    if (req.logoUrl != null) r.logoUrl = sanitizePhotoUrl(req.logoUrl);
    if (req.country != null) r.country = trimOrNull(req.country);
    if (req.address != null) r.address = trimOrNull(req.address);
    if (req.phone != null) r.phone = trimOrNull(req.phone);
    if (req.contactPerson != null) r.contactPerson = trimOrNull(req.contactPerson);
    if (req.isActive != null) r.isActive = req.isActive;
    r = restaurantRepo.save(r);
    auditService.log(u, "UPDATE", "Restaurant", r.id, null);
    return new RestaurantDto(
      r.id,
      r.tenantId,
      r.name,
      r.logoUrl,
      r.country,
      r.address,
      r.phone,
      r.contactPerson,
      r.isActive
    );
  }

  @DeleteMapping("/restaurants/{id}")
  public void deleteRestaurant(@PathVariable long id, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Restaurant r = restaurantRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
    if (!branchRepo.findByRestaurantId(r.id).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant has branches");
    }
    restaurantRepo.delete(r);
    auditService.log(u, "DELETE", "Restaurant", r.id, null);
  }

  // --- Branches ---
  public record BranchDto(
    long id,
    long tenantId,
    Long restaurantId,
    String name,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson,
    boolean isActive
  ) {}
  public record CreateBranchRequest(
    @NotBlank String name,
    Long restaurantId,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson
  ) {}
  public record UpdateBranchRequest(
    String name,
    Long restaurantId,
    String logoUrl,
    String country,
    String address,
    String phone,
    String contactPerson,
    Boolean isActive
  ) {}

  @GetMapping("/branches")
  public List<BranchDto> listBranches(
    @RequestParam(value = "tenantId", required = false) Long tenantId,
    @RequestParam(value = "restaurantId", required = false) Long restaurantId,
    @RequestParam(value = "isActive", required = false) Boolean isActive,
    Authentication auth
  ) {
    requireSuper(auth);
    List<Branch> list;
    if (tenantId != null && restaurantId != null) {
      list = branchRepo.findByTenantIdAndRestaurantId(tenantId, restaurantId);
    } else if (restaurantId != null) {
      list = branchRepo.findByRestaurantId(restaurantId);
    } else if (tenantId != null) {
      list = branchRepo.findByTenantId(tenantId);
    } else {
      list = branchRepo.findAll();
    }
    if (isActive != null) {
      list = list.stream().filter(b -> b.isActive == isActive).toList();
    }
    List<BranchDto> out = new ArrayList<>();
    for (Branch b : list) {
      out.add(new BranchDto(
        b.id,
        b.tenantId,
        b.restaurantId,
        b.name,
        b.logoUrl,
        b.country,
        b.address,
        b.phone,
        b.contactPerson,
        b.isActive
      ));
    }
    return out;
  }

  @PostMapping("/tenants/{tenantId}/branches")
  public BranchDto createBranch(@PathVariable long tenantId, @Valid @RequestBody CreateBranchRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Tenant t = tenantRepo.findById(tenantId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    if (!t.isActive) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant is inactive");
    }
    Long restaurantId = req.restaurantId;
    if (restaurantId == null) {
      restaurantId = restaurantRepo.findTop1ByTenantIdOrderByIdAsc(tenantId)
        .map(r -> r.id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant is required"));
    } else {
      Restaurant r = restaurantRepo.findById(restaurantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
      if (!r.tenantId.equals(tenantId)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant does not belong to tenant");
      }
    }
    Branch b = new Branch();
    b.tenantId = tenantId;
    b.restaurantId = restaurantId;
    b.name = req.name;
    b.logoUrl = sanitizePhotoUrl(req.logoUrl);
    b.country = trimOrNull(req.country);
    b.address = trimOrNull(req.address);
    b.phone = trimOrNull(req.phone);
    b.contactPerson = trimOrNull(req.contactPerson);
    b.isActive = true;
    b = branchRepo.save(b);
    auditService.log(u, "CREATE", "Branch", b.id, null);
    return new BranchDto(
      b.id,
      b.tenantId,
      b.restaurantId,
      b.name,
      b.logoUrl,
      b.country,
      b.address,
      b.phone,
      b.contactPerson,
      b.isActive
    );
  }

  @PatchMapping("/branches/{id}")
  public BranchDto updateBranch(@PathVariable long id, @RequestBody UpdateBranchRequest req, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Branch b = branchRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    if (req.name != null) b.name = req.name;
    if (req.restaurantId != null) {
      Restaurant r = restaurantRepo.findById(req.restaurantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurant not found"));
      if (!r.tenantId.equals(b.tenantId)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Restaurant does not belong to tenant");
      }
      b.restaurantId = r.id;
    }
    if (req.logoUrl != null) b.logoUrl = sanitizePhotoUrl(req.logoUrl);
    if (req.country != null) b.country = trimOrNull(req.country);
    if (req.address != null) b.address = trimOrNull(req.address);
    if (req.phone != null) b.phone = trimOrNull(req.phone);
    if (req.contactPerson != null) b.contactPerson = trimOrNull(req.contactPerson);
    if (req.isActive != null) b.isActive = req.isActive;
    b = branchRepo.save(b);
    auditService.log(u, "UPDATE", "Branch", b.id, null);
    return new BranchDto(
      b.id,
      b.tenantId,
      b.restaurantId,
      b.name,
      b.logoUrl,
      b.country,
      b.address,
      b.phone,
      b.contactPerson,
      b.isActive
    );
  }

  @DeleteMapping("/branches/{id}")
  public void deleteBranch(@PathVariable long id, Authentication auth) {
    StaffUser u = requireSuper(auth);
    Branch b = branchRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Branch not found"));
    branchRepo.delete(b);
    auditService.log(u, "DELETE", "Branch", b.id, null);
  }

  // --- Staff users (global create) ---
  public record StaffUserDto(
    long id,
    Long branchId,
    Long hallId,
    String username,
    String role,
    String permissions,
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
    Long hallId,
    String permissions,
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
    StaffUser u = requireSuper(auth);
    String role = req.role.trim().toUpperCase(Locale.ROOT);
    if (!Set.of(
      "WAITER", "HOST", "KITCHEN", "BAR",
      "CASHIER", "MARKETER", "ACCOUNTANT", "SUPPORT",
      "ADMIN", "MANAGER", "SUPER_ADMIN", "OWNER"
    ).contains(role)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
    }
    StaffUser su = new StaffUser();
    su.branchId = "SUPER_ADMIN".equals(role) ? null : req.branchId;
    su.hallId = normalizeHallId(req.hallId);
    if (su.branchId == null) {
      su.hallId = null;
    } else if (su.hallId != null) {
      validateHallScope(su.hallId, su.branchId);
    }
    su.username = req.username.trim();
    su.passwordHash = passwordEncoder.encode(req.password);
    su.role = role;
    try {
      su.permissions = PermissionUtils.normalize(req.permissions);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported permission");
    }
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
      su.id, su.branchId, su.hallId, su.username, su.role, su.permissions, su.isActive,
      su.firstName, su.lastName, su.age, su.gender, su.photoUrl,
      su.rating, su.recommended, su.experienceYears, su.favoriteItems
    );
  }

  public record UpdateStaffUserRequest(
    String password,
    String role,
    Long hallId,
    String permissions,
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
    StaffUser u = requireSuper(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    String prevRole = su.role;
    String prevPerms = normalizePermsForAudit(su.permissions);
    if (req.password != null && !req.password.isBlank()) {
      su.passwordHash = passwordEncoder.encode(req.password);
    }
    if (req.role != null) {
      String role = req.role.trim().toUpperCase(Locale.ROOT);
      if (!Set.of(
        "WAITER", "HOST", "KITCHEN", "BAR",
        "CASHIER", "MARKETER", "ACCOUNTANT", "SUPPORT",
        "ADMIN", "MANAGER", "SUPER_ADMIN", "OWNER"
      ).contains(role)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported role");
      }
      su.role = role;
    }
    if (req.hallId != null) {
      Long nextHallId = normalizeHallId(req.hallId);
      if (su.branchId == null) {
        nextHallId = null;
      } else if (nextHallId != null) {
        validateHallScope(nextHallId, su.branchId);
      }
      su.hallId = nextHallId;
    }
    if (req.permissions != null) {
      try {
        su.permissions = PermissionUtils.normalize(req.permissions);
      } catch (IllegalArgumentException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported permission");
      }
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
    auditService.log(u, "UPDATE", "StaffUser", su.id, null);
    String nextPerms = normalizePermsForAudit(su.permissions);
    if (!Objects.equals(prevRole, su.role) || !Objects.equals(prevPerms, nextPerms)) {
      Map<String, Object> details = new java.util.HashMap<>();
      if (!Objects.equals(prevRole, su.role)) {
        details.put("roleFrom", prevRole);
        details.put("roleTo", su.role);
      }
      if (!Objects.equals(prevPerms, nextPerms)) {
        details.put("permissionsFrom", prevPerms);
        details.put("permissionsTo", nextPerms);
      }
      auditService.log(u, "ROLE_PERMISSIONS_CHANGE", "StaffUser", su.id, toJsonSafe(details));
    }
    return new StaffUserDto(
      su.id, su.branchId, su.hallId, su.username, su.role, su.permissions, su.isActive,
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
    StaffUser u = requireSuper(auth);
    StaffUser su = staffUserRepo.findById(id)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    tableRepo.clearAssignedWaiter(su.id);
    staffUserRepo.delete(su);
    auditService.log(u, "DELETE", "StaffUser", su.id, null);
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

  public record BranchSummaryRow(
    long branchId,
    String branchName,
    Long restaurantId,
    String restaurantName,
    long ordersCount,
    long callsCount,
    long paidBillsCount,
    long grossCents,
    long tipsCents
  ) {}

  public record TopItemRow(long menuItemId, String name, long qty, long grossCents) {}

  public record WaiterMotivationRow(
    long staffUserId,
    String username,
    long ordersCount,
    long tipsCents,
    Double avgSlaMinutes
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
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    StatsService.Summary s = statsService.summaryForTenantFiltered(
      tenantId,
      fromTs,
      toTs,
      branchId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
    BranchReviewAgg reviewsAgg = branchReviewAggForTenant(tenantId, fromTs, toTs, branchId);
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
      reviewsAgg.avgRating,
      reviewsAgg.count
    );
  }

  private BranchReviewAgg branchReviewAggForTenant(Long tenantId, Instant fromTs, Instant toTs, Long branchId) {
    double sum = 0.0;
    long count = 0;
    List<Branch> branches = branchRepo.findByTenantId(tenantId);
    for (Branch b : branches) {
      if (branchId != null && !branchId.equals(b.id)) continue;
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
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    List<StatsService.BranchSummaryRow> rows = statsService.summaryByBranchForTenant(
      tenantId,
      fromTs,
      toTs,
      branchId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
    List<Long> branchIds = rows.stream().map(StatsService.BranchSummaryRow::branchId).toList();
    Map<Long, Branch> branchMap = branchRepo.findAllById(branchIds).stream()
      .collect(Collectors.toMap(b -> b.id, b -> b));
    Set<Long> restaurantIds = branchMap.values().stream()
      .map(b -> b.restaurantId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Map<Long, String> restaurantNames = restaurantIds.isEmpty()
      ? Map.of()
      : restaurantRepo.findAllById(restaurantIds).stream().collect(Collectors.toMap(r -> r.id, r -> r.name));
    List<BranchSummaryRow> out = new ArrayList<>();
    for (StatsService.BranchSummaryRow r : rows) {
      Branch b = branchMap.get(r.branchId());
      Long restaurantId = b == null ? null : b.restaurantId;
      String restaurantName = restaurantId == null ? null : restaurantNames.get(restaurantId);
      out.add(new BranchSummaryRow(
        r.branchId(),
        r.branchName(),
        restaurantId,
        restaurantName,
        r.ordersCount(),
        r.callsCount(),
        r.paidBillsCount(),
        r.grossCents(),
        r.tipsCents()
      ));
    }
    return out;
  }

  @GetMapping("/stats/top-items")
  public List<TopItemRow> getTopItems(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    int lim = limit == null ? 10 : limit;
    List<StatsService.TopItemRow> rows = statsService.topItemsForTenant(
      tenantId,
      fromTs,
      toTs,
      branchId,
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

  @GetMapping("/stats/top-items.csv")
  public ResponseEntity<String> getTopItemsCsv(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    @RequestParam(value = "limit", required = false) Integer limit,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    int lim = limit == null ? 50 : limit;
    List<StatsService.TopItemRow> rows = statsService.topItemsForTenant(
      tenantId,
      fromTs,
      toTs,
      branchId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs,
      lim
    );
    StringBuilder sb = new StringBuilder();
    sb.append("menu_item_id,name,qty,gross_cents\n");
    for (StatsService.TopItemRow r : rows) {
      sb.append(r.menuItemId()).append(',')
        .append(r.name() == null ? "" : r.name().replace(",", " ")).append(',')
        .append(r.qty()).append(',')
        .append(r.grossCents()).append('\n');
    }
    String filename = "tenant-top-items-" + tenantId + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
  }

  @GetMapping("/stats/top-waiters")
  public List<WaiterMotivationRow> getTopWaiters(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    List<StatsService.WaiterMotivationRow> rows = statsService.waiterMotivationForTenant(
      tenantId,
      fromTs,
      toTs,
      branchId,
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

  @GetMapping("/stats/top-waiters.csv")
  public ResponseEntity<String> getTopWaitersCsv(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    List<StatsService.WaiterMotivationRow> rows = statsService.waiterMotivationForTenant(
      tenantId,
      fromTs,
      toTs,
      branchId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
    StringBuilder sb = new StringBuilder();
    sb.append("staff_user_id,username,orders_count,tips_cents,avg_sla_minutes\n");
    for (StatsService.WaiterMotivationRow r : rows) {
      sb.append(r.staffUserId()).append(',')
        .append(r.username() == null ? "" : r.username().replace(",", " ")).append(',')
        .append(r.ordersCount()).append(',')
        .append(r.tipsCents()).append(',')
        .append(r.avgSlaMinutes() == null ? "" : r.avgSlaMinutes()).append('\n');
    }
    String filename = "tenant-top-waiters-" + tenantId + ".csv";
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType("text/csv"))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
      .body(sb.toString());
  }

  @GetMapping("/stats/summary.csv")
  public ResponseEntity<String> getSummaryCsv(
    @RequestParam(value = "tenantId") Long tenantId,
    @RequestParam(value = "from", required = false) String from,
    @RequestParam(value = "to", required = false) String to,
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "hallId", required = false) Long hallId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    StatsService.Summary s = statsService.summaryForTenantFiltered(
      tenantId,
      fromTs,
      toTs,
      branchId,
      hallId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
    BranchReviewAgg reviewsAgg = branchReviewAggForTenant(tenantId, fromTs, toTs, branchId);
    long avgCheckCents = s.paidBillsCount() == 0 ? 0L : (s.grossCents() / s.paidBillsCount());
    StringBuilder sb = new StringBuilder();
    sb.append("from,to,orders,calls,paid_bills,gross_cents,tips_cents,active_tables,avg_check_cents,avg_sla_minutes,avg_branch_rating,branch_reviews_count\n");
    sb.append(s.from()).append(',')
      .append(s.to()).append(',')
      .append(s.ordersCount()).append(',')
      .append(s.callsCount()).append(',')
      .append(s.paidBillsCount()).append(',')
      .append(s.grossCents()).append(',')
      .append(s.tipsCents()).append(',')
      .append(s.activeTablesCount()).append(',')
      .append(avgCheckCents).append(',')
      .append(s.avgSlaMinutes() == null ? "" : s.avgSlaMinutes()).append(',')
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
    @RequestParam(value = "branchId", required = false) Long branchId,
    @RequestParam(value = "status", required = false) String orderStatus,
    @RequestParam(value = "shiftFrom", required = false) String shiftFrom,
    @RequestParam(value = "shiftTo", required = false) String shiftTo,
    Authentication auth
  ) {
    requireSuper(auth);
    Instant fromTs = parseInstantOrDate(from, true);
    Instant toTs = parseInstantOrDate(to, false);
    Instant shiftFromTs = parseInstantOrDateOrNull(shiftFrom, true);
    Instant shiftToTs = parseInstantOrDateOrNull(shiftTo, false);
    List<StatsService.BranchSummaryRow> rows = statsService.summaryByBranchForTenant(
      tenantId,
      fromTs,
      toTs,
      branchId,
      orderStatus,
      shiftFromTs,
      shiftToTs
    );
    List<Long> branchIds = rows.stream().map(StatsService.BranchSummaryRow::branchId).toList();
    Map<Long, Branch> branchMap = branchRepo.findAllById(branchIds).stream()
      .collect(Collectors.toMap(b -> b.id, b -> b));
    Set<Long> restaurantIds = branchMap.values().stream()
      .map(b -> b.restaurantId)
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    Map<Long, String> restaurantNames = restaurantIds.isEmpty()
      ? Map.of()
      : restaurantRepo.findAllById(restaurantIds).stream().collect(Collectors.toMap(r -> r.id, r -> r.name));
    StringBuilder sb = new StringBuilder();
    sb.append("branch_id,branch_name,restaurant_id,restaurant_name,orders,calls,paid_bills,gross_cents,tips_cents\n");
    for (StatsService.BranchSummaryRow r : rows) {
      Branch b = branchMap.get(r.branchId());
      Long restaurantId = b == null ? null : b.restaurantId;
      String restaurantName = restaurantId == null ? "" : restaurantNames.getOrDefault(restaurantId, "");
      sb.append(r.branchId()).append(',')
        .append(r.branchName()).append(',')
        .append(restaurantId == null ? "" : restaurantId).append(',')
        .append(restaurantName.replace(",", " ")).append(',')
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

  private Set<String> effectivePermissions(StaffUser u) {
    Set<Permission> base = RolePermissions.forRole(u.role);
    Set<Permission> perms = base.isEmpty() ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(base);
    if (u.permissions != null && !u.permissions.isBlank()) {
      perms.addAll(PermissionUtils.parseLenient(u.permissions));
    }
    return perms.stream().map(Enum::name).collect(Collectors.toSet());
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

  private static Long normalizeHallId(Long hallId) {
    if (hallId == null) return null;
    return hallId <= 0 ? null : hallId;
  }

  private void validateHallScope(Long hallId, Long branchId) {
    if (hallId == null) return;
    if (branchId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall scope requires branch");
    }
    BranchHall hall = hallRepo.findById(hallId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hall not found"));
    if (!Objects.equals(hall.branchId, branchId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hall belongs to another branch");
    }
  }

  private static String maskToken(String token) {
    if (token == null || token.isBlank()) return null;
    String v = token.trim();
    if (v.length() <= 8) return "***";
    return v.substring(0, 4) + "..." + v.substring(v.length() - 4);
  }

  private static String trimTrailingSlash(String v) {
    if (v == null) return "";
    String s = v.trim();
    if (s.endsWith("/")) return s.substring(0, s.length() - 1);
    return s;
  }

  private static String normalizePermsForAudit(String v) {
    if (v == null) return "";
    String t = v.trim();
    return t.isEmpty() ? "" : t;
  }

  private static String toJsonSafe(Object payload) {
    if (payload == null) return null;
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
    } catch (Exception e) {
      return null;
    }
  }
}
