package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "menu_item_ingredients")
public class MenuItemIngredient {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "inventory_item_id", nullable = false)
  public Long inventoryItemId;

  @Column(name = "qty_per_item", nullable = false)
  public Double qtyPerItem;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  public Instant updatedAt;
}
