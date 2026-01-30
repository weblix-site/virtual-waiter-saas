package md.virtualwaiter.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "table_id", nullable = false)
  public Long tableId;

  @Column(name = "guest_session_id", nullable = false)
  public Long guestSessionId;

  @Column(name = "handled_by_staff_id")
  public Long handledByStaffId;

  /** NEW | ACCEPTED | IN_PROGRESS | READY | SERVED | CLOSED | CANCELLED */
  @Column(nullable = false)
  public String status = "NEW";

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "created_by_ip")
  public String createdByIp;

  @Column(name = "created_by_ua")
  public String createdByUa;
}
