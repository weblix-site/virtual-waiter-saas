package md.virtualwaiter.service;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class StatsService {
  private final NamedParameterJdbcTemplate jdbc;

  public StatsService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Summary(
    Instant from,
    Instant to,
    long ordersCount,
    long callsCount,
    long paidBillsCount,
    long grossCents,
    long tipsCents,
    long activeTablesCount,
    Double avgSlaMinutes
  ) {}

  public Summary summaryForBranch(long branchId, Instant from, Instant to) {
    return summaryForBranchFiltered(branchId, from, to, null, null, null);
  }

  public Summary summaryForTenant(long tenantId, Instant from, Instant to) {
    return summaryForTenantFiltered(tenantId, from, to, null, null, null, null, null, null);
  }

  public record DailyRow(
    String day,
    long ordersCount,
    long callsCount,
    long paidBillsCount,
    long grossCents,
    long tipsCents
  ) {}

  public java.util.List<DailyRow> dailyForBranch(long branchId, Instant from, Instant to) {
    return dailyForBranchFiltered(branchId, from, to, null, null, null);
  }

  public Summary summaryForBranchFiltered(long branchId, Instant from, Instant to, Long tableId, Long waiterId, Long hallId) {
    return summaryForBranchFiltered(branchId, from, to, tableId, waiterId, hallId, null, null, null, null);
  }

  public Summary summaryForBranchFiltered(
    long branchId,
    Instant from,
    Instant to,
    Long tableId,
    Long waiterId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo
  ) {
    return summaryForBranchFiltered(branchId, from, to, tableId, waiterId, hallId, orderStatus, shiftFrom, shiftTo, null);
  }

  public Summary summaryForBranchFiltered(
    long branchId,
    Instant from,
    Instant to,
    Long tableId,
    Long waiterId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("branchId", branchId);
    params.put("waiterId", waiterId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    String tableFilter = buildTableFilter(params, tableId, hallId);
    return runSummary(tableFilter, params);
  }

  public java.util.List<DailyRow> dailyForBranchFiltered(long branchId, Instant from, Instant to, Long tableId, Long waiterId, Long hallId) {
    return dailyForBranchFiltered(branchId, from, to, tableId, waiterId, hallId, null, null, null, null);
  }

  public java.util.List<DailyRow> dailyForBranchFiltered(
    long branchId,
    Instant from,
    Instant to,
    Long tableId,
    Long waiterId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo
  ) {
    return dailyForBranchFiltered(branchId, from, to, tableId, waiterId, hallId, orderStatus, shiftFrom, shiftTo, null);
  }

  public java.util.List<DailyRow> dailyForBranchFiltered(
    long branchId,
    Instant from,
    Instant to,
    Long tableId,
    Long waiterId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("branchId", branchId);
    params.put("waiterId", waiterId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    String tableFilter = buildTableFilter(params, tableId, hallId);
    return runDaily(tableFilter, params);
  }

  public record TopItemRow(long menuItemId, String name, long qty, long grossCents) {}
  public record TopCategoryRow(long categoryId, String name, long qty, long grossCents) {}

  public java.util.List<TopItemRow> topItemsForBranch(long branchId, Instant from, Instant to, Long tableId, Long waiterId, Long hallId, int limit) {
    return topItemsForBranch(branchId, from, to, tableId, waiterId, hallId, null, null, null, null, limit);
  }

  public java.util.List<TopItemRow> topItemsForBranch(
    long branchId,
    Instant from,
    Instant to,
    Long tableId,
    Long waiterId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone,
    int limit
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("branchId", branchId);
    params.put("waiterId", waiterId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    params.put("limit", Math.max(1, Math.min(limit, 200)));
    String tableFilter = buildTableFilter(params, tableId, hallId);
    String sql =
      "SELECT oi.menu_item_id AS menu_item_id,\n" +
      "       COALESCE(oi.name_snapshot, mi.name_ru, 'Unknown') AS name,\n" +
      "       COALESCE(SUM(oi.qty),0) AS qty,\n" +
      "       COALESCE(SUM(bri.line_total_cents),0) AS gross_cents\n" +
      "FROM bill_request_items bri\n" +
      "JOIN bill_requests br ON br.id = bri.bill_request_id\n" +
      "JOIN order_items oi ON oi.id = bri.order_item_id\n" +
      "JOIN orders o ON o.id = oi.order_id\n" +
      "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "JOIN tables t ON t.id = br.table_id\n" +
      "LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id\n" +
      "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "  AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "  AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId)\n" +
      "  AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "  AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "  AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "GROUP BY oi.menu_item_id, name\n" +
      "ORDER BY gross_cents DESC\n" +
      "LIMIT :limit";
    return jdbc.query(sql, params, (rs, rowNum) -> new TopItemRow(
      rs.getLong("menu_item_id"),
      rs.getString("name"),
      rs.getLong("qty"),
      rs.getLong("gross_cents")
    ));
  }

  public java.util.List<TopCategoryRow> topCategoriesForBranch(long branchId, Instant from, Instant to, Long tableId, Long waiterId, Long hallId, int limit) {
    return topCategoriesForBranch(branchId, from, to, tableId, waiterId, hallId, null, null, null, null, limit);
  }

  public java.util.List<TopCategoryRow> topCategoriesForBranch(
    long branchId,
    Instant from,
    Instant to,
    Long tableId,
    Long waiterId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone,
    int limit
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("branchId", branchId);
    params.put("waiterId", waiterId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    params.put("limit", Math.max(1, Math.min(limit, 200)));
    String tableFilter = buildTableFilter(params, tableId, hallId);
    String sql =
      "SELECT mc.id AS category_id,\n" +
      "       COALESCE(mc.name_ru, 'Unknown') AS name,\n" +
      "       COALESCE(SUM(oi.qty),0) AS qty,\n" +
      "       COALESCE(SUM(bri.line_total_cents),0) AS gross_cents\n" +
      "FROM bill_request_items bri\n" +
      "JOIN bill_requests br ON br.id = bri.bill_request_id\n" +
      "JOIN order_items oi ON oi.id = bri.order_item_id\n" +
      "JOIN orders o ON o.id = oi.order_id\n" +
      "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "JOIN tables t ON t.id = br.table_id\n" +
      "LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id\n" +
      "LEFT JOIN menu_categories mc ON mc.id = mi.category_id\n" +
      "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "  AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "  AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId)\n" +
      "  AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "  AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "  AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "GROUP BY mc.id, name\n" +
      "ORDER BY gross_cents DESC\n" +
      "LIMIT :limit";
    return jdbc.query(sql, params, (rs, rowNum) -> new TopCategoryRow(
      rs.getLong("category_id"),
      rs.getString("name"),
      rs.getLong("qty"),
      rs.getLong("gross_cents")
    ));
  }

  public record BranchSummaryRow(
    long branchId,
    String branchName,
    long ordersCount,
    long callsCount,
    long paidBillsCount,
    long grossCents,
    long tipsCents
  ) {}

  public record WaiterMotivationRow(
    long staffUserId,
    String username,
    long ordersCount,
    long tipsCents,
    Double avgSlaMinutes
  ) {}

  public java.util.List<WaiterMotivationRow> waiterMotivationForBranch(long branchId, Instant from, Instant to, Long hallId) {
    return waiterMotivationForBranch(branchId, from, to, hallId, null, null, null, null);
  }

  public java.util.List<WaiterMotivationRow> waiterMotivationForBranch(
    long branchId,
    Instant from,
    Instant to,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("branchId", branchId);
    params.put("hallId", hallId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    String sql =
      "WITH tips AS (\n" +
      "  SELECT o.handled_by_staff_id AS staff_id, COALESCE(SUM(br.tips_amount_cents),0) AS tips_cents\n" +
      "  FROM bill_requests br\n" +
      "  JOIN bill_request_items bri ON bri.bill_request_id = br.id\n" +
      "  JOIN order_items oi ON oi.id = bri.order_item_id\n" +
      "  JOIN orders o ON o.id = oi.order_id\n" +
      "  LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "  JOIN tables t ON t.id = br.table_id\n" +
      "  WHERE br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:hallId IS NULL OR t.hall_id = :hallId)\n" +
      "    AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY o.handled_by_staff_id\n" +
      ")\n" +
      "SELECT su.id AS staff_id, su.username AS username,\n" +
      "  COALESCE(COUNT(o.id) FILTER (WHERE (:hallId IS NULL OR t.hall_id = :hallId)\n" +
      "    AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)),0) AS orders_count,\n" +
      "  COALESCE(tips.tips_cents,0) AS tips_cents,\n" +
      "  AVG(EXTRACT(EPOCH FROM (o.ready_at - o.created_at))/60.0)\n" +
      "    FILTER (WHERE o.ready_at IS NOT NULL AND (:hallId IS NULL OR t.hall_id = :hallId)\n" +
      "      AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "      AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "      AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "      AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo))\n" +
      "    AS avg_sla_minutes\n" +
      "FROM staff_users su\n" +
      "LEFT JOIN orders o ON o.handled_by_staff_id = su.id AND o.created_at BETWEEN :fromTs AND :toTs\n" +
      "LEFT JOIN tables t ON t.id = o.table_id\n" +
      "LEFT JOIN tips ON tips.staff_id = su.id\n" +
      "WHERE su.branch_id = :branchId AND su.role = 'WAITER'\n" +
      "GROUP BY su.id, su.username, tips.tips_cents\n" +
      "ORDER BY orders_count DESC, tips_cents DESC";
    return jdbc.query(sql, params, (rs, rowNum) -> new WaiterMotivationRow(
      rs.getLong("staff_id"),
      rs.getString("username"),
      rs.getLong("orders_count"),
      rs.getLong("tips_cents"),
      (Double) rs.getObject("avg_sla_minutes")
    ));
  }

  public java.util.List<BranchSummaryRow> summaryByBranchForTenant(long tenantId, Instant from, Instant to) {
    return summaryByBranchForTenant(tenantId, from, to, null, null, null, null, null);
  }

  public java.util.List<BranchSummaryRow> summaryByBranchForTenant(
    long tenantId,
    Instant from,
    Instant to,
    Long branchId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("tenantId", tenantId);
    params.put("branchId", branchId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    String sql =
      "SELECT b.id AS branch_id, b.name AS branch_name,\n" +
      "  COALESCE(o.cnt,0) AS orders_count,\n" +
      "  COALESCE(c.cnt,0) AS calls_count,\n" +
      "  COALESCE(br.cnt,0) AS paid_bills_count,\n" +
      "  COALESCE(br.gross,0) AS gross_cents,\n" +
      "  COALESCE(br.tips,0) AS tips_cents\n" +
      "FROM branches b\n" +
      "LEFT JOIN (\n" +
      "  SELECT t.branch_id, COUNT(*) AS cnt\n" +
      "  FROM orders o\n" +
      "  JOIN tables t ON t.id = o.table_id\n" +
      "  LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "  WHERE o.created_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY t.branch_id\n" +
      ") o ON o.branch_id = b.id\n" +
      "LEFT JOIN (\n" +
      "  SELECT t.branch_id, COUNT(*) AS cnt\n" +
      "  FROM waiter_calls wc\n" +
      "  JOIN tables t ON t.id = wc.table_id\n" +
      "  LEFT JOIN staff_users su ON su.id = t.assigned_waiter_id\n" +
      "  WHERE wc.created_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:guestPhone IS NULL OR 1=0)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY t.branch_id\n" +
      ") c ON c.branch_id = b.id\n" +
      "LEFT JOIN (\n" +
      "  SELECT t.branch_id, COUNT(*) AS cnt, COALESCE(SUM(br.total_cents),0) AS gross, COALESCE(SUM(br.tips_amount_cents),0) AS tips\n" +
      "  FROM bill_requests br\n" +
      "  JOIN tables t ON t.id = br.table_id\n" +
      "  JOIN bill_request_items bri ON bri.bill_request_id = br.id\n" +
      "  JOIN order_items oi ON oi.id = bri.order_item_id\n" +
      "  JOIN orders o ON o.id = oi.order_id\n" +
      "  LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "  WHERE br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY t.branch_id\n" +
      ") br ON br.branch_id = b.id\n" +
      "WHERE b.tenant_id = :tenantId\n" +
      "  AND (:branchId IS NULL OR b.id = :branchId)\n" +
      "ORDER BY b.name ASC";

    return jdbc.query(sql, params, (rs, rowNum) -> new BranchSummaryRow(
      rs.getLong("branch_id"),
      rs.getString("branch_name"),
      rs.getLong("orders_count"),
      rs.getLong("calls_count"),
      rs.getLong("paid_bills_count"),
      rs.getLong("gross_cents"),
      rs.getLong("tips_cents")
    ));
  }

  private java.util.List<DailyRow> runDaily(String tableFilter, Map<String, Object> params) {
    String sql =
      "WITH days AS (\n" +
      "  SELECT generate_series(date_trunc('day', :fromTs::timestamptz), date_trunc('day', :toTs::timestamptz), interval '1 day') AS day\n" +
      "),\n" +
      "orders AS (\n" +
      "  SELECT date_trunc('day', o.created_at) AS day, COUNT(*) AS cnt\n" +
      "  FROM orders o\n" +
      "  JOIN tables t ON t.id = o.table_id\n" +
      "  LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "  WHERE " + tableFilter + " AND o.created_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY 1\n" +
      "),\n" +
      "calls AS (\n" +
      "  SELECT date_trunc('day', wc.created_at) AS day, COUNT(*) AS cnt\n" +
      "  FROM waiter_calls wc\n" +
      "  JOIN tables t ON t.id = wc.table_id\n" +
      "  LEFT JOIN staff_users su ON su.id = t.assigned_waiter_id\n" +
      "  WHERE " + tableFilter + " AND wc.created_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:guestPhone IS NULL OR 1=0)\n" +
      "    AND (:waiterId IS NULL OR t.assigned_waiter_id = :waiterId)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY 1\n" +
      "),\n" +
      "bills AS (\n" +
      "  SELECT date_trunc('day', br.confirmed_at) AS day,\n" +
      "         COUNT(DISTINCT br.id) AS cnt,\n" +
      "         COALESCE(SUM(bri.line_total_cents +\n" +
      "           COALESCE((COALESCE(br.tips_amount_cents,0) * bri.line_total_cents) / NULLIF(br.subtotal_cents,0),0)),0) AS gross,\n" +
      "         COALESCE(SUM(COALESCE((COALESCE(br.tips_amount_cents,0) * bri.line_total_cents) / NULLIF(br.subtotal_cents,0),0)),0) AS tips\n" +
      "  FROM bill_requests br\n" +
      "  JOIN tables t ON t.id = br.table_id\n" +
      "  JOIN bill_request_items bri ON bri.bill_request_id = br.id\n" +
      "  JOIN order_items oi ON oi.id = bri.order_item_id\n" +
      "  JOIN orders o ON o.id = oi.order_id\n" +
      "  LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "  WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY 1\n" +
      ")\n" +
      "SELECT d.day,\n" +
      "       COALESCE(o.cnt,0) AS orders_count,\n" +
      "       COALESCE(c.cnt,0) AS calls_count,\n" +
      "       COALESCE(b.cnt,0) AS paid_bills_count,\n" +
      "       COALESCE(b.gross,0) AS gross_cents,\n" +
      "       COALESCE(b.tips,0) AS tips_cents\n" +
      "FROM days d\n" +
      "LEFT JOIN orders o ON o.day = d.day\n" +
      "LEFT JOIN calls c ON c.day = d.day\n" +
      "LEFT JOIN bills b ON b.day = d.day\n" +
      "ORDER BY d.day ASC";

    return jdbc.query(sql, params, (rs, rowNum) -> new DailyRow(
      rs.getTimestamp("day").toInstant().toString().substring(0, 10),
      rs.getLong("orders_count"),
      rs.getLong("calls_count"),
      rs.getLong("paid_bills_count"),
      rs.getLong("gross_cents"),
      rs.getLong("tips_cents")
    ));
  }

  private Map<String, Object> baseParams(Instant from, Instant to) {
    Map<String, Object> params = new HashMap<>();
    params.put("fromTs", Timestamp.from(from));
    params.put("toTs", Timestamp.from(to));
    return params;
  }

  private String buildTableFilter(Map<String, Object> params, Long tableId, Long hallId) {
    StringBuilder sb = new StringBuilder("t.branch_id = :branchId");
    if (tableId != null) {
      sb.append(" AND t.id = :tableId");
      params.put("tableId", tableId);
    }
    if (hallId != null) {
      sb.append(" AND t.hall_id = :hallId");
      params.put("hallId", hallId);
    }
    return sb.toString();
  }

  private Summary runSummary(String tableFilter, Map<String, Object> params) {
    long ordersCount = queryLong(
      "SELECT COUNT(*) FROM orders o " +
        "JOIN tables t ON t.id = o.table_id " +
        "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id " +
        "WHERE " + tableFilter + " AND o.created_at BETWEEN :fromTs AND :toTs " +
        "AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone) " +
        "AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId) " +
        "AND (:orderStatus IS NULL OR o.status = :orderStatus) " +
        "AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom) " +
        "AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)",
      params
    );
    long callsCount = queryLong(
      "SELECT COUNT(*) FROM waiter_calls wc " +
        "JOIN tables t ON t.id = wc.table_id " +
        "LEFT JOIN staff_users su ON su.id = t.assigned_waiter_id " +
        "WHERE " + tableFilter + " AND wc.created_at BETWEEN :fromTs AND :toTs " +
        "AND (:guestPhone IS NULL OR 1=0) " +
        "AND (:waiterId IS NULL OR t.assigned_waiter_id = :waiterId) " +
        "AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom) " +
        "AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)",
      params
    );
    long paidBillsCount = queryLong(
      "SELECT COUNT(DISTINCT br.id) FROM bill_requests br " +
        "JOIN tables t ON t.id = br.table_id " +
        "JOIN bill_request_items bri ON bri.bill_request_id = br.id " +
        "JOIN order_items oi ON oi.id = bri.order_item_id " +
        "JOIN orders o ON o.id = oi.order_id " +
        "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id " +
        "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs " +
        "AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone) " +
        "AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId) " +
        "AND (:orderStatus IS NULL OR o.status = :orderStatus) " +
        "AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom) " +
        "AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)",
      params
    );
    long grossCents = queryLong(
      "SELECT COALESCE(SUM(bri.line_total_cents + " +
        "COALESCE((COALESCE(br.tips_amount_cents,0) * bri.line_total_cents) / NULLIF(br.subtotal_cents,0),0)),0) " +
        "FROM bill_requests br " +
        "JOIN tables t ON t.id = br.table_id " +
        "JOIN bill_request_items bri ON bri.bill_request_id = br.id " +
        "JOIN order_items oi ON oi.id = bri.order_item_id " +
        "JOIN orders o ON o.id = oi.order_id " +
        "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id " +
        "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs " +
        "AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone) " +
        "AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId) " +
        "AND (:orderStatus IS NULL OR o.status = :orderStatus) " +
        "AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom) " +
        "AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)",
      params
    );
    long tipsCents = queryLong(
      "SELECT COALESCE(SUM(COALESCE((COALESCE(br.tips_amount_cents,0) * bri.line_total_cents) / NULLIF(br.subtotal_cents,0),0)),0) " +
        "FROM bill_requests br " +
        "JOIN tables t ON t.id = br.table_id " +
        "JOIN bill_request_items bri ON bri.bill_request_id = br.id " +
        "JOIN order_items oi ON oi.id = bri.order_item_id " +
        "JOIN orders o ON o.id = oi.order_id " +
        "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id " +
        "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs " +
        "AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone) " +
        "AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId) " +
        "AND (:orderStatus IS NULL OR o.status = :orderStatus) " +
        "AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom) " +
        "AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)",
      params
    );
    long activeTablesCount = queryLong(
      "SELECT COUNT(DISTINCT o.table_id) FROM orders o " +
        "JOIN tables t ON t.id = o.table_id " +
        "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id " +
        "WHERE " + tableFilter + " AND o.created_at BETWEEN :fromTs AND :toTs " +
        "AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone) " +
        "AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId) " +
        "AND (:orderStatus IS NULL OR o.status = :orderStatus) " +
        "AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom) " +
        "AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)",
      params
    );
    Double avgSlaMinutes = queryDouble(
      "SELECT AVG(EXTRACT(EPOCH FROM (o.ready_at - o.created_at))/60.0) " +
        "FROM orders o " +
        "JOIN tables t ON t.id = o.table_id " +
        "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id " +
        "WHERE " + tableFilter + " AND o.created_at BETWEEN :fromTs AND :toTs " +
        "AND o.ready_at IS NOT NULL " +
        "AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone) " +
        "AND (:waiterId IS NULL OR o.handled_by_staff_id = :waiterId) " +
        "AND (:orderStatus IS NULL OR o.status = :orderStatus) " +
        "AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom) " +
        "AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)",
      params
    );

    Instant from = ((Timestamp) params.get("fromTs")).toInstant();
    Instant to = ((Timestamp) params.get("toTs")).toInstant();
    return new Summary(from, to, ordersCount, callsCount, paidBillsCount, grossCents, tipsCents, activeTablesCount, avgSlaMinutes);
  }

  private long queryLong(String sql, Map<String, Object> params) {
    Long v = jdbc.queryForObject(sql, params, Long.class);
    return v == null ? 0L : v;
  }

  private Double queryDouble(String sql, Map<String, Object> params) {
    return jdbc.queryForObject(sql, params, Double.class);
  }

  private String buildTenantTableFilter(Map<String, Object> params, Long branchId, Long hallId) {
    StringBuilder sb = new StringBuilder("t.branch_id IN (SELECT id FROM branches WHERE tenant_id = :tenantId)");
    if (branchId != null) {
      sb.append(" AND t.branch_id = :branchId");
      params.put("branchId", branchId);
    }
    if (hallId != null) {
      sb.append(" AND t.hall_id = :hallId");
      params.put("hallId", hallId);
    }
    return sb.toString();
  }

  public Summary summaryForTenantFiltered(
    long tenantId,
    Instant from,
    Instant to,
    Long branchId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("tenantId", tenantId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    String tableFilter = buildTenantTableFilter(params, branchId, hallId);
    return runSummary(tableFilter, params);
  }

  public java.util.List<TopItemRow> topItemsForTenant(
    long tenantId,
    Instant from,
    Instant to,
    Long branchId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone,
    int limit
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("tenantId", tenantId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    params.put("limit", Math.max(1, Math.min(limit, 200)));
    String tableFilter = buildTenantTableFilter(params, branchId, hallId);
    String sql =
      "SELECT oi.menu_item_id AS menu_item_id,\n" +
      "       COALESCE(oi.name_snapshot, mi.name_ru, 'Unknown') AS name,\n" +
      "       COALESCE(SUM(oi.qty),0) AS qty,\n" +
      "       COALESCE(SUM(bri.line_total_cents),0) AS gross_cents\n" +
      "FROM bill_request_items bri\n" +
      "JOIN bill_requests br ON br.id = bri.bill_request_id\n" +
      "JOIN order_items oi ON oi.id = bri.order_item_id\n" +
      "JOIN orders o ON o.id = oi.order_id\n" +
      "LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "JOIN tables t ON t.id = br.table_id\n" +
      "LEFT JOIN menu_items mi ON mi.id = oi.menu_item_id\n" +
      "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "  AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "  AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "  AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "  AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "GROUP BY oi.menu_item_id, name\n" +
      "ORDER BY gross_cents DESC\n" +
      "LIMIT :limit";
    return jdbc.query(sql, params, (rs, rowNum) -> new TopItemRow(
      rs.getLong("menu_item_id"),
      rs.getString("name"),
      rs.getLong("qty"),
      rs.getLong("gross_cents")
    ));
  }

  public java.util.List<WaiterMotivationRow> waiterMotivationForTenant(
    long tenantId,
    Instant from,
    Instant to,
    Long branchId,
    Long hallId,
    String orderStatus,
    Instant shiftFrom,
    Instant shiftTo,
    String guestPhone
  ) {
    Map<String, Object> params = baseParams(from, to);
    params.put("tenantId", tenantId);
    params.put("orderStatus", trimOrNull(orderStatus));
    putShiftParams(params, shiftFrom, shiftTo);
    putGuestPhone(params, guestPhone);
    String tableFilter = buildTenantTableFilter(params, branchId, hallId);
    String sql =
      "WITH tips AS (\n" +
      "  SELECT o.handled_by_staff_id AS staff_id, COALESCE(SUM(br.tips_amount_cents),0) AS tips_cents\n" +
      "  FROM bill_requests br\n" +
      "  JOIN bill_request_items bri ON bri.bill_request_id = br.id\n" +
      "  JOIN order_items oi ON oi.id = bri.order_item_id\n" +
      "  JOIN orders o ON o.id = oi.order_id\n" +
      "  LEFT JOIN staff_users su ON su.id = o.handled_by_staff_id\n" +
      "  JOIN tables t ON t.id = br.table_id\n" +
      "  WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "    AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)\n" +
      "  GROUP BY o.handled_by_staff_id\n" +
      ")\n" +
      "SELECT su.id AS staff_id, su.username AS username,\n" +
      "  COALESCE(COUNT(o.id) FILTER (WHERE (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "    AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "    AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "    AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo)),0) AS orders_count,\n" +
      "  COALESCE(tips.tips_cents,0) AS tips_cents,\n" +
      "  AVG(EXTRACT(EPOCH FROM (o.ready_at - o.created_at))/60.0)\n" +
      "    FILTER (WHERE o.ready_at IS NOT NULL AND (:guestPhone IS NULL OR o.guest_phone = :guestPhone)\n" +
      "      AND (:orderStatus IS NULL OR o.status = :orderStatus)\n" +
      "      AND (:shiftFrom IS NULL OR su.shift_started_at >= :shiftFrom)\n" +
      "      AND (:shiftTo IS NULL OR su.shift_started_at <= :shiftTo))\n" +
      "    AS avg_sla_minutes\n" +
      "FROM staff_users su\n" +
      "LEFT JOIN orders o ON o.handled_by_staff_id = su.id AND o.created_at BETWEEN :fromTs AND :toTs\n" +
      "LEFT JOIN tables t ON t.id = o.table_id\n" +
      "LEFT JOIN tips ON tips.staff_id = su.id\n" +
      "WHERE su.branch_id IN (SELECT id FROM branches WHERE tenant_id = :tenantId)\n" +
      "  AND su.role = 'WAITER'\n" +
      "  AND (:branchId IS NULL OR su.branch_id = :branchId)\n" +
      "GROUP BY su.id, su.username, tips.tips_cents\n" +
      "ORDER BY orders_count DESC, tips_cents DESC";
    return jdbc.query(sql, params, (rs, rowNum) -> new WaiterMotivationRow(
      rs.getLong("staff_id"),
      rs.getString("username"),
      rs.getLong("orders_count"),
      rs.getLong("tips_cents"),
      (Double) rs.getObject("avg_sla_minutes")
    ));
  }

  private static String trimOrNull(String v) {
    if (v == null) return null;
    String t = v.trim();
    return t.isEmpty() ? null : t;
  }

  private void putShiftParams(Map<String, Object> params, Instant shiftFrom, Instant shiftTo) {
    params.put("shiftFrom", shiftFrom == null ? null : Timestamp.from(shiftFrom));
    params.put("shiftTo", shiftTo == null ? null : Timestamp.from(shiftTo));
  }

  private void putGuestPhone(Map<String, Object> params, String guestPhone) {
    params.put("guestPhone", trimOrNull(guestPhone));
  }
}
