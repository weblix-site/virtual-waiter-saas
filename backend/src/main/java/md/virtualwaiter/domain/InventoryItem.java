package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "name_ru", nullable = false)
  public String nameRu;

  @Column(name = "name_ro")
  public String nameRo;

  @Column(name = "name_en")
  public String nameEn;

  @Column(name = "unit", nullable = false)
  public String unit = "pcs";

  @Column(name = "qty_on_hand", nullable = false)
  public Double qtyOnHand = 0.0;

  @Column(name = "min_qty", nullable = false)
  public Double minQty = 0.0;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  public Instant updatedAt;
}
