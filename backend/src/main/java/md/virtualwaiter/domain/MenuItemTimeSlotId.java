package md.virtualwaiter.domain;

import java.io.Serializable;
import java.util.Objects;

public class MenuItemTimeSlotId implements Serializable {
  public Long menuItemId;
  public Long timeSlotId;

  public MenuItemTimeSlotId() {}

  public MenuItemTimeSlotId(Long menuItemId, Long timeSlotId) {
    this.menuItemId = menuItemId;
    this.timeSlotId = timeSlotId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MenuItemTimeSlotId)) return false;
    MenuItemTimeSlotId that = (MenuItemTimeSlotId) o;
    return Objects.equals(menuItemId, that.menuItemId) && Objects.equals(timeSlotId, that.timeSlotId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(menuItemId, timeSlotId);
  }
}
