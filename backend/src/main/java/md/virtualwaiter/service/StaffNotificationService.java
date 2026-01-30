package md.virtualwaiter.service;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class StaffNotificationService {
  private final NamedParameterJdbcTemplate jdbc;

  public StaffNotificationService(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Counts(long newOrders, long newCalls, long newBills) {}

  public Counts countsSince(long branchId, Instant sinceOrders, Instant sinceCalls, Instant sinceBills) {
    Map<String, Object> params = new HashMap<>();
    params.put("branchId", branchId);
    params.put("sinceOrdersTs", Timestamp.from(sinceOrders));
    params.put("sinceCallsTs", Timestamp.from(sinceCalls));
    params.put("sinceBillsTs", Timestamp.from(sinceBills));

    long orders = queryLong(
      "SELECT COUNT(*) FROM orders o JOIN tables t ON t.id = o.table_id " +
        "WHERE t.branch_id = :branchId AND o.created_at > :sinceOrdersTs",
      params
    );
    long calls = queryLong(
      "SELECT COUNT(*) FROM waiter_calls wc JOIN tables t ON t.id = wc.table_id " +
        "WHERE t.branch_id = :branchId AND wc.created_at > :sinceCallsTs",
      params
    );
    long bills = queryLong(
      "SELECT COUNT(*) FROM bill_requests br JOIN tables t ON t.id = br.table_id " +
        "WHERE t.branch_id = :branchId AND br.created_at > :sinceBillsTs AND br.status = 'CREATED'",
      params
    );
    return new Counts(orders, calls, bills);
  }

  private long queryLong(String sql, Map<String, Object> params) {
    Long v = jdbc.queryForObject(sql, params, Long.class);
    return v == null ? 0L : v;
  }
}
