package md.virtualwaiter.security;

import md.virtualwaiter.domain.Branch;
import md.virtualwaiter.domain.CafeTable;
import md.virtualwaiter.domain.GuestSession;
import md.virtualwaiter.domain.Restaurant;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.repo.BranchRepo;
import md.virtualwaiter.repo.CafeTableRepo;
import md.virtualwaiter.repo.GuestSessionRepo;
import md.virtualwaiter.repo.RestaurantRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ReadOnlyFilter extends OncePerRequestFilter {
  private final StaffUserRepo staffUserRepo;
  private final BranchRepo branchRepo;
  private final RestaurantRepo restaurantRepo;
  private final CafeTableRepo cafeTableRepo;
  private final GuestSessionRepo guestSessionRepo;
  private final AuthzService authzService;

  public ReadOnlyFilter(
    StaffUserRepo staffUserRepo,
    BranchRepo branchRepo,
    RestaurantRepo restaurantRepo,
    CafeTableRepo cafeTableRepo,
    GuestSessionRepo guestSessionRepo,
    AuthzService authzService
  ) {
    this.staffUserRepo = staffUserRepo;
    this.branchRepo = branchRepo;
    this.restaurantRepo = restaurantRepo;
    this.cafeTableRepo = cafeTableRepo;
    this.guestSessionRepo = guestSessionRepo;
    this.authzService = authzService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String method = request.getMethod();
    if (HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method) || HttpMethod.OPTIONS.matches(method)) {
      return true;
    }
    String path = request.getRequestURI();
    return path.startsWith("/api/auth") || path.startsWith("/actuator");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {

    StaffUser user = resolveAuthUser();
    if (user != null && authzService.has(user, Permission.SUPERADMIN_ACCESS)) {
      filterChain.doFilter(request, response);
      return;
    }

    Long branchId = user != null ? user.branchId : null;
    if (branchId == null) {
      branchId = resolveBranchIdFromRequest(request);
    }

    if (branchId != null && isReadOnly(branchId)) {
      response.setStatus(HttpStatus.LOCKED.value());
      response.setContentType("text/plain");
      response.getWriter().write("Branch is read-only");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private StaffUser resolveAuthUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) return null;
    return staffUserRepo.findByUsername(auth.getName()).orElse(null);
  }

  private Long resolveBranchIdFromRequest(HttpServletRequest request) {
    Long branchId = parseLong(request.getParameter("branchId"));
    if (branchId != null) return branchId;

    Long tableId = parseLong(request.getParameter("tableId"));
    if (tableId != null) {
      Optional<CafeTable> table = cafeTableRepo.findById(tableId);
      if (table.isPresent()) return table.get().branchId;
    }

    String tablePublicId = request.getParameter("tablePublicId");
    if (tablePublicId != null && !tablePublicId.isBlank()) {
      Optional<CafeTable> table = cafeTableRepo.findByPublicId(tablePublicId);
      if (table.isPresent()) return table.get().branchId;
    }

    Long guestSessionId = parseLong(request.getParameter("guestSessionId"));
    if (guestSessionId != null) {
      Optional<GuestSession> session = guestSessionRepo.findById(guestSessionId);
      if (session.isPresent()) {
        Long sid = session.get().tableId;
        if (sid != null) {
          Optional<CafeTable> table = cafeTableRepo.findById(sid);
          if (table.isPresent()) return table.get().branchId;
        }
      }
    }

    return null;
  }

  private boolean isReadOnly(Long branchId) {
    Branch b = branchRepo.findById(branchId).orElse(null);
    if (b == null) return false;
    if (b.readOnly) return true;
    Restaurant r = restaurantRepo.findById(b.restaurantId).orElse(null);
    return r != null && r.readOnly;
  }

  private Long parseLong(String raw) {
    if (raw == null || raw.isBlank()) return null;
    try {
      return Long.parseLong(raw.trim());
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
