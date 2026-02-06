package md.virtualwaiter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "menu_item_time_slots")
@IdClass(MenuItemTimeSlotId.class)
public class MenuItemTimeSlot {
  @Id
  @Column(name = "menu_item_id", nullable = false)
  public Long menuItemId;

  @Id
  @Column(name = "time_slot_id", nullable = false)
  public Long timeSlotId;
}
