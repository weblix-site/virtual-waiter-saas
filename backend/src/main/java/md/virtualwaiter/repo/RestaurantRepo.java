package md.virtualwaiter.repo;

import md.virtualwaiter.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepo extends JpaRepository<Restaurant, Long> {
  List<Restaurant> findByTenantId(Long tenantId);
  Optional<Restaurant> findTop1ByTenantIdOrderByIdAsc(Long tenantId);
}
