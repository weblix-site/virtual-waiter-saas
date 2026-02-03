package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "waiter_calls")
public class WaiterCall {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "table_id", nullable = false)
  public Long tableId;

  @Column(name = "guest_session_id")
  public Long guestSessionId;

  /** NEW | ACKNOWLEDGED | CLOSED */
  @Column(nullable = false)
  public String status = "NEW";

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "created_by_ip")
  public String createdByIp;

  @Column(name = "created_by_ua")
  public String createdByUa;
}
