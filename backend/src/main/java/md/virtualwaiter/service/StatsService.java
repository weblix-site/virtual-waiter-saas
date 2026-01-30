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
