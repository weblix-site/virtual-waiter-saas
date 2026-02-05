package md.virtualwaiter.repo;

import md.virtualwaiter.domain.MenuTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuTemplateRepo extends JpaRepository<MenuTemplate, Long> {
  List<MenuTemplate> findByTenantIdAndRestaurantIdIsNullOrderByIdAsc(Long tenantId);
  List<MenuTemplate> findByTenantIdAndRestaurantIdOrderByIdAsc(Long tenantId, Long restaurantId);
}
