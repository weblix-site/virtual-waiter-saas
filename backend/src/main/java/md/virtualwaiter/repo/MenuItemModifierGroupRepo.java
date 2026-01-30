package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuItemModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemModifierGroupRepo extends JpaRepository<MenuItemModifierGroup, Long> {
  List<MenuItemModifierGroup> findByMenuItemIdOrderBySortOrderAscIdAsc(Long menuItemId);
  List<MenuItemModifierGroup> findByMenuItemIdIn(List<Long> menuItemIds);
  void deleteByMenuItemId(Long menuItemId);
}
