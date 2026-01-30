package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuCategoryRepo extends JpaRepository<MenuCategory, Long> {
  List<MenuCategory> findByBranchIdAndIsActiveOrderBySortOrderAscIdAsc(Long branchId, boolean isActive);
  List<MenuCategory> findByBranchIdOrderBySortOrderAscIdAsc(Long branchId);
}
