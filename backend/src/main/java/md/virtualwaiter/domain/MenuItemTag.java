package md.virtualwaiter.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "menu_item_tags")
@IdClass(MenuItemTagId.class)
public class MenuItemTag {
  @Id
  public Long menuItemId;

  @Id
  public Long tagId;
}
