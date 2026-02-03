package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "bill_request_items")
public class BillRequestItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "bill_request_id", nullable = false)
  public Long billRequestId;

  @Column(name = "order_item_id", nullable = false)
  public Long orderItemId;

  @Column(name = "line_total_cents", nullable = false)
  public int lineTotalCents;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();
}
