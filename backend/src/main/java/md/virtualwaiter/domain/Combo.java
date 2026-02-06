package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "combos")
public class Combo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "tenant_id", nullable = false)
  public Long tenantId;

  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;
}
