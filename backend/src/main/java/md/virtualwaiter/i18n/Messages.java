package md.virtualwaiter.i18n;

import java.util.HashMap;
import java.util.Map;

public final class Messages {
  private static final Map<String, String> EXACT = new HashMap<>();

  static {
    // Auth / access
    put("No auth", "error.no_auth");
    put("Unknown user", "error.unknown_user");
    put("Super admin required", "error.super_admin_required");
    put("Insufficient role", "error.insufficient_role");
    put("User has no branch", "error.user_no_branch");
    put("Admin key required", "error.admin_key_required");
    put("Admin role required", "error.admin_role_required");
    put("Super admin role required", "error.super_admin_role_required");

    // Tenants / branches / staff
    put("Tenant not found", "error.tenant_not_found");
    put("Tenant is inactive", "error.tenant_inactive");
    put("Branch not found", "error.branch_not_found");
    put("Unsupported role", "error.unsupported_role");
    put("Staff user not found", "error.staff_user_not_found");
    put("Currency not found", "error.currency_not_found");

    // QR / sessions / table / menu
    put("Invalid QR signature", "error.invalid_qr_signature");
    put("Table not found", "error.table_not_found");
    put("Menu item not found", "error.menu_item_not_found");
    put("Category not found", "error.category_not_found");
    put("Wrong branch", "error.wrong_branch");
    put("Session not found", "error.session_not_found");
    put("Session expired", "error.session_expired");
    put("Session secret not set", "error.session_secret_not_set");
    put("Invalid session secret", "error.invalid_session_secret");
    put("No active call", "error.no_active_call");
    put("guestSessionId required", "error.guest_session_id_required");

    // Rate limits
    put("Too many session starts from IP", "error.too_many_session_starts");
    put("Too many menu requests from IP", "error.too_many_menu_requests");
    put("Too many orders from IP", "error.too_many_orders_from_ip");
    put("Too many orders", "error.too_many_orders");
    put("Too many party requests from IP", "error.too_many_party_requests");
    put("Too many OTP requests from IP", "error.too_many_otp_requests");
    put("Too many OTP verify attempts from IP", "error.too_many_otp_verify");
    put("Too many waiter calls from IP", "error.too_many_waiter_calls");
    put("Too many calls", "error.too_many_calls");
    put("Too many bill requests", "error.too_many_bill_requests");

    // Orders / items / modifiers
    put("OTP required before first order", "error.otp_required_first_order");
    put("Order items empty", "error.order_items_empty");
    put("Order not found", "error.order_not_found");
    put("Order does not belong to session", "error.order_not_belong_session");
    put("Invalid modifiersJson", "error.invalid_modifiers_json");
    put("Modifiers not allowed for item", "error.modifiers_not_allowed");
    put("No unpaid items", "error.no_unpaid_items");
    put("Tips are disabled", "error.tips_disabled");
    put("Unsupported tips percent", "error.unsupported_tips_percent");

    // Party
    put("Party PIN is disabled", "error.party_pin_disabled");
    put("Failed to allocate PIN", "error.failed_allocate_pin");
    put("PIN must be 4 digits", "error.pin_must_be_4_digits");
    put("Party not found or expired", "error.party_not_found_or_expired");
    put("Not in a party", "error.not_in_party");
    put("Party not found", "error.party_not_found");
    put("OTP required before joining a party", "error.otp_required_before_party");

    // Bill requests
    put("BillRequest not found", "error.billrequest_not_found");
    put("Bill request not found", "error.billrequest_not_found");
    put("Bill request does not belong to session", "error.billrequest_not_belong_session");
    put("Bill request is not active", "error.billrequest_not_active");
    put("Bill request is not confirmed", "error.billrequest_not_confirmed");
    put("Unsupported paymentMethod", "error.unsupported_payment_method");
    put("Cash payment is disabled", "error.cash_disabled");
    put("Terminal payment is disabled", "error.terminal_disabled");
    put("Online payment disabled", "error.online_pay_disabled");
    put("Unsupported payment provider", "error.unsupported_payment_provider");
    put("Online payment provider required", "error.online_pay_provider_required");
    put("Amount invalid", "error.amount_invalid");
    put("Unsupported mode", "error.unsupported_mode");
    put("No orders for this session", "error.no_orders_for_session");
    put("orderItemIds required for SELECTED", "error.order_item_ids_required");
    put("Party required to pay other guests items", "error.party_required_pay_other");
    put("No orders for this table", "error.no_orders_for_table");
    put("Whole table payment is disabled", "error.whole_table_disabled");
    put("Party required to pay whole table", "error.party_required_pay_whole");
    put("Promo code not found", "error.promo_code_not_found");
    put("Promo code inactive", "error.promo_code_inactive");
    put("Promo code expired", "error.promo_code_expired");
    put("Promo code not active", "error.promo_code_not_active");
    put("Promo code exhausted", "error.promo_code_exhausted");
    put("Promo code invalid", "error.promo_code_invalid");

    // Admin layout / data
    put("layoutX + layoutW must be <= 100", "error.layout_x_w_exceed");
    put("layoutY + layoutH must be <= 100", "error.layout_y_h_exceed");
    put("layoutX out of range", "error.layout_x_out_of_range");
    put("layoutY out of range", "error.layout_y_out_of_range");
    put("layoutW out of range", "error.layout_w_out_of_range");
    put("layoutH out of range", "error.layout_h_out_of_range");
    put("hallId does not match planId", "error.hall_id_plan_mismatch");
    put("branchId is required", "error.branch_id_required");
    put("zonesJson must be array", "error.zones_json_array");
    put("zonesJson too large", "error.zones_json_too_large");
    put("zonesJson too many zones", "error.zones_json_too_many");
    put("zonesJson invalid zone", "error.zones_json_invalid_zone");
    put("zonesJson zone id too long", "error.zones_json_zone_id_too_long");
    put("zonesJson zone name too long", "error.zones_json_zone_name_too_long");
    put("zonesJson width must be > 0", "error.zones_json_width");
    put("zonesJson height must be > 0", "error.zones_json_height");
    put("zonesJson color invalid", "error.zones_json_color_invalid");
    put("zonesJson invalid", "error.zones_json_invalid");
    put("Unknown currency", "error.unknown_currency");
    put("Currency is inactive", "error.currency_inactive");
    put("tables required", "error.tables_required");
    put("tableIds required", "error.table_ids_required");
    put("Waiter not found", "error.waiter_not_found");
    put("Hall not found", "error.hall_not_found");
    put("Hall does not belong to branch", "error.hall_wrong_branch");
    put("Hall has tables assigned", "error.hall_has_tables");
    put("Body required", "error.body_required");
    put("Template not found", "error.template_not_found");
    put("Version not found", "error.version_not_found");
    put("Version does not belong to plan", "error.version_not_belong_plan");
    put("Modifier group not found", "error.modifier_group_not_found");
    put("Modifier option not found", "error.modifier_option_not_found");

    // Staff / kitchen
    put("Plan not found", "error.plan_not_found");
    put("Plan does not belong to hall", "error.plan_not_belong_hall");
    put("tableId or guestSessionId required", "error.table_or_session_required");
    put("Guest session not found", "error.guest_session_not_found");
    put("guestSessionId does not belong to tableId", "error.guest_session_not_belong_table");
    put("Invalid since", "error.invalid_since");
    put("Missing status", "error.missing_status");
    put("Unsupported status", "error.unsupported_status");
    put("Invalid status transition", "error.invalid_status_transition");
    put("Waiter call not found", "error.waiter_call_not_found");
    put("Party not found", "error.party_not_found");
    put("token/platform required", "error.token_platform_required");
    put("token required", "error.token_required");

    // OTP
    put("OTP resend cooldown", "error.otp_resend_cooldown");
    put("Challenge not found", "error.challenge_not_found");
    put("Challenge does not belong to session", "error.challenge_not_belong_session");
    put("Challenge not active", "error.challenge_not_active");
    put("OTP expired", "error.otp_expired");
    put("OTP locked", "error.otp_locked");
    put("Invalid OTP", "error.invalid_otp");
  }

