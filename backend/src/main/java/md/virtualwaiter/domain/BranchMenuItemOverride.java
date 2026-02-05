package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "branch_menu_item_overrides")
@IdClass(BranchMenuItemOverrideId.class)
public class BranchMenuItemOverride {
  @Id
  @Column(name = "branch_id", nullable = false)
  public Long branchId;

  @Id
  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "is_active", nullable = false)
  public boolean isActive = true;

  @Column(name = "is_stop_list", nullable = false)
  public boolean isStopList = false;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt = Instant.now();
}
