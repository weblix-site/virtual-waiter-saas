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
    long activeTablesCount
  ) {}

  public Summary summaryForBranch(long branchId, Instant from, Instant to) {
    String tableFilter = "t.branch_id = :branchId";
    Map<String, Object> params = baseParams(from, to);
    params.put("branchId", branchId);
    return runSummary(tableFilter, params);
  }

  public Summary summaryForTenant(long tenantId, Instant from, Instant to) {
    String tableFilter = "t.branch_id IN (SELECT id FROM branches WHERE tenant_id = :tenantId)";
    Map<String, Object> params = baseParams(from, to);
    params.put("tenantId", tenantId);
    return runSummary(tableFilter, params);
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
    String tableFilter = "t.branch_id = :branchId";
    Map<String, Object> params = baseParams(from, to);
    params.put("branchId", branchId);
    return runDaily(tableFilter, params);
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

  public java.util.List<BranchSummaryRow> summaryByBranchForTenant(long tenantId, Instant from, Instant to) {
    Map<String, Object> params = baseParams(from, to);
    params.put("tenantId", tenantId);
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
      "  FROM orders o JOIN tables t ON t.id = o.table_id\n" +
      "  WHERE o.created_at BETWEEN :fromTs AND :toTs\n" +
      "  GROUP BY t.branch_id\n" +
      ") o ON o.branch_id = b.id\n" +
      "LEFT JOIN (\n" +
      "  SELECT t.branch_id, COUNT(*) AS cnt\n" +
      "  FROM waiter_calls wc JOIN tables t ON t.id = wc.table_id\n" +
      "  WHERE wc.created_at BETWEEN :fromTs AND :toTs\n" +
      "  GROUP BY t.branch_id\n" +
      ") c ON c.branch_id = b.id\n" +
      "LEFT JOIN (\n" +
      "  SELECT t.branch_id, COUNT(*) AS cnt, COALESCE(SUM(br.total_cents),0) AS gross, COALESCE(SUM(br.tips_amount_cents),0) AS tips\n" +
      "  FROM bill_requests br JOIN tables t ON t.id = br.table_id\n" +
      "  WHERE br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
      "  GROUP BY t.branch_id\n" +
      ") br ON br.branch_id = b.id\n" +
      "WHERE b.tenant_id = :tenantId\n" +
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
      "  FROM orders o JOIN tables t ON t.id = o.table_id\n" +
      "  WHERE " + tableFilter + " AND o.created_at BETWEEN :fromTs AND :toTs\n" +
      "  GROUP BY 1\n" +
      "),\n" +
      "calls AS (\n" +
      "  SELECT date_trunc('day', wc.created_at) AS day, COUNT(*) AS cnt\n" +
      "  FROM waiter_calls wc JOIN tables t ON t.id = wc.table_id\n" +
      "  WHERE " + tableFilter + " AND wc.created_at BETWEEN :fromTs AND :toTs\n" +
      "  GROUP BY 1\n" +
      "),\n" +
      "bills AS (\n" +
      "  SELECT date_trunc('day', br.confirmed_at) AS day,\n" +
      "         COUNT(*) AS cnt,\n" +
      "         COALESCE(SUM(br.total_cents),0) AS gross,\n" +
      "         COALESCE(SUM(br.tips_amount_cents),0) AS tips\n" +
      "  FROM bill_requests br JOIN tables t ON t.id = br.table_id\n" +
      "  WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs\n" +
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

  private Summary runSummary(String tableFilter, Map<String, Object> params) {
    long ordersCount = queryLong(
      "SELECT COUNT(*) FROM orders o JOIN tables t ON t.id = o.table_id " +
        "WHERE " + tableFilter + " AND o.created_at BETWEEN :fromTs AND :toTs",
      params
    );
    long callsCount = queryLong(
      "SELECT COUNT(*) FROM waiter_calls wc JOIN tables t ON t.id = wc.table_id " +
        "WHERE " + tableFilter + " AND wc.created_at BETWEEN :fromTs AND :toTs",
      params
    );
    long paidBillsCount = queryLong(
      "SELECT COUNT(*) FROM bill_requests br JOIN tables t ON t.id = br.table_id " +
        "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs",
      params
    );
    long grossCents = queryLong(
      "SELECT COALESCE(SUM(br.total_cents),0) FROM bill_requests br JOIN tables t ON t.id = br.table_id " +
        "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs",
      params
    );
    long tipsCents = queryLong(
      "SELECT COALESCE(SUM(br.tips_amount_cents),0) FROM bill_requests br JOIN tables t ON t.id = br.table_id " +
        "WHERE " + tableFilter + " AND br.status = 'PAID_CONFIRMED' AND br.confirmed_at BETWEEN :fromTs AND :toTs",
      params
    );
    long activeTablesCount = queryLong(
      "SELECT COUNT(DISTINCT o.table_id) FROM orders o JOIN tables t ON t.id = o.table_id " +
        "WHERE " + tableFilter + " AND o.created_at BETWEEN :fromTs AND :toTs",
      params
    );

    Instant from = ((Timestamp) params.get("fromTs")).toInstant();
    Instant to = ((Timestamp) params.get("toTs")).toInstant();
    return new Summary(from, to, ordersCount, callsCount, paidBillsCount, grossCents, tipsCents, activeTablesCount);
  }

  private long queryLong(String sql, Map<String, Object> params) {
    Long v = jdbc.queryForObject(sql, params, Long.class);
    return v == null ? 0L : v;
  }
}
