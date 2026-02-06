package md.virtualwaiter.repo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import md.virtualwaiter.domain.MenuItemRecommendation;

public interface MenuItemRecommendationRepo extends JpaRepository<MenuItemRecommendation, Long> {
  List<MenuItemRecommendation> findBySourceItemIdOrderBySortOrderAscIdAsc(Long sourceItemId);
  List<MenuItemRecommendation> findBySourceItemIdIn(List<Long> sourceItemIds);
  void deleteBySourceItemId(Long sourceItemId);
}
