package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_events")
public class NotificationEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "event_type", nullable = false)
  public String eventType; // ORDER_NEW | WAITER_CALL | BILL_REQUEST

  @Column(name = "ref_id", nullable = false)
  public Long refId;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
