package md.virtualwaiter.domain;

import jakarta.persistence.*;
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
}
