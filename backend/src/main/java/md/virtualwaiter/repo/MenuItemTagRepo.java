package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuItemTag;
import md.virtualwaiter.domain.MenuItemTagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemTagRepo extends JpaRepository<MenuItemTag, MenuItemTagId> {
  List<MenuItemTag> findByMenuItemId(Long menuItemId);
  List<MenuItemTag> findByMenuItemIdIn(List<Long> menuItemIds);
  void deleteByMenuItemId(Long menuItemId);
}
