package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guest_favorite_items")
public class GuestFavoriteItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(nullable = false)
  public String phone;

  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "qty_total", nullable = false)
  public int qtyTotal;

  @Column(name = "last_order_at")
  public Instant lastOrderAt;
}
