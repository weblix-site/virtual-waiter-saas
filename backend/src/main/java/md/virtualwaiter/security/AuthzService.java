package md.virtualwaiter.security;

import md.virtualwaiter.domain.StaffUser;
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
    return RolePermissions.forRole(user.role).contains(permission);
  }
}
