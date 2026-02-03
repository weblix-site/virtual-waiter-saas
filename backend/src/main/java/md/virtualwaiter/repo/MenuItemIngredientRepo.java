package md.virtualwaiter.repo;

import java.util.List;
import md.virtualwaiter.domain.MenuItemIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemIngredientRepo extends JpaRepository<MenuItemIngredient, Long> {
  List<MenuItemIngredient> findByMenuItemId(long menuItemId);
  void deleteByMenuItemId(long menuItemId);
}
