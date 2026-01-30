package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepo extends JpaRepository<MenuItem, Long> {
  List<MenuItem> findByCategoryIdInAndIsActiveAndIsStopList(List<Long> categoryIds, boolean isActive, boolean isStopList);
  List<MenuItem> findByCategoryId(Long categoryId);
  List<MenuItem> findByCategoryIdIn(List<Long> categoryIds);
}
