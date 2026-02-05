package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuCategoryRepo extends JpaRepository<MenuCategory, Long> {
  List<MenuCategory> findByTenantIdAndIsActiveOrderBySortOrderAscIdAsc(Long tenantId, boolean isActive);
  List<MenuCategory> findByTenantIdOrderBySortOrderAscIdAsc(Long tenantId);
}
