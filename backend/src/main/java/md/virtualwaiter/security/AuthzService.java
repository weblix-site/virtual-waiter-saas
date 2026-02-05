package md.virtualwaiter.security;

import md.virtualwaiter.domain.StaffUser;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthzService {

  public void require(StaffUser user, Permission permission) {
    if (!has(user, permission)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permission");
    }
  }

  public boolean has(StaffUser user, Permission permission) {
    if (user == null) return false;
    Set<Permission> base = RolePermissions.forRole(user.role);
    Set<Permission> perms = base.isEmpty() ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(base);
    if (user.permissions != null && !user.permissions.isBlank()) {
      perms.addAll(PermissionUtils.parseLenient(user.permissions));
    }
    return perms.contains(permission);
  }
}