  private Messages() {}

  public static Resolved resolve(String raw) {
    if (raw == null) return null;
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) return null;

    // Exact matches
    String exactKey = EXACT.get(trimmed);
    if (exactKey != null) return new Resolved(exactKey, new Object[0]);

    // Prefix matches with ids
    if (trimmed.startsWith("Unknown menu item: ")) {
      return new Resolved("error.unknown_menu_item", new Object[] { suffix(trimmed) });
    }
    if (trimmed.startsWith("Missing required modifiers for group ")) {
      return new Resolved("error.missing_required_modifiers", new Object[] { suffix(trimmed) });
    }
    if (trimmed.startsWith("Unknown modifier option: ")) {
      return new Resolved("error.unknown_modifier_option", new Object[] { suffix(trimmed) });
    }
    if (trimmed.startsWith("Modifier option not allowed for this item: ")) {
      return new Resolved("error.modifier_option_not_allowed", new Object[] { suffix(trimmed) });
    }
    if (trimmed.startsWith("Not enough modifiers for group ")) {
      return new Resolved("error.not_enough_modifiers", new Object[] { suffix(trimmed) });
    }
    if (trimmed.startsWith("Too many modifiers for group ")) {
      return new Resolved("error.too_many_modifiers", new Object[] { suffix(trimmed) });
    }
    if (trimmed.startsWith("Order item not available: ")) {
      return new Resolved("error.order_item_not_available", new Object[] { suffix(trimmed) });
    }
    if (trimmed.startsWith("Order item missing: ")) {
      return new Resolved("error.order_item_missing", new Object[] { suffix(trimmed) });
    }

    return new Resolved(trimmed, new Object[0]);
  }

  private static String suffix(String raw) {
    int idx = raw.indexOf(": ");
    return idx >= 0 ? raw.substring(idx + 2) : raw;
  }

  private static void put(String key, String msgKey) {
    EXACT.put(key, msgKey);
  }

  public record Resolved(String key, Object[] args) {}
}
