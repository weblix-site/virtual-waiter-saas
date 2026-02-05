package md.virtualwaiter.domain;

import java.io.Serializable;
import java.util.Objects;

public class BranchMenuItemOverrideId implements Serializable {
  public Long branchId;
  public Long menuItemId;

  public BranchMenuItemOverrideId() {}

  public BranchMenuItemOverrideId(Long branchId, Long menuItemId) {
    this.branchId = branchId;
    this.menuItemId = menuItemId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BranchMenuItemOverrideId that = (BranchMenuItemOverrideId) o;
    return Objects.equals(branchId, that.branchId) && Objects.equals(menuItemId, that.menuItemId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(branchId, menuItemId);
  }
}
