package md.virtualwaiter.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class RolePermissions {
  private static final Set<Permission> ADMIN_ALL = EnumSet.of(
    Permission.ADMIN_ACCESS,
    Permission.STAFF_VIEW,
    Permission.STAFF_MANAGE,
    Permission.MENU_VIEW,
    Permission.MENU_MANAGE,
    Permission.REPORTS_VIEW,
    Permission.AUDIT_VIEW,
    Permission.SETTINGS_MANAGE,
    Permission.PAYMENTS_MANAGE,
    Permission.INVENTORY_MANAGE,
    Permission.LOYALTY_MANAGE,
    Permission.GUEST_FLAGS_MANAGE,
    Permission.MEDIA_MANAGE,
    Permission.HALL_PLAN_MANAGE
  );

  private static final Set<Permission> SUPERADMIN_ALL = EnumSet.of(
    Permission.SUPERADMIN_ACCESS,
    Permission.ADMIN_ACCESS,
    Permission.STAFF_VIEW,
    Permission.STAFF_MANAGE,
    Permission.MENU_VIEW,
    Permission.MENU_MANAGE,
    Permission.REPORTS_VIEW,
    Permission.AUDIT_VIEW,
    Permission.SETTINGS_MANAGE,
    Permission.PAYMENTS_MANAGE,
    Permission.INVENTORY_MANAGE,
    Permission.LOYALTY_MANAGE,
    Permission.GUEST_FLAGS_MANAGE,
    Permission.MEDIA_MANAGE,
    Permission.HALL_PLAN_MANAGE,
    Permission.FEATURE_FLAGS_MANAGE
  );

  private static final Set<Permission> CASHIER_PERMS = EnumSet.of(
    Permission.ADMIN_ACCESS,
    Permission.REPORTS_VIEW,
    Permission.PAYMENTS_MANAGE
  );

  private static final Set<Permission> MARKETER_PERMS = EnumSet.of(
    Permission.ADMIN_ACCESS,
    Permission.REPORTS_VIEW,
    Permission.LOYALTY_MANAGE,
    Permission.MENU_VIEW
  );

  private static final Set<Permission> ACCOUNTANT_PERMS = EnumSet.of(
    Permission.ADMIN_ACCESS,
    Permission.REPORTS_VIEW,
    Permission.PAYMENTS_MANAGE,
    Permission.AUDIT_VIEW
  );

  private static final Set<Permission> SUPPORT_PERMS = EnumSet.of(
    Permission.ADMIN_ACCESS,
    Permission.AUDIT_VIEW,
    Permission.REPORTS_VIEW
  );

  private RolePermissions() {}

  public static Set<Permission> forRole(String role) {
    if (role == null) return Collections.emptySet();
    String r = role.trim().toUpperCase(Locale.ROOT);
    return switch (r) {
      case "SUPER_ADMIN" -> SUPERADMIN_ALL;
      case "OWNER", "ADMIN", "MANAGER" -> ADMIN_ALL;
      case "CASHIER" -> CASHIER_PERMS;
      case "MARKETER" -> MARKETER_PERMS;
      case "ACCOUNTANT" -> ACCOUNTANT_PERMS;
      case "SUPPORT" -> SUPPORT_PERMS;
      default -> Collections.emptySet();
    };
  }
}
