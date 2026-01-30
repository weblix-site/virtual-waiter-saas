package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "order_id", nullable = false)
  public Long orderId;

  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "name_snapshot", nullable = false)
  public String nameSnapshot;

  @Column(name = "unit_price_cents", nullable = false)
  public int unitPriceCents;

  @Column(name = "base_price_cents")
  public Integer basePriceCents;

  @Column(name = "modifiers_price_cents")
  public Integer modifiersPriceCents;

  @Column(nullable = false)
  public int qty;

  @Column
  public String comment;

  /** JSON string with selected modifiers (MVP: stored as-is) */
  @Column(name = "modifiers_json")
  public String modifiersJson;

  @Column(name = "bill_request_id")
  public Long billRequestId;

  @Column(name = "is_closed", nullable = false)
  public boolean isClosed = false;

  @Column(name = "closed_at")
  public java.time.Instant closedAt;

}
