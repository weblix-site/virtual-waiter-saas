package md.virtualwaiter.domain;

import java.io.Serializable;
import java.util.Objects;

public class MenuItemTagId implements Serializable {
  public Long menuItemId;
  public Long tagId;

  public MenuItemTagId() {}

  public MenuItemTagId(Long menuItemId, Long tagId) {
    this.menuItemId = menuItemId;
    this.tagId = tagId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MenuItemTagId that = (MenuItemTagId) o;
    return Objects.equals(menuItemId, that.menuItemId) && Objects.equals(tagId, that.tagId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(menuItemId, tagId);
  }
}
