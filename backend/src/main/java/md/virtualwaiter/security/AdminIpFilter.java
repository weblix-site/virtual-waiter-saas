package md.virtualwaiter.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import md.virtualwaiter.domain.BranchSettings;
import md.virtualwaiter.domain.StaffUser;
import md.virtualwaiter.repo.BranchSettingsRepo;
import md.virtualwaiter.repo.StaffUserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;

@Component
@Order(2)
public class AdminIpFilter extends OncePerRequestFilter {
  private final StaffUserRepo staffUserRepo;
  private final BranchSettingsRepo branchSettingsRepo;

  public AdminIpFilter(StaffUserRepo staffUserRepo, BranchSettingsRepo branchSettingsRepo) {
    this.staffUserRepo = staffUserRepo;
    this.branchSettingsRepo = branchSettingsRepo;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getName() != null) {
      StaffUser user = staffUserRepo.findByUsername(auth.getName()).orElse(null);
      if (user != null && isAdminScoped(user)) {
        BranchSettings s = user.branchId == null ? null : branchSettingsRepo.findById(user.branchId).orElse(null);
        if (s != null) {
          String allow = s.adminIpAllowlist;
          String deny = s.adminIpDenylist;
          if (!isBlank(allow) || !isBlank(deny)) {
            String clientIp = resolveClientIp(request);
            if (isBlank(clientIp)) {
              if (!isBlank(allow)) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "IP not allowed");
                return;
              }
            } else {
              if (matchesAny(clientIp, deny)) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "IP denied");
                return;
              }
              if (!isBlank(allow) && !matchesAny(clientIp, allow)) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "IP not allowed");
                return;
              }
            }
          }
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private boolean isAdminScoped(StaffUser user) {
    if (user.role == null) return false;
    String r = user.role.trim().toUpperCase(Locale.ROOT);
    return switch (r) {
      case "SUPER_ADMIN" -> false;
      case "ADMIN", "OWNER", "MANAGER", "CASHIER", "MARKETER", "ACCOUNTANT", "SUPPORT" -> true;
      default -> false;
    };
  }

  private String resolveClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      String first = xff.split(",")[0].trim();
      if (!first.isBlank()) {
        return stripPort(first);
      }
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return stripPort(realIp.trim());
    }
    String remote = request.getRemoteAddr();
    return remote == null ? null : stripPort(remote);
  }

  private String stripPort(String ip) {
    if (ip == null) return null;
    String s = ip.trim();
    if (s.startsWith("[") && s.contains("]")) {
      return s.substring(1, s.indexOf(']'));
    }
    int colon = s.lastIndexOf(':');
    if (colon > 0 && s.indexOf('.') > 0) {
      return s.substring(0, colon);
    }
    return s;
  }

  private boolean matchesAny(String ip, String list) {
    if (isBlank(list)) return false;
    String[] parts = list.split("[,\\n\\r\\t ;]+");
    for (String p : parts) {
      String t = p.trim();
      if (t.isEmpty()) continue;
      if (matches(ip, t)) return true;
    }
    return false;
  }

  private boolean matches(String ip, String rule) {
    try {
      if (rule.contains("/")) {
        String[] parts = rule.split("/");
        if (parts.length != 2) return false;
        InetAddress net = InetAddress.getByName(parts[0].trim());
        int prefix = Integer.parseInt(parts[1].trim());
        InetAddress addr = InetAddress.getByName(ip);
        return matchesCidr(addr, net, prefix);
      }
      InetAddress addr = InetAddress.getByName(ip);
      InetAddress ruleAddr = InetAddress.getByName(rule);
      return addr.equals(ruleAddr);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean matchesCidr(InetAddress addr, InetAddress net, int prefix) {
    byte[] a = addr.getAddress();
    byte[] n = net.getAddress();
    if (a.length != n.length) return false;
    int bits = prefix;
    int i = 0;
    while (bits >= 8) {
      if (a[i] != n[i]) return false;
      bits -= 8;
      i++;
    }
    if (bits <= 0) return true;
    int mask = (0xff << (8 - bits)) & 0xff;
    return (a[i] & mask) == (n[i] & mask);
  }

  private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
