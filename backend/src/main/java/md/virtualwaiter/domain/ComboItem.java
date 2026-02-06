package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "combo_items")
public class ComboItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "combo_id", nullable = false)
  public Long comboId;

  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "min_qty", nullable = false)
  public int minQty = 0;

  @Column(name = "max_qty", nullable = false)
  public int maxQty = 1;

  @Column(name = "sort_order", nullable = false)
  public int sortOrder = 0;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
