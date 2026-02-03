package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "menu_item_modifier_groups")
public class MenuItemModifierGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Column(name = "group_id", nullable = false)
  public Long groupId;

  @Column(name = "is_required", nullable = false)
  public boolean isRequired = false;

  @Column(name = "min_select")
  public Integer minSelect;

  @Column(name = "max_select")
  public Integer maxSelect;

  @Column(name = "sort_order", nullable = false)
  public int sortOrder = 0;
}
