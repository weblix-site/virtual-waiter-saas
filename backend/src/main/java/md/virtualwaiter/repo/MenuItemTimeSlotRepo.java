package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuItemTimeSlot;
import md.virtualwaiter.domain.MenuItemTimeSlotId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemTimeSlotRepo extends JpaRepository<MenuItemTimeSlot, MenuItemTimeSlotId> {
  List<MenuItemTimeSlot> findByMenuItemId(long menuItemId);
  List<MenuItemTimeSlot> findByMenuItemIdIn(List<Long> menuItemIds);
  void deleteByMenuItemId(long menuItemId);
}
